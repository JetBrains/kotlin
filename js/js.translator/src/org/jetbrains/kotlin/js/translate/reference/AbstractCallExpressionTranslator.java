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

package org.jetbrains.kotlin.js.translate.reference;

import org.jetbrains.kotlin.js.backend.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.psi.KtCallExpression;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;

public abstract class AbstractCallExpressionTranslator extends AbstractTranslator {

    @NotNull
    protected final KtCallExpression expression;
    @NotNull
    protected final ResolvedCall<? extends FunctionDescriptor> resolvedCall;
    @Nullable
    protected final JsExpression receiver;

    protected AbstractCallExpressionTranslator(
            @NotNull KtCallExpression expression,
            @Nullable JsExpression receiver,
            @NotNull TranslationContext context
    ) {
        super(context);
        this.expression = expression;
        this.resolvedCall = CallUtilKt.getFunctionResolvedCallWithAssert(expression, bindingContext());
        this.receiver = receiver;
    }

}
