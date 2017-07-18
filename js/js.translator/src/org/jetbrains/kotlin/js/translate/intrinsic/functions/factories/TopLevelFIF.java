/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.translate.intrinsic.functions.factories;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.js.backend.ast.JsExpression;
import org.jetbrains.kotlin.js.backend.ast.JsInvocation;
import org.jetbrains.kotlin.js.backend.ast.JsNameRef;
import org.jetbrains.kotlin.js.patterns.DescriptorPredicate;
import org.jetbrains.kotlin.js.patterns.NamePredicate;
import org.jetbrains.kotlin.js.translate.callTranslator.CallInfo;
import org.jetbrains.kotlin.js.translate.callTranslator.CallInfoExtensionsKt;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsic;
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsicWithReceiverComputed;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils;
import org.jetbrains.kotlin.js.translate.utils.UtilsKt;
import org.jetbrains.kotlin.resolve.DescriptorFactory;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.List;
import java.util.Map;

import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.FQ_NAMES;
import static org.jetbrains.kotlin.js.patterns.PatternBuilder.pattern;

public final class TopLevelFIF extends CompositeFIF {
    public static final DescriptorPredicate EQUALS_IN_ANY = pattern("kotlin", "Any", "equals");
    @NotNull
    private static final KotlinFunctionIntrinsic KOTLIN_ANY_EQUALS = new KotlinFunctionIntrinsic("equals") {
        @NotNull
        @Override
        public JsExpression apply(
                @NotNull CallInfo callInfo,
                @NotNull List<? extends JsExpression> arguments, @NotNull TranslationContext context
        ) {
            if (CallInfoExtensionsKt.isSuperInvocation(callInfo)) {
                JsExpression dispatchReceiver = callInfo.getDispatchReceiver();
                assert arguments.size() == 1 && dispatchReceiver != null;
                return JsAstUtils.equality(dispatchReceiver, arguments.get(0));
            }

            return super.apply(callInfo, arguments, context);
        }
    };

    @NotNull
    public static final KotlinFunctionIntrinsic KOTLIN_EQUALS = new KotlinFunctionIntrinsic("equals");

    @NotNull
    private static final KotlinFunctionIntrinsic KOTLIN_SUBSEQUENCE = new KotlinFunctionIntrinsic("subSequence");

    @NotNull
    private static final DescriptorPredicate HASH_CODE_IN_ANY = pattern("kotlin", "Any", "hashCode");
    @NotNull
    private static final KotlinFunctionIntrinsic KOTLIN_HASH_CODE = new KotlinFunctionIntrinsic("hashCode");

    @NotNull
    private static final FunctionIntrinsic RETURN_RECEIVER_INTRINSIC = new FunctionIntrinsicWithReceiverComputed() {
        @NotNull
        @Override
        public JsExpression apply(
                @Nullable JsExpression receiver,
                @NotNull List<? extends JsExpression> arguments,
                @NotNull TranslationContext context
        ) {
            assert receiver != null;
            return receiver;
        }
    };


    private static JsExpression getReferenceToOnlyTypeParameter(
            @NotNull CallInfo callInfo, @NotNull TranslationContext context
    ) {
        ResolvedCall<? extends CallableDescriptor> resolvedCall = callInfo.getResolvedCall();
        Map<TypeParameterDescriptor, KotlinType> typeArguments = resolvedCall.getTypeArguments();

        assert typeArguments.size() == 1;
        KotlinType type = typeArguments.values().iterator().next();

        return UtilsKt.getReferenceToJsClass(type, context);
    }

    private static final FunctionIntrinsic JS_CLASS_FUN_INTRINSIC = new FunctionIntrinsic() {
        @NotNull
        @Override
        public JsExpression apply(
                @NotNull CallInfo callInfo,
                @NotNull List<? extends JsExpression> arguments,
                @NotNull TranslationContext context
        ) {
            return getReferenceToOnlyTypeParameter(callInfo, context);
        }
    };


