// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.invertBoolean;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
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
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author ven
 */
public class InvertBooleanProcessor extends BaseRefactoringProcessor {
  private static final Logger LOG = Logger.getInstance(InvertBooleanProcessor.class);
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
      namedElement instanceof PsiNamedElement && !Objects.equals(((PsiNamedElement)namedElement).getName(), myNewName);
    myRenameProcessor = canRename ? new RenameProcessor(project, namedElement, newName, false, false) {
      @NotNull
      @Override
      protected ConflictsDialog createConflictsDialog(@NotNull MultiMap<PsiElement, String> conflicts, final UsageInfo @Nullable [] usages) {
        return new ConflictsDialog(myProject, conflicts, usages == null ? null : () -> InvertBooleanProcessor.this.execute(usages), false, true);
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
  protected UsageViewDescriptor createUsageViewDescriptor(UsageInfo @NotNull [] usages) {
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
  protected UsageInfo @NotNull [] findUsages() {
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
  protected void refreshElements(PsiElement @NotNull [] elements) {
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
  protected void performRefactoring(UsageInfo @NotNull [] usages) {
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
    return InvertBooleanHandler.getRefactoringName();
  }
}
