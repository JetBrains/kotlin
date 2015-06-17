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

package org.jetbrains.kotlin.js.translate.expression;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.backend.js.ast.metadata.MetadataProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.JetNodeTypes;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.js.descriptorUtils.DescriptorUtilsKt;
import org.jetbrains.kotlin.js.patterns.NamePredicate;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.TemporaryVariable;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.js.translate.utils.BindingUtils;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtIsExpression;
import org.jetbrains.kotlin.psi.KtTypeReference;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.types.KotlinType;

import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.isAnyOrNullableAny;
import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.isFunctionOrExtensionFunctionType;
import static org.jetbrains.kotlin.js.descriptorUtils.DescriptorUtilsPackage.getNameIfStandardType;
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
        JsExpression isCheck = translateIsCheck(temporary.assignmentExpression(), typeReference);
        JsExpression onFail;

        if (isSafeCast(expression)) {
            onFail = JsLiteral.NULL;
        }
        else {
            JsExpression throwCCEFunRef = context().namer().throwClassCastExceptionFunRef();
            onFail = new JsInvocation(throwCCEFunRef);
        }

        JsConditional conditional = new JsConditional(isCheck, temporary.reference(), onFail);
        MetadataProperties.setCastExpression(conditional, true);
        return conditional;
    }

    @NotNull
    public JsExpression translateIsExpression(@NotNull KtIsExpression expression) {
        JsExpression left = Translation.translateAsExpression(expression.getLeftHandSide(), context());
        KtTypeReference typeReference = expression.getTypeReference();
        assert typeReference != null;
        JsExpression result = translateIsCheck(left, typeReference);
        if (expression.isNegated()) {
            return negated(result);
        }
        return result;
    }

    @NotNull
    public JsExpression translateIsCheck(@NotNull JsExpression subject, @NotNull KtTypeReference typeReference) {
        KotlinType type = BindingUtils.getTypeByReference(bindingContext(), typeReference);
        JsExpression checkFunReference = getIsTypeCheckCallable(type);

        if (isReifiedTypeParameter(type) && findChildByType(typeReference, KtNodeTypes.NULLABLE_TYPE) != null) {
            checkFunReference = namer().orNull(checkFunReference);
        }

        return new JsInvocation(checkFunReference, subject);
    }

    @NotNull
    public JsExpression getIsTypeCheckCallable(@NotNull KotlinType type) {
        JsExpression callable = doGetIsTypeCheckCallable(type);

        if (isNullableType(type) && !isReifiedTypeParameter(type)) {
            return namer().orNull(callable);
        }

        return callable;
    }

    @NotNull
    private JsExpression doGetIsTypeCheckCallable(@NotNull KotlinType type) {
        if (isAnyOrNullableAny(type)) return namer().isAny();

        if (isFunctionOrExtensionFunctionType(type)) return namer().isTypeOf(program().getStringLiteral("function"));

        JsExpression builtinCheck = getIsTypeCheckCallableForBuiltin(type);
        if (builtinCheck != null) return builtinCheck;

        TypeParameterDescriptor typeParameterDescriptor = getTypeParameterDescriptorOrNull(type);
        if (typeParameterDescriptor != null) {
            if (typeParameterDescriptor.isReified()) {
                return getIsTypeCheckCallableForReifiedType(typeParameterDescriptor);
            }

            return namer().isAny();
        }

        JsNameRef typeName = getClassNameReference(type);
        return namer().isInstanceOf(typeName);
    }

    @Nullable
    private JsExpression getIsTypeCheckCallableForBuiltin(@NotNull KotlinType type) {
        Name typeName = DescriptorUtilsKt.getNameIfStandardType(type);

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
    private JsNameRef getClassNameReference(@NotNull KotlinType type) {
        ClassDescriptor referencedClass = DescriptorUtils.getClassDescriptorForType(type);
        return context().getQualifiedReference(referencedClass);
    }

    @NotNull
    public JsExpression translateExpressionPattern(@NotNull JsExpression expressionToMatch, @NotNull KtExpression patternExpression) {
        JsExpression expressionToMatchAgainst = translateExpressionForExpressionPattern(patternExpression);
        return equality(expressionToMatch, expressionToMatchAgainst);
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
