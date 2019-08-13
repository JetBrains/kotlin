// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.chainsSearch;

import com.intellij.compiler.chainsSearch.context.ChainCompletionContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


public class OperationChain {
  private static final Logger LOG = Logger.getInstance(OperationChain.class);

  @NotNull
  private final ChainOperation[] myReverseOperations;
  private final RefChainOperation myHeadOperation;
  private final MethodCall myHeadMethodCall;
  private final int myWeight;
  private final PsiClass myQualifierClass;

  @Nullable
  public static OperationChain create(@NotNull RefChainOperation operation,
                                      int weight,
                                      @NotNull ChainCompletionContext context) {
    if (operation instanceof MethodCall) {
      MethodCall signature = (MethodCall) operation;
      PsiClass qualifier = context.resolvePsiClass(signature.getQualifierDef());
      if (qualifier == null || (!signature.isStatic() && InheritanceUtil.isInheritorOrSelf(context.getTarget().getTargetClass(), qualifier, true))) {
        return null;
      }
      PsiMethod[] methods = context.resolve(signature);
      if (methods.length == 0) return null;
      Set<PsiClass> classes = Arrays.stream(methods)
        .flatMap(m -> Arrays.stream(m.getParameterList().getParameters()))
        .map(p -> PsiUtil.resolveClassInType(p.getType()))
        .collect(Collectors.toSet());
      PsiClass contextClass = context.getTarget().getTargetClass();
      if (classes.contains(contextClass)) {
        return null;
      }
      classes.add(contextClass);
      return new OperationChain(qualifier, new ChainOperation[] {new ChainOperation.MethodCall(methods)}, signature, signature, weight);
    }
    else {
      TypeCast cast = (TypeCast)operation;
      PsiClass operand = context.resolvePsiClass(cast.getCompilerRef());
      PsiClass castType = context.resolvePsiClass(cast.getCastTypeRef());
      if (operand == null || castType == null) return null;
      return new OperationChain(operand, new ChainOperation[] {new ChainOperation.TypeCast(operand, castType)}, cast, null, weight);
    }
  }

  private OperationChain(@NotNull PsiClass qualifierClass,
                        @NotNull ChainOperation[] reverseOperations,
                        RefChainOperation signature,
                        MethodCall headMethodSign,
                        int weight) {
    myQualifierClass = qualifierClass;
    myReverseOperations = reverseOperations;
    myHeadOperation = signature;
    myHeadMethodCall = headMethodSign;
    myWeight = weight;
  }

  public boolean hasCast() {
    return Arrays.stream(myReverseOperations).anyMatch(op -> op instanceof ChainOperation.TypeCast);
  }

  @Nullable
  public MethodCall getHeadMethodCall() {
    return myHeadMethodCall;
  }

  @NotNull
  public RefChainOperation getHead() {
    return myHeadOperation;
  }

  public int length() {
    return myReverseOperations.length;
  }

  public PsiClass getQualifierClass() {
    return myQualifierClass;
  }

  @NotNull
  public PsiMethod[] getFirst() {
    return ((ChainOperation.MethodCall) myReverseOperations[0]).getCandidates();
  }

  public ChainOperation[] getPath() {
    return ArrayUtil.reverseArray(myReverseOperations);
  }

  public int getChainWeight() {
    return myWeight;
  }

  @Nullable
  OperationChain continuationWithMethod(@NotNull MethodCall signature,
                                        int weight,
                                        @NotNull ChainCompletionContext context) {
    OperationChain head = create(signature, weight, context);
    if (head == null) return null;

    ChainOperation[] newReverseOperations = new ChainOperation[length() + 1];
    System.arraycopy(myReverseOperations, 0, newReverseOperations, 0, myReverseOperations.length);
    newReverseOperations[length()] = head.getPath()[0];
    return new OperationChain(head.getQualifierClass(), newReverseOperations, head.getHead() , signature, Math.min(weight, getChainWeight()));
  }

