/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.test.utils;

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
