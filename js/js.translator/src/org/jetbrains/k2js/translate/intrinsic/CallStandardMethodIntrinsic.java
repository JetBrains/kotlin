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

package org.jetbrains.k2js.translate.intrinsic;

import com.google.common.collect.Lists;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.translate.context.TranslationContext;

import java.util.List;

import static org.jetbrains.k2js.translate.utils.JsAstUtils.newInvocation;

/**
 * @author Pavel Talanov
 */
public final class CallStandardMethodIntrinsic implements Intrinsic {

    @NotNull
    private final String methodName;

    private final boolean receiverShouldBeNotNull;
    private final int expectedParamsNumber;

    public CallStandardMethodIntrinsic(@NotNull String methodName, boolean receiverShouldBeNotNull, int expectedParamsNumber) {
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
        assert arguments.size() == expectedParamsNumber;
        JsNameRef iteratorFunName = AstUtil.newQualifiedNameRef(methodName);
        return newInvocation(iteratorFunName, composeArguments(receiver, arguments));
    }

    @NotNull
    private static List<JsExpression> composeArguments(@Nullable JsExpression receiver, @NotNull List<JsExpression> arguments) {
        if (receiver != null) {
            List<JsExpression> args = Lists.newArrayList();
            args.add(receiver);
            args.addAll(arguments);
            return args;
        }
        else {
            return arguments;
        }
    }
}