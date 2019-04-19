// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.folding.impl;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.StringTokenizer;

/**
 * Performs {@code 'PSI element <-> signature'} mappings on the basis of unique path of {@link PsiNamedElement PSI named elements}
 * to the PSI root.
 * <p/>
 * Thread-safe.
 * 
 * @author Denis Zhdanov
 */
public class PsiNamesElementSignatureProvider extends AbstractElementSignatureProvider {

  private static final String TYPE_MARKER            = "n";
  private static final String TOP_LEVEL_CHILD_MARKER = "!!top";
  private static final String DOC_COMMENT_MARKER     = "!!doc";
  private static final String CODE_BLOCK_MARKER      = "!!block";
  
  @Override
  protected PsiElement restoreBySignatureTokens(@NotNull PsiFile file,
                                                @NotNull PsiElement parent,
                                                @NotNull final String type,
                                                @NotNull StringTokenizer tokenizer,
                                                @Nullable StringBuilder processingInfoStorage)
  {
    if (!TYPE_MARKER.equals(type)) {
      if (processingInfoStorage != null) {
        processingInfoStorage.append(String.format(
          "Stopping '%s' provider because given signature doesn't have expected type - can work with '%s' but got '%s'%n",
          getClass().getName(), TYPE_MARKER, type
        ));
      }
      return null;
    }
    String elementMarker = tokenizer.nextToken();
    if (TOP_LEVEL_CHILD_MARKER.equals(elementMarker)) {
      PsiElement result = null;
      for (PsiElement child = file.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof PsiWhiteSpace) {
          continue;
        }
        if (result == null) {
          result = child;
        }
        else {
          if (processingInfoStorage != null) {
            processingInfoStorage.append(String.format(
              "Stopping '%s' provider because it has top level marker but more than one non white-space child: %s%n",
              getClass().getName(), Arrays.toString(file.getChildren())
            ));
          }
          // More than one top-level non-white space children. Can't match.
          return null;
        }
      }
      if (processingInfoStorage != null) {
        processingInfoStorage.append(String.format(
          "Finished processing of '%s' provider because all of its top-level children have been processed: %s%n",
          getClass().getName(), Arrays.toString(file.getChildren())
        ));
      }
      return result;
    }
    if (DOC_COMMENT_MARKER.equals(elementMarker)) {
      PsiElement candidate = parent.getFirstChild();
      return candidate instanceof PsiComment ? candidate : null; 
    }
    if (CODE_BLOCK_MARKER.equals(elementMarker)) {
      int index = 0;
      if (tokenizer.hasMoreTokens()) {
        String indexStr = tokenizer.nextToken();
        try {
          index = Integer.parseInt(indexStr);
        }
        catch (NumberFormatException e) {
          if (processingInfoStorage != null) {
            processingInfoStorage.append("Invalid block index: ").append(indexStr).append("\n");
          }
        }
      }
      for (PsiElement child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (isBlockElement(child)) {
          if (--index < 0) {
            return child;
          }
        }
      }
      return null;
    }

