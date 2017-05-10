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

package org.jetbrains.kotlin.js.translate.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.LocalVariableAccessorDescriptor;
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.TemporaryConstVariable;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.expression.InlineMetadata;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.kotlin.serialization.deserialization.FindClassInModuleKt;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeUtils;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.kotlin.js.backend.ast.JsBinaryOperator.*;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getCallableDescriptorForOperationExpression;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.assignment;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.createDataDescriptor;

public final class TranslationUtils {

    private TranslationUtils() {
    }

    @NotNull
    public static JsPropertyInitializer translateFunctionAsEcma5PropertyDescriptor(@NotNull JsFunction function,
            @NotNull FunctionDescriptor descriptor,
            @NotNull TranslationContext context) {
        JsExpression functionExpression = function;
        if (InlineUtil.isInline(descriptor)) {
            InlineMetadata metadata = InlineMetadata.compose(function, descriptor, context.getConfig());
            functionExpression = metadata.getFunctionWithMetadata();
        }

        if (DescriptorUtils.isExtension(descriptor) ||
            descriptor instanceof PropertyAccessorDescriptor &&
            shouldAccessViaFunctions(((PropertyAccessorDescriptor) descriptor).getCorrespondingProperty())
        ) {
            return translateExtensionFunctionAsEcma5DataDescriptor(functionExpression, descriptor, context);
        }
        else {
            JsStringLiteral getOrSet = context.program().getStringLiteral(getAccessorFunctionName(descriptor));
            return new JsPropertyInitializer(getOrSet, functionExpression);
        }
    }

    @NotNull
    private static String getAccessorFunctionName(@NotNull FunctionDescriptor descriptor) {
        boolean isGetter = descriptor instanceof PropertyGetterDescriptor || descriptor instanceof LocalVariableAccessorDescriptor.Getter;
        return isGetter ? "get" : "set";
    }

    @NotNull
    public static JsFunction simpleReturnFunction(@NotNull JsScope functionScope, @NotNull JsExpression returnExpression) {
        return new JsFunction(functionScope, new JsBlock(new JsReturn(returnExpression)), "<simpleReturnFunction>");
    }

    @NotNull
    private static JsPropertyInitializer translateExtensionFunctionAsEcma5DataDescriptor(@NotNull JsExpression functionExpression,
            @NotNull FunctionDescriptor descriptor, @NotNull TranslationContext context) {
        JsObjectLiteral meta = createDataDescriptor(functionExpression, ModalityKt.isOverridable(descriptor), false);
        return new JsPropertyInitializer(context.getNameForDescriptor(descriptor).makeRef(), meta);
    }

    @NotNull
    public static JsExpression translateExclForBinaryEqualLikeExpr(@NotNull JsBinaryOperation baseBinaryExpression) {
        JsBinaryOperator negatedOperator = notOperator(baseBinaryExpression.getOperator());
        assert negatedOperator != null : "Can't negate operator: " + baseBinaryExpression.getOperator();
        return new JsBinaryOperation(negatedOperator, baseBinaryExpression.getArg1(), baseBinaryExpression.getArg2());
    }

    public static boolean isEqualLikeOperator(@NotNull JsBinaryOperator operator) {
        return notOperator(operator) != null;
    }

    @Nullable
    private static JsBinaryOperator notOperator(@NotNull JsBinaryOperator operator) {
        switch (operator) {
            case REF_EQ:
                return REF_NEQ;
            case REF_NEQ:
                return REF_EQ;
            case EQ:
                return NEQ;
            case NEQ:
                return EQ;
            default:
                return null;
        }
    }

    @NotNull
    public static JsBinaryOperation isNullCheck(@NotNull JsExpression expressionToCheck) {
        return nullCheck(expressionToCheck, false);
    }

    @NotNull
    private static JsBinaryOperation isNotNullCheck(@NotNull JsExpression expressionToCheck) {
        return nullCheck(expressionToCheck, true);
    }

