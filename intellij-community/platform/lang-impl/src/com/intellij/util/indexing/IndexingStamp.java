// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.InvalidVirtualFileAccessException;
import com.intellij.openapi.vfs.newvfs.FileAttribute;
import com.intellij.openapi.vfs.newvfs.persistent.FSRecords;
import com.intellij.psi.stubs.StubIndexKey;
import com.intellij.util.SmartList;
import com.intellij.util.SystemProperties;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.IntObjectMap;
import com.intellij.util.io.DataInputOutputUtil;
import gnu.trove.TObjectLongHashMap;
import gnu.trove.TObjectLongProcedure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Eugene Zhuravlev
 *
 * A file has three indexed states (per particular index): indexed (with particular index_stamp), outdated and (trivial) unindexed
 * if index version is advanced or we rebuild it then index_stamp is advanced, we rebuild everything
 * if we get remove file event -> we should remove all indexed state from indices data for it (if state is nontrivial)
 * and set its indexed state to outdated
 * if we get other event we set indexed state to outdated
 *
 * It is assumed that index stamps are monotonically increasing.
 */
public final class IndexingStamp {
  private static final long INDEX_DATA_OUTDATED_STAMP = -2L;

  private static final int VERSION = 15;
  private static final ConcurrentMap<ID<?, ?>, IndexVersion> ourIndexIdToCreationStamp = new ConcurrentHashMap<>();
  private static final long ourVfsCreationStamp = FSRecords.getCreationTimestamp();

  static final int INVALID_FILE_ID = 0;

  private IndexingStamp() {}

  public static void initPersistentIndexStamp(DataInput in) throws IOException {
    IndexVersion.advanceIndexStamp(DataInputOutputUtil.readTIME(in));
  }

  public static void savePersistentIndexStamp(DataOutput out) throws IOException {
    DataInputOutputUtil.writeTIME(out, IndexVersion.ourLastStamp);
  }

  static class IndexVersion {
    private static volatile long ourLastStamp; // ensure any file index stamp increases
    final long myModificationCount;
    final int myIndexVersion;
    final int myCommonIndicesVersion;
    final long myVfsCreationStamp;

    private IndexVersion(long modificationCount, int indexVersion, long vfsCreationStamp) {
      myModificationCount = modificationCount;
      advanceIndexStamp(modificationCount);
      myIndexVersion = indexVersion;
      myCommonIndicesVersion = VERSION;
      myVfsCreationStamp = vfsCreationStamp;
    }

    private static void advanceIndexStamp(long modificationCount) {
      //noinspection NonAtomicOperationOnVolatileField
      ourLastStamp = Math.max(modificationCount, ourLastStamp);
    }

    IndexVersion(DataInput in) throws IOException {
      myIndexVersion = DataInputOutputUtil.readINT(in);
      myCommonIndicesVersion = DataInputOutputUtil.readINT(in);
      myVfsCreationStamp = DataInputOutputUtil.readTIME(in);
      myModificationCount = DataInputOutputUtil.readTIME(in);
      advanceIndexStamp(myModificationCount);
    }

    void write(DataOutput os) throws IOException {
      DataInputOutputUtil.writeINT(os, myIndexVersion);
      DataInputOutputUtil.writeINT(os, myCommonIndicesVersion);
      DataInputOutputUtil.writeTIME(os, myVfsCreationStamp);
      DataInputOutputUtil.writeTIME(os, myModificationCount);
    }

    IndexVersion nextVersion(int indexVersion, long vfsCreationStamp) {
      long modificationCount = calcNextVersion(this == NON_EXISTING_INDEX_VERSION ? ourLastStamp : myModificationCount);
      return new IndexVersion(modificationCount, indexVersion, vfsCreationStamp);
    }

    private static long calcNextVersion(long modificationCount) {
      return Math.max(
        System.currentTimeMillis(),
        Math.max(modificationCount + MIN_FS_MODIFIED_TIMESTAMP_RESOLUTION, ourLastStamp + OUR_INDICES_TIMESTAMP_INCREMENT)
      );
    }
  }

