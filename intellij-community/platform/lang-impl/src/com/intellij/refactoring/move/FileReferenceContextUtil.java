// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.refactoring.move;

import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceOwner;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiFileReference;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class FileReferenceContextUtil {
  private static final Logger LOG = Logger.getInstance(FileReferenceContextUtil.class);
  private static final Key<Pair<PsiFileSystemItem, Integer>> REF_FILE_SYSTEM_ITEM_KEY = Key.create("REF_FILE_SYSTEM_ITEM_KEY");

  private FileReferenceContextUtil() {
  }

  public static Map<String, PsiFileSystemItem> encodeFileReferences(PsiElement element) {
    final Map<String, PsiFileSystemItem> map = new HashMap<>();
    if (element == null || element instanceof PsiCompiledElement || isBinary(element)) return map;
    element.accept(new PsiRecursiveElementWalkingVisitor(true) {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        if (element instanceof PsiLanguageInjectionHost && element.isValid()) {
          InjectedLanguageManager.getInstance(element.getProject())
            .enumerate(element, (injectedPsi, places) -> encodeFileReferences(injectedPsi));
        }

        final PsiReference[] refs = element.getReferences();
        for (int refIndex = 0; refIndex < refs.length; refIndex++) {
          PsiReference reference = refs[refIndex];
          final PsiFileReference ref = reference instanceof FileReferenceOwner ?
                                       ((FileReferenceOwner)reference).getLastFileReference() :
                                       null;
          if (ref != null && encodeFileReference(element, ref, map, refIndex)) break;
        }
        super.visitElement(element);
      }
    });
    return map;
  }

  private static boolean encodeFileReference(PsiElement element, PsiFileReference ref, Map<String, PsiFileSystemItem> map, int refIndex) {
    final ResolveResult[] results = ref.multiResolve(false);
    for (ResolveResult result : results) {
      if (result.getElement() instanceof PsiFileSystemItem) {
        PsiFileSystemItem fileSystemItem = (PsiFileSystemItem)result.getElement();
        element.putCopyableUserData(REF_FILE_SYSTEM_ITEM_KEY, Pair.create(fileSystemItem, refIndex));
        map.put(element.getText(), fileSystemItem);
        return true;
      }
    }
    return false;
  }

  private static boolean isBinary(PsiElement element) {
    final PsiFile containingFile = element.getContainingFile();
    if (containingFile == null || containingFile.getFileType().isBinary()) return true;
    return false;
  }

  public static void decodeFileReferences(PsiElement element) {
    if (element == null || element instanceof PsiCompiledElement || isBinary(element)) return;
    element.accept(new PsiRecursiveElementVisitor(true) {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        Pair<PsiFileSystemItem, Integer> pair = element.getCopyableUserData(REF_FILE_SYSTEM_ITEM_KEY);
        final PsiFileSystemItem item = pair != null ? pair.first : null;
        element.putCopyableUserData(REF_FILE_SYSTEM_ITEM_KEY, null);
        element = bindElement(element, item, pair != null ? pair.second : -1);
        if (element != null) {
          element.acceptChildren(this);
        }

        if (element instanceof PsiLanguageInjectionHost) {
          InjectedLanguageManager.getInstance(element.getProject())
            .enumerate(element, (injectedPsi, places) -> decodeFileReferences(injectedPsi));
        }
      }
    });
  }

  public static void decodeFileReferences(PsiElement element, final Map<String, PsiFileSystemItem> map, final TextRange range) {
    if (element == null || element instanceof PsiCompiledElement || isBinary(element)) return;
    element.accept(new PsiRecursiveElementVisitor(true) {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        if (!range.intersects(element.getTextRange())) return;
        String text = element.getText();
        PsiFileSystemItem item = map.get(text);
        element.putCopyableUserData(REF_FILE_SYSTEM_ITEM_KEY, Pair.create(item, -1));
        element.acceptChildren(this);
      }
    });
    decodeFileReferences(element);
  }

  private static PsiElement bindElement(final PsiElement element, PsiFileSystemItem item, int refIndex) {
    if (item != null && item.isValid() && item.getVirtualFile() != null) {
      PsiReference[] refs = element.getReferences();
      if (refIndex >= 0 && refs.length > refIndex) {
        PsiReference ref = refs[refIndex];
        if (ref instanceof FileReferenceOwner) {
          PsiElement result = bindAndCheckElement(ref, element, item);
          if (result != null) return result;
        }
      }

      for (PsiReference ref : refs) {
        if (ref instanceof FileReferenceOwner) {
          PsiElement result = bindAndCheckElement(ref, element, item);
          if (result != null) return result;
          break;
        }
      }
    }
    return element;
  }

  @Nullable
  private static PsiElement bindAndCheckElement(@NotNull PsiReference ref,
                                                @NotNull PsiElement element,
                                                @NotNull PsiFileSystemItem item) {
    final PsiFileReference fileReference = ((FileReferenceOwner)ref).getLastFileReference();
    if (fileReference != null) {
      try {
        PsiElement newElement = fileReference.bindToElement(item);
        if (newElement != null) {
          // assertion for troubles like IDEA-59540
          LOG.assertTrue(element.getClass() == newElement.getClass(), "Reference " + ref + " violated contract of bindToElement()");
        }
        return newElement;
      }
      catch (IncorrectOperationException e) {
        LOG.error(e);
      }
    }

    return null;
  }
}