/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.refactoring.changeSignature;

import com.intellij.ide.actions.CopyReferenceAction;
import com.intellij.lang.findUsages.DescriptiveNameUtil;
import com.intellij.openapi.command.undo.BasicUndoableAction;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.command.undo.UndoableAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.listeners.RefactoringEventData;
import com.intellij.refactoring.listeners.UndoRefactoringElementListener;
import com.intellij.refactoring.listeners.impl.RefactoringTransaction;
import com.intellij.refactoring.rename.ResolveSnapshotProvider;
import com.intellij.refactoring.rename.inplace.VariableInplaceRenamer;
import com.intellij.refactoring.util.MoveRenameUsageInfo;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.MultiMap;
import java.util.HashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Maxim.Medvedev
 */
public abstract class ChangeSignatureProcessorBase extends BaseRefactoringProcessor {
  private static final Logger LOG = Logger.getInstance(ChangeSignatureProcessorBase.class);
  protected static final String REFACTORING_ID = "refactoring.changeSignature";

  protected final ChangeInfo myChangeInfo;
  protected final PsiManager myManager;


  protected ChangeSignatureProcessorBase(Project project, ChangeInfo changeInfo) {
    super(project);
    myChangeInfo = changeInfo;
    myManager = PsiManager.getInstance(project);
  }

  protected ChangeSignatureProcessorBase(Project project, @Nullable Runnable prepareSuccessfulCallback, ChangeInfo changeInfo) {
    super(project, prepareSuccessfulCallback);
    myChangeInfo = changeInfo;
    myManager = PsiManager.getInstance(project);
  }

  @Override
  @NotNull
  protected UsageInfo[] findUsages() {
    return findUsages(myChangeInfo);
  }

  public static void collectConflictsFromExtensions(@NotNull Ref<UsageInfo[]> refUsages,
                                                    MultiMap<PsiElement, String> conflictDescriptions,
                                                    ChangeInfo changeInfo) {
    for (ChangeSignatureUsageProcessor usageProcessor : ChangeSignatureUsageProcessor.EP_NAME.getExtensions()) {
      final MultiMap<PsiElement, String> conflicts = usageProcessor.findConflicts(changeInfo, refUsages);
      for (PsiElement key : conflicts.keySet()) {
        Collection<String> collection = conflictDescriptions.get(key);
        if (collection.isEmpty()) collection = new java.util.HashSet<>();
        collection.addAll(conflicts.get(key));
        conflictDescriptions.put(key, collection);
      }
    }
  }

  @NotNull
  public static UsageInfo[] findUsages(ChangeInfo changeInfo) {
    List<UsageInfo> infos = new ArrayList<>();
    final ChangeSignatureUsageProcessor[] processors = ChangeSignatureUsageProcessor.EP_NAME.getExtensions();
    for (ChangeSignatureUsageProcessor processor : processors) {
      for (UsageInfo info : processor.findUsages(changeInfo)) {
        LOG.assertTrue(info != null, processor);
        infos.add(info);
      }
    }
    infos = filterUsages(infos);
    return infos.toArray(UsageInfo.EMPTY_ARRAY);
  }

  protected static List<UsageInfo> filterUsages(List<? extends UsageInfo> infos) {
    Map<PsiElement, MoveRenameUsageInfo> moveRenameInfos = new HashMap<>();
    Set<PsiElement> usedElements = new HashSet<>();

    List<UsageInfo> result = new ArrayList<>(infos.size() / 2);
    for (UsageInfo info : infos) {
      LOG.assertTrue(info != null);
      PsiElement element = info.getElement();
      if (info instanceof MoveRenameUsageInfo) {
        if (usedElements.contains(element)) continue;
        moveRenameInfos.put(element, (MoveRenameUsageInfo)info);
      }
      else {
        moveRenameInfos.remove(element);
        usedElements.add(element);
        if (!(info instanceof PossiblyIncorrectUsage) || ((PossiblyIncorrectUsage)info).isCorrect()) {
          result.add(info);
        }
      }
    }
    result.addAll(moveRenameInfos.values());
    return result;
  }


