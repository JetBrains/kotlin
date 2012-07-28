/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.test;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsFunction;
import com.google.dart.compiler.backend.js.ast.JsStringLiteral;
import org.jetbrains.annotations.NotNull;

import static com.google.dart.compiler.util.AstUtil.newBlock;
import static com.google.dart.compiler.util.AstUtil.newInvocation;

/**
 * @author Pavel Talanov
 */
public abstract class CommonUnitTester extends JSTester {

    @Override
    public void constructTestMethodInvocation(@NotNull JsExpression functionToTestCall,
            @NotNull JsStringLiteral testName) {
        JsFunction functionToTest = new JsFunction(getContext().jsScope());
        functionToTest.setBody(newBlock(functionToTestCall.makeStmt()));
        getBlock().getStatements().add(newInvocation(getTestMethodRef(), testName, functionToTest).makeStmt());
    }

    @NotNull
    protected abstract JsExpression getTestMethodRef();
}
