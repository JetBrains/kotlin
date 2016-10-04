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
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.declaration.ClassTranslator;
import org.jetbrains.kotlin.js.translate.declaration.PropertyTranslatorKt;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.js.translate.general.TranslatorVisitor;
import org.jetbrains.kotlin.js.translate.operation.BinaryOperationTranslator;
import org.jetbrains.kotlin.js.translate.operation.UnaryOperationTranslator;
import org.jetbrains.kotlin.js.translate.reference.*;
import org.jetbrains.kotlin.js.translate.utils.BindingUtils;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.UtilsKt;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilsKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.kotlin.types.expressions.DoubleColonLHS;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.kotlin.js.translate.context.Namer.GET_KCLASS;
import static org.jetbrains.kotlin.js.translate.context.Namer.GET_KCLASS_FROM_EXPRESSION;
import static org.jetbrains.kotlin.js.translate.context.Namer.getCapturedVarAccessor;
import static org.jetbrains.kotlin.js.translate.general.Translation.translateAsExpression;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.*;
import static org.jetbrains.kotlin.js.translate.utils.ErrorReportingUtils.message;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.convertToStatement;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.newVar;
import static org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.getReceiverParameterForDeclaration;
import static org.jetbrains.kotlin.js.translate.utils.TranslationUtils.translateInitializerForProperty;
import static org.jetbrains.kotlin.resolve.BindingContext.*;
import static org.jetbrains.kotlin.resolve.BindingContextUtils.isVarCapturedInClosure;
import static org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt.getResolvedCallWithAssert;
import static org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.isFunctionExpression;
import static org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.isFunctionLiteral;