    if (!tokenizer.hasMoreTokens()) {
      if (processingInfoStorage != null) {
        processingInfoStorage.append(String.format("Stopping '%s' provider because it has no more data to process%n", getClass().getName()));
      }
      return null;
    }
    try {
      int index = Integer.parseInt(tokenizer.nextToken());
      if (processingInfoStorage != null) {
        processingInfoStorage.append(String.format("Looking for the child with a name '%s' # %d at the element '%s'%n",
                                                   elementMarker, index, parent));
      }
      return restoreElementInternal(parent, unescape(elementMarker), index, PsiNamedElement.class);
    }
    catch (NumberFormatException e) {
      return null;
    }
  }

  @Override
  public String getSignature(@NotNull final PsiElement element) {
    StringBuilder buffer = null;
    for (PsiElement current = element; current != null && !(current instanceof PsiFile); current = current.getParent()) {
      int length = buffer == null ? 0 : buffer.length();
      StringBuilder b = getSignature(current, buffer);
      if (b == null && buffer != null && current.getParent() instanceof PsiFile && canResolveTopLevelChild(current)) {
        buffer.append(TYPE_MARKER).append(ELEMENT_TOKENS_SEPARATOR).append(TOP_LEVEL_CHILD_MARKER).append(ELEMENTS_SEPARATOR);
        break;
      } 
      buffer = b;
      if (buffer == null || length >= buffer.length()) {
        return null;
      }
      buffer.append(ELEMENTS_SEPARATOR);
    }

    if (buffer == null) {
      return null;
    } 
    
    buffer.setLength(buffer.length() - 1);
    return buffer.toString();
  }

  /**
   * Allows to answer if it's possible to use {@link #TOP_LEVEL_CHILD_MARKER} for the given element.
   * 
   * @param element  element to check
   * @return         {@code true} if {@link #TOP_LEVEL_CHILD_MARKER} can be used for the given element; {@code false} otherwise
   */
  private static boolean canResolveTopLevelChild(@NotNull PsiElement element) {
    final PsiElement parent = element.getParent();
    if (parent == null) {
      return false;
    }
    for (PsiElement child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof PsiWhiteSpace) {
        continue;
      }
      if (child != element) {
        return false;
      } 
    }
    return true;
  }
  
  /**
   * Tries to produce signature for the exact given PSI element.
   * 
   * @param element  target element
   * @param buffer   buffer to store the signature in
   * @return         buffer that contains signature of the given element if it was produced;
   *                 {@code null} as an indication that signature for the given element was not produced
   */
  @Nullable
  private static StringBuilder getSignature(@NotNull PsiElement element, @Nullable StringBuilder buffer) {
    if (element instanceof PsiNamedElement) {
      PsiNamedElement named = (PsiNamedElement)element;
      final String name = named.getName();
      if (StringUtil.isEmpty(name)) {
        return null;
      }
      int index = getChildIndex(named, element.getParent(), name, PsiNamedElement.class);
      if (index < 0) return null;
      StringBuilder bufferToUse = buffer;
      if (bufferToUse == null) {
        bufferToUse = new StringBuilder();
      }
      bufferToUse.append(TYPE_MARKER).append(ELEMENT_TOKENS_SEPARATOR).append(escape(name))
        .append(ELEMENT_TOKENS_SEPARATOR).append(index);
      return bufferToUse;
    }
    if (element instanceof PsiComment) {
      PsiElement parent = element.getParent();
      boolean nestedComment = false;
      if (parent instanceof PsiComment && parent.getTextRange().equals(element.getTextRange())) {
        parent = parent.getParent();
        nestedComment = true;
      }
      if (parent instanceof PsiNamedElement && (nestedComment || parent.getFirstChild() == element)) {
        // Consider doc comment element that is the first child of named element to be a doc comment.
        StringBuilder bufferToUse = buffer;
        if (bufferToUse == null) {
          bufferToUse = new StringBuilder();
        }
        bufferToUse.append(TYPE_MARKER).append(ELEMENT_TOKENS_SEPARATOR).append(DOC_COMMENT_MARKER);
        return bufferToUse;
      }
    }

    PsiElement parent = element.getParent();
    if (parent instanceof PsiNamedElement && !(parent instanceof PsiFile)) {
      if (isBlockElement(element)) {
        int index = getBlockElementIndex(element);
        StringBuilder bufferToUse = buffer;
        if (bufferToUse == null) {
          bufferToUse = new StringBuilder();
        }
        bufferToUse.append(TYPE_MARKER).append(ELEMENT_TOKENS_SEPARATOR).append(CODE_BLOCK_MARKER);
        if (index > 0) {
          bufferToUse.append(ELEMENT_TOKENS_SEPARATOR).append(index);
        }
        return bufferToUse;
      }
    } 
     
    return null;
  }

  private static boolean isBlockElement(@NotNull PsiElement element) {
    PsiElement firstChild = element.getFirstChild();
    PsiElement lastChild = element.getLastChild();
    return firstChild != null && "{".equals(firstChild.getText()) && lastChild != null && "}".equals(lastChild.getText());
  }

  private static int getBlockElementIndex(@NotNull PsiElement element) {
    int i = 0;
    for (PsiElement sibling : element.getParent().getChildren()) {
      if (element.equals(sibling)) {
        return i;
      }
      if (isBlockElement(sibling)) {
        i++;
      }
    }
    throw new RuntimeException("Malformed PSI");
  }
}
