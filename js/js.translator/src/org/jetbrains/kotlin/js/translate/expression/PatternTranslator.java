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

package org.jetbrains.kotlin.js.translate.expression;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.builtins.PrimitiveType;
import org.jetbrains.kotlin.builtins.ReflectionTypes;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.js.patterns.NamePredicate;
import org.jetbrains.kotlin.js.patterns.typePredicates.TypePredicatesKt;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.TemporaryVariable;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.js.translate.intrinsic.functions.factories.TopLevelFIF;
import org.jetbrains.kotlin.js.translate.utils.BindingUtils;
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtIsExpression;
import org.jetbrains.kotlin.psi.KtTypeReference;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.types.DynamicTypesKt;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.typeUtil.TypeUtilsKt;

import java.util.Collections;

import static org.jetbrains.kotlin.builtins.FunctionTypesKt.isFunctionTypeOrSubtype;
import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.isAnyOrNullableAny;
import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.isArray;
import static org.jetbrains.kotlin.js.descriptorUtils.DescriptorUtilsKt.getNameIfStandardType;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getTypeByReference;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getTypeForExpression;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.equality;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.negated;
import static org.jetbrains.kotlin.psi.KtPsiUtil.findChildByType;
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

        TemporaryVariable temporary = context().declareTemporary(expressionToCast);

        KtTypeReference typeReference = expression.getRight();
        assert typeReference != null: "Cast expression must have type reference";
        KotlinType sourceType = getTypeForExpression(bindingContext(), left);
        JsExpression isCheck = translateIsCheck(temporary.assignmentExpression(), sourceType, typeReference);
        if (isCheck == null) return expressionToCast;

        JsExpression onFail;

        if (isSafeCast(expression)) {
            onFail = JsLiteral.NULL;
        }
        else {
            JsExpression throwCCEFunRef = Namer.throwClassCastExceptionFunRef();
            onFail = new JsInvocation(throwCCEFunRef);
        }

        return new JsConditional(isCheck, temporary.reference(), onFail);
    }

    @NotNull
    public JsExpression translateIsExpression(@NotNull KtIsExpression expression) {
        JsExpression left = Translation.translateAsExpression(expression.getLeftHandSide(), context());
        KtTypeReference typeReference = expression.getTypeReference();
        assert typeReference != null;
        KotlinType sourceType = getTypeForExpression(bindingContext(), expression.getLeftHandSide());
        JsExpression result = translateIsCheck(left, sourceType, typeReference);
        if (result == null) return JsLiteral.getBoolean(!expression.isNegated());

        if (expression.isNegated()) {
            return negated(result);
        }
        return result;
    }

    @Nullable
    public JsExpression translateIsCheck(@NotNull JsExpression subject, @Nullable KotlinType sourceType,
            @NotNull KtTypeReference targetTypeReference) {
        KotlinType targetType = getTypeByReference(bindingContext(), targetTypeReference);
        if (sourceType != null && !DynamicTypesKt.isDynamic(sourceType) && TypeUtilsKt.isSubtypeOf(sourceType, targetType)) return null;

        JsExpression checkFunReference = doGetIsTypeCheckCallable(targetType);
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

        if (isNullableType(type)) {
            return namer().orNull(callable);
        }

        return callable;
    }

    @NotNull
    private JsExpression doGetIsTypeCheckCallable(@NotNull KotlinType type) {
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
                result = result != null ? namer().andPredicate(result, next) : next;
            }
            assert result != null : "KotlinType is expected to return at least one upper bound: " + type;
            return result;
        }


        ClassDescriptor referencedClass = DescriptorUtils.getClassDescriptorForType(type);
        JsNameRef typeName = context().getQualifiedReference(referencedClass);
        return referencedClass.getKind() != ClassKind.OBJECT ? namer().isInstanceOf(typeName) : namer().isInstanceOfObject(typeName);
    }

    @Nullable
    private JsExpression getIsTypeCheckCallableForBuiltin(@NotNull KotlinType type) {
        if (isAnyOrNullableAny(type)) return namer().isAny();

        if (isFunctionTypeOrSubtype(type) && !ReflectionTypes.isNumberedKPropertyOrKMutablePropertyType(type)) {
            return namer().isTypeOf(program().getStringLiteral("function"));
        }

        if (isArray(type)) return Namer.IS_ARRAY_FUN_REF;

        if (TypePredicatesKt.getCHAR_SEQUENCE().apply(type)) return namer().isCharSequence();

        if (TypePredicatesKt.getCOMPARABLE().apply(type)) return namer().isComparable();

        return null;
    }

    @Nullable
    private JsExpression getIsTypeCheckCallableForPrimitiveBuiltin(@NotNull KotlinType type) {
        Name typeName = getNameIfStandardType(type);

        if (NamePredicate.STRING.apply(typeName)) {
            return namer().isTypeOf(program().getStringLiteral("string"));
        }

        if (NamePredicate.BOOLEAN.apply(typeName)) {
            return namer().isTypeOf(program().getStringLiteral("boolean"));
        }

        if (NamePredicate.LONG.apply(typeName)) {
            return namer().isInstanceOf(Namer.kotlinLong());
        }

        if (NamePredicate.NUMBER.apply(typeName)) {
            return namer().kotlin(Namer.IS_NUMBER);
        }

        if (NamePredicate.CHAR.apply(typeName)) {
            return namer().kotlin(Namer.IS_CHAR);
        }

        if (NamePredicate.PRIMITIVE_NUMBERS_MAPPED_TO_PRIMITIVE_JS.apply(typeName)) {
            return namer().isTypeOf(program().getStringLiteral("number"));
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
        else if (expressionToMatchAgainst == JsLiteral.NULL) {
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

        PrimitiveType primitive = KotlinBuiltIns.getPrimitiveTypeByFqName(DescriptorUtilsKt.getFqNameUnsafe(descriptor));
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

    private static boolean isSafeCast(@NotNull KtBinaryExpressionWithTypeRHS expression) {
        return expression.getOperationReference().getReferencedNameElementType() == KtTokens.AS_SAFE;
    }

    private static boolean isUnsafeCast(@NotNull KtBinaryExpressionWithTypeRHS expression) {
        return expression.getOperationReference().getReferencedNameElementType() == KtTokens.AS_KEYWORD;
    }
}
