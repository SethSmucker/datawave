package datawave.query.jexl.visitors;

import static datawave.query.jexl.functions.EvaluationPhaseFilterFunctionsDescriptor.EXCLUDE_REGEX;
import static datawave.query.jexl.functions.EvaluationPhaseFilterFunctionsDescriptor.INCLUDE_REGEX;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.jexl3.parser.ASTAndNode;
import org.apache.commons.jexl3.parser.ASTFunctionNode;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTOrNode;
import org.apache.commons.jexl3.parser.JexlNode;
import org.apache.commons.jexl3.parser.JexlNodes;
import org.apache.log4j.Logger;

import datawave.core.common.logging.ThreadConfigurableLogger;
import datawave.query.config.ShardQueryConfiguration;
import datawave.query.exceptions.DatawaveFatalQueryException;
import datawave.query.jexl.JexlASTHelper;
import datawave.query.jexl.JexlNodeFactory;
import datawave.query.jexl.functions.FunctionJexlNodeVisitor;
import datawave.query.language.functions.jexl.Jexl;
import datawave.query.language.tree.FunctionNode;
import datawave.query.parser.JavaRegexAnalyzer;
import datawave.query.parser.JavaRegexAnalyzer.JavaRegexParseException;
import datawave.query.util.MetadataHelper;
import datawave.webservice.query.exception.DatawaveErrorCode;
import datawave.webservice.query.exception.QueryException;

/**
 * Rewrites evaluation phase filter functions for index only fields into their equivalent regex nodes. For a multi-fielded filter function like
 * <code>filter:includeRegex(FIELD_A||FIELD_B,'regex.*')</code> to be rewritten all fields must be index-only.
 * <ul>
 * <li><code>filter:includeRegex</code> becomes an <code>ASTERNode</code></li>
 * <li><code>filter:excludeRegex</code> becomes an <code>ASTNRNode</code></li>
 * </ul>
 */
public class RegexFunctionVisitor extends FunctionIndexQueryExpansionVisitor {

    private static final Logger log = ThreadConfigurableLogger.getLogger(RegexFunctionVisitor.class);

    protected Set<String> nonEventFields;

    public RegexFunctionVisitor(ShardQueryConfiguration config, MetadataHelper metadataHelper, Set<String> nonEventFields) {
        super(config, metadataHelper, null);
        this.nonEventFields = nonEventFields;
    }

    @SuppressWarnings("unchecked")
    public static <T extends JexlNode> T expandRegex(ShardQueryConfiguration config, MetadataHelper metadataHelper, Set<String> nonEventFields, T script) {
        RegexFunctionVisitor visitor = new RegexFunctionVisitor(config, metadataHelper, nonEventFields);
        JexlNode root = (T) script.jjtAccept(visitor, null);
        root = TreeFlatteningRebuildingVisitor.flatten(root);
        return (T) root;
    }

    @Override
    public Object visit(ASTFunctionNode node, Object data) {
        JexlNode returnNode = copy(node);
        FunctionJexlNodeVisitor functionMetadata = new FunctionJexlNodeVisitor();
        node.jjtAccept(functionMetadata, null);

        if (functionMetadata.name().equals(INCLUDE_REGEX) || functionMetadata.name().equals(EXCLUDE_REGEX)) {
            List<JexlNode> arguments = functionMetadata.args();

            List<ASTIdentifier> identifiers = JexlASTHelper.getIdentifiers(arguments.get(0));
            List<JexlNode> extractChildren = new ArrayList<>(identifiers.size());
            List<JexlNode> keepChildren = new ArrayList<>(identifiers.size());

            for (ASTIdentifier identifier : identifiers) {
                JexlNode regexNode = buildRegexNode(identifier, functionMetadata.name(), JexlNodes.getIdentifierOrLiteralAsString(arguments.get(1)));
                if (regexNode != null) {
                    extractChildren.add(regexNode);
                } else {
                    keepChildren.add(JexlNodeFactory.buildIdentifier(identifier.getName()));
                }

            }

            if (extractChildren.size() == 0) {
                // nothing to rewrite
                return returnNode;
            } else if (keepChildren.size() == 0) {
                // rewrite all nodes
                if (identifiers.size() == 1) {
                    // we've already rewritten our one node
                    returnNode = extractChildren.get(0);
                } else {
                    if (functionMetadata.name().equals(INCLUDE_REGEX) && arguments.get(0) instanceof ASTOrNode) {
                        returnNode = JexlNodeFactory.createOrNode(extractChildren);
                    } else {
                        // build an AND node because of how DeMorgan's law works with expanding negations
                        returnNode = JexlNodeFactory.createAndNode(extractChildren);
                    }
                }
            } else {
                // construct each and put it all together
                JexlNode extractNode;
                List<JexlNode> joint = new ArrayList<>();
                List<JexlNode> newArgs = new ArrayList<>();

                if (functionMetadata.name().equals(INCLUDE_REGEX) && arguments.get(0) instanceof ASTOrNode) {
                    newArgs.add(JexlNodeFactory.createOrNode(keepChildren));
                    extractNode = JexlNodeFactory.createOrNode(extractChildren);
                } else {
                    newArgs.add(JexlNodeFactory.createAndNode(keepChildren));
                    extractNode = JexlNodeFactory.createAndNode(extractChildren);
                }

                newArgs.add(arguments.get(1));
                JexlNode newFunc = FunctionJexlNodeVisitor.makeFunctionFrom(functionMetadata.namespace(), functionMetadata.name(),
                                newArgs.toArray(new JexlNode[0]));

                joint.add(extractNode);
                joint.add(newFunc);

                if (functionMetadata.name().equals(INCLUDE_REGEX) && arguments.get(0) instanceof ASTOrNode) {
                    returnNode = JexlNodeFactory.createOrNode(joint);
                } else {
                    returnNode = JexlNodeFactory.createAndNode(joint);
                }
            }
        }

        if (log.isTraceEnabled()) {
            log.trace("Rewrote \"" + JexlStringBuildingVisitor.buildQueryWithoutParse(node) + "\" into \""
                            + JexlStringBuildingVisitor.buildQueryWithoutParse(returnNode) + "\"");
        }
        return returnNode;
    }

    /**
     * Builds a regex node given a field name, regex and original function name.
     *
     * @param identifier
     *            the field
     * @param functionName
     *            the original function name, either <code>includeRegex</code> or <code>excludeRegex</code>
     * @param regex
     *            the regex
     * @return a new regex node, or null if no such regex node could be built
     */
    private JexlNode buildRegexNode(ASTIdentifier identifier, String functionName, String regex) {
        String field = JexlASTHelper.deconstructIdentifier(identifier.getName());
        if (nonEventFields.contains(field.toUpperCase())) {
            try {
                JavaRegexAnalyzer jra = new JavaRegexAnalyzer(regex);
                if (!jra.isNgram()) {
                    if (functionName.equals(INCLUDE_REGEX)) {
                        return JexlNodeFactory.buildERNode(field, regex);
                    } else {
                        return JexlNodeFactory.buildNRNode(field, regex);
                    }
                }
            } catch (JavaRegexParseException e) {
                QueryException qe = new QueryException(DatawaveErrorCode.INVALID_REGEX);
                throw new DatawaveFatalQueryException(qe);
            }
        }
        return null;
    }
}
