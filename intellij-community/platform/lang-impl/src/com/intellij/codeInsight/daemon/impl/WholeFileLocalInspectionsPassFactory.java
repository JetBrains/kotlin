// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeHighlighting.*;
import com.intellij.codeInsight.daemon.DaemonBundle;
import com.intellij.codeInsight.daemon.ProblemHighlightFilter;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.ex.InspectionProfileWrapper;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.profile.ProfileChangeAdapter;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.ObjectIntMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

final class WholeFileLocalInspectionsPassFactory implements TextEditorHighlightingPassFactory, Disposable {
  private final Set<PsiFile> mySkipWholeInspectionsCache = ContainerUtil.createWeakSet(); // guarded by mySkipWholeInspectionsCache
  private final ObjectIntMap<PsiFile> myPsiModificationCount = ContainerUtil.createWeakKeyIntValueMap(); // guarded by myPsiModificationCount
  private final Project myProject;

  static final class MyRegistrar implements TextEditorHighlightingPassFactoryRegistrar {
    @Override
    public void registerHighlightingPassFactory(@NotNull TextEditorHighlightingPassRegistrar registrar, @NotNull Project project) {
      WholeFileLocalInspectionsPassFactory factory = new WholeFileLocalInspectionsPassFactory(project, registrar);
      Disposer.register(project, factory);
    }
  }

  private WholeFileLocalInspectionsPassFactory(@NotNull Project project, @NotNull TextEditorHighlightingPassRegistrar registrar) {
    myProject = project;
    // can run in the same time with LIP, but should start after it, since I believe whole-file inspections would run longer
    registrar.registerTextEditorHighlightingPass(this, null, new int[]{Pass.LOCAL_INSPECTIONS}, true, Pass.WHOLE_FILE_LOCAL_INSPECTIONS);

    myProject.getMessageBus().connect(this).subscribe(ProfileChangeAdapter.TOPIC, new ProfileChangeAdapter() {
      @Override
      public void profileChanged(InspectionProfile profile) {
        clearCaches();
      }

      @Override
      public void profileActivated(InspectionProfile oldProfile, @Nullable InspectionProfile profile) {
        clearCaches();
      }
    });
  }

  @Override
  public void dispose() {
    clearCaches();
  }

  private void clearCaches() {
    synchronized (mySkipWholeInspectionsCache) {
      mySkipWholeInspectionsCache.clear();
    }
    synchronized (myPsiModificationCount) {
      myPsiModificationCount.clear();
    }
  }

  @Override
  @Nullable
  public TextEditorHighlightingPass createHighlightingPass(@NotNull final PsiFile file, @NotNull final Editor editor) {
    long actualCount = PsiManager.getInstance(myProject).getModificationTracker().getModificationCount();
    synchronized (myPsiModificationCount) {
      if (myPsiModificationCount.get(file) == (int)actualCount) {
        return null; //optimization
      }
    }

    if (!ProblemHighlightFilter.shouldHighlightFile(file)) {
      return null;
     }

    synchronized (mySkipWholeInspectionsCache) {
      if (mySkipWholeInspectionsCache.contains(file)) {
        return null;
      }
    }
    ProperTextRange visibleRange = VisibleHighlightingPassFactory.calculateVisibleRange(editor);
    return new LocalInspectionsPass(file, editor.getDocument(), 0, file.getTextLength(), visibleRange, true,
                                    new DefaultHighlightInfoProcessor(), false) {
      @NotNull
      @Override
      List<LocalInspectionToolWrapper> getInspectionTools(@NotNull InspectionProfileWrapper profile) {
        List<LocalInspectionToolWrapper> tools = super.getInspectionTools(profile);
        List<LocalInspectionToolWrapper> result = ContainerUtil.filter(tools, LocalInspectionToolWrapper::runForWholeFile);
        if (result.isEmpty()) {
          synchronized (mySkipWholeInspectionsCache) {
            mySkipWholeInspectionsCache.add(file);
          }
        }
        return result;
      }

      @Override
      protected String getPresentableName() {
        return DaemonBundle.message("pass.whole.inspections");
      }

      @Override
      protected void applyInformationWithProgress() {
        super.applyInformationWithProgress();
        long modificationCount = PsiManager.getInstance(myProject).getModificationTracker().getModificationCount();
        synchronized (myPsiModificationCount) {
          myPsiModificationCount.put(file, (int)modificationCount);
        }
      }
    };
  }
}
