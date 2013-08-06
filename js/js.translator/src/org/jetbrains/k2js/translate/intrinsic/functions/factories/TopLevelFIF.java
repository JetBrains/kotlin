/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.intrinsic.functions.factories;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.BuiltInFunctionIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.CallStandardMethodIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.FunctionIntrinsic;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.DescriptorPredicate;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.NamePredicate;
import org.jetbrains.k2js.translate.reference.CallTranslator;

import java.util.List;

import static org.jetbrains.k2js.translate.intrinsic.functions.basic.FunctionIntrinsic.CallParametersAwareFunctionIntrinsic;
import static org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder.pattern;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.generateCallArgumentList;

public final class TopLevelFIF extends CompositeFIF {
    @NotNull
    public static final CallStandardMethodIntrinsic EQUALS = new CallStandardMethodIntrinsic(new JsNameRef("equals", "Kotlin"), true, 1);
    @NotNull
    private static final FunctionIntrinsic RETURN_RECEIVER_INTRINSIC = new FunctionIntrinsic() {
        @NotNull
        @Override
        public JsExpression apply(
                @Nullable JsExpression receiver,
                @NotNull List<JsExpression> arguments,
                @NotNull TranslationContext context
        ) {
            assert receiver != null;
            return receiver;
        }
    };
    @NotNull
    public static final FunctionIntrinsicFactory INSTANCE = new TopLevelFIF();

    private TopLevelFIF() {
        add(pattern("jet", "toString").receiverExists(true), new BuiltInFunctionIntrinsic("toString"));
        add(pattern("jet", "equals").receiverExists(true), EQUALS);
        add(pattern(NamePredicate.PRIMITIVE_NUMBERS, "equals"), EQUALS);
        add(pattern("String|Boolean|Char|Number.equals"), EQUALS);
        add(pattern("jet", "arrayOfNulls"), new CallStandardMethodIntrinsic(new JsNameRef("nullArray", "Kotlin"), false, 1));
        add(pattern("jet", "iterator").receiverExists(true), RETURN_RECEIVER_INTRINSIC);
        add(new DescriptorPredicate() {
                @Override
                public boolean apply(@NotNull FunctionDescriptor descriptor) {
                    if (!descriptor.getName().asString().equals("invoke")) {
                        return false;
                    }
                    int parameterCount = descriptor.getValueParameters().size();
                    DeclarationDescriptor fun = descriptor.getContainingDeclaration();
                    return fun == (descriptor.getReceiverParameter() == null
                                   ? KotlinBuiltIns.getInstance().getFunction(parameterCount)
                                   : KotlinBuiltIns.getInstance().getExtensionFunction(parameterCount));
                }
            }, new CallParametersAwareFunctionIntrinsic() {
                @NotNull
                @Override
                public JsExpression apply(@NotNull CallTranslator callTranslator, @NotNull List<JsExpression> arguments, @NotNull TranslationContext context) {
                    JsExpression thisExpression = callTranslator.getCallParameters().getThisObject();
                    if (thisExpression == null) {
                        return new JsInvocation(callTranslator.getCallParameters().getFunctionReference(), arguments);
                    }
                    else if (callTranslator.getResolvedCall().getReceiverArgument().exists()) {
                        return callTranslator.extensionFunctionCall(false);
                    }
                    else {
                        return new JsInvocation(new JsNameRef("call", callTranslator.getCallParameters().getFunctionReference()),
                                                generateCallArgumentList(thisExpression, arguments));
                    }
                }
            }
        );

        add(pattern("java", "util", "set").receiverExists(true), new FunctionIntrinsic() {
            @NotNull
            @Override
            public JsExpression apply(
                    @Nullable JsExpression receiver, @NotNull List<JsExpression> arguments, @NotNull TranslationContext context
            ) {
                return new JsInvocation(new JsNameRef("put", receiver), arguments);
            }
        });
    }
}
