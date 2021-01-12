// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.impl;

import com.intellij.find.FindBundle;
import com.intellij.find.FindInProjectSearchEngine;
import com.intellij.find.FindModel;
import com.intellij.find.findInProject.FindInProjectManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.impl.CoreProgressManager;
import com.intellij.openapi.progress.util.TooManyUsagesStatus;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.search.PsiSearchHelperImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopeUtil;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.FindUsagesProcessPresentation;
import com.intellij.usages.UsageLimitUtil;
import com.intellij.usages.impl.UsageViewManagerImpl;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author peter
 */
class FindInProjectTask {
  private static final Comparator<VirtualFile> SEARCH_RESULT_FILE_COMPARATOR =
    Comparator.comparing((VirtualFile f) -> f instanceof VirtualFileWithId ? ((VirtualFileWithId)f).getId() : 0)
      .thenComparing(VirtualFile::getName) // in case files without id are also searched
      .thenComparing(VirtualFile::getPath);
  private static final Logger LOG = Logger.getInstance(FindInProjectTask.class);
  private static final int FILES_SIZE_LIMIT = 70 * 1024 * 1024; // megabytes.
  private final FindModel myFindModel;
  private final Project myProject;
  private final PsiManager myPsiManager;
  @Nullable private final VirtualFile myDirectory;
  private final ProjectFileIndex myProjectFileIndex;
  private final FileIndex myFileIndex;
  private final Condition<VirtualFile> myFileMask;
  private final ProgressIndicator myProgress;
  @Nullable private final Module myModule;
  private final Set<VirtualFile> myLargeFiles = Collections.synchronizedSet(new THashSet<>());
  private final Set<? extends VirtualFile> myFilesToScanInitially;
  private final AtomicLong myTotalFilesSize = new AtomicLong();
  private final @NotNull List<FindInProjectSearchEngine.@NotNull FindInProjectSearcher> mySearchers;

  FindInProjectTask(@NotNull final FindModel findModel, @NotNull final Project project, @NotNull Set<? extends VirtualFile> filesToScanInitially) {
    myFindModel = findModel;
    myProject = project;
    myFilesToScanInitially = filesToScanInitially;
    myDirectory = FindInProjectUtil.getDirectory(findModel);
    myPsiManager = PsiManager.getInstance(project);

    final String moduleName = findModel.getModuleName();
    myModule = moduleName == null ? null : ReadAction.compute(() -> ModuleManager.getInstance(project).findModuleByName(moduleName));
    myProjectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    myFileIndex = myModule == null ? myProjectFileIndex : ModuleRootManager.getInstance(myModule).getFileIndex();

    Condition<CharSequence> patternCondition = FindInProjectUtil.createFileMaskCondition(findModel.getFileFilter());

    myFileMask = file -> file != null && patternCondition.value(file.getNameSequence());

    final ProgressIndicator progress = ProgressManager.getInstance().getProgressIndicator();
    myProgress = progress != null ? progress : new EmptyProgressIndicator();

    TooManyUsagesStatus.createFor(myProgress);

    mySearchers = ContainerUtil.mapNotNull(FindInProjectSearchEngine.EP_NAME.getExtensions(), se -> se.createSearcher(findModel, project));
  }

