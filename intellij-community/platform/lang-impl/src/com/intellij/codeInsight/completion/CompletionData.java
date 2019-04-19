// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.completion;

import com.intellij.codeInsight.TailType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupItem;
import com.intellij.codeInsight.lookup.LookupValueWithUIHint;
import com.intellij.codeInsight.lookup.PresentableLookupValue;
import com.intellij.codeInsight.template.Template;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.paths.PsiDynaReference;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.CharPattern;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.ObjectPattern;
import com.intellij.psi.*;
import com.intellij.psi.filters.ElementFilter;
import com.intellij.psi.filters.TrueFilter;
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference;
import com.intellij.psi.meta.PsiMetaData;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.patterns.StandardPatterns.not;

/**
 * @deprecated see {@link CompletionContributor}
 */
@Deprecated
public class CompletionData {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.completion.CompletionData");
  public static final ObjectPattern.Capture<Character> NOT_JAVA_ID = not(CharPattern.javaIdentifierPartCharacter());
  private final List<CompletionVariant> myCompletionVariants = new ArrayList<>();

  protected CompletionData(){ }

  private boolean isScopeAcceptable(PsiElement scope){

    for (final CompletionVariant variant : myCompletionVariants) {
      if (variant.isScopeAcceptable(scope)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @deprecated 
   * @see CompletionContributor
   */
  @Deprecated
  protected void registerVariant(CompletionVariant variant){
    myCompletionVariants.add(variant);
  }

  public void completeReference(final PsiReference reference, final Set<? super LookupElement> set, @NotNull final PsiElement position, final PsiFile file) {
    final CompletionVariant[] variants = findVariants(position, file);
    boolean hasApplicableVariants = false;
    for (CompletionVariant variant : variants) {
      if (variant.hasReferenceFilter()) {
        variant.addReferenceCompletions(reference, position, set, file, this);
        hasApplicableVariants = true;
      }
    }

    if (!hasApplicableVariants) {
      myGenericVariant.addReferenceCompletions(reference, position, set, file, this);
    }
  }

  public void addKeywordVariants(Set<? super CompletionVariant> set, PsiElement position, final PsiFile file) {
    ContainerUtil.addAll(set, findVariants(position, file));
  }

  void completeKeywordsBySet(final Set<LookupElement> set, Set<? extends CompletionVariant> variants){
    for (final CompletionVariant variant : variants) {
      variant.addKeywords(set, this);
    }
  }

  public String findPrefix(PsiElement insertedElement, int offsetInFile){
    return findPrefixStatic(insertedElement, offsetInFile);
  }

  public CompletionVariant[] findVariants(final PsiElement position, final PsiFile file){
    final List<CompletionVariant> variants = new ArrayList<>();
      PsiElement scope = position;
      if(scope == null){
        scope = file;
      }
      while (scope != null) {
        boolean breakFlag = false;
        if (isScopeAcceptable(scope)){

          for (final CompletionVariant variant : myCompletionVariants) {
            if (variant.isVariantApplicable(position, scope) && !variants.contains(variant)) {
              variants.add(variant);
              if (variant.isScopeFinal(scope)) {
                breakFlag = true;
              }
            }
          }
        }
        if(breakFlag)
          break;
        scope = scope.getContext();
        if (scope instanceof PsiDirectory) break;
      }
      return variants.toArray(new CompletionVariant[0]);
  }

  protected final CompletionVariant myGenericVariant = new CompletionVariant() {
    @Override
    void addReferenceCompletions(PsiReference reference, PsiElement position, Set<? super LookupElement> set, final PsiFile file,
                                 final CompletionData completionData) {
      completeReference(reference, position, set, TailType.NONE, TrueFilter.INSTANCE, this);
    }
  };

  @Nullable
  public static String getReferencePrefix(@NotNull PsiElement insertedElement, int offsetInFile) {
    try {
      PsiReference ref = insertedElement.getContainingFile().findReferenceAt(offsetInFile);
      if (ref != null) {
        PsiElement element = ref.getElement();
        int offsetInElement = offsetInFile - element.getTextRange().getStartOffset();
        for (TextRange refRange : ReferenceRange.getRanges(ref)) {
          if (refRange.contains(offsetInElement)) {
            int beginIndex = refRange.getStartOffset();
            String text = element.getText();
            if (beginIndex > offsetInElement || beginIndex < 0 || offsetInElement < 0 || offsetInElement > text.length() || beginIndex > text.length()) {
              throw new AssertionError("Inconsistent reference range:" +
                                       " ref=" + ref.getClass() +
                                       " element=" + element.getClass() +
                                       " ref.start=" + refRange.getStartOffset() +
                                       " offset=" + offsetInElement +
                                       " psi.length=" + text.length());
            }
            return text.substring(beginIndex, offsetInElement);
          }
        }
      }
    }
    catch (IndexNotReadyException ignored) {
    }
    return null;
  }

  public static String findPrefixStatic(final PsiElement insertedElement, final int offsetInFile, ElementPattern<Character> prefixStartTrim) {
    if(insertedElement == null) return "";

    final Document document = insertedElement.getContainingFile().getViewProvider().getDocument();
    assert document != null;
    LOG.assertTrue(!PsiDocumentManager.getInstance(insertedElement.getProject()).isUncommited(document), "Uncommitted");

    final String prefix = getReferencePrefix(insertedElement, offsetInFile);
    if (prefix != null) return prefix;

    if (insertedElement.getTextRange().equals(insertedElement.getContainingFile().getTextRange()) || insertedElement instanceof PsiComment) {
      return CompletionUtil.findJavaIdentifierPrefix(insertedElement, offsetInFile);
    }

    return findPrefixDefault(insertedElement, offsetInFile, prefixStartTrim);
  }

  public static String findPrefixStatic(final PsiElement insertedElement, final int offsetInFile) {
    return findPrefixStatic(insertedElement, offsetInFile, NOT_JAVA_ID);
  }

  public static String findPrefixDefault(final PsiElement insertedElement, final int offset, @NotNull final ElementPattern trimStart) {
    String substr = insertedElement.getText().substring(0, offset - insertedElement.getTextRange().getStartOffset());
    if (substr.length() == 0 || Character.isWhitespace(substr.charAt(substr.length() - 1))) return "";

    substr = substr.trim();

    int i = 0;
    while (substr.length() > i && trimStart.accepts(substr.charAt(i))) i++;
    return substr.substring(i).trim();
  }

  public static LookupElement objectToLookupItem(final @NotNull Object object) {
    if (object instanceof LookupElement) return (LookupElement)object;

    String s = null;
    TailType tailType = TailType.NONE;
    if (object instanceof PsiElement){
      s = PsiUtilCore.getName((PsiElement)object);
    }
    else if (object instanceof PsiMetaData) {
      s = ((PsiMetaData)object).getName();
    }
    else if (object instanceof String) {
      s = (String)object;
    }
    else if (object instanceof Template) {
      s = ((Template) object).getKey();
    }
    else if (object instanceof PresentableLookupValue) {
      s = ((PresentableLookupValue)object).getPresentation();
    }
    if (s == null) {
      throw new AssertionError("Null string for object: " + object + " of class " + object.getClass());
    }

    LookupItem item = new LookupItem(object, s);

    if (object instanceof LookupValueWithUIHint && ((LookupValueWithUIHint) object).isBold()) {
      item.setBold();
    }
    item.setAttribute(LookupItem.TAIL_TYPE_ATTR, tailType);
    return item;
  }


  protected void addLookupItem(Set<? super LookupElement> set, TailType tailType, @NotNull Object completion, final CompletionVariant variant) {
    LookupElement ret = objectToLookupItem(completion);
    if (ret == null) return;
    if (!(ret instanceof LookupItem)) {
      set.add(ret);
      return;
    }

    LookupItem item = (LookupItem)ret;

    final InsertHandler insertHandler = variant.getInsertHandler();
    if(insertHandler != null && item.getInsertHandler() == null) {
      item.setInsertHandler(insertHandler);
      item.setTailType(TailType.UNKNOWN);
    }
    else if (tailType != TailType.NONE) {
      item.setTailType(tailType);
    }
    final Map<Object, Object> itemProperties = variant.getItemProperties();
    for (final Object key : itemProperties.keySet()) {
      item.setAttribute(key, itemProperties.get(key));
    }

    set.add(ret);
  }

  protected void completeReference(PsiReference reference, PsiElement position, Set<? super LookupElement> set, TailType tailType, ElementFilter filter, CompletionVariant variant) {
    if (reference instanceof PsiMultiReference) {
      for (PsiReference ref : getReferences((PsiMultiReference)reference)) {
        completeReference(ref, position, set, tailType, filter, variant);
      }
    }
    else if (reference instanceof PsiDynaReference) {
      for (PsiReference ref : ((PsiDynaReference<?>)reference).getReferences()) {
        completeReference(ref, position, set, tailType, filter, variant);
      }
    }
    else{
      final Object[] completions = reference.getVariants();
      for (Object completion : completions) {
        if (completion == null) {
          LOG.error("Position=" + position + "\n;Reference=" + reference + "\n;variants=" + Arrays.toString(completions));
          continue;
        }
        if (completion instanceof PsiElement) {
          final PsiElement psiElement = (PsiElement)completion;
          if (filter.isClassAcceptable(psiElement.getClass()) && filter.isAcceptable(psiElement, position)) {
            addLookupItem(set, tailType, completion, variant);
          }
        }
        else {
          if (completion instanceof LookupItem) {
            final Object o = ((LookupItem)completion).getObject();
            if (o instanceof PsiElement) {
              if (!filter.isClassAcceptable(o.getClass()) || !filter.isAcceptable(o, position)) continue;
            }
          }
          try {
            addLookupItem(set, tailType, completion, variant);
          }
          catch (AssertionError e) {
            LOG.error("Caused by variant from reference: " + reference.getClass(), e);
          }
        }
      }
    }
  }

  protected static PsiReference[] getReferences(final PsiMultiReference multiReference) {
    final PsiReference[] references = multiReference.getReferences();
    final List<PsiReference> hard = ContainerUtil.findAll(references, object -> !object.isSoft());
    if (!hard.isEmpty()) {
      return hard.toArray(PsiReference.EMPTY_ARRAY);
    }
    return references;
  }

  void addKeywords(Set<LookupElement> set, CompletionVariant variant, Object comp, TailType tailType) {
    if (!(comp instanceof String)) return;

    for (final LookupElement item : set) {
      if (item.getObject().toString().equals(comp)) {
        return;
      }
    }
    addLookupItem(set, tailType, comp, variant);
  }
}
