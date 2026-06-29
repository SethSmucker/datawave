package datawave.next.scanner;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds the execution hints for the search and retrieval scans issued by the document scheduler.
 * <p>
 * Each scan carries a {@code scan_type} hint that selects the server-side executor, plus an optional block cache policy.
 * <p>
 * The policy uses the {@code cacheUsage.<scanType>.index} and {@code cacheUsage.<scanType>.data} keys from the SimpleScanDispatcher documentation.
 */
final class DocumentScanHints {

    static final String SCAN_TYPE = "scan_type";

    private DocumentScanHints() {
        // utility class
    }

    /**
     * Build the execution hints for a search (field index / candidate) scan.
     */
    static Map<String,String> searchHints(DocumentScannerConfig config) {
        Map<String,String> hints = new HashMap<>();
        hints.put(SCAN_TYPE, config.getSearchScanHintPool());
        addCacheUsage(hints, config.getSearchScanHintPool(), config.getSearchIndexCacheUsage(), config.getSearchDataCacheUsage());
        return hints;
    }

    /**
     * Build the execution hints for a retrieval (document / event) scan.
     */
    static Map<String,String> retrievalHints(DocumentScannerConfig config) {
        Map<String,String> hints = new HashMap<>();
        hints.put(SCAN_TYPE, config.getRetrievalScanHintPool());
        addCacheUsage(hints, config.getRetrievalScanHintPool(), config.getRetrievalIndexCacheUsage(), config.getRetrievalDataCacheUsage());
        return hints;
    }

    /**
     * Add the cacheUsage hints for the given scan type when a policy is configured; a null policy is skipped so the table default applies.
     */
    private static void addCacheUsage(Map<String,String> hints, String scanType, String indexCacheUsage, String dataCacheUsage) {
        if (indexCacheUsage != null) {
            hints.put("cacheUsage." + scanType + ".index", indexCacheUsage);
        }
        if (dataCacheUsage != null) {
            hints.put("cacheUsage." + scanType + ".data", dataCacheUsage);
        }
    }
}