  void findUsages(@NotNull FindUsagesProcessPresentation processPresentation, @NotNull Processor<? super UsageInfo> consumer) {
    CoreProgressManager.assertUnderProgress(myProgress);

    try {
      myProgress.setIndeterminate(true);
      myProgress.setText(FindBundle.message("progress.text.scanning.indexed.files"));
      Set<VirtualFile> filesForFastWordSearch = ReadAction.nonBlocking(this::getFilesForFastWordSearch).executeSynchronously();
      myProgress.setIndeterminate(false);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Searching for " + myFindModel.getStringToFind() + " in " + filesForFastWordSearch.size() + " indexed files");
      }

      searchInFiles(filesForFastWordSearch, processPresentation, consumer);

      myProgress.setIndeterminate(true);
      myProgress.setText(FindBundle.message("progress.text.scanning.non.indexed.files"));
      boolean canRelyOnIndices = canRelyOnSearchers();
      final Collection<VirtualFile> otherFiles = collectFilesInScope(filesForFastWordSearch, canRelyOnIndices);
      myProgress.setIndeterminate(false);

      if (LOG.isDebugEnabled()) {
        LOG.debug("Searching for " + myFindModel.getStringToFind() + " in " + otherFiles.size() + " non-indexed files");
      }
      myProgress.checkCanceled();
      long start = System.currentTimeMillis();
      searchInFiles(otherFiles, processPresentation, consumer);
      if (canRelyOnIndices && otherFiles.size() > 1000) {
        long time = System.currentTimeMillis() - start;
        logStats(otherFiles, time);
      }
    }
    catch (ProcessCanceledException e) {
      processPresentation.setCanceled(true);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Usage search canceled", e);
      }
    }

    if (!myLargeFiles.isEmpty()) {
      processPresentation.setLargeFilesWereNotScanned(myLargeFiles);
    }

    if (!myProgress.isCanceled()) {
      myProgress.setText(FindBundle.message("find.progress.search.completed"));
    }
  }

  private static void logStats(@NotNull Collection<? extends VirtualFile> otherFiles, long time) {
    Map<String, Long> extensionToCount = otherFiles.stream()
      .collect(Collectors.groupingBy(file -> StringUtil.toLowerCase(StringUtil.notNullize(file.getExtension())), Collectors.counting()));
    String topExtensions = extensionToCount
      .entrySet().stream()
      .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
      .map(entry -> entry.getKey() + "(" + entry.getValue() + ")")
      .limit(10)
      .collect(Collectors.joining(", "));

    LOG.info("Search in " + otherFiles.size() + " files with unknown types took " + time + "ms.\n" +
             "Mapping their extensions to an existing file type (e.g. Plain Text) might speed up the search.\n" +
             "Most frequent non-indexed file extensions: " + topExtensions);
  }

  private void searchInFiles(@NotNull Collection<? extends VirtualFile> virtualFiles,
                             @NotNull FindUsagesProcessPresentation processPresentation,
                             @NotNull final Processor<? super UsageInfo> consumer) {
    AtomicInteger occurrenceCount = new AtomicInteger();
    AtomicInteger processedFileCount = new AtomicInteger();
    Map<VirtualFile, Set<UsageInfo>> usagesBeingProcessed = new ConcurrentHashMap<>();
    Processor<VirtualFile> processor = virtualFile -> {
      if (!virtualFile.isValid()) return true;

      long fileLength = UsageViewManagerImpl.getFileLength(virtualFile);
      if (fileLength == -1) return true; // Binary or invalid

      final boolean skipProjectFile = ProjectUtil.isProjectOrWorkspaceFile(virtualFile) && !myFindModel.isSearchInProjectFiles();
      if (skipProjectFile && !Registry.is("find.search.in.project.files")) return true;

      if (fileLength > FileUtilRt.LARGE_FOR_CONTENT_LOADING) {
        myLargeFiles.add(virtualFile);
        return true;
      }

      myProgress.checkCanceled();
      if (myProgress.isRunning()) {
        double fraction = (double)processedFileCount.incrementAndGet() / virtualFiles.size();
        myProgress.setFraction(fraction);
      }
      String text = FindBundle.message("find.searching.for.string.in.file.progress",
                                       myFindModel.getStringToFind(), virtualFile.getPresentableUrl());
      myProgress.setText(text);
      myProgress.setText2(FindBundle.message("find.searching.for.string.in.file.occurrences.progress", occurrenceCount));

      Pair.NonNull<PsiFile, VirtualFile> pair = ReadAction.compute(() -> findFile(virtualFile));
      if (pair == null) return true;

      Set<UsageInfo> processedUsages = usagesBeingProcessed.computeIfAbsent(virtualFile, __ -> ContainerUtil.newConcurrentSet());
      PsiFile psiFile = pair.first;
      VirtualFile sourceVirtualFile = pair.second;
      AtomicBoolean projectFileUsagesFound = new AtomicBoolean();
      if (!FindInProjectUtil.processUsagesInFile(psiFile, sourceVirtualFile, myFindModel, info -> {
        if (skipProjectFile) {
          projectFileUsagesFound.set(true);
          return true;
        }
        if (processedUsages.contains(info)) {
          return true;
        }
        boolean success = consumer.process(info);
        processedUsages.add(info);
        return success;
      })) return false;
      usagesBeingProcessed.remove(virtualFile); // after the whole virtualFile processed successfully, remove mapping to save memory

      if (projectFileUsagesFound.get()) {
        processPresentation.projectFileUsagesFound(() -> {
          FindModel model = myFindModel.clone();
          model.setSearchInProjectFiles(true);
          FindInProjectManager.getInstance(myProject).startFindInProject(model);
        });
        return true;
      }

      long totalSize;
      if (processedUsages.isEmpty()) {
        totalSize = myTotalFilesSize.get();
      }
      else {
        occurrenceCount.addAndGet(processedUsages.size());
        totalSize = myTotalFilesSize.addAndGet(fileLength);
      }

      if (totalSize > FILES_SIZE_LIMIT) {
        TooManyUsagesStatus tooManyUsagesStatus = TooManyUsagesStatus.getFrom(myProgress);
        if (tooManyUsagesStatus.switchTooManyUsagesStatus()) {
          UIUtil.invokeLaterIfNeeded(() -> {
            String message = FindBundle.message("find.excessive.total.size.prompt",
                                                UsageViewManagerImpl.presentableSize(myTotalFilesSize.longValue()),
                                                ApplicationNamesInfo.getInstance().getProductName());
            UsageLimitUtil.Result ret = UsageLimitUtil.showTooManyUsagesWarning(myProject, message);
            if (ret == UsageLimitUtil.Result.ABORT) {
              myProgress.cancel();
            }
            tooManyUsagesStatus.userResponded();
          });
        }
        tooManyUsagesStatus.pauseProcessingIfTooManyUsages();
        myProgress.checkCanceled();
      }
      return true;
    };
    List<VirtualFile> sorted = ContainerUtil.sorted(virtualFiles, SEARCH_RESULT_FILE_COMPARATOR);
    PsiSearchHelperImpl.processFilesConcurrentlyDespiteWriteActions(myProject, sorted, myProgress, new AtomicBoolean(), processor);
  }

  // must return non-binary files
  @NotNull
  private Collection<VirtualFile> collectFilesInScope(@NotNull final Set<VirtualFile> alreadySearched, final boolean skipIndexed) {
    SearchScope customScope = myFindModel.isCustomScope() ? myFindModel.getCustomScope() : null;
    final GlobalSearchScope globalCustomScope = customScope == null ? null : GlobalSearchScopeUtil.toGlobalSearchScope(customScope, myProject);


    class EnumContentIterator implements ContentIterator {
      private final Set<VirtualFile> myFiles = new CompactVirtualFileSet();

      @Override
      public boolean processFile(@NotNull final VirtualFile virtualFile) {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
          @Override
          public void run() {
            ProgressManager.checkCanceled();
            if (virtualFile.isDirectory() || !virtualFile.isValid() ||
                !myFileMask.value(virtualFile) ||
                globalCustomScope != null && !globalCustomScope.contains(virtualFile)) {
              return;
            }

            if (skipIndexed && ContainerUtil.find(mySearchers, p -> p.isCovered(virtualFile)) != null) {
              return;
            }

            Pair.NonNull<PsiFile, VirtualFile> pair = findFile(virtualFile);
            if (pair == null) return;
            VirtualFile sourceVirtualFile = pair.second;

            if (sourceVirtualFile != null && !alreadySearched.contains(sourceVirtualFile)) {
              myFiles.add(sourceVirtualFile);
            }
          }
        });
        return true;
      }

      @NotNull
      private Collection<VirtualFile> getFiles() {
        return myFiles;
      }
    }

    final EnumContentIterator iterator = new EnumContentIterator();

    if (customScope instanceof LocalSearchScope) {
      for (VirtualFile file : GlobalSearchScopeUtil.getLocalScopeFiles((LocalSearchScope)customScope)) {
        iterator.processFile(file);
      }
    }
    else if (customScope instanceof Iterable) {  // GlobalSearchScope can span files out of project roots e.g. FileScope / FilesScope
      //noinspection unchecked
      for (VirtualFile file : (Iterable<VirtualFile>)customScope) {
        iterator.processFile(file);
      }
    }
    else if (myDirectory != null) {
      boolean checkExcluded = !ProjectFileIndex.SERVICE.getInstance(myProject).isExcluded(myDirectory) && !Registry.is("find.search.in.excluded.dirs");
      VirtualFileVisitor.Option limit = VirtualFileVisitor.limit(myFindModel.isWithSubdirectories() ? -1 : 1);
      VfsUtilCore.visitChildrenRecursively(myDirectory, new VirtualFileVisitor<Void>(limit) {
        @Override
        public boolean visitFile(@NotNull VirtualFile file) {
          if (checkExcluded && myProjectFileIndex.isExcluded(file)) return false;
          iterator.processFile(file);
          return true;
        }
      });
    }
    else {
      boolean success = myFileIndex.iterateContent(iterator);
      if (success && globalCustomScope != null && globalCustomScope.isSearchInLibraries()) {
        final VirtualFile[] librarySources = ReadAction.compute(() -> {
          OrderEnumerator enumerator = myModule == null ? OrderEnumerator.orderEntries(myProject) : OrderEnumerator.orderEntries(myModule);
          return enumerator.withoutModuleSourceEntries().withoutDepModules().getSourceRoots();
        });
        iterateAll(librarySources, globalCustomScope, iterator);
      }
    }
    return iterator.getFiles();
  }

  private static void iterateAll(VirtualFile @NotNull [] files, @NotNull final GlobalSearchScope searchScope, @NotNull final ContentIterator iterator) {
    final FileTypeManager fileTypeManager = FileTypeManager.getInstance();
    final VirtualFileFilter contentFilter = file -> file.isDirectory() ||
           !fileTypeManager.isFileIgnored(file) && !file.getFileType().isBinary() && searchScope.contains(file);
    for (VirtualFile file : files) {
      if (!VfsUtilCore.iterateChildrenRecursively(file, contentFilter, iterator)) break;
    }
  }

  private boolean canRelyOnSearchers() {
    return ContainerUtil.find(mySearchers, s -> s.isReliable()) != null;
  }

  @NotNull
  private Set<VirtualFile> getFilesForFastWordSearch() {
    final Set<VirtualFile> resultFiles = new CompactVirtualFileSet();
    for(VirtualFile file:myFilesToScanInitially) {
      if (myFileMask.value(file)) {
        resultFiles.add(file);
      }
    }

    for (FindInProjectSearchEngine.FindInProjectSearcher searcher : mySearchers) {
      Collection<VirtualFile> virtualFiles = searcher.searchForOccurrences();
      for (VirtualFile file : virtualFiles) {
        if (myFileMask.value(file)) resultFiles.add(file);
      }
    }

    return resultFiles;
  }

  private Pair.NonNull<PsiFile, VirtualFile> findFile(@NotNull final VirtualFile virtualFile) {
    PsiFile psiFile = myPsiManager.findFile(virtualFile);
    if (psiFile != null) {
      PsiElement sourceFile = psiFile.getNavigationElement();
      if (sourceFile instanceof PsiFile) psiFile = (PsiFile)sourceFile;
      if (psiFile.getFileType().isBinary()) {
        psiFile = null;
      }
    }
    VirtualFile sourceVirtualFile = PsiUtilCore.getVirtualFile(psiFile);
    if (psiFile == null || psiFile.getFileType().isBinary() || sourceVirtualFile == null || sourceVirtualFile.getFileType().isBinary()) {
      return null;
    }

    return Pair.createNonNull(psiFile, sourceVirtualFile);
  }
}
