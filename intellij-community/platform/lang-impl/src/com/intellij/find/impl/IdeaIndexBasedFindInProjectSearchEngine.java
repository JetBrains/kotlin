// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.impl;

import com.intellij.find.FindInProjectSearchEngine;
import com.intellij.find.FindModel;
import com.intellij.find.ngrams.TrigramIndex;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.util.text.TrigramBuilder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.impl.cache.CacheManager;
import com.intellij.psi.search.*;
import com.intellij.util.Processors;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.DumbModeAccessType;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexImpl;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class IdeaIndexBasedFindInProjectSearchEngine implements FindInProjectSearchEngine {
  @Override
  public @NotNull FindInProjectSearcher createSearcher(@NotNull FindModel findModel, @NotNull Project project) {
    return new MyFindInProjectSearcher(project, findModel);
  }

  private static final class MyFindInProjectSearcher implements FindInProjectSearcher {
    private @NotNull final ProjectFileIndex myFileIndex;
    private @NotNull final FileBasedIndexImpl myFileBasedIndex;
    private @NotNull final Project myProject;
    private @NotNull final FindModel myFindModel;

    private final boolean myHasTrigrams;
    private final String myStringToFindInIndices;

    MyFindInProjectSearcher(@NotNull Project project, @NotNull FindModel findModel) {
      myProject = project;
      myFindModel = findModel;
      myFileIndex = ProjectFileIndex.SERVICE.getInstance(myProject);
      myFileBasedIndex = (FileBasedIndexImpl)FileBasedIndex.getInstance();
      String stringToFind = findModel.getStringToFind();

      if (findModel.isRegularExpressions()) {
        stringToFind = FindInProjectUtil.buildStringToFindForIndicesFromRegExp(stringToFind, project);
      }

      myStringToFindInIndices = stringToFind;

      myHasTrigrams = hasTrigrams(myStringToFindInIndices);
    }

    @Override
    public @NotNull Collection<VirtualFile> searchForOccurrences() {
      String stringToFind = getStringToFindInIndexes(myFindModel, myProject);

      if (stringToFind.isEmpty() || (DumbService.getInstance(myProject).isDumb() && !FileBasedIndex.isIndexAccessDuringDumbModeEnabled())) {
        return Collections.emptySet();
      }


      final GlobalSearchScope scope = GlobalSearchScopeUtil.toGlobalSearchScope(FindInProjectUtil.getScopeFromModel(myProject, myFindModel),
                                                                                myProject);

      IntSet keys = new IntOpenHashSet();
      TrigramBuilder.processTrigrams(stringToFind, new TrigramBuilder.TrigramProcessor() {
        @Override
        public boolean test(int value) {
          keys.add(value);
          return true;
        }
      });

      if (!keys.isEmpty()) {
        List<VirtualFile> hits = new ArrayList<>();
        FileBasedIndex.getInstance().ignoreDumbMode(() -> {
          FileBasedIndex.getInstance().getFilesWithKey(TrigramIndex.INDEX_ID, keys, Processors.cancelableCollectProcessor(hits), scope);
        }, DumbModeAccessType.RAW_INDEX_DATA_ACCEPTABLE);

        return Collections.unmodifiableCollection(hits);
      }

      Set<VirtualFile> resultFiles = new HashSet<>();
      PsiSearchHelper helper = PsiSearchHelper.getInstance(myProject);
      helper.processCandidateFilesForText(scope, UsageSearchContext.ANY, myFindModel.isCaseSensitive(), stringToFind, file -> {
        ContainerUtil.addIfNotNull(resultFiles, file);
        return true;
      });

      // in case our word splitting is incorrect
      CacheManager cacheManager = CacheManager.getInstance(myProject);
      VirtualFile[] filesWithWord = cacheManager.getVirtualFilesWithWord(stringToFind, UsageSearchContext.ANY, scope,
                                                                         myFindModel.isCaseSensitive());

      Collections.addAll(resultFiles, filesWithWord);
      return Collections.unmodifiableCollection(resultFiles);
    }

    @Override
    public boolean isReliable() {
      if (DumbService.isDumb(myProject)) return false;

      // a local scope may be over a non-indexed file
      if (myFindModel.getCustomScope() instanceof LocalSearchScope) return false;

      if (myHasTrigrams) return true;

      // $ is used to separate words when indexing plain-text files but not when indexing
      // Java identifiers, so we can't consistently break a string containing $ characters into words
      return myFindModel.isWholeWordsOnly() &&
             myStringToFindInIndices.indexOf('$') < 0 &&
             !StringUtil.getWordsIn(myStringToFindInIndices).isEmpty();
    }

    @Override
    public boolean isCovered(@NotNull VirtualFile file) {
      return myHasTrigrams && isCoveredByIndex(file) && (myFileIndex.isInContent(file) || myFileIndex.isInLibrary(file));
    }

    private boolean isCoveredByIndex(@NotNull VirtualFile file) {
      FileType fileType = file.getFileType();
      return TrigramIndex.isIndexable(fileType) && myFileBasedIndex.isIndexingCandidate(file, TrigramIndex.INDEX_ID);
    }

    private static boolean hasTrigrams(@NotNull String text) {
      return !TrigramBuilder.processTrigrams(text, new TrigramBuilder.TrigramProcessor() {
        @Override
        public boolean test(int value) {
          return false;
        }
      });
    }

    @NotNull
    private static String getStringToFindInIndexes(@NotNull FindModel findModel, @NotNull Project project) {
      String stringToFind = findModel.getStringToFind();

      if (findModel.isRegularExpressions()) {
        stringToFind = FindInProjectUtil.buildStringToFindForIndicesFromRegExp(stringToFind, project);
      }

      return stringToFind;
    }
  }
}
