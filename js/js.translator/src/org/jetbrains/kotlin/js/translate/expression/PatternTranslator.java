/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.translate.expression;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.builtins.PrimitiveType;
import org.jetbrains.kotlin.builtins.ReflectionTypes;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.patterns.NamePredicate;
import org.jetbrains.kotlin.js.patterns.typePredicates.TypePredicatesKt;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.TemporaryVariable;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.js.translate.intrinsic.functions.factories.ArrayFIF;
import org.jetbrains.kotlin.js.translate.intrinsic.functions.factories.TopLevelFIF;
import org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator;
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils;
import org.jetbrains.kotlin.js.translate.utils.BindingUtils;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtIsExpression;
import org.jetbrains.kotlin.psi.KtTypeReference;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.Collections;

import static org.jetbrains.kotlin.builtins.FunctionTypesKt.isFunctionTypeOrSubtype;
import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.isArray;
import static org.jetbrains.kotlin.js.descriptorUtils.DescriptorUtilsKt.getNameIfStandardType;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getTypeByReference;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.equality;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.not;
import static org.jetbrains.kotlin.psi.KtPsiUtil.*;
import static org.jetbrains.kotlin.types.TypeUtils.*;

public final class PatternTranslator extends AbstractTranslator {

    @NotNull
    public static PatternTranslator newInstance(@NotNull TranslationContext context) {
        return new PatternTranslator(context);
    }

    private PatternTranslator(@NotNull TranslationContext context) {
        super(context);
    }

    public static boolean isCastExpression(@NotNull KtBinaryExpressionWithTypeRHS expression) {
        return isSafeCast(expression) || isUnsafeCast(expression);
    }

    @NotNull
    public JsExpression translateCastExpression(@NotNull KtBinaryExpressionWithTypeRHS expression) {
        assert isCastExpression(expression): "Expected cast expression, got " + expression;
        KtExpression left = expression.getLeft();
        JsExpression expressionToCast = Translation.translateAsExpression(left, context());

        KtTypeReference typeReference = expression.getRight();
        assert typeReference != null: "Cast expression must have type reference";

        KotlinType anyType = context().getCurrentModule().getBuiltIns().getAnyType();
        expressionToCast = TranslationUtils.coerce(context(), expressionToCast, anyType);

        TemporaryVariable temporary = context().declareTemporary(expressionToCast, expression);
        JsExpression isCheck = translateIsCheck(temporary.assignmentExpression(), typeReference);
        if (isCheck == null) return expressionToCast;

        JsExpression onFail;

        if (isSafeCast(expression)) {
            onFail = new JsNullLiteral();
        }
        else {
            JsExpression throwCCEFunRef = Namer.throwClassCastExceptionFunRef();
            onFail = new JsInvocation(throwCCEFunRef);
        }

        JsExpression result = new JsConditional(isCheck, temporary.reference(), onFail);

        KotlinType targetType = getTypeByReference(bindingContext(), typeReference);
        if (isSafeCast(expression)) {
            targetType = targetType.unwrap().makeNullableAsSpecified(true);
        }
        return TranslationUtils.coerce(context(), result, targetType);
    }

    @NotNull
    public JsExpression translateIsExpression(@NotNull KtIsExpression expression) {
        KtExpression left = expression.getLeftHandSide();
        JsExpression expressionToCheck = Translation.translateAsExpression(left, context());

        KotlinType anyType = context().getCurrentModule().getBuiltIns().getAnyType();
        expressionToCheck = TranslationUtils.coerce(context(), expressionToCheck, anyType);

        KtTypeReference typeReference = expression.getTypeReference();
        assert typeReference != null;
        JsExpression result = translateIsCheck(expressionToCheck, typeReference);
        if (result == null) return new JsBooleanLiteral(!expression.isNegated());

        if (expression.isNegated()) {
            return not(result);
        }
        return result;
    }

    @Nullable
    public JsExpression translateIsCheck(@NotNull JsExpression subject, @NotNull KtTypeReference targetTypeReference) {
        KotlinType targetType = getTypeByReference(bindingContext(), targetTypeReference);

        JsExpression checkFunReference = doGetIsTypeCheckCallable(targetType);
        if (checkFunReference == null) {
            return null;
        }

        boolean isReifiedType = isReifiedTypeParameter(targetType);
        if (!isReifiedType && isNullableType(targetType) ||
            isReifiedType && findChildByType(targetTypeReference, KtNodeTypes.NULLABLE_TYPE) != null
        ) {
            checkFunReference = namer().orNull(checkFunReference);
        }

        return new JsInvocation(checkFunReference, subject);
    }

    @NotNull
    public JsExpression getIsTypeCheckCallable(@NotNull KotlinType type) {
        JsExpression callable = doGetIsTypeCheckCallable(type);
        assert callable != null : "This method should be called only to translate reified type parameters. " +
                                  "`callable` should never be null for reified type parameters. " +
                                  "Actual type: " + type;

        // If the type is reified, rely on the corresponding is type callable.
        // Otherwise make sure that passing null yields true
        if (!isReifiedTypeParameter(type) && isNullableType(type)) {
            return namer().orNull(callable);
        }

        return callable;
    }

