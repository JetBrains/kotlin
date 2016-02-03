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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.js.descriptorUtils.DescriptorUtilsKt;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.js.patterns.NamePredicate;
import org.jetbrains.kotlin.js.translate.utils.BindingUtils;
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtBinaryExpression;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtIsExpression;
import org.jetbrains.kotlin.psi.KtTypeReference;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.types.KotlinType;

import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getTypeByReference;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.*;

public final class PatternTranslator extends AbstractTranslator {

    @NotNull
    public static PatternTranslator newInstance(@NotNull TranslationContext context) {
        return new PatternTranslator(context);
    }

    private PatternTranslator(@NotNull TranslationContext context) {
        super(context);
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
        JsInvocation isCheck = new JsInvocation(checkFunReference, subject);

        if (isNullable(typeReference)) {
            return addNullCheck(subject, isCheck);
        }

        return isCheck;
    }

    @NotNull
    public JsExpression getIsTypeCheckCallable(@NotNull KotlinType type) {
        JsExpression builtinCheck = getIsTypeCheckCallableForBuiltin(type);
        if (builtinCheck != null) return builtinCheck;

        ClassifierDescriptor typeDescriptor = type.getConstructor().getDeclarationDescriptor();

        if (typeDescriptor instanceof TypeParameterDescriptor) {
            TypeParameterDescriptor typeParameterDescriptor = (TypeParameterDescriptor) typeDescriptor;

            if (typeParameterDescriptor.isReified()) {
                return getIsTypeCheckCallableForReifiedType(typeParameterDescriptor);
            }
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
            return namer().isInstanceOf(Namer.KOTLIN_LONG_NAME_REF);
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
    private static JsExpression addNullCheck(@NotNull JsExpression expressionToMatch, @NotNull JsInvocation isCheck) {
        return or(TranslationUtils.isNullCheck(expressionToMatch), isCheck);
    }

    private boolean isNullable(KtTypeReference typeReference) {
        return getTypeByReference(bindingContext(), typeReference).isMarkedNullable();
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

    @NotNull
    // Note that expressionToMatch must be a single variable
    public JsExpression translateRangePattern(@NotNull JsExpression expressionToMatch, @NotNull KtExpression patternExpression) {
        if (isRangePattern(patternExpression)) {
            KtBinaryExpression rangePatternExpression = (KtBinaryExpression) patternExpression;
            // FIXME: Don't know what to do when one of getLeft() and getRight() is null
            JsExpression lower = Translation.translateAsExpression(rangePatternExpression.getLeft(), context());
            JsExpression upper = Translation.translateAsExpression(rangePatternExpression.getRight(), context());
            return and(greaterThanEq(expressionToMatch, lower), lessThanEq(expressionToMatch, upper));
        } else {
            // TODO: figure out if it is possible
            //JsExpression expressionToMatchAgainst = translateExpressionForExpressionPattern(patternExpression);
            throw new UnsupportedOperationException("Don't know how to translate general range in when .. in R expression");
        }
    }

    private static boolean isRangePattern(@NotNull KtExpression expression) {
        if (!(expression instanceof KtBinaryExpression)) {
            return false;
        }
        KtBinaryExpression binary = (KtBinaryExpression) expression;
        return binary.getOperationToken() == KtTokens.RANGE;
    }
}
