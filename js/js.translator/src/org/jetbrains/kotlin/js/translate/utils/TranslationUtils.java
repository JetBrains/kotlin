/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.translate.utils;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.FunctionTypesKt;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.builtins.PrimitiveType;
import org.jetbrains.kotlin.config.CoroutineLanguageVersionSettingsUtilKt;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.kotlin.descriptors.impl.LocalVariableAccessorDescriptor;
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.backend.ast.metadata.BoxingKind;
import org.jetbrains.kotlin.js.backend.ast.metadata.MetadataProperties;
import org.jetbrains.kotlin.js.backend.ast.metadata.SpecialFunction;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.TemporaryConstVariable;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.expression.InlineMetadata;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.js.translate.intrinsic.functions.factories.ArrayFIF;
import org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator;
import org.jetbrains.kotlin.js.translate.utils.jsAstUtils.AstUtilsKt;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.kotlin.resolve.source.KotlinSourceElementKt;
import org.jetbrains.kotlin.types.DynamicTypesKt;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.jetbrains.kotlin.js.backend.ast.JsBinaryOperator.*;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getCallableDescriptorForOperationExpression;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.*;
import static org.jetbrains.kotlin.js.translate.utils.UtilsKt.hasOrInheritsParametersWithDefaultValue;

public final class TranslationUtils {
    private static final Set<FqNameUnsafe> CLASSES_WITH_NON_BOXED_CHARS = new HashSet<>(Arrays.asList(
            new FqNameUnsafe("kotlin.collections.CharIterator"),
            new FqNameUnsafe("kotlin.ranges.CharProgression"),
            new FqNameUnsafe("kotlin.js.internal.CharCompanionObject"),
            new FqNameUnsafe("kotlin.Char.Companion"),
            KotlinBuiltIns.FQ_NAMES.charSequence, KotlinBuiltIns.FQ_NAMES.number
    ));

    private TranslationUtils() {
    }

