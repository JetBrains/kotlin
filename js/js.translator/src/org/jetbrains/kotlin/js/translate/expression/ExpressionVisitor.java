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
import com.google.dart.compiler.backend.js.ast.metadata.MetadataProperties;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.declaration.ClassTranslator;
import org.jetbrains.kotlin.js.translate.expression.loopTranslator.LoopTranslator;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.js.translate.general.TranslatorVisitor;
import org.jetbrains.kotlin.js.translate.operation.BinaryOperationTranslator;
import org.jetbrains.kotlin.js.translate.operation.UnaryOperationTranslator;
import org.jetbrains.kotlin.js.translate.reference.*;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilsKt;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.constants.NullValue;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeUtils;

import java.util.List;

import static org.jetbrains.kotlin.js.translate.context.Namer.getCapturedVarAccessor;
import static org.jetbrains.kotlin.js.translate.general.Translation.translateAsExpression;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.*;
import static org.jetbrains.kotlin.js.translate.utils.ErrorReportingUtils.message;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.convertToStatement;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.newVar;
import static org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.getReceiverParameterForDeclaration;
import static org.jetbrains.kotlin.js.translate.utils.TranslationUtils.translateInitializerForProperty;
import static org.jetbrains.kotlin.resolve.BindingContext.DECLARATION_TO_DESCRIPTOR;
import static org.jetbrains.kotlin.resolve.BindingContext.LABEL_TARGET;
import static org.jetbrains.kotlin.resolve.BindingContext.REFERENCE_TARGET;
import static org.jetbrains.kotlin.resolve.BindingContextUtils.isVarCapturedInClosure;

public final class ExpressionVisitor extends TranslatorVisitor<JsNode> {
    @Override
    protected JsNode emptyResult(@NotNull TranslationContext context) {
        return context.getEmptyExpression();
    }

    @Override
    @NotNull
    public JsNode visitConstantExpression(@NotNull KtConstantExpression expression, @NotNull TranslationContext context) {
        return translateConstantExpression(expression, context).source(expression);
    }

    @NotNull
    private static JsNode translateConstantExpression(@NotNull KtConstantExpression expression, @NotNull TranslationContext context) {
        CompileTimeConstant<?> compileTimeValue = ConstantExpressionEvaluator.getConstant(expression, context.bindingContext());
        assert compileTimeValue != null : message(expression, "Expression is not compile time value: " + expression.getText() + " ");
        KotlinType expectedType = context.bindingContext().getType(expression);
        ConstantValue<?> constant = compileTimeValue.toConstantValue(expectedType != null ? expectedType : TypeUtils.NO_EXPECTED_TYPE);
        if (constant instanceof NullValue) {
            return JsLiteral.NULL;
        }
        Object value = constant.getValue();
        if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return context.program().getNumberLiteral(((Number) value).intValue());
        }
        else if (value instanceof Long) {
            return JsAstUtils.newLong((Long) value, context);
        }
        else if (value instanceof Number) {
            return context.program().getNumberLiteral(((Number) value).doubleValue());
        }
        else if (value instanceof Boolean) {
            return JsLiteral.getBoolean((Boolean) value);
        }

        //TODO: test
        if (value instanceof String) {
            return context.program().getStringLiteral((String) value);
        }
        if (value instanceof Character) {
            return context.program().getStringLiteral(value.toString());
        }