    @Nullable
    private JsExpression doGetIsTypeCheckCallable(@NotNull KotlinType type) {
        ClassifierDescriptor targetDescriptor = type.getConstructor().getDeclarationDescriptor();
        if (targetDescriptor != null && AnnotationsUtils.isNativeInterface(targetDescriptor)) {
            return type.isMarkedNullable() ? null : namer().isInstanceOf(JsAstUtils.pureFqn("Object", null));
        }

        JsExpression builtinCheck = getIsTypeCheckCallableForBuiltin(type);
        if (builtinCheck != null) return builtinCheck;

        builtinCheck = getIsTypeCheckCallableForPrimitiveBuiltin(type);
        if (builtinCheck != null) return builtinCheck;

        TypeParameterDescriptor typeParameterDescriptor = getTypeParameterDescriptorOrNull(type);
        if (typeParameterDescriptor != null) {
            if (typeParameterDescriptor.isReified()) {
                return getIsTypeCheckCallableForReifiedType(typeParameterDescriptor);
            }

            JsExpression result = null;
            for (KotlinType upperBound : typeParameterDescriptor.getUpperBounds()) {
                JsExpression next = doGetIsTypeCheckCallable(upperBound);
                if (next != null) {
                    result = result != null ? namer().andPredicate(result, next) : next;
                }
            }
            return result;
        }

        ClassDescriptor referencedClass = DescriptorUtils.getClassDescriptorForType(type);
        JsExpression typeName = ReferenceTranslator.translateAsTypeReference(referencedClass, context());
        return namer().isInstanceOf(typeName);
    }

    @Nullable
    private JsExpression getIsTypeCheckCallableForBuiltin(@NotNull KotlinType type) {
        if (isFunctionTypeOrSubtype(type) && !ReflectionTypes.isNumberedKPropertyOrKMutablePropertyType(type)) {
            return namer().isTypeOf(new JsStringLiteral("function"));
        }

        if (isArray(type)) {
            if (ArrayFIF.typedArraysEnabled(context().getConfig())) {
                return namer().isArray();
            }
            else {
                return Namer.IS_ARRAY_FUN_REF;
            }

        }

        if (TypePredicatesKt.getCHAR_SEQUENCE().test(type)) return namer().isCharSequence();

        if (TypePredicatesKt.getCOMPARABLE().test(type)) return namer().isComparable();

        return null;
    }

    @Nullable
    private JsExpression getIsTypeCheckCallableForPrimitiveBuiltin(@NotNull KotlinType type) {
        Name typeName = getNameIfStandardType(type);

        if (NamePredicate.STRING.test(typeName)) {
            return namer().isTypeOf(new JsStringLiteral("string"));
        }

        if (NamePredicate.BOOLEAN.test(typeName)) {
            return namer().isTypeOf(new JsStringLiteral("boolean"));
        }

        if (NamePredicate.LONG.test(typeName)) {
            return namer().isInstanceOf(Namer.kotlinLong());
        }

        if (NamePredicate.NUMBER.test(typeName)) {
            return namer().kotlin(Namer.IS_NUMBER);
        }

        if (NamePredicate.CHAR.test(typeName)) {
            return namer().kotlin(Namer.IS_CHAR);
        }

        if (NamePredicate.PRIMITIVE_NUMBERS_MAPPED_TO_PRIMITIVE_JS.test(typeName)) {
            return namer().isTypeOf(new JsStringLiteral("number"));
        }

        if (ArrayFIF.typedArraysEnabled(context().getConfig())) {
            if (KotlinBuiltIns.isPrimitiveArray(type)) {
                PrimitiveType arrayType = KotlinBuiltIns.getPrimitiveArrayElementType(type);
                assert arrayType != null;
                return namer().isPrimitiveArray(arrayType);
            }
        }

        return null;
    }

    @NotNull
    private JsExpression getIsTypeCheckCallableForReifiedType(@NotNull TypeParameterDescriptor typeParameter) {
        assert typeParameter.isReified(): "Expected reified type, actual: " + typeParameter;
        DeclarationDescriptor containingDeclaration = typeParameter.getContainingDeclaration();
        assert containingDeclaration instanceof CallableDescriptor:
                "Expected type parameter " + typeParameter +
                " to be contained in CallableDescriptor, actual: " + containingDeclaration.getClass();

        JsExpression alias = context().getAliasForDescriptor(typeParameter);
        assert alias != null: "No alias found for reified type parameter: " + typeParameter;
        return alias;
    }

    @NotNull
    public JsExpression translateExpressionPattern(
            @NotNull KotlinType type,
            @NotNull JsExpression expressionToMatch,
            @NotNull KtExpression patternExpression
    ) {
        JsExpression expressionToMatchAgainst = translateExpressionForExpressionPattern(patternExpression);
        KotlinType patternType = BindingUtils.getTypeForExpression(bindingContext(), patternExpression);

        EqualityType matchEquality = equalityType(type);
        EqualityType patternEquality = equalityType(patternType);

        if (matchEquality == EqualityType.PRIMITIVE && patternEquality == EqualityType.PRIMITIVE) {
            return equality(expressionToMatch, expressionToMatchAgainst);
        }
        else if (expressionToMatchAgainst instanceof JsNullLiteral) {
            return TranslationUtils.nullCheck(expressionToMatch, false);
        }
        else {
            return TopLevelFIF.KOTLIN_EQUALS.apply(expressionToMatch, Collections.singletonList(expressionToMatchAgainst), context());
        }
    }

    @NotNull
    private static EqualityType equalityType(@NotNull KotlinType type) {
        DeclarationDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        if (!(descriptor instanceof ClassDescriptor)) return EqualityType.GENERAL;

        PrimitiveType primitive = KotlinBuiltIns.getPrimitiveType(descriptor);
        if (primitive == null) return EqualityType.GENERAL;

        return primitive == PrimitiveType.LONG ? EqualityType.LONG : EqualityType.PRIMITIVE;
    }

    private enum EqualityType {
        PRIMITIVE,
        LONG,
        GENERAL
    }

    @NotNull
    public JsExpression translateExpressionForExpressionPattern(@NotNull KtExpression patternExpression) {
        return Translation.translateAsExpression(patternExpression, context());
    }
}