  @Override
  protected boolean isPreviewUsages(@NotNull UsageInfo[] usages) {
    for (ChangeSignatureUsageProcessor processor : ChangeSignatureUsageProcessor.EP_NAME.getExtensions()) {
      if (processor.shouldPreviewUsages(myChangeInfo, usages)) return true;
    }
    return super.isPreviewUsages(usages);
  }

  @Nullable
  @Override
  protected String getRefactoringId() {
    return REFACTORING_ID;
  }

  @Nullable
  @Override
  protected RefactoringEventData getBeforeData() {
    RefactoringEventData data = new RefactoringEventData();
    data.addElement(getChangeInfo().getMethod());
    return data;
  }

  @Nullable
  @Override
  protected RefactoringEventData getAfterData(@NotNull UsageInfo[] usages) {
    RefactoringEventData data = new RefactoringEventData();
    data.addElement(getChangeInfo().getMethod());
    return data;
  }

  @Override
  protected void performRefactoring(@NotNull UsageInfo[] usages) {
    RefactoringTransaction transaction = getTransaction();
    final ChangeInfo changeInfo = myChangeInfo;
    final RefactoringElementListener elementListener = transaction == null ? null : transaction.getElementListener(changeInfo.getMethod());
    final String fqn = CopyReferenceAction.elementToFqn(changeInfo.getMethod());
    if (fqn != null) {
      UndoableAction action = new BasicUndoableAction() {
        @Override
        public void undo() {
          if (elementListener instanceof UndoRefactoringElementListener) {
            ((UndoRefactoringElementListener)elementListener).undoElementMovedOrRenamed(changeInfo.getMethod(), fqn);
          }
        }

        @Override
        public void redo() {
        }
      };
      UndoManager.getInstance(myProject).undoableActionPerformed(action);
    }
    try {
      doChangeSignature(changeInfo, usages);
      final PsiElement method = changeInfo.getMethod();
      LOG.assertTrue(method.isValid());
      if (elementListener != null && changeInfo.isNameChanged()) {
        elementListener.elementRenamed(method);
      }
    }
    catch (IncorrectOperationException e) {
      LOG.error(e);
    }
  }

  public static void doChangeSignature(ChangeInfo changeInfo, @NotNull UsageInfo[] usages) {
    final ChangeSignatureUsageProcessor[] processors = ChangeSignatureUsageProcessor.EP_NAME.getExtensions();

    final ResolveSnapshotProvider resolveSnapshotProvider = changeInfo.isParameterNamesChanged() ?
                                                            VariableInplaceRenamer.INSTANCE.forLanguage(changeInfo.getMethod().getLanguage()) : null;
    final List<ResolveSnapshotProvider.ResolveSnapshot> snapshots = new ArrayList<>();
    for (ChangeSignatureUsageProcessor processor : processors) {
      if (resolveSnapshotProvider != null) {
        processor.registerConflictResolvers(snapshots, resolveSnapshotProvider, usages, changeInfo);
      }
    }

    for (UsageInfo usage : usages) {
      for (ChangeSignatureUsageProcessor processor : processors) {
        if (processor.processUsage(changeInfo, usage, true, usages)) break;
      }
    }

    LOG.assertTrue(changeInfo.getMethod().isValid());
    for (ChangeSignatureUsageProcessor processor : processors) {
      if (processor.processPrimaryMethod(changeInfo)) break;
    }

    for (UsageInfo usage : usages) {
      for (ChangeSignatureUsageProcessor processor : processors) {
        if (processor.processUsage(changeInfo, usage, false, usages)) break;
      }
    }

    if (!snapshots.isEmpty()) {
      for (ParameterInfo parameterInfo : changeInfo.getNewParameters()) {
        for (ResolveSnapshotProvider.ResolveSnapshot snapshot : snapshots) {
          snapshot.apply(parameterInfo.getName());
        }
      }
    }
  }

  @NotNull
  @Override
  protected String getCommandName() {
    return RefactoringBundle.message("changing.signature.of.0", DescriptiveNameUtil.getDescriptiveName(myChangeInfo.getMethod()));
  }

  public ChangeInfo getChangeInfo() {
    return myChangeInfo;
  }
}
