// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.diagnostic.Activity;
import com.intellij.diagnostic.StartUpMeasurer;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.impl.ExtensionPointImpl;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.newvfs.ManagingFS;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFSImpl;
import com.intellij.util.io.DataOutputStream;
import com.intellij.util.io.IOUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import static com.intellij.serviceContainer.ComponentManagerImplKt.handleComponentError;

class FileBasedIndexDataInitialization extends IndexInfrastructure.DataInitialization<IndexConfiguration> {
  private static final NotificationGroup NOTIFICATIONS = NotificationGroup.balloonGroup("Indexing", PluginManagerCore.CORE_ID);
  private static final Logger LOG = Logger.getInstance(FileBasedIndexDataInitialization.class);

  private final IndexConfiguration state = new IndexConfiguration();
  private final IndexVersionRegistrationSink registrationResultSink = new IndexVersionRegistrationSink();
  private boolean currentVersionCorrupted;
  @NotNull
  private final FileBasedIndexImpl myFileBasedIndex;
  @NotNull
  private final RegisteredIndexes myRegisteredIndexes;

  FileBasedIndexDataInitialization(@NotNull FileBasedIndexImpl index, @NotNull RegisteredIndexes registeredIndexes) {
    myFileBasedIndex = index;
    myRegisteredIndexes = registeredIndexes;
  }

  private void initAssociatedDataForExtensions() {
    Activity activity = StartUpMeasurer.startActivity("file index extensions iteration");
    Iterator<FileBasedIndexExtension<?, ?>> extensions =
      IndexInfrastructure.hasIndices() ?
      ((ExtensionPointImpl<FileBasedIndexExtension<?, ?>>)FileBasedIndexExtension.EXTENSION_POINT_NAME.getPoint()).iterator() :
      Collections.emptyIterator();

    // todo: init contentless indices first ?
    while (extensions.hasNext()) {
      FileBasedIndexExtension<?, ?> extension = extensions.next();
      if (extension == null) break;
      ID<?, ?> name = extension.getName();
      RebuildStatus.registerIndex(name);

      myRegisteredIndexes.registerIndexExtension(extension);

      addNestedInitializationTask(() -> {
        try {
          FileBasedIndexImpl.registerIndexer(extension, state, registrationResultSink);
        }
        catch (IOException io) {
          throw io;
        }
        catch (Throwable t) {
          handleComponentError(t, extension.getClass().getName(), null);
        }
      });
    }

    myRegisteredIndexes.extensionsDataWasLoaded();
    activity.end();
  }

  @Override
  protected void prepare() {
    // PersistentFS lifecycle should contain FileBasedIndex lifecycle, so,
    // 1) we call for it's instance before index creation to make sure it's initialized
    // 2) we dispose FileBasedIndex before PersistentFS disposing
    PersistentFSImpl fs = (PersistentFSImpl)ManagingFS.getInstance();
    FileBasedIndexImpl fileBasedIndex = (FileBasedIndexImpl)FileBasedIndex.getInstance();
    Disposable disposable = () -> new FileBasedIndexImpl.MyShutDownTask().run();
    ApplicationManager.getApplication().addApplicationListener(new MyApplicationListener(fileBasedIndex), disposable);
    Disposer.register(fs, disposable);
    myFileBasedIndex.setUpShutDownTask();

    initAssociatedDataForExtensions();

    PersistentIndicesConfiguration.loadConfiguration();

    currentVersionCorrupted = CorruptionMarker.invalidateIndexesIfNeeded();

    for (FileBasedIndexInfrastructureExtension ex : FileBasedIndexInfrastructureExtension.EP_NAME.getExtensions()) {
      FileBasedIndexInfrastructureExtension.InitializationResult result = ex.initialize();
      currentVersionCorrupted = currentVersionCorrupted &&
                                result == FileBasedIndexInfrastructureExtension.InitializationResult.INDEX_REBUILD_REQUIRED;
    }
  }

  @Override
  protected void onThrowable(@NotNull Throwable t) {
    FileBasedIndexImpl.LOG.error(t);
  }