  public static synchronized void rewriteVersion(@NotNull ID<?,?> indexId, final int version) throws IOException {
    if (FileBasedIndex.USE_IN_MEMORY_INDEX) return;
    File file = IndexInfrastructure.getVersionFile(indexId);
    if (FileBasedIndexImpl.LOG.isDebugEnabled()) {
      FileBasedIndexImpl.LOG.debug("Rewriting " + file + "," + version);
    }
    IndexVersion newIndexVersion = getIndexVersion(indexId).nextVersion(version, FSRecords.getCreationTimestamp());

    if (file.exists()) {
      FileUtil.deleteWithRenaming(file);
    } else {
      //noinspection ResultOfMethodCallIgnored
      file.getParentFile().mkdirs();
    }
    try (final DataOutputStream os = FileUtilRt.doIOOperation(new FileUtilRt.RepeatableIOOperation<DataOutputStream, FileNotFoundException>() {
      @Override
      public @Nullable DataOutputStream execute(boolean lastAttempt) throws FileNotFoundException {
        try {
          return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        }
        catch (FileNotFoundException ex) {
          if (lastAttempt) throw ex;
          return null;
        }
      }
    })) {
      assert os != null;

      newIndexVersion.write(os);
      ourIndexIdToCreationStamp.put(indexId, newIndexVersion);
    }
  }

  private static final int MIN_FS_MODIFIED_TIMESTAMP_RESOLUTION = 2000; // https://en.wikipedia.org/wiki/File_Allocation_Table,
  // 1s for ext3 / hfs+ http://unix.stackexchange.com/questions/11599/determine-file-system-timestamp-accuracy
  // https://en.wikipedia.org/wiki/HFS_Plus

  private static final int OUR_INDICES_TIMESTAMP_INCREMENT = SystemProperties.getIntProperty("idea.indices.timestamp.resolution", 1);

  public static IndexVersionDiff versionDiffers(@NotNull ID<?,?> indexId, int currentIndexVersion) {
    IndexVersion version = getIndexVersion(indexId);
    if (version.myIndexVersion == -1) return new IndexVersionDiff.InitialBuild(currentIndexVersion);

    if (version.myIndexVersion != currentIndexVersion) {
      return new IndexVersionDiff.VersionChanged(version.myIndexVersion, currentIndexVersion, "index version");
    }

    if (version.myCommonIndicesVersion != VERSION) {
      return new IndexVersionDiff.VersionChanged(version.myCommonIndicesVersion, VERSION, "common index version");
    }

    if (version.myVfsCreationStamp != ourVfsCreationStamp) {
      return new IndexVersionDiff.VersionChanged(version.myVfsCreationStamp, ourVfsCreationStamp, "vfs creation stamp");
    }

    return IndexVersionDiff.UP_TO_DATE;
  }

  public interface IndexVersionDiff {
    @NotNull
    String getLogText();

    IndexVersionDiff UP_TO_DATE = new IndexVersionDiff() {
      @Override
      public @NotNull String getLogText() {
        return "";
      }
    };

    class InitialBuild implements IndexVersionDiff {
      private final int myVersion;

      public InitialBuild(int version) {myVersion = version;}

      @Override
      public @NotNull String getLogText() {
        return "(v = " + myVersion + ")";
      }
    }

    class CorruptedRebuild implements IndexVersionDiff {
      private final int myVersion;

      public CorruptedRebuild(int version) {myVersion = version;}

      @Override
      public @NotNull String getLogText() {
        return "(corrupted, v = " + myVersion + ")";
      }
    }

    class VersionChanged implements IndexVersionDiff {
      private final long myPreviousVersion;
      private final long myActualVersion;
      private final String myVersionType;

      public VersionChanged(long previousVersion, long actualVersion, String type) {
        myPreviousVersion = previousVersion;
        myActualVersion = actualVersion;
        myVersionType = type;
      }

      @Override
      public @NotNull String getLogText() {
        return "(" + myVersionType + " : " + myPreviousVersion + " -> " + myActualVersion + ")";
      }
    }
  }