    @NotNull
    public static JsBinaryOperation nullCheck(@NotNull JsExpression expressionToCheck, boolean isNegated) {
        JsBinaryOperator operator = isNegated ? JsBinaryOperator.NEQ : JsBinaryOperator.EQ;
        return new JsBinaryOperation(operator, expressionToCheck, JsLiteral.NULL);
    }

    @NotNull
    public static JsConditional notNullConditional(
            @NotNull JsExpression expression,
            @NotNull JsExpression elseExpression,
            @NotNull TranslationContext context
    ) {
        JsExpression testExpression;
        JsExpression thenExpression;
        if (isCacheNeeded(expression)) {
            TemporaryConstVariable tempVar = context.getOrDeclareTemporaryConstVariable(expression);
            testExpression = isNotNullCheck(tempVar.value());
            thenExpression = tempVar.value();
        }
        else {
            testExpression = isNotNullCheck(expression);
            thenExpression = expression;
        }

        return new JsConditional(testExpression, thenExpression, elseExpression);
    }

    @NotNull
    public static JsNameRef backingFieldReference(@NotNull TranslationContext context, @NotNull PropertyDescriptor descriptor) {
        DeclarationDescriptor containingDescriptor = descriptor.getContainingDeclaration();
        JsName backingFieldName = containingDescriptor instanceof PackageFragmentDescriptor ?
                                  context.getInnerNameForDescriptor(descriptor) :
                                  context.getNameForDescriptor(descriptor);

        if (!JsDescriptorUtils.isSimpleFinalProperty(descriptor) && !(containingDescriptor instanceof PackageFragmentDescriptor)) {
            backingFieldName = context.getNameForBackingField(descriptor);
        }

        JsExpression receiver;
        if (containingDescriptor instanceof PackageFragmentDescriptor) {
            receiver = null;
        }
        else {
            receiver = context.getDispatchReceiver(JsDescriptorUtils.getReceiverParameterForDeclaration(containingDescriptor));
        }
        return new JsNameRef(backingFieldName, receiver);
    }

    @NotNull
    public static JsExpression assignmentToBackingField(@NotNull TranslationContext context,
            @NotNull PropertyDescriptor descriptor,
            @NotNull JsExpression assignTo) {
        JsNameRef backingFieldReference = backingFieldReference(context, descriptor);
        return assignment(backingFieldReference, assignTo);
    }

    @Nullable
    public static JsExpression translateInitializerForProperty(@NotNull KtProperty declaration,
            @NotNull TranslationContext context) {
        JsExpression jsInitExpression = null;
        KtExpression initializer = declaration.getInitializer();
        if (initializer != null) {
            jsInitExpression = Translation.translateAsExpression(initializer, context);

            KotlinType propertyType = BindingContextUtils.getNotNull(context.bindingContext(), BindingContext.VARIABLE, declaration).getType();
            KotlinType initType = context.bindingContext().getType(initializer);

            jsInitExpression = boxCastIfNeeded(jsInitExpression, initType, propertyType);
        }
        return jsInitExpression;
    }

    @NotNull
    public static JsExpression translateBaseExpression(@NotNull TranslationContext context,
            @NotNull KtUnaryExpression expression) {
        KtExpression baseExpression = PsiUtils.getBaseExpression(expression);
        return Translation.translateAsExpression(baseExpression, context);
    }

    @NotNull
    public static JsExpression translateLeftExpression(
            @NotNull TranslationContext context,
            @NotNull KtBinaryExpression expression,
            @NotNull JsBlock block
    ) {
        KtExpression left = expression.getLeft();
        assert left != null : "Binary expression should have a left expression: " + expression.getText();
        return Translation.translateAsExpression(left, context, block);
    }

    @NotNull
    public static JsExpression translateRightExpression(
            @NotNull TranslationContext context,
            @NotNull KtBinaryExpression expression,
            @NotNull JsBlock block) {
        KtExpression rightExpression = expression.getRight();
        assert rightExpression != null : "Binary expression should have a right expression";
        return Translation.translateAsExpression(rightExpression, context, block);
    }

