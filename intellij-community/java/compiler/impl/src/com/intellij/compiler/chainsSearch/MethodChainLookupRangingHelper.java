// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.chainsSearch;

import com.intellij.codeInsight.NullableNotNullManager;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.ExpressionLookupItem;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementDecorator;
import com.intellij.codeInsight.lookup.VariableLookupItem;
import com.intellij.compiler.chainsSearch.completion.lookup.ChainCompletionNewVariableLookupElement;
import com.intellij.compiler.chainsSearch.completion.lookup.JavaRelevantChainLookupElement;
import com.intellij.compiler.chainsSearch.context.ChainCompletionContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.Couple;
import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Collectors;

public class MethodChainLookupRangingHelper {
  @NotNull
  public static LookupElement toLookupElement(OperationChain chain,
                                              ChainCompletionContext context) {
    int unreachableParametersCount = 0;
    int matchedParametersInContext = 0;
    LookupElement chainLookupElement = null;

    for (ChainOperation op : chain.getPath()) {
      if (op instanceof ChainOperation.MethodCall) {
        PsiMethod method = ObjectUtils.notNull(MethodChainsSearchUtil.getMethodWithMinNotPrimitiveParameters(((ChainOperation.MethodCall)op).getCandidates(),
                                                                                                             context.getTarget().getTargetClass()));
        Couple<Integer> info = calculateParameterInfo(method, context);
        unreachableParametersCount += info.getFirst();
        matchedParametersInContext += info.getSecond();

        if (chainLookupElement == null) {
          LookupElement qualifierLookupElement = method.hasModifierProperty(PsiModifier.STATIC) ? null : createQualifierLookupElement(chain.getQualifierClass(), context);
          LookupElement headLookupElement = createMethodLookupElement(method);
          chainLookupElement = qualifierLookupElement == null ? headLookupElement : new JavaChainLookupElement(qualifierLookupElement, headLookupElement);
        } else {
          chainLookupElement = new JavaChainLookupElement(chainLookupElement, new JavaMethodCallElement(method));
        }
      } else {
        if (chainLookupElement == null) {
          chainLookupElement = createQualifierLookupElement(chain.getQualifierClass(), context);
        }
        PsiClass castClass = ((ChainOperation.TypeCast)op).getCastClass();
        PsiClassType type = JavaPsiFacade.getElementFactory(castClass.getProject()).createType(castClass);
        chainLookupElement = PrioritizedLookupElement.withPriority(CastingLookupElementDecorator.createCastingElement(chainLookupElement, type), -1);
      }
    }

    if (context.getTarget().isIteratorAccess()) {
      chainLookupElement = decorateWithIteratorAccess(chain.getFirst()[0], chainLookupElement);
    }

    return new JavaRelevantChainLookupElement(ObjectUtils.notNull(chainLookupElement),
                                              new ChainRelevance(chain.length(), unreachableParametersCount, matchedParametersInContext));
  }

  @NotNull
  private static LookupElementDecorator<LookupElement> decorateWithIteratorAccess(PsiMethod method, LookupElement chainLookupElement) {
    return new LookupElementDecorator<LookupElement>(chainLookupElement) {
      @Override
      public void handleInsert(@NotNull InsertionContext context) {
        super.handleInsert(context);
        Document document = context.getDocument();
        int tail = context.getTailOffset();
        PsiType tailReturnType = method.getReturnType();
        if (tailReturnType instanceof PsiArrayType) {
          document.insertString(tail, "[0]");
          context.getEditor().getCaretModel().moveToOffset(tail + 1);
        } else {
          PsiClass returnClass = ObjectUtils.notNull(PsiUtil.resolveClassInClassTypeOnly(tailReturnType));
          PsiDocumentManager.getInstance(context.getProject()).doPostponedOperationsAndUnblockDocument(document);
          if (InheritanceUtil.isInheritor(returnClass, CommonClassNames.JAVA_UTIL_LIST)) {
            document.insertString(tail, ".get(0)");
            context.getEditor().getCaretModel().moveToOffset(tail + 5);
          } else if (InheritanceUtil.isInheritor(returnClass, CommonClassNames.JAVA_UTIL_COLLECTION)) {
            document.insertString(tail, ".iterator().next()");
          } else if (InheritanceUtil.isInheritor(returnClass, CommonClassNames.JAVA_UTIL_ITERATOR)) {
            document.insertString(tail, ".next()");
          } else if (InheritanceUtil.isInheritor(returnClass, CommonClassNames.JAVA_UTIL_STREAM_STREAM)) {
            document.insertString(tail, ".findFirst().get()");
          }
        }
      }
    };
  }

  @NotNull
  private static LookupElement createQualifierLookupElement(@NotNull PsiClass qualifierClass,
                                                            @NotNull ChainCompletionContext context) {
    PsiNamedElement element = context.getQualifiers(qualifierClass).findFirst().orElse(null);
    if (element == null) {
      return new ChainCompletionNewVariableLookupElement(qualifierClass, context);
    } else {
      if (element instanceof PsiVariable) {
        return new VariableLookupItem((PsiVariable)element);
      }
      else if (element instanceof PsiMethod) {
        return createMethodLookupElement((PsiMethod)element);
      }
      throw new AssertionError("unexpected element: " + element);
    }
  }

  @NotNull
  private static Couple<Integer> calculateParameterInfo(@NotNull PsiMethod method,
                                                        @NotNull ChainCompletionContext context) {
    int unreachableParametersCount = 0;
    int matchedParametersInContext = 0;
    for (PsiParameter parameter : method.getParameterList().getParameters()) {
      PsiType type = parameter.getType();
      if (!ChainCompletionContext.isWidelyUsed(type)) {
        Collection<PsiElement> contextVariables = context.getQualifiers(type).collect(Collectors.toList());
        PsiElement contextVariable = ContainerUtil.getFirstItem(contextVariables, null);
        if (contextVariable != null) {
          matchedParametersInContext++;
          continue;
        }
        if (!NullableNotNullManager.isNullable(parameter)) {
          unreachableParametersCount++;
        }
      }
    }

    return Couple.of(unreachableParametersCount, matchedParametersInContext);
  }

  @NotNull
  private static LookupElement createMethodLookupElement(@NotNull PsiMethod method) {
    LookupElement result;
    if (method.isConstructor()) {
      PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(method.getProject());
      result = new ExpressionLookupItem(elementFactory.createExpressionFromText("new " + method.getContainingClass().getQualifiedName() + "()", null));
    } else if (method.hasModifierProperty(PsiModifier.STATIC)) {
      result = new JavaMethodCallElement(method, false, true);
    } else {
      result = new JavaMethodCallElement(method);
    }
    return result;
  }
}

