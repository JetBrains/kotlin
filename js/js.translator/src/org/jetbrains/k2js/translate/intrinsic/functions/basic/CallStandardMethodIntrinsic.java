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

package org.jetbrains.k2js.translate.intrinsic.functions.basic;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.translate.context.TranslationContext;

import java.util.List;

import static org.jetbrains.k2js.translate.utils.ErrorReportingUtils.atLocation;

/**
 * @author Pavel Talanov
 */
public final class CallStandardMethodIntrinsic extends FunctionIntrinsic {
    @NotNull
    private final JsNameRef methodName;

    private final boolean receiverShouldBeNotNull;
    private final int expectedParamsNumber;

    public CallStandardMethodIntrinsic(@NotNull JsNameRef methodName, boolean receiverShouldBeNotNull, int expectedParamsNumber) {
        this.methodName = methodName;
        this.receiverShouldBeNotNull = receiverShouldBeNotNull;
        this.expectedParamsNumber = expectedParamsNumber;
    }

    @NotNull
    @Override
    public JsExpression apply(@Nullable JsExpression receiver,
            @NotNull List<JsExpression> arguments,
            @NotNull TranslationContext context) {
        assert (receiver != null == receiverShouldBeNotNull);
        assert arguments.size() == expectedParamsNumber : errorMessage(receiver, arguments);
        return new JsInvocation(methodName, composeArguments(receiver, arguments));
    }

    @NotNull
    private String errorMessage(@Nullable JsExpression receiver, @NotNull List<JsExpression> arguments) {
        return "Incorrect number of arguments " + arguments.size() + " when expected " + expectedParamsNumber + " on method " + methodName + " " +
               atLocation(receiver, arguments);
    }

    @NotNull
    private static List<JsExpression> composeArguments(@Nullable JsExpression receiver, @NotNull List<JsExpression> arguments) {
        if (receiver != null) {
            List<JsExpression> args = new SmartList<JsExpression>(receiver);
            args.addAll(arguments);
            return args;
        }
        else {
            return arguments;
        }
    }
}