    public static boolean hasCorrespondingFunctionIntrinsic(@NotNull TranslationContext context,
            @NotNull KtOperationExpression expression) {
        CallableDescriptor operationDescriptor = getCallableDescriptorForOperationExpression(context.bindingContext(), expression);

        if (operationDescriptor == null || !(operationDescriptor instanceof FunctionDescriptor)) return true;

        KotlinType returnType = operationDescriptor.getReturnType();
        if (returnType != null &&
            (KotlinBuiltIns.isChar(returnType) || KotlinBuiltIns.isLong(returnType) || KotlinBuiltIns.isInt(returnType))) {
            return false;
        }

        if (context.intrinsics().getFunctionIntrinsic((FunctionDescriptor) operationDescriptor).exists()) return true;

        return false;
    }

    @NotNull
    public static List<JsExpression> generateInvocationArguments(
            @NotNull JsExpression receiver,
            @NotNull List<? extends JsExpression> arguments
    ) {
        List<JsExpression> argumentList = new ArrayList<>(1 + arguments.size());
        argumentList.add(receiver);
        argumentList.addAll(arguments);
        return argumentList;
    }

    public static boolean isCacheNeeded(@NotNull JsExpression expression) {
        if (expression instanceof JsLiteral.JsValueLiteral) return false;
        if (expression instanceof JsNameRef && ((JsNameRef) expression).getQualifier() == null) return false;
        if (expression instanceof JsBinaryOperation) {
            JsBinaryOperation operation = (JsBinaryOperation) expression;
            JsBinaryOperator operator = operation.getOperator();
            if (operator.isAssignment() || operator == COMMA) return true;
            return isCacheNeeded(operation.getArg1()) || isCacheNeeded(operation.getArg2());
        }
        if (expression instanceof JsUnaryOperation) {
            JsUnaryOperation operation = (JsUnaryOperation) expression;
            JsUnaryOperator operator = operation.getOperator();
            switch (operator) {
                case BIT_NOT:
                case NEG:
                case POS:
                case NOT:
                case TYPEOF:
                case VOID:
                    return isCacheNeeded(operation.getArg());
                default:
                    return true;
            }
        }

        return true;
    }

    @NotNull
    public static JsConditional sure(@NotNull JsExpression expression, @NotNull TranslationContext context) {
        JsInvocation throwNPE = new JsInvocation(Namer.throwNPEFunctionRef());
        JsConditional ensureNotNull = notNullConditional(expression, throwNPE, context);

        JsExpression thenExpression = ensureNotNull.getThenExpression();
        if (thenExpression instanceof JsNameRef) {
            JsName name = ((JsNameRef) thenExpression).getName();
            if (name != null) {
                // associate(cache) ensureNotNull expression to new TemporaryConstVariable with same name.
                context.associateExpressionToLazyValue(ensureNotNull, new TemporaryConstVariable(name, ensureNotNull));
            }
        }

        return ensureNotNull;
    }

    public static boolean isSimpleNameExpressionNotDelegatedLocalVar(@Nullable KtExpression expression, @NotNull TranslationContext context) {
        if (!(expression instanceof KtSimpleNameExpression)) {
            return false;
        }
        DeclarationDescriptor descriptor = context.bindingContext().get(BindingContext.REFERENCE_TARGET, ((KtSimpleNameExpression) expression));
        return !((descriptor instanceof LocalVariableDescriptor) && ((LocalVariableDescriptor) descriptor).isDelegated()) &&
                !((descriptor instanceof PropertyDescriptor) && propertyAccessedByFunctionsInternally((PropertyDescriptor) descriptor, context));
    }

    private static boolean propertyAccessedByFunctionsInternally(@NotNull PropertyDescriptor p, @NotNull TranslationContext context) {
        return !JsDescriptorUtils.isSimpleFinalProperty(p) && context.isFromCurrentModule(p) || shouldAccessViaFunctions(p);
    }