public final class ExpressionVisitor extends TranslatorVisitor<JsNode> {
    @Override
    protected JsNode emptyResult(@NotNull TranslationContext context) {
        return JsLiteral.NULL;
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

        JsNode result = Translation.translateConstant(compileTimeValue, expression, context);
        if (result == null) {
            throw new AssertionError(message(expression, "Unsupported constant expression: " + expression.getText() + " "));
        }

        return result;
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
    public JsNode visitReturnExpression(@NotNull KtReturnExpression jetReturnExpression, @NotNull TranslationContext context) {
        KtExpression returned = jetReturnExpression.getReturnedExpression();

        // TODO: add related descriptor to context and use it here
        KtDeclarationWithBody parent = PsiTreeUtil.getParentOfType(jetReturnExpression, KtDeclarationWithBody.class);
        if (parent instanceof KtSecondaryConstructor) {
            return new JsReturn(new JsNameRef(Namer.ANOTHER_THIS_PARAMETER_NAME)).source(jetReturnExpression);
        }

        JsReturn jsReturn;
        if (returned == null) {
            jsReturn = new JsReturn(null);
        }
        else {
            JsExpression jsReturnExpression = translateAsExpression(returned, context);

            jsReturn = new JsReturn(jsReturnExpression);
        }

        MetadataProperties.setReturnTarget(jsReturn, getNonLocalReturnTarget(jetReturnExpression, context));

        return jsReturn.source(jetReturnExpression);
    }

    @Nullable
    private static FunctionDescriptor getNonLocalReturnTarget(
            @NotNull KtReturnExpression expression,
            @NotNull TranslationContext context
    ) {
        DeclarationDescriptor descriptor = context.getDeclarationDescriptor();
        assert descriptor instanceof CallableMemberDescriptor : "Return expression can only be inside callable declaration: " +
                                                                PsiUtilsKt.getTextWithLocation(expression);
        KtSimpleNameExpression target = expression.getTargetLabel();

        //call inside lambda
        if (isFunctionLiteral(descriptor) || isFunctionExpression(descriptor)) {
            if (target == null) {
                if (isFunctionLiteral(descriptor)) {
                    return BindingContextUtils.getContainingFunctionSkipFunctionLiterals(descriptor, true).getFirst();
                }
            }
            else {
                PsiElement element = context.bindingContext().get(LABEL_TARGET, target);
                descriptor = context.bindingContext().get(DECLARATION_TO_DESCRIPTOR, element);
            }
        }

        assert descriptor == null || descriptor instanceof FunctionDescriptor :
                "Function descriptor expected to be target of return label: " + PsiUtilsKt.getTextWithLocation(expression);
        return (FunctionDescriptor) descriptor;
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

        KtExpression delegateExpression = expression.getDelegateExpression();
        if (delegateExpression != null) {
            SmartList<JsPropertyInitializer> propertyInitializers = new SmartList<JsPropertyInitializer>();
            PropertyTranslatorKt.translateAccessors((VariableDescriptorWithAccessors) descriptor, propertyInitializers, context);
            assert propertyInitializers.size() == 1 : descriptor;
            initializer = propertyInitializers.get(0).getValueExpr();
            JsPropertyInitializer delegateInitializer = new JsPropertyInitializer(
                    context.program().getStringLiteral(Namer.getDelegateName(descriptor.getName().asString())),
                    Translation.translateAsExpression(delegateExpression, context)
            );
            ((JsObjectLiteral) initializer).getPropertyInitializers().add(delegateInitializer);
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
    public JsNode visitClassLiteralExpression(
            @NotNull KtClassLiteralExpression expression, TranslationContext context
    ) {
        KtExpression receiverExpression = expression.getReceiverExpression();
        assert receiverExpression != null : "Class literal expression should have a left-hand side";

        DoubleColonLHS lhs = context.bindingContext().get(DOUBLE_COLON_LHS, receiverExpression);
        assert lhs != null : "Class literal expression should have LHS resolved";

        if (lhs instanceof DoubleColonLHS.Expression && !((DoubleColonLHS.Expression) lhs).isObject()) {
            JsExpression receiver = translateAsExpression(receiverExpression, context);
            return new JsInvocation(context.namer().kotlin(GET_KCLASS_FROM_EXPRESSION), receiver);
        }

        return new JsInvocation(context.namer().kotlin(GET_KCLASS), UtilsKt.getReferenceToJsClass(lhs.getType(), context));
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
        return ReferenceTranslator.translateSimpleName(expression, context).source(expression);
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
    public JsNode visitStringTemplateExpression(@NotNull KtStringTemplateExpression expression, @NotNull TranslationContext context) {
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
    public JsNode visitDotQualifiedExpression(@NotNull KtDotQualifiedExpression expression, @NotNull TranslationContext context) {
        return QualifiedExpressionTranslator.translateQualifiedExpression(expression, context);
    }

    @Override
    public JsNode visitLabeledExpression(@NotNull KtLabeledExpression expression, @NotNull TranslationContext context) {
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
    public JsNode visitBinaryWithTypeRHSExpression(
            @NotNull KtBinaryExpressionWithTypeRHS expression,
            @NotNull TranslationContext context
    ) {
        JsExpression jsExpression;

        if (PatternTranslator.isCastExpression(expression)) {
            jsExpression = PatternTranslator.newInstance(context).translateCastExpression(expression);
        }
        else {
            jsExpression = Translation.translateAsExpression(expression.getLeft(), context);
        }

        return jsExpression.source(expression);
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
    public JsNode visitBreakExpression(@NotNull KtBreakExpression expression, @NotNull TranslationContext context) {
        return new JsBreak(getTargetLabel(expression, context)).source(expression);
    }

    @Override
    @NotNull
    public JsNode visitContinueExpression(@NotNull KtContinueExpression expression, @NotNull TranslationContext context) {
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
        ResolvedCall<? extends CallableDescriptor> resolvedCall = getResolvedCallWithAssert(expression, context.bindingContext());
        return context.getDispatchReceiver((ReceiverParameterDescriptor) resolvedCall.getResultingDescriptor());
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
    public JsNode visitObjectLiteralExpression(@NotNull KtObjectLiteralExpression expression, @NotNull TranslationContext context) {
        ClassDescriptor descriptor = BindingUtils.getClassDescriptor(context.bindingContext(), expression.getObjectDeclaration());
        ClassTranslator.TranslationResult result = translateClassOrObject(expression.getObjectDeclaration(), descriptor, context);
        List<JsPropertyInitializer> properties = result.getProperties();
        context.getDefinitionPlace().getProperties().addAll(properties);

        JsExpression constructor = context.getQualifiedReference(descriptor);
        List<DeclarationDescriptor> closure = context.getClassOrConstructorClosure(descriptor);
        List<JsExpression> closureArgs = new ArrayList<JsExpression>();
        if (closure != null) {
            for (DeclarationDescriptor capturedValue : closure) {
                closureArgs.add(context.getArgumentForClosureConstructor(capturedValue));
            }
        }

        // In case of object expressions like this:
        //   object : SuperClass(A, B, ...)
        // we may capture local variables in expressions A, B, etc. We don't want to generate local fields for these variables.
        // Our ClassTranslator is capable of such thing, but case of object expression is a little special.
        // Consider the following:
        //
        //   class A(val x: Int) {
        //      fun foo() { object : A(x) }
        //
        // By calling A(x) super constructor we capture `this` explicitly. However, we can't tell which `A::this` we are mentioning,
        // either `this` of an object literal or `this` of enclosing `class A`.
        // Frontend treats it as `this` of enclosing class declaration, therefore it expects backend to generate
        // super call in scope of `fun foo()` rather than define inner scope for object's constructor.
        // Thus we generate this call here rather than relying on ClassTranslator.
        ResolvedCall<FunctionDescriptor> superCall = BindingUtils.getSuperCall(context.bindingContext(),
                                                                               expression.getObjectDeclaration());
        if (superCall != null) {
            assert context.getDeclarationDescriptor() != null : "This expression should be inside declaration: " +
                    PsiUtilsKt.getTextWithLocation(expression);
            TranslationContext superCallContext = context.newDeclaration(context.getDeclarationDescriptor(), result.getDefinitionPlace());
            closureArgs.addAll(CallArgumentTranslator.translate(superCall, null, superCallContext).getTranslateArguments());
        }

        return new JsNew(constructor, closureArgs);
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

    @Override
    public JsNode visitClass(@NotNull KtClass klass, TranslationContext context) {
        ClassDescriptor descriptor = BindingUtils.getClassDescriptor(context.bindingContext(), klass);
        context.getDefinitionPlace().getProperties().addAll(translateClassOrObject(klass, descriptor, context).getProperties());
        return JsEmpty.INSTANCE;
    }

    @Override
    public JsNode visitTypeAlias(@NotNull KtTypeAlias typeAlias, TranslationContext data) {
        // Resolved by front-end, not used by backend
        return JsEmpty.INSTANCE;
    }

    private static ClassTranslator.TranslationResult translateClassOrObject(
            @NotNull KtClassOrObject declaration,
            @NotNull ClassDescriptor descriptor,
            @NotNull TranslationContext context
    ) {
        JsScope scope = context.getScopeForDescriptor(descriptor);
        TranslationContext classContext = context.innerWithUsageTracker(scope, descriptor);
        return ClassTranslator.translate(declaration, classContext);
    }
}
