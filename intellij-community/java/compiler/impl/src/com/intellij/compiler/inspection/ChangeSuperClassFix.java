// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.inspection;

import com.intellij.CommonBundle;
import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.compiler.JavaCompilerBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.refactoring.ui.MemberSelectionPanel;
import com.intellij.refactoring.util.classMembers.MemberInfo;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ChangeSuperClassFix implements LocalQuickFix, HighPriorityAction {
  @NotNull
  private final SmartPsiElementPointer<PsiClass> myNewSuperClass;
  @NotNull
  private final SmartPsiElementPointer<PsiClass> myOldSuperClass;
  @NotNull
  private final SmartPsiElementPointer<PsiClass> myTargetClass;
  private final int myInheritorCount;
  @NotNull
  private final String myNewSuperName;
  private final boolean myImplements;

  public ChangeSuperClassFix(@NotNull PsiClass targetClass,
                             @NotNull PsiClass newSuperClass,
                             @NotNull PsiClass oldSuperClass,
                             final int percent,
                             final boolean isImplements) {
    final SmartPointerManager smartPointerManager = SmartPointerManager.getInstance(newSuperClass.getProject());
    myNewSuperName = Objects.requireNonNull(newSuperClass.getQualifiedName());
    myTargetClass = smartPointerManager.createSmartPsiElementPointer(targetClass);
    myNewSuperClass = smartPointerManager.createSmartPsiElementPointer(newSuperClass);
    myOldSuperClass = smartPointerManager.createSmartPsiElementPointer(oldSuperClass);
    myInheritorCount = percent;
    myImplements = isImplements;
  }

  @NotNull
  @TestOnly
  public PsiClass getNewSuperClass() {
    return Objects.requireNonNull(myNewSuperClass.getElement());
  }

  @TestOnly
  public int getInheritorCount() {
    return myInheritorCount;
  }

  @NotNull
  @Override
  public String getName() {
    return String.format("Make " + (myImplements ? "implements" : "extends") + " '%s'", myNewSuperName);
  }

  @NotNull
  @Override
  public String getFamilyName() {
    return InspectionsBundle.message("group.names.inheritance.issues");
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  @Override
  public void applyFix(@NotNull final Project project, @NotNull final ProblemDescriptor problemDescriptor) {
    final PsiClass oldSuperClass = myOldSuperClass.getElement();
    final PsiClass newSuperClass = myNewSuperClass.getElement();
    if (oldSuperClass == null || newSuperClass == null) return;
    PsiClass aClass = myTargetClass.getElement();
    if (aClass == null || !FileModificationService.getInstance().preparePsiElementsForWrite(aClass)) return;
    changeSuperClass(aClass, oldSuperClass, newSuperClass);
  }

  /**
   * oldSuperClass and newSuperClass can be interfaces or classes in any combination
   * <p/>
   * 1. does not check that oldSuperClass is really super of aClass
   * 2. does not check that newSuperClass not exists in currently existed supers
   */
  private static void changeSuperClass(@NotNull final PsiClass aClass,
                                       @NotNull final PsiClass oldSuperClass,
                                       @NotNull final PsiClass newSuperClass) {
    PsiMethod[] ownMethods = aClass.getMethods();
    // first is own method, second is parent
    List<Pair<PsiMethod, Set<PsiMethod>>> oldOverridenMethods =
      ContainerUtil.mapNotNull(ownMethods, m -> {
        if (m.isConstructor()) return null;
        PsiMethod[] supers = m.findSuperMethods(oldSuperClass);
        if (supers.length == 0) return null;
        return Pair.create(m, ContainerUtil.set(supers));
      });

    JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(aClass.getProject());
    PsiElementFactory factory = psiFacade.getElementFactory();
    WriteAction.run(() -> {
      PsiElement ref;
      if (aClass instanceof PsiAnonymousClass) {
        ref = ((PsiAnonymousClass)aClass).getBaseClassReference().replace(factory.createClassReferenceElement(newSuperClass));
      }
      else {
        PsiReferenceList extendsList = Objects.requireNonNull(aClass.getExtendsList());
        PsiJavaCodeReferenceElement[] refElements =
          ArrayUtil.mergeArrays(getReferences(extendsList), getReferences(aClass.getImplementsList()));
        for (PsiJavaCodeReferenceElement refElement : refElements) {
          if (refElement.isReferenceTo(oldSuperClass)) {
            refElement.delete();
          }
        }

        PsiReferenceList list;
        if (newSuperClass.isInterface() && !aClass.isInterface()) {
          list = aClass.getImplementsList();
        }
        else {
          list = extendsList;
          PsiJavaCodeReferenceElement[] elements = list.getReferenceElements();
          if (elements.length == 1) {
            PsiClass objectClass = psiFacade.findClass(CommonClassNames.JAVA_LANG_OBJECT, aClass.getResolveScope());
            if (objectClass != null && elements[0].isReferenceTo(objectClass)) {
              elements[0].delete();
            }
          }
        }
        assert list != null;
        ref = list.add(factory.createClassReferenceElement(newSuperClass));
      }
      JavaCodeStyleManager.getInstance(aClass.getProject()).shortenClassReferences(ref);
    });

    List<MemberInfo> memberInfos = oldOverridenMethods.stream().filter(m -> {
      Set<PsiMethod> newSupers = ContainerUtil.set(m.getFirst().findSuperMethods(newSuperClass));
      return !newSupers.equals(m.getSecond());
    }).map(m -> m.getFirst())
      .map(m -> {
      MemberInfo info = new MemberInfo(m);
      info.setChecked(true);
      return info;
    }).collect(Collectors.toList());

    if (memberInfos.isEmpty()) {
      return;
    }

    List<PsiMethod> toDelete = getOverridenMethodsToDelete(memberInfos, newSuperClass.getName(), aClass.getProject());
    if (!toDelete.isEmpty()) {
      WriteAction.run(() -> {
        for (PsiMethod method : toDelete) {
          method.delete();
        }
      });
    }
  }

  private static PsiJavaCodeReferenceElement @NotNull [] getReferences(PsiReferenceList list) {
    return list == null ? PsiJavaCodeReferenceElement.EMPTY_ARRAY : list.getReferenceElements();
  }

  @NotNull
  private static List<PsiMethod> getOverridenMethodsToDelete(List<MemberInfo> candidates,
                                                             String newClassName,
                                                             Project project) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return ContainerUtil.map(candidates, c -> (PsiMethod)c.getMember());
    }
    MemberSelectionPanel panel =
      new MemberSelectionPanel("<html>Choose members to delete since they are already defined in <b>" + newClassName + "</b>",
                               candidates,
                               null);
    DialogWrapper dlg = new DialogWrapper(project, false) {

      {
        setOKButtonText(CommonBundle.message("button.without.mnemonic.remove"));
        setTitle(JavaCompilerBundle.message("choose.members"));
        init();
      }
      @NotNull
      @Override
      protected JComponent createCenterPanel() {
        return panel;
      }
    };
    return dlg.showAndGet()
           ? ContainerUtil.map(panel.getTable().getSelectedMemberInfos(), info -> (PsiMethod)info.getMember())
           : Collections.emptyList();
  }
}
