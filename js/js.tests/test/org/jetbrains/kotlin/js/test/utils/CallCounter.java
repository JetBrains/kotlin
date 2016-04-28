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

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CallCounter extends RecursiveJsVisitor {

    private final List<JsNameRef> callsNameRefs = new ArrayList<JsNameRef>();

    @NotNull
    private final Set<String> exceptFunctionNames;

    @NotNull
    public static CallCounter countCalls(@NotNull JsNode node) {
        return countCalls(node, Collections.<String>emptySet());
    }

    @NotNull
    public static CallCounter countCalls(@NotNull JsNode node, @NotNull Set<String> exceptFunctionNames) {
        CallCounter visitor = new CallCounter(new HashSet<String>(exceptFunctionNames));
        node.accept(visitor);

        return visitor;
    }

    private CallCounter(@NotNull Set<String> exceptFunctionNames) {
        this.exceptFunctionNames = exceptFunctionNames;
    }

    public int getTotalCallsCount() {
        return callsNameRefs.size();
    }

    public int getQualifiedCallsCount(String... qualifiers) {
        int count = 0;
        List<String> expectedQualifierChain = new ArrayList<String>();
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
            JsName name = callNameRef.getName();
            if (name != null && name.getIdent().equals(expectedName)) {
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

    private static boolean matchesQualifiers(JsNameRef nameRef, List<String> expectedQualifierChain) {
        JsExpression currentQualifier = nameRef;

        for (String expectedQualifier : expectedQualifierChain) {
            if (!(currentQualifier instanceof JsNameRef)) {
                return false;
            }

            JsNameRef currentNameRef = (JsNameRef) currentQualifier;
            JsName name = currentNameRef.getName();
            if (name == null || !name.getIdent().equals(expectedQualifier)) {
                return false;
            }

            currentQualifier = currentNameRef.getQualifier();
        }

        return true;
    }
}
