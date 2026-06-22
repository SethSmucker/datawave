package datawave.next.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

public class DocumentScanHintsTest {

    @Test
    public void searchHintsAlwaysIncludeScanType() {
        DocumentScannerConfig config = new DocumentScannerConfig();
        config.setSearchScanHintPool("search");

        Map<String,String> hints = DocumentScanHints.searchHints(config);

        // with no cache policy configured, only the scan_type hint is set
        assertEquals(1, hints.size());
        assertEquals("search", hints.get("scan_type"));
    }

    @Test
    public void searchHintsIncludeConfiguredCacheUsage() {
        DocumentScannerConfig config = new DocumentScannerConfig();
        config.setSearchScanHintPool("search");
        config.setSearchIndexCacheUsage("enabled");
        config.setSearchDataCacheUsage("enabled");

        Map<String,String> hints = DocumentScanHints.searchHints(config);

        assertEquals(3, hints.size());
        assertEquals("search", hints.get("scan_type"));
        // the cache usage hint key embeds the scan type, per the SimpleScanDispatcher documentation
        assertEquals("enabled", hints.get("cacheUsage.search.index"));
        assertEquals("enabled", hints.get("cacheUsage.search.data"));
    }

    @Test
    public void searchHintsOmitUnsetCachePolicies() {
        DocumentScannerConfig config = new DocumentScannerConfig();
        config.setSearchScanHintPool("search");
        config.setSearchIndexCacheUsage("opportunistic");
        // data cache usage intentionally left unset

        Map<String,String> hints = DocumentScanHints.searchHints(config);

        assertEquals(2, hints.size());
        assertEquals("opportunistic", hints.get("cacheUsage.search.index"));
        assertFalse(hints.containsKey("cacheUsage.search.data"));
    }

    @Test
    public void retrievalHintsIncludeConfiguredCacheUsage() {
        DocumentScannerConfig config = new DocumentScannerConfig();
        config.setRetrievalScanHintPool("retrieval");
        config.setRetrievalIndexCacheUsage("disabled");
        config.setRetrievalDataCacheUsage("disabled");

        Map<String,String> hints = DocumentScanHints.retrievalHints(config);

        assertEquals(3, hints.size());
        assertEquals("retrieval", hints.get("scan_type"));
        assertEquals("disabled", hints.get("cacheUsage.retrieval.index"));
        assertEquals("disabled", hints.get("cacheUsage.retrieval.data"));
    }

    @Test
    public void cacheUsageKeyTracksScanTypeName() {
        DocumentScannerConfig config = new DocumentScannerConfig();
        config.setRetrievalScanHintPool("pool-a");
        config.setRetrievalDataCacheUsage("disabled");

        Map<String,String> hints = DocumentScanHints.retrievalHints(config);

        assertEquals("disabled", hints.get("cacheUsage.pool-a.data"));
        // the default scan type name is not used when a custom pool is configured
        assertNull(hints.get("cacheUsage.retrieval.data"));
    }
}
