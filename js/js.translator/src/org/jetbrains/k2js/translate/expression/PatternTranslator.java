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

package org.jetbrains.k2js.translate.expression;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetIsExpression;
import org.jetbrains.jet.lang.psi.JetTypeReference;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.NamePredicate;
import org.jetbrains.k2js.translate.utils.BindingUtils;
import org.jetbrains.k2js.translate.utils.JsAstUtils;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getTypeByReference;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.*;
import static org.jetbrains.k2js.translate.utils.JsDescriptorUtils.getNameIfStandardType;

public final class PatternTranslator extends AbstractTranslator {

    @NotNull
    public static PatternTranslator newInstance(@NotNull TranslationContext context) {
        return new PatternTranslator(context);
    }

    private PatternTranslator(@NotNull TranslationContext context) {
        super(context);
    }

    @NotNull
    public JsExpression translateIsExpression(@NotNull JetIsExpression expression) {
        JsExpression left = Translation.translateAsExpression(expression.getLeftHandSide(), context());
        JetTypeReference typeReference = expression.getTypeReference();
        assert typeReference != null;
        JsExpression result = translateIsCheck(left, typeReference);
        if (expression.isNegated()) {
            return negated(result);
        }
        return result;
    }

    @NotNull
    public JsExpression translateIsCheck(@NotNull JsExpression subject, @NotNull JetTypeReference typeReference) {
        JsExpression result = translateAsIntrinsicTypeCheck(subject, typeReference);
        if (result != null) {
            return result;
        }
        return translateAsIsCheck(subject, typeReference);
    }

    @NotNull
    private JsExpression translateAsIsCheck(@NotNull JsExpression expressionToMatch,
                                            @NotNull JetTypeReference typeReference) {
        JsInvocation isCheck = new JsInvocation(context().namer().isOperationReference(),
                                                     expressionToMatch, getClassNameReference(typeReference));
        if (isNullable(typeReference)) {
            return addNullCheck(expressionToMatch, isCheck);
        }
        return isCheck;
    }

    @Nullable
    private JsExpression translateAsIntrinsicTypeCheck(@NotNull JsExpression expressionToMatch,
                                                       @NotNull JetTypeReference typeReference) {
        Name typeName = getNameIfStandardType(getTypeByReference(bindingContext(), typeReference));
        if (typeName == null) {
            return null;
        }

        String jsSTypeName;
        if (NamePredicate.STRING.apply(typeName)) {
            jsSTypeName = "string";
        }
        else if (NamePredicate.LONG.apply(typeName)) {
            return JsAstUtils.isLong(expressionToMatch);
        }
        else if (NamePredicate.NUMBER.apply(typeName)) {
            return JsAstUtils.isNumber(expressionToMatch);
        }
        else if (NamePredicate.CHAR.apply(typeName)) {
            return JsAstUtils.isChar(expressionToMatch);
        }
        else if (NamePredicate.PRIMITIVE_NUMBERS_MAPPED_TO_PRIMITIVE_JS.apply(typeName)) {
            jsSTypeName = "number";
        }
        else {
            return null;
        }
        return typeof(expressionToMatch, program().getStringLiteral(jsSTypeName));
    }

    @NotNull
    private static JsExpression addNullCheck(@NotNull JsExpression expressionToMatch, @NotNull JsInvocation isCheck) {
        return or(TranslationUtils.isNullCheck(expressionToMatch), isCheck);
    }

    private boolean isNullable(JetTypeReference typeReference) {
        return getTypeByReference(bindingContext(), typeReference).isNullable();
    }

    @NotNull
    private JsNameRef getClassNameReference(@NotNull JetTypeReference typeReference) {
        ClassDescriptor referencedClass = BindingUtils.getClassDescriptorForTypeReference
            (bindingContext(), typeReference);
        return context().getQualifiedReference(referencedClass);
    }

    @NotNull
    public JsExpression translateExpressionPattern(@NotNull JsExpression expressionToMatch, @NotNull JetExpression patternExpression) {
        JsExpression expressionToMatchAgainst = translateExpressionForExpressionPattern(patternExpression);
        return equality(expressionToMatch, expressionToMatchAgainst);
    }

    @NotNull
    public JsExpression translateExpressionForExpressionPattern(@NotNull JetExpression patternExpression) {
        return Translation.translateAsExpression(patternExpression, context());
    }
}
