// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.folding.impl;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * @author Denis Zhdanov
 */
public abstract class AbstractElementSignatureProvider implements ElementSignatureProvider {
  private static final int CHILDREN_COUNT_LIMIT = 100;

  protected static final String ELEMENTS_SEPARATOR = ";";
  protected static final String ELEMENT_TOKENS_SEPARATOR = "#";

  private static final String ESCAPE_CHAR = "\\";
  private static final List<String> ESCAPE_FROM = Arrays.asList(ESCAPE_CHAR, ELEMENT_TOKENS_SEPARATOR, ELEMENTS_SEPARATOR);
  private static final List<String> ESCAPE_TO = Arrays.asList(ESCAPE_CHAR + ESCAPE_CHAR, ESCAPE_CHAR + "s", ESCAPE_CHAR + "h");

  @Override
  @Nullable
  public PsiElement restoreBySignature(@NotNull PsiFile file, @NotNull String signature, @Nullable StringBuilder processingInfoStorage) {
    int semicolonIndex = signature.indexOf(ELEMENTS_SEPARATOR);
    PsiElement parent;

    if (semicolonIndex >= 0) {
      String parentSignature = signature.substring(semicolonIndex + 1);
      if (processingInfoStorage != null) {
        processingInfoStorage.append(String.format(
          "Provider '%s'. Restoring parent by signature '%s'...%n", getClass().getName(), parentSignature
        ));
      }
      parent = restoreBySignature(file, parentSignature, processingInfoStorage);
      if (processingInfoStorage != null) {
        processingInfoStorage.append(String.format("Restored parent by signature '%s': %s%n", parentSignature, parent));
      }
      if (parent == null) return null;
      signature = signature.substring(0, semicolonIndex);
    }
    else {
      parent = file;
    }

    StringTokenizer tokenizer = new StringTokenizer(signature, ELEMENT_TOKENS_SEPARATOR);
    String type = tokenizer.nextToken();
    if (processingInfoStorage != null) {
      processingInfoStorage.append(String.format(
        "Provider '%s'. Restoring target element by signature '%s'. Parent: %s, same as the given parent: %b%n",
        getClass().getName(), signature, parent, parent == file
      ));
    }
    return restoreBySignatureTokens(file, parent, type, tokenizer, processingInfoStorage);
  }

  @Nullable
  protected abstract PsiElement restoreBySignatureTokens(@NotNull PsiFile file,
                                                         @NotNull PsiElement parent,
                                                         @NotNull String type,
                                                         @NotNull StringTokenizer tokenizer,
                                                         @Nullable StringBuilder processingInfoStorage);

  /**
   * @return -1, if {@code parent} has too many children and calculating child index would be too slow
   */
  protected static <T extends PsiNamedElement> int getChildIndex(T element, PsiElement parent, String name, Class<? extends T> hisClass) {
    PsiFile file = parent.getContainingFile();
    Set<PsiElement> cache = file == null ? null :
      CachedValuesManager.getCachedValue(file, () -> new CachedValueProvider.Result<>(ContainerUtil.createWeakSet(), file));
    if (cache != null && cache.contains(parent)) return -1; 
    PsiElement[] children = parent.getChildren();
    if (children.length > CHILDREN_COUNT_LIMIT) {
      if (cache != null) cache.add(parent);
      return -1;
    }

    int index = 0;

    for (PsiElement child : children) {
      if (ReflectionUtil.isAssignable(hisClass, child.getClass())) {
        T namedChild = hisClass.cast(child);
        final String childName = namedChild.getName();

        if (Comparing.equal(name, childName)) {
          if (namedChild.equals(element)) {
            return index;
          }
          index++;
        }
      }
    }

    return index;
  }

  @Nullable
  protected static <T extends PsiNamedElement> T restoreElementInternal(@NotNull PsiElement parent,
                                                              String name,
                                                              int index,
                                                              @NotNull Class<T> hisClass)
  {
    PsiElement[] children = parent.getChildren();

    for (PsiElement child : children) {
      if (ReflectionUtil.isAssignable(hisClass, child.getClass())) {
        T namedChild = hisClass.cast(child);
        final String childName = namedChild.getName();

        if (Comparing.equal(name, childName)) {
          if (index == 0) {
            return namedChild;
          }
          index--;
        }
      }
    }

    return null;
  }

  protected static String escape(String name) {
    return StringUtil.replace(name, ESCAPE_FROM, ESCAPE_TO);
  }

  protected static String unescape(String name) {
    return StringUtil.replace(name, ESCAPE_TO, ESCAPE_FROM);
  }
}
