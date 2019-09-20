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
package com.intellij.refactoring.invertBoolean;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.refactoring.rename.RenameUtil;
import com.intellij.refactoring.ui.ConflictsDialog;
import com.intellij.refactoring.util.MoveRenameUsageInfo;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewDescriptor;
import com.intellij.util.IncorrectOperationException;
import java.util.HashMap;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * @author ven
 */
public class InvertBooleanProcessor extends BaseRefactoringProcessor {
  private static final Logger LOG = Logger.getInstance("#com.intellij.refactoring.invertBoolean.InvertBooleanMethodProcessor");
  private final InvertBooleanDelegate myDelegate;

  private PsiElement myElement;
  private final String myNewName;
  private final RenameProcessor myRenameProcessor;
  private final Map<UsageInfo, SmartPsiElementPointer> myToInvert = new HashMap<>();
  private final SmartPointerManager mySmartPointerManager;

  public InvertBooleanProcessor(final PsiElement namedElement, final String newName) {
    super(namedElement.getProject());
    myElement = namedElement;
    myNewName = newName;
    final Project project = namedElement.getProject();
    final boolean canRename =
      namedElement instanceof PsiNamedElement && !Comparing.equal(((PsiNamedElement)namedElement).getName(), myNewName);
    myRenameProcessor = canRename ? new RenameProcessor(project, namedElement, newName, false, false) {
      @NotNull
      @Override
      protected ConflictsDialog createConflictsDialog(@NotNull MultiMap<PsiElement, String> conflicts, @Nullable final UsageInfo[] usages) {
        return new ConflictsDialog(myProject, conflicts, usages == null ? null : (Runnable)() -> InvertBooleanProcessor.this.execute(usages), false, true);
      }

      @Override
      protected void prepareSuccessful() {
        InvertBooleanProcessor.this.prepareSuccessful();
      }
    } : null;
    mySmartPointerManager = SmartPointerManager.getInstance(project);
    myDelegate = InvertBooleanDelegate.findInvertBooleanDelegate(myElement);
    LOG.assertTrue(myDelegate != null);
  }

  @Override
  @NotNull
  protected UsageViewDescriptor createUsageViewDescriptor(@NotNull UsageInfo[] usages) {
    return new InvertBooleanUsageViewDescriptor(myElement);
  }

  @Override
  protected boolean preprocessUsages(@NotNull Ref<UsageInfo[]> refUsages) {
    final MultiMap<PsiElement, String> conflicts = new MultiMap<>();
    final UsageInfo[] usageInfos = refUsages.get();
    myDelegate.findConflicts(usageInfos, conflicts);
    
    if (!conflicts.isEmpty())  {
      return showConflicts(conflicts, usageInfos);
    }

    if (myRenameProcessor == null || myRenameProcessor.preprocessUsages(refUsages)) {
      prepareSuccessful();
      return true;
    }
    return false;
  }

  @Override
  @NotNull
  protected UsageInfo[] findUsages() {
    final List<SmartPsiElementPointer> toInvert = new ArrayList<>();

    final LinkedHashSet<PsiElement> elementsToInvert = new LinkedHashSet<>();
    myDelegate.collectRefElements(myElement, myRenameProcessor, myNewName, elementsToInvert);
    for (PsiElement element : elementsToInvert) {
      toInvert.add(mySmartPointerManager.createSmartPsiElementPointer(element));
    }

    final UsageInfo[] renameUsages = myRenameProcessor != null ? myRenameProcessor.findUsages() : UsageInfo.EMPTY_ARRAY;

    final SmartPsiElementPointer[] usagesToInvert = toInvert.toArray(new SmartPsiElementPointer[0]);

    //merge rename and invert usages
    Map<PsiElement, UsageInfo> expressionsToUsages = new HashMap<>();
    List<UsageInfo> result = new ArrayList<>();
    for (UsageInfo renameUsage : renameUsages) {
      expressionsToUsages.put(renameUsage.getElement(), renameUsage);
      result.add(renameUsage);
    }

    for (SmartPsiElementPointer pointer : usagesToInvert) {
      final PsiElement expression = pointer.getElement();
      if (!expressionsToUsages.containsKey(expression)) {
        final UsageInfo usageInfo = new UsageInfo(expression);
        expressionsToUsages.put(expression, usageInfo);
        result.add(usageInfo); //fake UsageInfo
        myToInvert.put(usageInfo, pointer);
      } else {
        myToInvert.put(expressionsToUsages.get(expression), pointer);
      }
    }

    return result.toArray(UsageInfo.EMPTY_ARRAY);
  }

  @Override
  protected void refreshElements(@NotNull PsiElement[] elements) {
    myElement = elements[0];
  }

  private static UsageInfo[] extractUsagesForElement(PsiElement element, UsageInfo[] usages) {
    final ArrayList<UsageInfo> extractedUsages = new ArrayList<>(usages.length);
    for (UsageInfo usage : usages) {
      if (usage instanceof MoveRenameUsageInfo) {
        MoveRenameUsageInfo usageInfo = (MoveRenameUsageInfo)usage;
        if (element.equals(usageInfo.getReferencedElement())) {
          extractedUsages.add(usageInfo);
        }
      }
    }
    return extractedUsages.toArray(UsageInfo.EMPTY_ARRAY);
  }

  @Override
  protected void performRefactoring(@NotNull UsageInfo[] usages) {
    if (myRenameProcessor != null) {
      for (final PsiElement element : myRenameProcessor.getElements()) {
        try {
          RenameUtil.doRename(element, myRenameProcessor.getNewName(element), extractUsagesForElement(element, usages), myProject, null);
        }
        catch (final IncorrectOperationException e) {
          RenameUtil.showErrorMessage(e, element, myProject);
          return;
        }
      }
    }


    for (UsageInfo usage : usages) {
      final SmartPsiElementPointer pointerToInvert = myToInvert.get(usage);
      if (pointerToInvert != null) {
        PsiElement element = pointerToInvert.getElement();
        LOG.assertTrue(element != null);
        InvertBooleanDelegate delegate = InvertBooleanDelegate.findInvertBooleanDelegate(element);
        try {
          (delegate != null ? delegate : myDelegate).replaceWithNegatedExpression(element);
        }
        catch (IncorrectOperationException e) {
          LOG.error(e);
        }
      }
    }

    myDelegate.invertElementInitializer(myElement);
  }

  @NotNull
  @Override
  protected String getCommandName() {
    return InvertBooleanHandler.REFACTORING_NAME;
  }
}
