/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.js.backend.ast.*;

import java.util.*;

public class CallCounter extends RecursiveJsVisitor {

    private final List<JsNameRef> callsNameRefs = new ArrayList<>();

    @NotNull
    private final Set<String> exceptFunctionNames;

    @NotNull
    private final Set<String> exceptScopes;

    private int excludedScopeOccurrenceCount;

    @NotNull
    public static CallCounter countCalls(@NotNull JsNode node) {
        return countCalls(node, Collections.emptySet());
    }

    @NotNull
    public static CallCounter countCalls(@NotNull JsNode node, @NotNull Set<String> exceptFunctionNames) {
        CallCounter visitor = new CallCounter(new HashSet<>(exceptFunctionNames), Collections.emptySet());
        node.accept(visitor);

        return visitor;
    }

    @NotNull
    public static CallCounter countCallsWithExcludedScopes(@NotNull JsNode node, @NotNull Set<String> exceptScopes) {
        CallCounter visitor = new CallCounter(Collections.emptySet(), new HashSet<>(exceptScopes));
        node.accept(visitor);

        return visitor;
    }

    private CallCounter(@NotNull Set<String> exceptFunctionNames, @NotNull Set<String> exceptScopes) {
        this.exceptFunctionNames = exceptFunctionNames;
        this.exceptScopes = exceptScopes;
    }

    public int getTotalCallsCount() {
        return callsNameRefs.size();
    }

    public int getQualifiedCallsCount(String... qualifiers) {
        int count = 0;
        List<String> expectedQualifierChain = new ArrayList<>();
        Collections.addAll(expectedQualifierChain, qualifiers);

        for (JsNameRef callNameRef : callsNameRefs) {
            if (matchesQualifiers(callNameRef, expectedQualifierChain)) {
                count++;
            }
        }

        return count;
    }

    public int getUnqualifiedCallsCount(String expectedName) {
        int count = 0;

        for (JsNameRef callNameRef : callsNameRefs) {
            String name = callNameRef.getIdent();
            if (name.equals(expectedName)) {
                count++;
            }
        }

        return count;
    }

    @Override
    public void visitInvocation(@NotNull JsInvocation invocation) {
        super.visitInvocation(invocation);
        JsExpression qualifier = invocation.getQualifier();

        if (qualifier instanceof JsNameRef) {
            JsNameRef nameRef = (JsNameRef) qualifier;
            if (!exceptFunctionNames.contains(nameRef.getIdent())) {
                callsNameRefs.add(nameRef);
            }
        }
    }

    @Override
    public void visitFunction(@NotNull JsFunction x) {
        if (x.getName() != null && exceptScopes.contains(x.getName().getIdent())) {
            excludedScopeOccurrenceCount++;
            return;
        }
        super.visitFunction(x);
    }

    @Override
    public void visitVars(@NotNull JsVars x) {
        for (JsVars.JsVar jsVar : x.getVars()) {
            if (jsVar.getInitExpression() == null) continue;
            if (!exceptScopes.contains(jsVar.getName().getIdent())) {
                accept(jsVar.getInitExpression());
            }
            else {
                excludedScopeOccurrenceCount++;
            }
        }
    }

    private static boolean matchesQualifiers(JsNameRef nameRef, List<String> expectedQualifierChain) {
        JsExpression currentQualifier = nameRef;

        for (String expectedQualifier : expectedQualifierChain) {
            if (!(currentQualifier instanceof JsNameRef)) {
                return false;
            }

            JsNameRef currentNameRef = (JsNameRef) currentQualifier;
            String name = currentNameRef.getIdent();
            if (!name.equals(expectedQualifier)) {
                return false;
            }

            currentQualifier = currentNameRef.getQualifier();
        }

        return true;
    }

    public int getExcludedScopeOccurrenceCount() {
        return excludedScopeOccurrenceCount;
    }
}