    public static boolean shouldAccessViaFunctions(@NotNull CallableDescriptor descriptor) {
        if (descriptor instanceof PropertyDescriptor) {
            return shouldAccessViaFunctions((PropertyDescriptor) descriptor);
        }
        else if (descriptor instanceof PropertyAccessorDescriptor) {
            return shouldAccessViaFunctions(((PropertyAccessorDescriptor) descriptor).getCorrespondingProperty());
        }
        else {
            return false;
        }
    }

    private static boolean shouldAccessViaFunctions(@NotNull PropertyDescriptor property) {
        if (AnnotationsUtils.hasJsNameInAccessors(property)) return true;
        for (PropertyDescriptor overriddenProperty : property.getOverriddenDescriptors()) {
            if (shouldAccessViaFunctions(overriddenProperty)) return true;
        }
        return false;
    }

    @NotNull
    public static JsExpression translateContinuationArgument(@NotNull TranslationContext context) {
        CallableDescriptor continuationDescriptor = getEnclosingContinuationParameter(context);
        return ReferenceTranslator.translateAsValueReference(continuationDescriptor, context);
    }

    @NotNull
    private static VariableDescriptor getEnclosingContinuationParameter(@NotNull TranslationContext context) {
        VariableDescriptor result = context.getContinuationParameterDescriptor();
        if (result == null) {
            assert context.getParent() != null;
            result = getEnclosingContinuationParameter(context.getParent());
        }
        return result;
    }

    @NotNull
    public static ClassDescriptor getCoroutineBaseClass(@NotNull TranslationContext context) {
        FqName className = DescriptorUtils.COROUTINES_PACKAGE_FQ_NAME.child(Name.identifier("CoroutineImpl"));
        ClassDescriptor descriptor = FindClassInModuleKt.findClassAcrossModuleDependencies(
                context.getCurrentModule(), ClassId.topLevel(className));
        assert descriptor != null;
        return descriptor;
    }

    @NotNull
    public static PropertyDescriptor getCoroutineProperty(@NotNull TranslationContext context, @NotNull String name) {
        return getCoroutineBaseClass(context).getUnsubstitutedMemberScope()
                .getContributedVariables(Name.identifier(name), NoLookupLocation.FROM_DESERIALIZATION)
                .iterator().next();
    }


    @NotNull
    public static FunctionDescriptor getCoroutineDoResumeFunction(@NotNull TranslationContext context) {
        return getCoroutineBaseClass(context).getUnsubstitutedMemberScope()
                .getContributedFunctions(Name.identifier("doResume"), NoLookupLocation.FROM_DESERIALIZATION)
                .iterator().next();
    }

    public static boolean isOverridableFunctionWithDefaultParameters(@NotNull FunctionDescriptor descriptor) {
        return DescriptorUtilsKt.hasOrInheritsParametersWithDefaultValue(descriptor) &&
               !(descriptor instanceof ConstructorDescriptor) &&
               descriptor.getContainingDeclaration() instanceof ClassDescriptor &&
               ModalityKt.isOverridable(descriptor);
    }

    private static boolean overridesReturnAny(CallableDescriptor c) {
        KotlinType returnType = c.getOriginal().getReturnType();
        assert returnType != null;
        if (KotlinBuiltIns.isAnyOrNullableAny(returnType) || TypeUtils.isTypeParameter(returnType)) return true;
        for (CallableDescriptor o : c.getOverriddenDescriptors()) {
            if (overridesReturnAny(o)) return true;
        }
        return false;
    }


    public static boolean shouldBoxReturnValue(CallableDescriptor c) {
        return overridesReturnAny(c) || c instanceof CallableMemberDescriptor && ModalityKt.isOverridable((CallableMemberDescriptor)c);
    }

    @NotNull
    public static JsExpression boxCastIfNeeded(@NotNull JsExpression e, @Nullable KotlinType castFrom, @Nullable KotlinType castTo) {
        if (castFrom != null && KotlinBuiltIns.isCharOrNullableChar(castFrom) &&
            castTo != null && !KotlinBuiltIns.isCharOrNullableChar(castTo)
        ) {
            return JsAstUtils.charToBoxedChar(e);
        }
        return e;
    }
}