  @Override
  protected IndexConfiguration finish() {
    try {
      state.finalizeFileTypeMappingForIndices();

      showChangedIndexesNotification();

      registrationResultSink.logChangedAndFullyBuiltIndices(
        FileBasedIndexImpl.LOG,
        "Indexes to be rebuilt after version change:",
        currentVersionCorrupted ? "Indexes to be rebuilt after corruption:" : "Indices to be built:"
      );

      state.freeze();
      myRegisteredIndexes.setState(state); // memory barrier
      // check if rebuild was requested for any index during registration
      for (ID<?, ?> indexId : state.getIndexIDs()) {
        try {
          RebuildStatus.clearIndexIfNecessary(indexId, () -> myFileBasedIndex.clearIndex(indexId));
        }
        catch (StorageException e) {
          myFileBasedIndex.requestRebuild(indexId);
          FileBasedIndexImpl.LOG.error(e);
        }
      }

      myFileBasedIndex.registerIndexableSet(new AdditionalIndexableFileSet(), null);
      return state;
    }
    finally {

      myFileBasedIndex.setUpFlusher();
      myRegisteredIndexes.ensureLoadedIndexesUpToDate();
      myRegisteredIndexes.markInitialized();  // this will ensure that all changes to component's state will be visible to other threads
      saveRegisteredIndicesAndDropUnregisteredOnes(state.getIndexIDs());
    }
  }

  private void showChangedIndexesNotification() {
    if (ApplicationManager.getApplication().isHeadlessEnvironment() || !Registry.is("ide.showIndexRebuildMessage")) return;

    String rebuildNotification = null;

    if (currentVersionCorrupted) {
      rebuildNotification = IndexingBundle.message("index.corrupted.notification.text");
    }
    else if (registrationResultSink.hasChangedIndexes()) {
      rebuildNotification = IndexingBundle.message("index.format.changed.notification.text", registrationResultSink.changedIndices());
    }

    if (rebuildNotification != null) {
      NOTIFICATIONS.createNotification(IndexingBundle.message("index.rebuild.notification.title"), rebuildNotification, NotificationType.INFORMATION, null).notify(null);
    }
  }

  private static void saveRegisteredIndicesAndDropUnregisteredOnes(@NotNull Collection<? extends ID<?, ?>> ids) {
    if (ApplicationManager.getApplication().isDisposed() || !IndexInfrastructure.hasIndices()) {
      return;
    }
    final File registeredIndicesFile = new File(PathManager.getIndexRoot(), "registered");
    final Set<String> indicesToDrop = new THashSet<>();
    boolean exceptionThrown = false;
    if (registeredIndicesFile.exists()) {
      try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(registeredIndicesFile)))) {
        final int size = in.readInt();
        for (int idx = 0; idx < size; idx++) {
          indicesToDrop.add(IOUtil.readString(in));
        }
      }
      catch (Throwable e) { // workaround for IDEA-194253
        LOG.info(e);
        exceptionThrown = true;
        ids.stream().map(ID::getName).forEach(indicesToDrop::add);
      }
    }
    if (!exceptionThrown) {
      for (ID<?, ?> key : ids) {
        indicesToDrop.remove(key.getName());
      }
    }
    if (!indicesToDrop.isEmpty()) {
      LOG.info("Dropping indices:" + StringUtil.join(indicesToDrop, ","));
      for (String s : indicesToDrop) {
        FileUtil.deleteWithRenaming(IndexInfrastructure.getFileBasedIndexRootDir(s));
      }
    }

    FileUtil.createIfDoesntExist(registeredIndicesFile);
    try (com.intellij.util.io.DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(registeredIndicesFile)))) {
      os.writeInt(ids.size());
      for (ID<?, ?> id : ids) {
        IOUtil.writeString(id.getName(), os);
      }
    }
    catch (IOException e) {
      LOG.info(e);
    }
  }

  private static class MyApplicationListener implements ApplicationListener {
    private final FileBasedIndexImpl myFileBasedIndex;

    MyApplicationListener(FileBasedIndexImpl fileBasedIndex) {myFileBasedIndex = fileBasedIndex;}

    @Override
    public void writeActionStarted(@NotNull Object action) {
      myFileBasedIndex.clearUpToDateIndexesForUnsavedOrTransactedDocs();
    }
  }
}