    @NotNull
    public static JsPropertyInitializer translateFunctionAsEcma5PropertyDescriptor(
            @NotNull JsFunction function, @NotNull FunctionDescriptor descriptor,
            @NotNull TranslationContext context
    ) {
        JsExpression functionExpression = function;
        if (InlineUtil.isInline(descriptor)) {
            InlineMetadata metadata = InlineMetadata.compose(function, descriptor, context);
            PsiElement sourceInfo = KotlinSourceElementKt.getPsi(descriptor.getSource());
            functionExpression = metadata.functionWithMetadata(context, sourceInfo);
        }

        if (DescriptorUtils.isExtension(descriptor) ||
            descriptor instanceof PropertyAccessorDescriptor &&
            shouldAccessViaFunctions(((PropertyAccessorDescriptor) descriptor).getCorrespondingProperty())
        ) {
            return translateExtensionFunctionAsEcma5DataDescriptor(functionExpression, descriptor, context);
        }
        else {
            JsStringLiteral getOrSet = new JsStringLiteral(getAccessorFunctionName(descriptor));
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
        JsReturn jsReturn = new JsReturn(returnExpression);
        jsReturn.setSource(returnExpression.getSource());
        return new JsFunction(functionScope, new JsBlock(jsReturn), "<simpleReturnFunction>");
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
        return new JsBinaryOperation(operator, expressionToCheck, new JsNullLiteral());
    }

    @NotNull
    private static JsExpression prepareForNullCheck(
            @NotNull KtExpression ktSubject,
            @NotNull JsExpression expression,
            @NotNull TranslationContext context
    ) {
        KotlinType type = context.bindingContext().getType(ktSubject);
        if (type == null) {
            type = context.getCurrentModule().getBuiltIns().getAnyType();
        }

        return coerce(context, expression, TypeUtils.makeNullable(type));
    }

    @NotNull
    public static JsBinaryOperation nullCheck(
            @NotNull KtExpression ktSubject,
            @NotNull JsExpression expressionToCheck,
            @NotNull TranslationContext context,
            boolean isNegated
    ) {
        return nullCheck(prepareForNullCheck(ktSubject, expressionToCheck, context), isNegated);
    }

    @NotNull
    public static JsBinaryOperation nullCheck(
            @NotNull KotlinType expressionType,
            @NotNull JsExpression expressionToCheck,
            @NotNull TranslationContext context,
            boolean isNegated
    ) {
        return nullCheck(coerce(context, expressionToCheck, TypeUtils.makeNullable(expressionType)), isNegated);
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
    public static JsName getNameForBackingField(@NotNull TranslationContext context, @NotNull PropertyDescriptor descriptor) {
        if (isReferenceToSyntheticBackingField(descriptor)) {
            return context.getNameForBackingField(descriptor);
        }

        DeclarationDescriptor containingDescriptor = descriptor.getContainingDeclaration();
        return containingDescriptor instanceof PackageFragmentDescriptor ?
                                  context.getInnerNameForDescriptor(descriptor) :
                                  context.getNameForDescriptor(descriptor);
    }

    public static boolean isReferenceToSyntheticBackingField(@NotNull PropertyDescriptor descriptor) {
        DeclarationDescriptor containingDescriptor = descriptor.getContainingDeclaration();
        return !JsDescriptorUtils.isSimpleFinalProperty(descriptor) && !(containingDescriptor instanceof PackageFragmentDescriptor);
    }

    @NotNull
    public static JsNameRef backingFieldReference(@NotNull TranslationContext context, @NotNull PropertyDescriptor descriptor) {
        DeclarationDescriptor containingDescriptor = descriptor.getContainingDeclaration();

        JsExpression receiver;
        if (containingDescriptor instanceof PackageFragmentDescriptor) {
            receiver = null;
        }
        else {
            receiver = context.getDispatchReceiver(JsDescriptorUtils.getReceiverParameterForDeclaration(containingDescriptor));
        }

        JsNameRef result = new JsNameRef(getNameForBackingField(context, descriptor), receiver);
        MetadataProperties.setType(result, getReturnTypeForCoercion(descriptor, true));

        return result;
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

            KotlinType propertyType = BindingContextUtils.getNotNull(
                    context.bindingContext(), BindingContext.VARIABLE, declaration).getType();
            jsInitExpression = coerce(context, jsInitExpression, propertyType);
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

        if (context.intrinsics().getFunctionIntrinsic((FunctionDescriptor) operationDescriptor, context) != null) return true;

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
    public static JsExpression sure(@NotNull KtExpression ktExpression, @NotNull JsExpression expression, @NotNull TranslationContext context) {
        return new JsInvocation(context.getReferenceToIntrinsic(Namer.NULL_CHECK_INTRINSIC_NAME),
                                prepareForNullCheck(ktExpression, expression, context));
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
    public static ValueParameterDescriptor getEnclosingContinuationParameter(@NotNull TranslationContext context) {
        ValueParameterDescriptor result = context.getContinuationParameterDescriptor();
        if (result == null) {
            assert context.getParent() != null;
            result = getEnclosingContinuationParameter(context.getParent());
        }
        return result;
    }

    @NotNull
    public static ClassDescriptor getCoroutineBaseClass(@NotNull TranslationContext context) {
        FqName className = CoroutineLanguageVersionSettingsUtilKt.coroutinesPackageFqName(context.getLanguageVersionSettings())
                .child(Name.identifier("CoroutineImpl"));
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
        return hasOrInheritsParametersWithDefaultValue(descriptor) &&
                !(descriptor instanceof ConstructorDescriptor) &&
                descriptor.getContainingDeclaration() instanceof ClassDescriptor &&
                ModalityKt.isOverridable(descriptor);
    }

    @NotNull
    public static KotlinType getReturnTypeForCoercion(@NotNull CallableDescriptor descriptor) {
        return getReturnTypeForCoercion(descriptor, false);
    }

    @NotNull
    public static KotlinType getReturnTypeForCoercion(@NotNull CallableDescriptor descriptor, boolean forcePrivate) {
        descriptor = descriptor.getOriginal();

        if (FunctionTypesKt.getFunctionalClassKind(descriptor) != null || descriptor instanceof AnonymousFunctionDescriptor) {
            return getAnyTypeFromSameModule(descriptor);
        }

        Collection<? extends CallableDescriptor> overridden = descriptor.getOverriddenDescriptors();
        if (overridden.isEmpty()) {
            KotlinType returnType = descriptor.getReturnType();
            if (returnType == null) {
                return getAnyTypeFromSameModule(descriptor);
            }

            DeclarationDescriptor container = descriptor.getContainingDeclaration();
            boolean isPublic = descriptor.getVisibility().effectiveVisibility(descriptor, true).getPublicApi() && !forcePrivate;
            if (KotlinBuiltIns.isCharOrNullableChar(returnType) && container instanceof ClassDescriptor && isPublic) {
                ClassDescriptor containingClass = (ClassDescriptor) container;
                FqNameUnsafe containingClassName = DescriptorUtilsKt.getFqNameUnsafe(containingClass);
                if (!CLASSES_WITH_NON_BOXED_CHARS.contains(containingClassName) &&
                    !KotlinBuiltIns.isPrimitiveType(containingClass.getDefaultType()) &&
                    !KotlinBuiltIns.isPrimitiveArray(containingClassName)
                ) {
                    return getAnyTypeFromSameModule(descriptor);
                }
            }
            return returnType;
        }

        Set<KotlinType> typesFromOverriddenCallables = overridden.stream()
                .map(o -> getReturnTypeForCoercion(o, forcePrivate))
                .collect(Collectors.toSet());
        return typesFromOverriddenCallables.size() == 1
               ? typesFromOverriddenCallables.iterator().next()
               : getAnyTypeFromSameModule(descriptor);
    }

    @NotNull
    private static KotlinType getAnyTypeFromSameModule(@NotNull DeclarationDescriptor descriptor) {
        return DescriptorUtils.getContainingModule(descriptor).getBuiltIns().getAnyType();
    }

    @NotNull
    public static KotlinType getDispatchReceiverTypeForCoercion(@NotNull CallableDescriptor descriptor) {
        descriptor = descriptor.getOriginal();
        if (descriptor.getDispatchReceiverParameter() == null) {
            throw new IllegalArgumentException("This method can only be used for class members; " +
                                               "given descriptor is not a member of a class " + descriptor);
        }

        Collection<? extends CallableDescriptor> overridden = descriptor.getOverriddenDescriptors();
        if (overridden.isEmpty()) {
            return descriptor.getDispatchReceiverParameter().getType();
        }

        Set<KotlinType> typesFromOverriddenCallables = overridden.stream()
                .map(TranslationUtils::getDispatchReceiverTypeForCoercion)
                .collect(Collectors.toSet());
        return typesFromOverriddenCallables.size() == 1
               ? typesFromOverriddenCallables.iterator().next()
               : getAnyTypeFromSameModule(descriptor);
    }

    @NotNull
    public static JsExpression coerce(@NotNull TranslationContext context, @NotNull JsExpression value, @NotNull KotlinType to) {
        if (DynamicTypesKt.isDynamic(to)) return value;

        KotlinType from = MetadataProperties.getType(value);
        if (from == null) {
            from = context.getCurrentModule().getBuiltIns().getAnyType();
        }

        if (from.equals(to)) return value;

        if (KotlinBuiltIns.isCharOrNullableChar(to)) {
            if (!KotlinBuiltIns.isCharOrNullableChar(from) && !(value instanceof JsNullLiteral)) {
                value = boxedCharToChar(context, value);
            }
        }
        else if (KotlinBuiltIns.isUnit(to)) {
            if (!KotlinBuiltIns.isUnit(from)) {
                value = unitToVoid(value);
            }
        }
        else if (KotlinBuiltIns.isCharOrNullableChar(from)) {
            if (!KotlinBuiltIns.isCharOrNullableChar(to) && !(value instanceof JsNullLiteral)) {
                value = charToBoxedChar(context, value);
            }
        }
        else if (KotlinBuiltIns.isUnit(from)) {
            if (!KotlinBuiltIns.isUnit(to) && !MetadataProperties.isUnit(value)) {
                value = voidToUnit(context, value);
            }
        }

        PrimitiveType signedPrimitiveFromUnsigned = ArrayFIF.INSTANCE.unsignedPrimitiveToSigned(to);
        if (signedPrimitiveFromUnsigned != null) {
            if (KotlinBuiltIns.isInt(from)) {
                switch (signedPrimitiveFromUnsigned) {
                    case BYTE:
                        value = AstUtilsKt.toByte(context, value);
                        break;
                    case SHORT:
                        value = AstUtilsKt.toShort(context, value);
                        break;
                }
                DeclarationDescriptor d = to.getConstructor().getDeclarationDescriptor();
                if (d instanceof ClassDescriptor) {
                    value =  new JsNew(ReferenceTranslator.translateAsTypeReference((ClassDescriptor) d, context),
                                     Collections.singletonList(value));
                }
            }
        }

        MetadataProperties.setType(value, to);
        return value;
    }

    @NotNull
    private static JsExpression voidToUnit(@NotNull TranslationContext context, @NotNull JsExpression expression) {
        ClassDescriptor unit = context.getCurrentModule().getBuiltIns().getUnit();
        JsExpression unitRef = ReferenceTranslator.translateAsValueReference(unit, context);
        return JsAstUtils.newSequence(Arrays.asList(expression, unitRef));
    }

    @NotNull
    private static JsExpression unitToVoid(@NotNull JsExpression expression) {
        if (expression instanceof JsBinaryOperation) {
            JsBinaryOperation binary = (JsBinaryOperation) expression;
            if (binary.getOperator() == JsBinaryOperator.COMMA && MetadataProperties.isUnit(binary.getArg2())) {
                return binary.getArg1();
            }
        }
        return expression;
    }

    @NotNull
    public static JsExpression charToBoxedChar(@NotNull TranslationContext context, @NotNull JsExpression expression) {
        if (expression instanceof JsInvocation) {
            JsInvocation invocation = (JsInvocation) expression;
            BoxingKind existingKind = MetadataProperties.getBoxing(invocation);
            switch (existingKind) {
                case UNBOXING:
                    return invocation.getArguments().get(0);
                case BOXING:
                    return expression;
                case NONE:
                    break;
            }
        }

        JsInvocation result = invokeSpecialFunction(context, SpecialFunction.TO_BOXED_CHAR, expression);
        result.setSource(expression.getSource());
        MetadataProperties.setBoxing(result, BoxingKind.BOXING);
        return result;
    }

    @NotNull
    private static JsExpression boxedCharToChar(@NotNull TranslationContext context, @NotNull JsExpression expression) {
        if (expression instanceof JsInvocation) {
            JsInvocation invocation = (JsInvocation) expression;
            BoxingKind existingKind = MetadataProperties.getBoxing(invocation);
            switch (existingKind) {
                case BOXING:
                    return invocation.getArguments().get(0);
                case UNBOXING:
                    return expression;
                case NONE:
                    break;
            }
        }

        JsInvocation result = invokeSpecialFunction(context, SpecialFunction.UNBOX_CHAR, expression);
        result.setSource(expression.getSource());
        MetadataProperties.setBoxing(result, BoxingKind.UNBOXING);
        return result;
    }

    @NotNull
    public static JsInvocation invokeSpecialFunction(
            @NotNull TranslationContext context,
            @NotNull SpecialFunction function, @NotNull JsExpression... arguments
    ) {
        JsName name = context.getNameForSpecialFunction(function);
        return new JsInvocation(pureFqn(name, null), arguments);
    }

    @NotNull
    public static String getTagForSpecialFunction(@NotNull SpecialFunction specialFunction) {
        return "special:" + specialFunction.name();
    }

    @NotNull
    public static JsExpression getIntrinsicFqn(@NotNull String name) {
        JsExpression fqn = pureFqn(Namer.KOTLIN_NAME, null);
        for (String part : StringUtil.split(name, ".")) {
            fqn = pureFqn(part, fqn);
        }
        return fqn;
    }
}
