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

package org.jetbrains.kotlin.js.translate.test;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.js.backend.ast.*;

public abstract class CommonUnitTester extends JSTester {

    @Override
    public void constructTestMethodInvocation(@NotNull JsExpression functionToTestCall,
            @NotNull JsStringLiteral testName) {
        JsFunction functionToTest = new JsFunction(getContext().scope(), "test function");
        functionToTest.setBody(new JsBlock(functionToTestCall.makeStmt()));
        getBlock().getStatements().add(new JsInvocation(getTestMethodRef(), testName, functionToTest).makeStmt());
    }

    @NotNull
    protected abstract JsExpression getTestMethodRef();
}