        throw new AssertionError(message(expression, "Unsupported constant expression: " + expression.getText() + " "));
    }

    @Override
    @NotNull
    public JsNode visitBlockExpression(@NotNull KtBlockExpression jetBlock, @NotNull TranslationContext context) {
        List<KtExpression> statements = jetBlock.getStatements();
        JsBlock jsBlock = new JsBlock();
        for (KtExpression statement : statements) {
            JsNode jsNode = Translation.translateExpression(statement, context, jsBlock);
            JsStatement jsStatement = convertToStatement(jsNode);
            if (!JsAstUtils.isEmptyStatement(jsStatement)) {
                jsBlock.getStatements().add(jsStatement);
            }
        }
        return jsBlock;
    }

    @Override
    public JsNode visitDestructuringDeclaration(@NotNull KtDestructuringDeclaration multiDeclaration, @NotNull TranslationContext context) {
        KtExpression jetInitializer = multiDeclaration.getInitializer();
        assert jetInitializer != null : "Initializer for multi declaration must be not null";
        JsExpression initializer = Translation.translateAsExpression(jetInitializer, context);
        return DestructuringDeclarationTranslator.translate(multiDeclaration, context.scope().declareTemporary(), initializer, context);
    }

    @Override
    @NotNull
    public JsNode visitReturnExpression(@NotNull KtReturnExpression jetReturnExpression,
            @NotNull TranslationContext context) {
        KtExpression returned = jetReturnExpression.getReturnedExpression();

        // TODO: add related descriptor to context and use it here
        KtDeclarationWithBody parent = PsiTreeUtil.getParentOfType(jetReturnExpression, KtDeclarationWithBody.class);
        if (parent instanceof KtSecondaryConstructor) {
            return new JsReturn(new JsNameRef(Namer.ANOTHER_THIS_PARAMETER_NAME)).source(jetReturnExpression);
        }
        if (returned == null) {
            return new JsReturn(null).source(jetReturnExpression);
        }
        JsExpression jsReturnExpression = translateAsExpression(returned, context);
        if (JsAstUtils.isEmptyExpression(jsReturnExpression)) {
            return context.getEmptyExpression();
        }
        return new JsReturn(jsReturnExpression).source(jetReturnExpression);
    }

    @Override
    @NotNull
    public JsNode visitParenthesizedExpression(@NotNull KtParenthesizedExpression expression,
            @NotNull TranslationContext context) {
        KtExpression expressionInside = expression.getExpression();
        if (expressionInside != null) {
            return Translation.translateExpression(expressionInside, context);
        }
        return JsEmpty.INSTANCE;
    }

    @Override
    @NotNull
    public JsNode visitBinaryExpression(@NotNull KtBinaryExpression expression,
            @NotNull TranslationContext context) {
        return BinaryOperationTranslator.translate(expression, context);
    }

    @Override
    @NotNull
    // assume it is a local variable declaration
    public JsNode visitProperty(@NotNull KtProperty expression, @NotNull TranslationContext context) {
        VariableDescriptor descriptor = BindingContextUtils.getNotNull(context.bindingContext(), BindingContext.VARIABLE, expression);
        JsExpression initializer = translateInitializerForProperty(expression, context);
        if (initializer != null && JsAstUtils.isEmptyExpression(initializer)) {
            return context.getEmptyExpression();
        }

        JsName name = context.getNameForDescriptor(descriptor);
        if (isVarCapturedInClosure(context.bindingContext(), descriptor)) {
            JsNameRef alias = getCapturedVarAccessor(name.makeRef());
            initializer = JsAstUtils.wrapValue(alias, initializer == null ? JsLiteral.NULL : initializer);
        }

        return newVar(name, initializer).source(expression);
    }

    @Override
    @NotNull
    public JsNode visitCallableReferenceExpression(@NotNull KtCallableReferenceExpression expression, @NotNull TranslationContext context) {
        return CallableReferenceTranslator.INSTANCE.translate(expression, context);
    }

    @Override
    @NotNull
    public JsNode visitCallExpression(
            @NotNull KtCallExpression expression,
            @NotNull TranslationContext context
    ) {
        return CallExpressionTranslator.translate(expression, null, context).source(expression);
    }

    @Override
    @NotNull
    public JsNode visitIfExpression(@NotNull KtIfExpression expression, @NotNull TranslationContext context) {
        assert expression.getCondition() != null : "condition should not ne null: " + expression.getText();
        JsExpression testExpression = Translation.translateAsExpression(expression.getCondition(), context);
        if (JsAstUtils.isEmptyExpression(testExpression)) {
            return testExpression;
        }

        boolean isKotlinExpression = BindingContextUtilsKt.isUsedAsExpression(expression, context.bindingContext());

        KtExpression thenExpression = expression.getThen();
        KtExpression elseExpression = expression.getElse();

        JsStatement thenStatement =
                thenExpression != null ? Translation.translateAsStatementAndMergeInBlockIfNeeded(thenExpression, context) : null;
        JsStatement elseStatement =
                elseExpression != null ? Translation.translateAsStatementAndMergeInBlockIfNeeded(elseExpression, context) : null;

        if (isKotlinExpression) {
            JsExpression jsThenExpression = JsAstUtils.extractExpressionFromStatement(thenStatement);
            JsExpression jsElseExpression = JsAstUtils.extractExpressionFromStatement(elseStatement);
            boolean canBeJsExpression = jsThenExpression != null && jsElseExpression != null;
            if (canBeJsExpression) {
                return new JsConditional(testExpression, jsThenExpression, jsElseExpression).source(expression);
            }
        }
        JsIf ifStatement = new JsIf(testExpression, thenStatement, elseStatement);
        return ifStatement.source(expression);
    }

    @Override
    @NotNull
    public JsExpression visitSimpleNameExpression(@NotNull KtSimpleNameExpression expression,
            @NotNull TranslationContext context) {
        return ReferenceTranslator.translateSimpleNameWithQualifier(expression, null, context).source(expression);
    }

    @Override
    @NotNull
    public JsNode visitWhileExpression(@NotNull KtWhileExpression expression, @NotNull TranslationContext context) {
        return LoopTranslator.createWhile(false, expression, context);
    }

    @Override
    @NotNull
    public JsNode visitDoWhileExpression(@NotNull KtDoWhileExpression expression, @NotNull TranslationContext context) {
        return LoopTranslator.createWhile(true, expression, context);
    }

    @Override
    @NotNull
    public JsNode visitStringTemplateExpression(@NotNull KtStringTemplateExpression expression,
            @NotNull TranslationContext context) {
        JsStringLiteral stringLiteral = resolveAsStringConstant(expression, context);
        if (stringLiteral != null) {
            return stringLiteral;
        }
        return resolveAsTemplate(expression, context).source(expression);
    }

    @NotNull
    private static JsNode resolveAsTemplate(@NotNull KtStringTemplateExpression expression,
            @NotNull TranslationContext context) {
        return StringTemplateTranslator.translate(expression, context);
    }

    @Nullable
    private static JsStringLiteral resolveAsStringConstant(@NotNull KtExpression expression,
            @NotNull TranslationContext context) {
        Object value = getCompileTimeValue(context.bindingContext(), expression);
        if (value == null) {
            return null;
        }
        assert value instanceof String : "Compile time constant template should be a String constant.";
        String constantString = (String) value;
        return context.program().getStringLiteral(constantString);
    }

    @Override
    @NotNull
    public JsNode visitDotQualifiedExpression(@NotNull KtDotQualifiedExpression expression,
            @NotNull TranslationContext context) {
        return QualifiedExpressionTranslator.translateQualifiedExpression(expression, context);
    }

    @Override
    public JsNode visitLabeledExpression(
            @NotNull KtLabeledExpression expression, TranslationContext context
    ) {
        KtExpression baseExpression = expression.getBaseExpression();
        assert baseExpression != null;

        if (BindingContextUtilsKt.isUsedAsExpression(expression, context.bindingContext())) {
            return Translation.translateAsExpression(baseExpression, context).source(expression);
        }

        JsScope scope = context.scope();
        assert scope instanceof JsFunctionScope: "Labeled statement is unexpected outside of function scope";
        JsFunctionScope functionScope = (JsFunctionScope) scope;

        String labelIdent = getReferencedName(expression.getTargetLabel());

        JsName labelName = functionScope.enterLabel(labelIdent);
        JsStatement baseStatement = Translation.translateAsStatement(baseExpression, context);
        functionScope.exitLabel();

        return new JsLabel(labelName, baseStatement).source(expression);
    }

    @Override
    @NotNull
    public JsNode visitPrefixExpression(
            @NotNull KtPrefixExpression expression,
            @NotNull TranslationContext context
    ) {
        return UnaryOperationTranslator.translate(expression, context).source(expression);
    }

    @Override
    @NotNull
    public JsNode visitPostfixExpression(@NotNull KtPostfixExpression expression,
            @NotNull TranslationContext context) {
        return UnaryOperationTranslator.translate(expression, context).source(expression);
    }

    @Override
    @NotNull
    public JsNode visitIsExpression(@NotNull KtIsExpression expression,
            @NotNull TranslationContext context) {
        return Translation.patternTranslator(context).translateIsExpression(expression);
    }

    @Override
    @NotNull
    public JsNode visitSafeQualifiedExpression(@NotNull KtSafeQualifiedExpression expression,
            @NotNull TranslationContext context) {
        return QualifiedExpressionTranslator.translateQualifiedExpression(expression, context).source(expression);
    }

    @Override
    @Nullable
    public JsNode visitWhenExpression(@NotNull KtWhenExpression expression,
            @NotNull TranslationContext context) {
        return WhenTranslator.translate(expression, context);
    }

    @Override
    @NotNull
    public JsNode visitBinaryWithTypeRHSExpression(@NotNull KtBinaryExpressionWithTypeRHS expression,
            @NotNull TranslationContext context) {
        JsExpression jsExpression = Translation.translateAsExpression(expression.getLeft(), context);

        if (expression.getOperationReference().getReferencedNameElementType() != KtTokens.AS_KEYWORD)
            return jsExpression.source(expression);

        KtTypeReference right = expression.getRight();
        assert right != null;

        KotlinType rightType = BindingContextUtils.getNotNull(context.bindingContext(), BindingContext.TYPE, right);
        KotlinType leftType = BindingContextUtils.getTypeNotNull(context.bindingContext(), expression.getLeft());
        if (TypeUtils.isNullableType(rightType) || !TypeUtils.isNullableType(leftType)) {
            return jsExpression.source(expression);
        }

        // KT-2670
        // we actually do not care for types in js
        return TranslationUtils.sure(jsExpression, context).source(expression);
    }

    private static String getReferencedName(KtSimpleNameExpression expression) {
        return expression.getReferencedName()
                .replaceAll("^@", "")
                .replaceAll("(?:^`(.*)`$)", "$1");
    }

    private static JsNameRef getTargetLabel(KtExpressionWithLabel expression, TranslationContext context) {
        KtSimpleNameExpression labelElement = expression.getTargetLabel();
        if (labelElement == null) {
            return null;
        }

        String labelIdent = getReferencedName(labelElement);
        JsScope scope = context.scope();
        assert scope instanceof JsFunctionScope: "Labeled statement is unexpected outside of function scope";
        JsName labelName = ((JsFunctionScope) scope).findLabel(labelIdent);
        assert labelName != null;
        return labelName.makeRef();
    }

    @Override
    @NotNull
    public JsNode visitBreakExpression(@NotNull KtBreakExpression expression,
            @NotNull TranslationContext context) {
        return new JsBreak(getTargetLabel(expression, context)).source(expression);
    }

    @Override
    @NotNull
    public JsNode visitContinueExpression(@NotNull KtContinueExpression expression,
            @NotNull TranslationContext context) {
        return new JsContinue(getTargetLabel(expression, context)).source(expression);
    }

    @Override
    @NotNull
    public JsNode visitLambdaExpression(@NotNull KtLambdaExpression expression, @NotNull TranslationContext context) {
        return new LiteralFunctionTranslator(context).translate(expression.getFunctionLiteral());
    }

    @Override
    @NotNull
    public JsNode visitNamedFunction(@NotNull KtNamedFunction expression, @NotNull TranslationContext context) {
        JsExpression alias = new LiteralFunctionTranslator(context).translate(expression);

        FunctionDescriptor descriptor = getFunctionDescriptor(context.bindingContext(), expression);
        JsName name = context.getNameForDescriptor(descriptor);
        if (InlineUtil.isInline(descriptor)) {
            MetadataProperties.setStaticRef(name, alias);
        }

        boolean isExpression = BindingContextUtilsKt.isUsedAsExpression(expression, context.bindingContext());
        JsNode result = isExpression ? alias : JsAstUtils.newVar(name, alias);

        return result.source(expression);
    }

    @Override
    @NotNull
    public JsNode visitThisExpression(@NotNull KtThisExpression expression, @NotNull TranslationContext context) {
        DeclarationDescriptor thisExpression =
                getDescriptorForReferenceExpression(context.bindingContext(), expression.getInstanceReference());
        assert thisExpression != null : "This expression must reference a descriptor: " + expression.getText();

        return context.getDispatchReceiver(getReceiverParameterForDeclaration(thisExpression)).source(expression);
    }

    @Override
    @NotNull
    public JsNode visitArrayAccessExpression(@NotNull KtArrayAccessExpression expression,
            @NotNull TranslationContext context) {
        return AccessTranslationUtils.translateAsGet(expression, context);
    }

    @Override
    @NotNull
    public JsNode visitSuperExpression(@NotNull KtSuperExpression expression, @NotNull TranslationContext context) {
        DeclarationDescriptor superTarget = getSuperTarget(context, expression);
        ReceiverParameterDescriptor receiver = getReceiverParameterForDeclaration(superTarget);
        return context.getDispatchReceiver(receiver);
    }

    @Override
    @NotNull
    public JsNode visitForExpression(@NotNull KtForExpression expression,
            @NotNull TranslationContext context) {
        return LoopTranslator.translateForExpression(expression, context).source(expression);
    }

    @Override
    @NotNull
    public JsNode visitTryExpression(
            @NotNull KtTryExpression expression,
            @NotNull TranslationContext context
    ) {
        return new TryTranslator(expression, context).translate();
    }

    @Override
    @NotNull
    public JsNode visitThrowExpression(@NotNull KtThrowExpression expression,
            @NotNull TranslationContext context) {
        KtExpression thrownExpression = expression.getThrownExpression();
        assert thrownExpression != null : "Thrown expression must not be null";
        return new JsThrow(translateAsExpression(thrownExpression, context)).source(expression);
    }

    @Override
    @NotNull
    public JsNode visitObjectLiteralExpression(@NotNull KtObjectLiteralExpression expression,
            @NotNull TranslationContext context) {
        return ClassTranslator.generateObjectLiteral(expression.getObjectDeclaration(), context);
    }

    @Override
    @NotNull
    public JsNode visitObjectDeclaration(@NotNull KtObjectDeclaration expression,
            @NotNull TranslationContext context) {
        DeclarationDescriptor descriptor = getDescriptorForElement(context.bindingContext(), expression);
        JsName name = context.getNameForDescriptor(descriptor);
        JsExpression value = ClassTranslator.generateClassCreation(expression, context);
        return newVar(name, value).source(expression);
    }

    @Override
    public JsNode visitAnnotatedExpression(@NotNull KtAnnotatedExpression expression, TranslationContext context) {
        for (KtAnnotationEntry entry : expression.getAnnotationEntries()) {
            AnnotationDescriptor descriptor = context.bindingContext().get(BindingContext.ANNOTATION, entry);
            if (descriptor == null) continue;

            ClassifierDescriptor classifierDescriptor = descriptor.getType().getConstructor().getDeclarationDescriptor();
            if (classifierDescriptor == null) continue;

            KotlinRetention retention = DescriptorUtilsKt.getAnnotationRetention(classifierDescriptor);

            if (retention == KotlinRetention.SOURCE) {
                KtExpression baseExpression = expression.getBaseExpression();
                if (baseExpression == null) continue;

                return baseExpression.accept(this, context);
            }
        }

        return super.visitAnnotatedExpression(expression, context);
    }

    @NotNull
    private static DeclarationDescriptor getSuperTarget(TranslationContext context, KtSuperExpression expression) {
        BindingContext bindingContext = context.bindingContext();
        PsiElement labelPsi = bindingContext.get(LABEL_TARGET, expression.getTargetLabel());
        ClassDescriptor labelTarget = (ClassDescriptor) bindingContext.get(DECLARATION_TO_DESCRIPTOR, labelPsi);
        if (labelTarget != null) return labelTarget;

        DeclarationDescriptor descriptor = bindingContext.get(REFERENCE_TARGET, expression.getInstanceReference());
        assert descriptor != null : "Missing declaration descriptor: " + PsiUtilsKt.getTextWithLocation(expression);
        return descriptor;
    }
}
