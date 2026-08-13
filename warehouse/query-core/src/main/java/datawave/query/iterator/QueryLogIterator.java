package datawave.query.iterator;

import static datawave.query.iterator.QueryOptions.QUERY_ID;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.OptionDescriber;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

/**
 * An iterator used to log the start and end of each method run by the {@link SortedKeyValueIterator} above it in the iterator stack. Logs the QueryID
 * associated with each method. Logs are written on the TServer where the iterator is running
 */
public class QueryLogIterator implements SortedKeyValueIterator<Key,Value>, OptionDescriber {

    private static final Logger log = Logger.getLogger(QueryLogIterator.class);
    private static final String CLASS_NAME = QueryLogIterator.class.getSimpleName();

    private String queryID;
    private SortedKeyValueIterator<Key,Value> source;
    private IteratorEnvironment env;

    /**
     * Default constructor
     */
    public QueryLogIterator() {
        // no-arg constructor
    }

    /**
     * Class copy constructor
     */
    public QueryLogIterator(QueryLogIterator other, IteratorEnvironment env) {
        this.source = other.source.deepCopy(env);
        this.env = other.env;
        this.queryID = other.queryID;
    }

    /**
     * Stores the query id and source for use by the wrapped methods. Nothing is invoked on the source here, so no start/end logging is done.
     */
    @Override
    public void init(SortedKeyValueIterator<Key,Value> source, Map<String,String> options, IteratorEnvironment env) throws IOException {
        this.queryID = options.get(QUERY_ID);
        this.source = source;
        this.env = env;
    }

    /**
     * Logs the query id and {@link SortedKeyValueIterator} method name before the method is run.
     */
    private void logStartOf(String methodName) {
        if (log.isDebugEnabled()) {
            log.debug(CLASS_NAME + " " + methodName + " Started QueryID: " + this.queryID);
        }
    }

    /**
     * Logs the query id and {@link SortedKeyValueIterator} method name after the method is run.
     */
    private void logEndOf(String methodName) {
        if (log.isDebugEnabled()) {
            log.debug(CLASS_NAME + " " + methodName + " Ended QueryID: " + this.queryID);
        }
    }

    /**
     * Wraps the hasTop() method of the iterator above it, tagging the thread name and MDC with the query id for any logging the source performs.
     */
    @Override
    public boolean hasTop() {
        String oldName = Thread.currentThread().getName();
        Thread.currentThread().setName(oldName + " -> " + this.queryID);
        MDC.put("queryID", queryID);
        try {
            return source.hasTop();
        } finally {
            Thread.currentThread().setName(oldName);
            MDC.remove("queryID");
        }

    }

    /**
     * Wraps the next() method of the iterator above it, logging the start and end of the method along with its query id.
     */
    @Override
    public void next() throws IOException {
        String oldName = Thread.currentThread().getName();
        Thread.currentThread().setName(oldName + " -> " + this.queryID);
        MDC.put("queryID", queryID);
        try {
            try {
                logStartOf("next()");
                source.next();
            } finally {
                logEndOf("next()");
            }
        } finally {
            Thread.currentThread().setName(oldName);
            MDC.remove("queryID");
        }

    }

    /**
     * Wraps the getTopKey() method of the iterator above it, tagging the thread name and MDC with the query id for any logging the source performs.
     */
    @Override
    public Key getTopKey() {
        String oldName = Thread.currentThread().getName();
        Thread.currentThread().setName(oldName + " -> " + this.queryID);
        MDC.put("queryID", queryID);
        try {
            Key k;
            try {
                k = source.getTopKey();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return k;
        } finally {
            Thread.currentThread().setName(oldName);
            MDC.remove("queryID");
        }

    }

    /**
     * Wraps the getTopValue() method of the iterator above it, tagging the thread name and MDC with the query id for any logging the source performs.
     */
    @Override
    public Value getTopValue() {
        String oldName = Thread.currentThread().getName();
        Thread.currentThread().setName(oldName + " -> " + this.queryID);
        MDC.put("queryID", queryID);
        try {
            Value v;
            try {
                v = source.getTopValue();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return v;
        } finally {
            Thread.currentThread().setName(oldName);
            MDC.remove("queryID");
        }

    }

    /**
     * Wraps the deepCopy() method of the iterator above it, tagging the thread name and MDC with the query id for any logging the source performs.
     */
    @Override
    public SortedKeyValueIterator<Key,Value> deepCopy(IteratorEnvironment iteratorEnvironment) {
        String oldName = Thread.currentThread().getName();
        Thread.currentThread().setName(oldName + " -> " + this.queryID);
        MDC.put("queryID", queryID);
        try {
            QueryLogIterator copy;

            try {
                copy = new QueryLogIterator(this, this.env);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return copy;
        } finally {
            Thread.currentThread().setName(oldName);
            MDC.remove("queryID");
        }

    }

    /**
     * Wraps the seek() method of the iterator above it, logging the start and end of the method along with its query id.
     */
    @Override
    public void seek(Range range, Collection<ByteSequence> collection, boolean b) throws IOException {

        String oldName = Thread.currentThread().getName();
        Thread.currentThread().setName(oldName + " -> " + this.queryID);
        MDC.put("queryID", queryID);
        try {
            try {
                logStartOf("seek()");
                this.source.seek(range, collection, b);
            } finally {
                logEndOf("seek()");
            }
        } finally {
            Thread.currentThread().setName(oldName);
            MDC.remove("queryID");
        }

    }

    /**
     * Returns a {@link org.apache.accumulo.core.iterators.OptionDescriber.IteratorOptions} object containing a description of the iterator and an option for
     * the QueryID.
     */
    @Override
    public IteratorOptions describeOptions() {
        Map<String,String> options = new HashMap<>();
        options.put(QUERY_ID, "The QueryID to be logged as methods are invoked");

        return new IteratorOptions(getClass().getSimpleName(), "An iterator used to log the QueryID", options, null);
    }

    /**
     * Returns true if the options provided contains the QueryID.
     */
    @Override
    public boolean validateOptions(Map<String,String> options) {
        return options.containsKey(QUERY_ID);
    }

}
