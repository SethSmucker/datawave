package datawave.ingest.mapreduce.job;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.accumulo.core.client.AccumuloClient;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.admin.ActiveCompaction;
import org.apache.accumulo.core.client.admin.ActiveCompaction.CompactionType;
import org.apache.accumulo.core.client.admin.InstanceOperations;
import org.apache.accumulo.core.client.admin.servers.ServerId;
import org.apache.accumulo.core.data.ResourceGroupId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the major compaction checks that decide whether {@link BulkIngestMapFileLoader} can bring more map files online.
 */
public class BulkIngestMapFileLoaderCompactionTest {

    private static final Set<ServerId> COMPACTORS = Set.of(new ServerId(ServerId.Type.COMPACTOR, ResourceGroupId.DEFAULT, "host1", 9133),
                    new ServerId(ServerId.Type.COMPACTOR, ResourceGroupId.DEFAULT, "host2", 9133));
    private static final long ONE_HOUR = 60 * 60 * 1000;
    // the loader's default -majcThreshold
    private static final int THRESHOLD = 3000;

    private AccumuloClient client;
    private InstanceOperations instanceOperations;

    @BeforeEach
    public void setup() {
        client = createMock(AccumuloClient.class);
        instanceOperations = createMock(InstanceOperations.class);
        expect(client.instanceOperations()).andReturn(instanceOperations).anyTimes();
    }

    @Test
    public void testCountsTheMajorCompactionsRunningOnTheCompactors() throws Exception {
        expectCompactions(compactions(CompactionType.MAJOR, CompactionType.MINOR, CompactionType.FULL, CompactionType.MINOR));
        replay(client, instanceOperations);

        assertEquals(2, new BulkIngestMapFileLoader(client, 0).getMajorCompactionCount());
        verify(client, instanceOperations);
    }

    @Test
    public void testReusesTheCountWithinTheCacheTime() throws Exception {
        expectCompactions(compactions(CompactionType.MAJOR));
        replay(client, instanceOperations);

        BulkIngestMapFileLoader loader = new BulkIngestMapFileLoader(client, ONE_HOUR);
        assertEquals(1, loader.getMajorCompactionCount());
        assertEquals(1, loader.getMajorCompactionCount());
        // the compactors were only asked once
        verify(client, instanceOperations);
    }

    @Test
    public void testCountsAgainOnceTheCacheTimePasses() throws Exception {
        expectCompactions(compactions(CompactionType.MAJOR));
        expectCompactions(compactions(CompactionType.MAJOR, CompactionType.FULL));
        replay(client, instanceOperations);

        BulkIngestMapFileLoader loader = new BulkIngestMapFileLoader(client, 0);
        assertEquals(1, loader.getMajorCompactionCount());
        assertEquals(2, loader.getMajorCompactionCount());
        verify(client, instanceOperations);
    }

    @Test
    public void testFailedCountIsZeroAndNotReused() throws Exception {
        expect(instanceOperations.getServers(ServerId.Type.COMPACTOR)).andReturn(COMPACTORS);
        expect(instanceOperations.getActiveCompactions(COMPACTORS)).andThrow(new AccumuloException("compactors unavailable"));
        expectCompactions(compactions(CompactionType.MAJOR));
        replay(client, instanceOperations);

        BulkIngestMapFileLoader loader = new BulkIngestMapFileLoader(client, ONE_HOUR);
        assertEquals(0, loader.getMajorCompactionCount());
        assertEquals(1, loader.getMajorCompactionCount());
        verify(client, instanceOperations);
    }

    @Test
    public void testCanBringMapFilesOnlineBelowTheThreshold() throws Exception {
        expectCompactions(Collections.nCopies(THRESHOLD - 1, compaction(CompactionType.MAJOR)));
        replay(client, instanceOperations);

        assertTrue(new BulkIngestMapFileLoader(client, 0).canBringMapFilesOnline(0, false));
        verify(client, instanceOperations);
    }

    @Test
    public void testCannotBringMapFilesOnlineAtTheThreshold() throws Exception {
        expectCompactions(Collections.nCopies(THRESHOLD, compaction(CompactionType.MAJOR)));
        replay(client, instanceOperations);

        assertFalse(new BulkIngestMapFileLoader(client, 0).canBringMapFilesOnline(0, false));
        verify(client, instanceOperations);
    }

    @Test
    public void testMinorCompactionsDoNotCountTowardsTheThreshold() throws Exception {
        expectCompactions(Collections.nCopies(THRESHOLD, compaction(CompactionType.MINOR)));
        replay(client, instanceOperations);

        assertTrue(new BulkIngestMapFileLoader(client, 0).canBringMapFilesOnline(0, false));
        verify(client, instanceOperations);
    }

    @Test
    public void testCannotBringMapFilesOnlineBeforeTheDelayPasses() throws Exception {
        expectCompactions(compactions());
        replay(client, instanceOperations);

        // the default -majcDelay is 0, so the last online time has to be in the future to still be within it
        assertFalse(new BulkIngestMapFileLoader(client, 0).canBringMapFilesOnline(System.currentTimeMillis() + ONE_HOUR, false));
        verify(client, instanceOperations);
    }

    private void expectCompactions(List<ActiveCompaction> compactions) throws Exception {
        expect(instanceOperations.getServers(ServerId.Type.COMPACTOR)).andReturn(COMPACTORS);
        expect(instanceOperations.getActiveCompactions(COMPACTORS)).andReturn(compactions);
    }

    private static List<ActiveCompaction> compactions(CompactionType... types) {
        List<ActiveCompaction> compactions = new ArrayList<>();
        for (CompactionType type : types) {
            compactions.add(compaction(type));
        }
        return compactions;
    }

    private static ActiveCompaction compaction(CompactionType type) {
        ActiveCompaction compaction = createMock(ActiveCompaction.class);
        expect(compaction.getType()).andReturn(type).anyTimes();
        replay(compaction);
        return compaction;
    }
}