  public static long getIndexCreationStamp(@NotNull ID<?, ?> indexName) {
    IndexVersion version = getIndexVersion(indexName);
    return version.myModificationCount;
  }

  private static final IndexVersion NON_EXISTING_INDEX_VERSION = new IndexVersion(0, -1, -1);

  private static @NotNull IndexVersion getIndexVersion(@NotNull ID<?, ?> indexName) {
    IndexVersion version = ourIndexIdToCreationStamp.get(indexName);
    if (version != null) return version;

    //noinspection SynchronizeOnThis
    synchronized (IndexingStamp.class) {
      version = ourIndexIdToCreationStamp.get(indexName);
      if (version != null) return version;

      File versionFile = IndexInfrastructure.getVersionFile(indexName);
      try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(versionFile)))) {

        version = new IndexVersion(in);
        ourIndexIdToCreationStamp.put(indexName, version);
        return version;
      }
      catch (IOException ignore) {
      }
      version = NON_EXISTING_INDEX_VERSION;
      ourIndexIdToCreationStamp.put(indexName, version);
    }
    return version;
  }

  public static FileIndexingState isFileIndexedStateCurrent(int fileId, ID<?, ?> indexName) {
    try {
      long stamp = getIndexStamp(fileId, indexName);
      if (stamp == 0) return FileIndexingState.NOT_INDEXED;
      return stamp == getIndexCreationStamp(indexName) ? FileIndexingState.UP_TO_DATE : FileIndexingState.OUT_DATED;
    }
    catch (RuntimeException e) {
      final Throwable cause = e.getCause();
      if (!(cause instanceof IOException)) {
        throw e; // in case of IO exceptions consider file unindexed
      }
    }

    return FileIndexingState.OUT_DATED;
  }

  public static void setFileIndexedStateCurrent(int fileId, ID<?, ?> id) {
    update(fileId, id, getIndexCreationStamp(id));
  }

  public static void setFileIndexedStateOutdated(int fileId, ID<?, ?> id) {
    update(fileId, id, INDEX_DATA_OUTDATED_STAMP);
  }

  /**
   * The class is meant to be accessed from synchronized block only
   */
  private static class Timestamps {
    private static final FileAttribute PERSISTENCE = new FileAttribute("__index_stamps__", 2, false);
    private TObjectLongHashMap<ID<?, ?>> myIndexStamps;
    private boolean myIsDirty = false;

    private Timestamps(@Nullable DataInputStream stream) throws IOException {
      if (stream != null) {
        int[] outdatedIndices = null;
        long dominatingIndexStamp = DataInputOutputUtil.readTIME(stream);
        long diff = dominatingIndexStamp - DataInputOutputUtil.timeBase;
        if (diff > 0 && diff < ID.MAX_NUMBER_OF_INDICES) {
          int numberOfOutdatedIndices = (int)diff;
          outdatedIndices = new int[numberOfOutdatedIndices];
          while(numberOfOutdatedIndices > 0) {
            outdatedIndices[--numberOfOutdatedIndices] = DataInputOutputUtil.readINT(stream);
          }
          dominatingIndexStamp = DataInputOutputUtil.readTIME(stream);
        }

        while(stream.available() > 0) {
          ID<?, ?> id = ID.findById(DataInputOutputUtil.readINT(stream));
          if (id != null && !(id instanceof StubIndexKey)) {
            long stamp = getIndexCreationStamp(id);
            if (stamp == 0) continue; // All (indices) IDs should be valid in this running session (e.g. we can have ID instance existing but index is not registered)
            if (myIndexStamps == null) myIndexStamps = new TObjectLongHashMap<>(5, 0.98f);
            if (stamp <= dominatingIndexStamp) myIndexStamps.put(id, stamp);
          }
        }

        if (outdatedIndices != null) {
          for(int outdatedIndexId:outdatedIndices) {
            ID<?, ?> id = ID.findById(outdatedIndexId);
            if (id != null && !(id instanceof StubIndexKey)) {
              if (getIndexCreationStamp(id) == 0) continue; // All (indices) IDs should be valid in this running session (e.g. we can have ID instance existing but index is not registered)
              long stamp = INDEX_DATA_OUTDATED_STAMP;
              if (myIndexStamps == null) myIndexStamps = new TObjectLongHashMap<>(5, 0.98f);
              if (stamp <= dominatingIndexStamp) myIndexStamps.put(id, stamp);
            }
          }
        }
      }
    }

    // Indexed stamp compact format:
    // (DataInputOutputUtil.timeBase + numberOfOutdatedIndices outdated_index_id+)? (dominating_index_stamp) index_id*
    // Note, that FSRecords.REASONABLY_SMALL attribute storage allocation policy will give an attribute 32 bytes to each file
    // Compact format allows 22 indexed states in this state
    private void writeToStream(final DataOutputStream stream) throws IOException {
      if (myIndexStamps != null && !myIndexStamps.isEmpty()) {
        final long[] data = new long[2];
        final int dominatingStampIndex = 0;
        final int numberOfOutdatedIndex = 1;
        myIndexStamps.forEachEntry(
          new TObjectLongProcedure<ID<?, ?>>() {
            @Override
            public boolean execute(ID<?, ?> a, long b) {
              if (b == INDEX_DATA_OUTDATED_STAMP) {
                ++data[numberOfOutdatedIndex];
                b = getIndexCreationStamp(a);
              }
              data[dominatingStampIndex] = Math.max(data[dominatingStampIndex], b);

              return true;
            }
          }
        );
        if (data[numberOfOutdatedIndex] > 0) {
          assert data[numberOfOutdatedIndex] < ID.MAX_NUMBER_OF_INDICES;
          DataInputOutputUtil.writeTIME(stream, DataInputOutputUtil.timeBase + data[numberOfOutdatedIndex]);
          myIndexStamps.forEachEntry(new TObjectLongProcedure<ID<?, ?>>() {
            @Override
            public boolean execute(final ID<?, ?> id, final long timestamp) {
              try {
                if (timestamp == INDEX_DATA_OUTDATED_STAMP) {
                  DataInputOutputUtil.writeINT(stream, id.getUniqueId());
                }
                return true;
              }
              catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
          });
        }
        DataInputOutputUtil.writeTIME(stream, data[dominatingStampIndex]);
        myIndexStamps.forEachEntry(new TObjectLongProcedure<ID<?, ?>>() {
          @Override
          public boolean execute(final ID<?, ?> id, final long timestamp) {
            try {
              if (timestamp == INDEX_DATA_OUTDATED_STAMP) return true;
              DataInputOutputUtil.writeINT(stream, id.getUniqueId());
              return true;
            }
            catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
        });
      }
      else {
        DataInputOutputUtil.writeTIME(stream, DataInputOutputUtil.timeBase);
      }
    }

    private long get(ID<?, ?> id) {
      return myIndexStamps != null? myIndexStamps.get(id) : 0L;
    }

    private void set(ID<?, ?> id, long tmst) {
      if (myIndexStamps == null) myIndexStamps = new TObjectLongHashMap<>(5, 0.98f);

      if (tmst == INDEX_DATA_OUTDATED_STAMP && !myIndexStamps.contains(id)) {
        return;
      }
      long previous = myIndexStamps.put(id, tmst);
      if (previous != tmst) myIsDirty = true;
    }

    public boolean isDirty() {
      return myIsDirty;
    }
  }

  private static final IntObjectMap<IndexingStamp.Timestamps> myTimestampsCache = ContainerUtil.createConcurrentIntObjectMap();
  private static final BlockingQueue<Integer> ourFinishedFiles = new ArrayBlockingQueue<>(100);

  public static long getIndexStamp(int fileId, ID<?, ?> indexName) {
    Lock readLock = getStripedLock(fileId).readLock();
    readLock.lock();
    try {
      Timestamps stamp = createOrGetTimeStamp(fileId);
      if (stamp != null) return stamp.get(indexName);
      return 0;
    } finally {
      readLock.unlock();
    }
  }

  @TestOnly
  public static void dropIndexingTimeStamps(int fileId) throws IOException {
    myTimestampsCache.remove(fileId);
    try (DataOutputStream out =  FSRecords.writeAttribute(fileId, Timestamps.PERSISTENCE)) {
      new Timestamps(null).writeToStream(out);
    }
  }

  private static Timestamps createOrGetTimeStamp(int id) {
    boolean isValid = id > 0;
    if (!isValid) {
      id = -id;
    }
    Timestamps timestamps = myTimestampsCache.get(id);
    if (timestamps == null) {
      try (final DataInputStream stream = FSRecords.readAttributeWithLock(id, Timestamps.PERSISTENCE)) {
        timestamps = new Timestamps(stream);
      }
      catch (IOException e) {
        FSRecords.handleError(e);
        throw new RuntimeException(e);
      }
      if (isValid) myTimestampsCache.put(id, timestamps);
    }
    return timestamps;
  }

  public static void update(int fileId, @NotNull ID<?, ?> indexName, final long indexCreationStamp) {
    if (fileId < 0 || fileId == INVALID_FILE_ID) return;
    Lock writeLock = getStripedLock(fileId).writeLock();
    writeLock.lock();
    try {
      Timestamps stamp = createOrGetTimeStamp(fileId);
      if (stamp != null) stamp.set(indexName, indexCreationStamp);
    } finally {
      writeLock.unlock();
    }
  }

  public static @NotNull List<ID<?,?>> getNontrivialFileIndexedStates(int fileId) {
    if (fileId != INVALID_FILE_ID) {
      Lock readLock = getStripedLock(fileId).readLock();
      readLock.lock();
      try {
        Timestamps stamp = createOrGetTimeStamp(fileId);
        if (stamp != null && stamp.myIndexStamps != null && !stamp.myIndexStamps.isEmpty()) {
          final SmartList<ID<?, ?>> retained = new SmartList<>();
          stamp.myIndexStamps.forEach(object -> {
            retained.add(object);
            return true;
          });
          return retained;
        }
      }
      catch (InvalidVirtualFileAccessException ignored /*ok to ignore it here*/) {
      }
      finally {
        readLock.unlock();
      }
    }
    return Collections.emptyList();
  }

  public static void flushCaches() {
    flushCache(null);
  }

  public static void flushCache(@Nullable Integer finishedFile) {
    if (finishedFile != null && finishedFile == INVALID_FILE_ID) finishedFile = 0;
    // todo make better (e.g. FinishedFiles striping, remove integers)
    while (finishedFile == null || !ourFinishedFiles.offer(finishedFile)) {
      List<Integer> files = new ArrayList<>(ourFinishedFiles.size());
      ourFinishedFiles.drainTo(files);

      if (!files.isEmpty()) {
        for (Integer file : files) {
          Lock writeLock = getStripedLock(file).writeLock();
          writeLock.lock();
          try {
            Timestamps timestamp = myTimestampsCache.remove(file);
            if (timestamp == null) continue;

            if (timestamp.isDirty() /*&& file.isValid()*/) {
              try (DataOutputStream sink = FSRecords.writeAttribute(file, Timestamps.PERSISTENCE)) {
                timestamp.writeToStream(sink);
              }
            }
          } catch (IOException e) {
            throw new RuntimeException(e);
          } finally {
            writeLock.unlock();
          }
        }
      }
      if (finishedFile == null) break;
      // else repeat until ourFinishedFiles.offer() succeeds
    }
  }

  private static final ReadWriteLock[] ourLocks = new ReadWriteLock[16];
  static {
    for(int i = 0; i < ourLocks.length; ++i) ourLocks[i] = new ReentrantReadWriteLock();
  }

  private static ReadWriteLock getStripedLock(int fileId) {
    if (fileId < 0) fileId = -fileId;
    return ourLocks[(fileId & 0xFF) % ourLocks.length];
  }
}