/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.jetbrains.k2js.inline;

import com.google.dart.compiler.backend.js.ast.*;

import java.util.*;

class FunctionInlineMutator {
    private static final String RESULT_LABEL = "result_inlined";

    private final JsInvocation call;
    private final JsScope callerScope;
    private final JsFunction invokedFunction;
    private final List<JsStatement> statements = new ArrayList<JsStatement>();
    private final IdentityHashMap<JsName, JsNameRef> renameMap = new IdentityHashMap<JsName, JsNameRef>();

    public static InlineableResult getInlineableCallReplacement(
            JsInvocation call,
            JsScope callerScope,
            JsFunction invokedFunction
    ) {
        return (new FunctionInlineMutator(call, callerScope, invokedFunction)).process();
    }

    private FunctionInlineMutator(JsInvocation call, JsScope callerScope, JsFunction invokedFunction) {
        this.call = call;
        this.callerScope = callerScope;
        this.invokedFunction = invokedFunction;
    }

    private InlineableResult process() {
        JsBlock inlinedBody = new JsBlock(statements);
        JsNameRef result = callerScope.declareName(RESULT_LABEL).makeRef();
        statements.addAll(invokedFunction.getBody().getStatements());
        return new InlineableResult(inlinedBody, result);
    }
}
