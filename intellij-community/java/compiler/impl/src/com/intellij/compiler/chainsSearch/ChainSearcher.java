// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.chainsSearch;

import com.intellij.compiler.backwardRefs.CompilerReferenceServiceEx;
import com.intellij.compiler.chainsSearch.context.ChainCompletionContext;
import com.intellij.compiler.chainsSearch.context.ChainSearchTarget;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.util.containers.IntStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.backwardRefs.CompilerRef;
import org.jetbrains.jps.backwardRefs.SignatureData;

import java.util.*;

public class ChainSearcher {
  @NotNull
  public static List<OperationChain> search(int pathMaximalLength,
                                            ChainSearchTarget searchTarget,
                                            int maxResultSize,
                                            ChainCompletionContext context,
                                            CompilerReferenceServiceEx compilerReferenceServiceEx) {
    SearchInitializer initializer = createInitializer(searchTarget, compilerReferenceServiceEx, context);
    return search(compilerReferenceServiceEx, initializer, pathMaximalLength, maxResultSize, context);
  }

  @NotNull
  private static SearchInitializer createInitializer(ChainSearchTarget target,
                                                     CompilerReferenceServiceEx referenceServiceEx,
                                                     ChainCompletionContext context) {
    SortedSet<ChainOpAndOccurrences<? extends RefChainOperation>> operations = new TreeSet<>();
    for (byte kind : target.getArrayKind()) {
      SortedSet<ChainOpAndOccurrences<MethodCall>> methods = referenceServiceEx.findMethodReferenceOccurrences(target.getClassQName(), kind, context);
      operations.addAll(methods);
    }

    if (operations.isEmpty()) {
      ChainOpAndOccurrences<TypeCast> typeCast = referenceServiceEx.getMostUsedTypeCast(target.getClassQName());
      if (typeCast != null) {
        operations.add(typeCast);
      }
    }

    return new SearchInitializer(operations, context);
  }

  @NotNull
  private static List<OperationChain> search(CompilerReferenceServiceEx referenceServiceEx,
                                             SearchInitializer initializer,
                                             int chainMaxLength,
                                             int maxResultSize,
                                             ChainCompletionContext context) {
    LinkedList<OperationChain> q = initializer.getChainQueue();

    List<OperationChain> result = new ArrayList<>();
    while (!q.isEmpty()) {

      OperationChain currentChain = q.poll();
      ProgressManager.checkCanceled();
      RefChainOperation head = currentChain.getHead();

      if (addChainIfTerminal(currentChain, result, chainMaxLength, context)) continue;

      // otherwise try to find chain continuation
      boolean updated = false;
      SortedSet<ChainOpAndOccurrences<MethodCall>> candidates = referenceServiceEx.findMethodReferenceOccurrences(head.getQualifierRawName(), SignatureData.ZERO_DIM, context);
      CompilerRef ref = head.getCompilerRef();
      for (ChainOpAndOccurrences<MethodCall> candidate : candidates) {
        if (candidate.getOccurrenceCount() * ChainSearchMagicConstants.FILTER_RATIO < currentChain.getChainWeight()) {
          break;
        }
        MethodCall sign = candidate.getOperation();
        if ((sign.isStatic() || !sign.getQualifierRawName().equals(context.getTarget().getClassQName())) &&
            (!(ref instanceof CompilerRef.JavaCompilerMethodRef) ||
             referenceServiceEx.mayHappen(candidate.getOperation().getCompilerRef(), ref, ChainSearchMagicConstants.METHOD_PROBABILITY_THRESHOLD))) {

          OperationChain
            continuation = currentChain.continuationWithMethod(candidate.getOperation(), candidate.getOccurrenceCount(), context);
          if (continuation != null) {
            boolean stopChain =
              candidate.getOperation().isStatic() || context.hasQualifier(context.resolvePsiClass(candidate.getOperation().getQualifierDef()));
            if (stopChain) {
              addChainIfNotPresent(continuation, result);
            }
            else {
              q.addFirst(continuation);
            }
            updated = true;
          }
        }
      }

      if (ref instanceof CompilerRef.JavaCompilerMethodRef) {
        CompilerRef.CompilerClassHierarchyElementDef def =
          referenceServiceEx.mayCallOfTypeCast((CompilerRef.JavaCompilerMethodRef)ref, ChainSearchMagicConstants.METHOD_PROBABILITY_THRESHOLD);
        if (def != null) {
          OperationChain
            continuation = currentChain.continuationWithCast(new TypeCast(def, head.getQualifierDef(), referenceServiceEx), context);
          if (continuation != null) {
            q.addFirst(continuation);
            updated = true;
          }
        }
      }

      if (!updated) {
        addChainIfQualifierCanBeOccurredInContext(currentChain, result, context, referenceServiceEx);
      }

      if (result.size() > maxResultSize) {
        return result;
      }
    }
    return result;
  }

  /**
   * To reduce false-positives we add a method to result only if its qualifier can be occurred together with context variables.
   */
  private static void addChainIfQualifierCanBeOccurredInContext(OperationChain currentChain,
                                                                List<OperationChain> result,
                                                                ChainCompletionContext context,
                                                                CompilerReferenceServiceEx referenceServiceEx) {
    RefChainOperation signature = currentChain.getHead();
    // type cast + introduced qualifier: it's too complex chain
    if (currentChain.hasCast()) return;
    if (!context.getTarget().getClassQName().equals(signature.getQualifierRawName())) {
      Set<CompilerRef> references = context.getContextClassReferences();
      boolean isRelevantQualifier = false;
      for (CompilerRef ref: references) {
        if (referenceServiceEx.mayHappen(signature.getQualifierDef(), ref, ChainSearchMagicConstants.VAR_PROBABILITY_THRESHOLD)) {
          isRelevantQualifier = true;
          break;
        }
      }

      if (references.isEmpty() || isRelevantQualifier) {
        addChainIfNotPresent(currentChain, result);
      }
    }
  }

  private static boolean addChainIfTerminal(OperationChain currentChain, List<OperationChain> result,
                                            int pathMaximalLength,
                                            ChainCompletionContext context) {
    MethodCall signature = currentChain.getHeadMethodCall();
    if (signature == null) return false;
    RefChainOperation head = currentChain.getHead();
    if (signature.isStatic() || context.hasQualifier(context.resolvePsiClass(head.getQualifierDef()))) {
      addChainIfNotPresent(currentChain, result);
      return true;
    }
    else if (currentChain.length() >= pathMaximalLength) {
      addChainIfNotPresent(currentChain.getHead() == signature ? currentChain : currentChain.removeHeadCast(context), result);
      return true;
    }
    return false;
  }

  private static void addChainIfNotPresent(OperationChain newChain, List<OperationChain> result) {
    if (result.isEmpty()) {
      result.add(newChain);
      return;
    }
    boolean doAdd = true;
    IntStack indicesToRemove = new IntStack();
    for (int i = 0; i < result.size(); i++) {
      OperationChain chain = result.get(i);
      OperationChain.CompareResult r = OperationChain.compare(chain, newChain);
      switch (r) {
        case LEFT_CONTAINS_RIGHT:
          indicesToRemove.push(i);
          break;
        case RIGHT_CONTAINS_LEFT:
        case EQUAL:
          doAdd = false;
          break;
        case NOT_EQUAL:
          break;
      }
    }
    while (!indicesToRemove.empty()) {
      result.remove(indicesToRemove.pop());
    }
    if (doAdd) {
      result.add(newChain);
    }
  }
}