  @Nullable
  OperationChain continuationWithCast(@NotNull TypeCast cast,
                                      @NotNull ChainCompletionContext context) {
    OperationChain head = create(cast, 0, context);
    if (head == null) return null;
    ChainOperation[] newReverseOperations = new ChainOperation[length() + 1];
    System.arraycopy(myReverseOperations, 0, newReverseOperations, 0, myReverseOperations.length);
    newReverseOperations[length()] = head.getPath()[0];
    return new OperationChain(head.getQualifierClass(), newReverseOperations, head.getHead(), myHeadMethodCall, getChainWeight());
  }

  @NotNull
  OperationChain removeHeadCast(@NotNull ChainCompletionContext context) {
    LOG.assertTrue(getHead() instanceof TypeCast);
    ChainOperation[] newReverseOperations = new ChainOperation[length() - 1];
    System.arraycopy(myReverseOperations, 0, newReverseOperations, 0, length() - 1);
    return new OperationChain(Objects.requireNonNull(context.resolvePsiClass(myHeadMethodCall.getQualifierDef())),
                              newReverseOperations,
                              myHeadMethodCall,
                              myHeadMethodCall,
                              getChainWeight());
  }

  @Override
  public String toString() {
    ChainOperation[] path = getPath();
    return Arrays.toString(path) + " on " + myQualifierClass.getName();
  }

  public static CompareResult compare(@NotNull OperationChain left, @NotNull OperationChain right) {
    if (left.length() == 0 || right.length() == 0) {
      throw new IllegalStateException("chains can't be empty");
    }

    int leftCurrentIdx = 0;
    int rightCurrentIdx = 0;

    while (leftCurrentIdx < left.length() && rightCurrentIdx < right.length()) {
      ChainOperation thisNext = left.myReverseOperations[leftCurrentIdx];
      ChainOperation thatNext = right.myReverseOperations[leftCurrentIdx];
      if (!lookSimilar(thisNext, thatNext)) {
        return CompareResult.NOT_EQUAL;
      }
      leftCurrentIdx++;
      rightCurrentIdx++;
    }
    if (leftCurrentIdx < left.length() && rightCurrentIdx == right.length()) {
      return CompareResult.LEFT_CONTAINS_RIGHT;
    }
    if (leftCurrentIdx == left.length() && rightCurrentIdx < right.length()) {
      return CompareResult.RIGHT_CONTAINS_LEFT;
    }

    return CompareResult.EQUAL;
  }

  public enum CompareResult {
    LEFT_CONTAINS_RIGHT,
    RIGHT_CONTAINS_LEFT,
    EQUAL,
    NOT_EQUAL
  }

  static boolean lookSimilar(ChainOperation op1, ChainOperation op2) {
    if (op1 instanceof ChainOperation.TypeCast || op2 instanceof ChainOperation.TypeCast) return false;

    PsiMethod[] methods1 = ((ChainOperation.MethodCall)op1).getCandidates();
    PsiMethod[] methods2 = ((ChainOperation.MethodCall)op2).getCandidates();

    PsiMethod repr1 = methods1[0];
    PsiMethod repr2 = methods2[0];
    if (repr1.hasModifierProperty(PsiModifier.STATIC) || repr2.hasModifierProperty(PsiModifier.STATIC)) return false;
    if (!repr1.getName().equals(repr2.getName()) ||
        repr1.getParameterList().getParametersCount() != repr2.getParameterList().getParametersCount()) {
      return false;
    }
    Set<PsiMethod> methodSet1 = ContainerUtil.newHashSet(methods1);
    Set<PsiMethod> methodSet2 = ContainerUtil.newHashSet(methods2);
    if (ContainerUtil.intersects(methodSet1, methodSet2)) return true;

    Set<PsiMethod> deepestSupers1 = methodSet1.stream().flatMap(m -> Arrays.stream(m.findDeepestSuperMethods())).collect(Collectors.toSet());
    return methodSet2.stream().flatMap(m -> Arrays.stream(m.findDeepestSuperMethods())).anyMatch(deepestSupers1::contains);
  }
}