    private static final FunctionIntrinsic ENUM_VALUES_INTRINSIC = new FunctionIntrinsic() {
        @NotNull
        @Override
        public JsExpression apply(
                @NotNull CallInfo callInfo,
                @NotNull List<? extends JsExpression> arguments,
                @NotNull TranslationContext context
        ) {
            JsExpression enumClassRef = getReferenceToOnlyTypeParameter(callInfo, context);

            FunctionDescriptor fd = DescriptorFactory.createEnumValuesMethod(context.getCurrentModule().getBuiltIns().getEnum());

            return new JsInvocation(new JsNameRef(context.getNameForDescriptor(fd), enumClassRef));
        }
    };


    private static final FunctionIntrinsic ENUM_VALUE_OF_INTRINSIC = new FunctionIntrinsic() {
        @NotNull
        @Override
        public JsExpression apply(
                @NotNull CallInfo callInfo,
                @NotNull List<? extends JsExpression> arguments,
                @NotNull TranslationContext context
        ) {
            JsExpression arg = arguments.get(2); // The first two are reified parameters

            JsExpression enumClassRef = getReferenceToOnlyTypeParameter(callInfo, context);

            FunctionDescriptor fd = DescriptorFactory.createEnumValueOfMethod(context.getCurrentModule().getBuiltIns().getEnum());

            return new JsInvocation(new JsNameRef(context.getNameForDescriptor(fd), enumClassRef), arg);
        }
    };

    private static final FunctionIntrinsic STRING_SUBSTRING = new FunctionIntrinsicWithReceiverComputed() {
        @NotNull
        @Override
        public JsExpression apply(
                @Nullable JsExpression receiver,
               @NotNull List<? extends JsExpression> arguments,
               @NotNull TranslationContext context
        ) {
            return new JsInvocation(new JsNameRef("substring", receiver), arguments);
        }
    };


    @NotNull
    public static final KotlinFunctionIntrinsic TO_STRING = new KotlinFunctionIntrinsic("toString");

    @NotNull
    private static final FunctionIntrinsic CHAR_TO_STRING = new FunctionIntrinsicWithReceiverComputed() {
        @NotNull
        @Override
        public JsExpression apply(
                @Nullable JsExpression receiver, @NotNull List<? extends JsExpression> arguments, @NotNull TranslationContext context
        ) {
            assert receiver != null;
            receiver = TranslationUtils.coerce(context, receiver, context.getCurrentModule().getBuiltIns().getCharType());
            return JsAstUtils.charToString(receiver);
        }
    };


    @NotNull
    public static final FunctionIntrinsicFactory INSTANCE = new TopLevelFIF();

    private TopLevelFIF() {
        add(EQUALS_IN_ANY, KOTLIN_ANY_EQUALS);
        add(pattern("Char.toString"), CHAR_TO_STRING);
        add(pattern("kotlin", "toString").isExtensionOf(FQ_NAMES.any.asString()), TO_STRING);
        add(pattern("kotlin", "equals").isExtensionOf(FQ_NAMES.any.asString()), KOTLIN_EQUALS);
        add(HASH_CODE_IN_ANY, KOTLIN_HASH_CODE);
        add(pattern(NamePredicate.PRIMITIVE_NUMBERS, "equals"), KOTLIN_EQUALS);
        add(pattern("String|Boolean|Char|Number.equals"), KOTLIN_EQUALS);
        add(pattern("String.subSequence"), STRING_SUBSTRING);
        add(pattern("CharSequence.subSequence"), KOTLIN_SUBSEQUENCE);
        add(pattern("kotlin", "iterator").isExtensionOf(FQ_NAMES.iterator.asString()), RETURN_RECEIVER_INTRINSIC);

        add(pattern("kotlin.js", "Json", "get"), ArrayFIF.GET_INTRINSIC);
        add(pattern("kotlin.js", "Json", "set"), ArrayFIF.SET_INTRINSIC);

        add(pattern("kotlin.js", "jsClass"), JS_CLASS_FUN_INTRINSIC);

        add(pattern("kotlin", "enumValues"), ENUM_VALUES_INTRINSIC);
        add(pattern("kotlin", "enumValueOf"), ENUM_VALUE_OF_INTRINSIC);
    }

}
