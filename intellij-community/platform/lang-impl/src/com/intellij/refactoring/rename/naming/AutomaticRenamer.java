// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.rename.naming;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.refactoring.rename.RenameUtil;
import com.intellij.refactoring.rename.UnresolvableCollisionUsageInfo;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;

import java.util.*;

/**
 * @author dsl
 */
public abstract class AutomaticRenamer {
  private static final Logger LOG = Logger.getInstance("#com.intellij.refactoring.rename.naming.AutomaticRenamer");

  private final LinkedHashMap<PsiNamedElement, String> myRenames = new LinkedHashMap<>();
  protected final List<PsiNamedElement> myElements;

  protected AutomaticRenamer() {
    myElements = new ArrayList<>();
  }

  public boolean hasAnythingToRename() {
    return myRenames.values().stream().anyMatch(Objects::nonNull);
  }

  public void findUsages(List<UsageInfo> result, final boolean searchInStringsAndComments, final boolean searchInNonJavaFiles) {
    findUsages(result, searchInStringsAndComments, searchInNonJavaFiles, null);
  }

  public void findUsages(List<UsageInfo> result,
                         final boolean searchInStringsAndComments,
                         final boolean searchInNonJavaFiles,
                         List<UnresolvableCollisionUsageInfo> unresolvedUsages) {
    findUsages(result, searchInStringsAndComments, searchInNonJavaFiles, unresolvedUsages, null);
  }

  public void findUsages(List<UsageInfo> result,
                         final boolean searchInStringsAndComments,
                         final boolean searchInNonJavaFiles,
                         List<UnresolvableCollisionUsageInfo> unresolvedUsages,
                         Map<PsiElement, String> allRenames) {
    for (Iterator<PsiNamedElement> iterator = myElements.iterator(); iterator.hasNext();) {
      final PsiNamedElement variable = iterator.next();
      RenameProcessor.assertNonCompileElement(variable);
      final boolean success = findUsagesForElement(variable, result, searchInStringsAndComments, searchInNonJavaFiles, unresolvedUsages, allRenames);
      if (!success) {
        iterator.remove();
      }
    }
  }

  private boolean findUsagesForElement(PsiNamedElement element,
                                       List<UsageInfo> result,
                                       final boolean searchInStringsAndComments,
                                       final boolean searchInNonJavaFiles,
                                       List<UnresolvableCollisionUsageInfo> unresolvedUsages,
                                       Map<PsiElement, String> allRenames) {
    final String newName = getNewName(element);
    if (newName != null) {

      final LinkedHashMap<PsiNamedElement, String> renames = new LinkedHashMap<>(myRenames);
      if (allRenames != null) {
        for (PsiElement psiElement : allRenames.keySet()) {
          if (psiElement instanceof PsiNamedElement) {
            renames.put((PsiNamedElement)psiElement, allRenames.get(psiElement));
          }
        }
      }
      final UsageInfo[] usages = RenameUtil.findUsages(element, newName, searchInStringsAndComments, searchInNonJavaFiles, renames);
      for (final UsageInfo usage : usages) {
        if (usage instanceof UnresolvableCollisionUsageInfo) {
          if (unresolvedUsages != null) {
            unresolvedUsages.add((UnresolvableCollisionUsageInfo)usage);
          }
          return false;
        }
      }
      ContainerUtil.addAll(result, usages);
    }
    return true;
  }

  public List<PsiNamedElement> getElements() {
    return Collections.unmodifiableList(myElements);
  }

  public String getNewName(PsiNamedElement namedElement) {
    return myRenames.get(namedElement);
  }

  public Map<PsiNamedElement, String> getRenames() {
    return Collections.unmodifiableMap(myRenames);
  }

  public void setRename(PsiNamedElement element, String replacement) {
    LOG.assertTrue(myRenames.put(element, replacement) != null);
  }

  public void doNotRename(PsiNamedElement element) {
    LOG.assertTrue(myRenames.remove(element) != null);
  }

  protected void suggestAllNames(final String oldClassName, String newClassName) {
    final NameSuggester suggester = new NameSuggester(oldClassName, newClassName);
    for (int varIndex = myElements.size() - 1; varIndex >= 0; varIndex--) {
      final PsiNamedElement element = myElements.get(varIndex);
      final String name = element.getName();
      if (!myRenames.containsKey(element) && name != null) {
        String newName = suggestNameForElement(element, suggester, newClassName, oldClassName);
        if (!newName.equals(name)) {
          myRenames.put(element, newName);
        }
        else {
          myRenames.put(element, null);
        }
      }
      if (myRenames.get(element) == null) {
        myElements.remove(varIndex);
      }
    }
  }

  protected String suggestNameForElement(PsiNamedElement element, NameSuggester suggester, String newClassName, String oldClassName) {
    String name = element.getName();
    if (oldClassName.equals(name)) {
      return newClassName;
    }
    String canonicalName = nameToCanonicalName(name, element);
    final String newCanonicalName = suggester.suggestName(canonicalName);
    if (newCanonicalName.length() == 0) {
      LOG.error("oldClassName = " + oldClassName + ", newClassName = " + newClassName + ", name = " + name + ", canonicalName = " +
                canonicalName + ", newCanonicalName = " + newCanonicalName);
    }
    return canonicalNameToName(newCanonicalName, element);
  }

  @NonNls
  protected String canonicalNameToName(@NonNls String canonicalName, PsiNamedElement element) {
    return canonicalName;
  }

  protected String nameToCanonicalName(@NonNls String name, PsiNamedElement element) {
    return name;
  }

  public boolean allowChangeSuggestedName() {
    return true;
  }

  public boolean isSelectedByDefault() {
    return false;
  }

  @Nls(capitalization = Nls.Capitalization.Title)
  public abstract String getDialogTitle();

  @Nls(capitalization = Nls.Capitalization.Sentence)
  public abstract String getDialogDescription();

  public abstract String entityName();
}