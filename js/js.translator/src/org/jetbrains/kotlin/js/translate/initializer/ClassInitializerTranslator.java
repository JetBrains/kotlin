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

package org.jetbrains.kotlin.js.translate.initializer;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.context.UsageTracker;
import org.jetbrains.kotlin.js.translate.declaration.DelegationTranslator;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.js.translate.reference.CallArgumentTranslator;
import org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator;
import org.jetbrains.kotlin.js.translate.utils.BindingUtils;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils;
import org.jetbrains.kotlin.js.translate.utils.jsAstUtils.AstUtilsKt;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtEnumEntry;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument;
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.types.typeUtil.TypeUtilsKt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.*;
import static org.jetbrains.kotlin.js.translate.utils.FunctionBodyTranslator.setDefaultValueForArguments;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.pureFqn;
import static org.jetbrains.kotlin.js.translate.utils.PsiUtils.getPrimaryConstructorParameters;

public final class ClassInitializerTranslator extends AbstractTranslator {
    @NotNull
    private final KtClassOrObject classDeclaration;
    @NotNull
    private final JsFunction initFunction;
    @NotNull
    private final TranslationContext context;
    @NotNull
    private final ClassDescriptor classDescriptor;

    private final ConstructorDescriptor primaryConstructor;

    private int ordinal;

    public ClassInitializerTranslator(
            @NotNull KtClassOrObject classDeclaration,
            @NotNull TranslationContext context,
            @NotNull JsFunction initFunction
    ) {
        super(context);
        this.classDeclaration = classDeclaration;
        this.initFunction = initFunction;
        this.context = context.contextWithScope(initFunction);
        classDescriptor = BindingUtils.getClassDescriptor(bindingContext(), classDeclaration);
        primaryConstructor = classDescriptor.getUnsubstitutedPrimaryConstructor();
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

    @NotNull
    @Override
    protected TranslationContext context() {
        return context;
    }

    public void generateInitializeMethod(DelegationTranslator delegationTranslator) {
        addOuterClassReference(classDescriptor);

        if (primaryConstructor != null) {
            initFunction.getBody().getStatements().addAll(setDefaultValueForArguments(primaryConstructor, context()));

            mayBeAddCallToSuperMethod(initFunction);

            //NOTE: while we translate constructor parameters we also add property initializer statements
            // for properties declared as constructor parameters
            initFunction.getParameters().addAll(translatePrimaryConstructorParameters());

            // Initialize enum 'name' and 'ordinal' before translating property initializers.
            if (classDescriptor.getKind() == ClassKind.ENUM_CLASS) {
                addEnumClassParameters(initFunction, classDeclaration);
            }
        }

        addThrowableCall();

        delegationTranslator.addInitCode(initFunction.getBody().getStatements());
        new InitializerVisitor().traverseContainer(classDeclaration, context().innerBlock(initFunction.getBody()));
    }

    private static void addEnumClassParameters(JsFunction constructorFunction, PsiElement psiElement) {
        JsName nameParamName = constructorFunction.getScope().declareFreshName("name");
        JsName ordinalParamName = constructorFunction.getScope().declareFreshName("ordinal");
        constructorFunction.getParameters().addAll(0, Arrays.asList(new JsParameter(nameParamName), new JsParameter(ordinalParamName)));

        JsStatement nameAssignment = JsAstUtils.assignmentToThisField(Namer.ENUM_NAME_FIELD, nameParamName.makeRef().source(psiElement));
        constructorFunction.getBody().getStatements().add(nameAssignment);

        JsStatement ordinalAssignment = JsAstUtils.assignmentToThisField(
                Namer.ENUM_ORDINAL_FIELD, ordinalParamName.makeRef().source(psiElement));
        constructorFunction.getBody().getStatements().add(ordinalAssignment);
    }

    private void addOuterClassReference(ClassDescriptor classDescriptor) {
        JsName outerName = context.getOuterClassReference(classDescriptor);
        if (outerName == null) return;

        initFunction.getParameters().add(0, new JsParameter(outerName));

        JsExpression paramRef = pureFqn(outerName, null);
        JsExpression assignment = JsAstUtils.assignment(pureFqn(outerName, new JsThisRef()), paramRef).source(classDeclaration);
        initFunction.getBody().getStatements().add(new JsExpressionStatement(assignment));
    }

    @NotNull
    public static JsExpression generateEnumEntryInstanceCreation(
            @NotNull TranslationContext context,
            @NotNull KtEnumEntry enumEntry,
            int ordinal
    ) {
        ResolvedCall<? extends FunctionDescriptor> resolvedCall = getSuperCall(context.bindingContext(), enumEntry);
        if (resolvedCall == null) {
            assert enumEntry.getInitializerList() == null : "Super call is missing on an enum entry with explicit initializer list " +
                                                            PsiUtilsKt.getTextWithLocation(enumEntry);
            resolvedCall = CallUtilKt.getFunctionResolvedCallWithAssert(enumEntry, context.bindingContext());
        }

        JsExpression nameArg = new JsStringLiteral(enumEntry.getName());
        JsExpression ordinalArg = new JsIntLiteral(ordinal);
        List<JsExpression> additionalArgs = Arrays.asList(nameArg, ordinalArg);

        JsExpression call = CallTranslator.translate(context, resolvedCall);
        if (call instanceof JsInvocation) {
            JsInvocation invocation = (JsInvocation) call;
            invocation.getArguments().addAll(0, additionalArgs);
        }
        else if (call instanceof JsNew) {
            JsNew invocation = (JsNew) call;
            invocation.getArguments().addAll(0, additionalArgs);
        }

        return call.source(enumEntry);
    }

    private void mayBeAddCallToSuperMethod(JsFunction initializer) {
        if (classDeclaration.hasModifier(KtTokens.ENUM_KEYWORD)) {
            addCallToSuperMethod(Collections.emptyList(), initializer, classDeclaration);
        }
        else if (hasAncestorClass(bindingContext(), classDeclaration)) {
            ResolvedCall<FunctionDescriptor> superCall = getSuperCall(bindingContext(), classDeclaration);

            if (superCall == null) {
                if (DescriptorUtils.isEnumEntry(classDescriptor)) {
                    addCallToSuperMethod(getAdditionalArgumentsForEnumConstructor(), initializer, classDeclaration);
                }
                return;
            }

            if (JsDescriptorUtils.isImmediateSubtypeOfError(classDescriptor)) {
                emulateSuperCallToNativeError(context, classDescriptor, superCall, new JsThisRef());
                return;
            }

            if (classDeclaration instanceof KtEnumEntry) {
                JsExpression expression = CallTranslator.translate(context(), superCall, null);
                expression.setSource(classDeclaration);

                JsExpression fixedInvocation = AstUtilsKt.toInvocationWith(
                        expression, getAdditionalArgumentsForEnumConstructor(), 0, new JsThisRef());
                initFunction.getBody().getStatements().add(fixedInvocation.makeStmt());
            }
            else {
                List<JsExpression> arguments = new ArrayList<>();

                ConstructorDescriptor superDescriptor = (ConstructorDescriptor) superCall.getResultingDescriptor();
                if (superDescriptor instanceof TypeAliasConstructorDescriptor) {
                    superDescriptor = ((TypeAliasConstructorDescriptor) superDescriptor).getUnderlyingConstructorDescriptor();
                }

                List<DeclarationDescriptor> superclassClosure = context.getClassOrConstructorClosure(superDescriptor);
                if (superclassClosure != null) {
                    UsageTracker tracker = context.usageTracker();
                    if (tracker != null) {
                        for (DeclarationDescriptor capturedValue : superclassClosure) {
                            tracker.used(capturedValue);
                            arguments.add(tracker.getCapturedDescriptorToJsName().get(capturedValue).makeRef());
                        }
                    }
                }

                if (superCall.getDispatchReceiver() != null) {
                    JsExpression receiver = context.getDispatchReceiver(JsDescriptorUtils.getReceiverParameterForReceiver(
                             superCall.getDispatchReceiver()));
                    arguments.add(receiver);
                }

                if (!DescriptorUtils.isAnonymousObject(classDescriptor)) {
                    arguments.addAll(CallArgumentTranslator.translate(superCall, null, context()).getTranslateArguments());
                }
                else {
                    for (ValueParameterDescriptor parameter : superDescriptor.getValueParameters()) {
                        JsName parameterName = context.getNameForDescriptor(parameter);
                        arguments.add(parameterName.makeRef());
                        initializer.getParameters().add(new JsParameter(parameterName));
                    }
                }

                if (superDescriptor.isPrimary()) {
                    addCallToSuperMethod(arguments, initializer, superCall.getCall().getCallElement());
                }
                else {
                    int maxValueArgumentIndex = 0;
                    for (ValueParameterDescriptor arg : superCall.getValueArguments().keySet()) {
                        ResolvedValueArgument resolvedArg = superCall.getValueArguments().get(arg);
                        if (!(resolvedArg instanceof DefaultValueArgument)) {
                            maxValueArgumentIndex = Math.max(maxValueArgumentIndex, arg.getIndex() + 1);
                        }
                    }
                    int padSize = superDescriptor.getValueParameters().size() - maxValueArgumentIndex;
                    while (padSize-- > 0) {
                        arguments.add(Namer.getUndefinedExpression());
                    }
                    addCallToSuperSecondaryConstructor(arguments, superDescriptor);
                }
            }
        }
    }

    public static void emulateSuperCallToNativeError(
            @NotNull TranslationContext context,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull ResolvedCall<? extends FunctionDescriptor> superCall,
            @NotNull JsExpression receiver
    ) {
        ClassDescriptor superClass = DescriptorUtilsKt.getSuperClassOrAny(classDescriptor);
        JsExpression superClassRef = ReferenceTranslator.translateAsTypeReference(superClass, context);
        JsExpression superInvocation = new JsInvocation(Namer.getFunctionCallRef(superClassRef), receiver.deepCopy());
        List<JsStatement> statements = context.getCurrentBlock().getStatements();
        statements.add(JsAstUtils.asSyntheticStatement(superInvocation));

        JsExpression messageArgument = Namer.getUndefinedExpression();
        JsExpression causeArgument = new JsNullLiteral();
        for (ValueParameterDescriptor param : superCall.getResultingDescriptor().getValueParameters()) {
            ResolvedValueArgument argument = superCall.getValueArguments().get(param);
            if (!(argument instanceof ExpressionValueArgument)) continue;

            ExpressionValueArgument exprArgument = (ExpressionValueArgument) argument;
            assert exprArgument.getValueArgument() != null;

            KtExpression value = exprArgument.getValueArgument().getArgumentExpression();
            assert value != null;
            JsExpression jsValue = Translation.translateAsExpression(value, context);

            if (KotlinBuiltIns.isStringOrNullableString(param.getType())) {
                messageArgument = context.cacheExpressionIfNeeded(jsValue);
            }
            else if (TypeUtilsKt.isConstructedFromClassWithGivenFqName(param.getType(), KotlinBuiltIns.FQ_NAMES.throwable)) {
                causeArgument = context.cacheExpressionIfNeeded(jsValue);
            }
            else {
                statements.add(JsAstUtils.asSyntheticStatement(jsValue));
            }
        }

        PropertyDescriptor messageProperty = DescriptorUtils.getPropertyByName(
                classDescriptor.getUnsubstitutedMemberScope(), Name.identifier("message"));
        JsExpression messageRef = pureFqn(context.getNameForBackingField(messageProperty), receiver.deepCopy());
        JsExpression messageIsUndefined = JsAstUtils.typeOfIs(messageArgument, new JsStringLiteral("undefined"));
        JsExpression causeIsNull = new JsBinaryOperation(JsBinaryOperator.NEQ, causeArgument, new JsNullLiteral());
        JsExpression causeToStringCond = JsAstUtils.and(messageIsUndefined, causeIsNull);
        JsExpression causeToString = new JsInvocation(pureFqn("toString", Namer.kotlinObject()), causeArgument.deepCopy());

        JsExpression correctedMessage;
        if (causeArgument instanceof JsNullLiteral) {
             correctedMessage = messageArgument.deepCopy();
        }
        else  {
            if (JsAstUtils.isUndefinedExpression(messageArgument)) {
                causeToStringCond = causeIsNull;
            }
            correctedMessage = new JsConditional(causeToStringCond, causeToString, messageArgument);
        }

        statements.add(JsAstUtils.asSyntheticStatement(JsAstUtils.assignment(messageRef, correctedMessage)));

        PropertyDescriptor causeProperty = DescriptorUtils.getPropertyByName(
                classDescriptor.getUnsubstitutedMemberScope(), Name.identifier("cause"));
        JsExpression causeRef = pureFqn(context.getNameForBackingField(causeProperty), receiver.deepCopy());
        statements.add(JsAstUtils.asSyntheticStatement(JsAstUtils.assignment(causeRef, causeArgument.deepCopy())));
    }

    @NotNull
    private List<JsExpression> getAdditionalArgumentsForEnumConstructor() {
        List<JsExpression> additionalArguments = new ArrayList<>();
        additionalArguments.add(new JsStringLiteral(classDescriptor.getName().asString()));
        additionalArguments.add(new JsIntLiteral(ordinal));
        return additionalArguments;
    }

    private void addCallToSuperMethod(@NotNull List<JsExpression> arguments, @NotNull JsFunction initializer, @NotNull PsiElement psi) {
        if (initializer.getName() == null) {
            JsName ref = context().scope().declareName(Namer.CALLEE_NAME);
            initializer.setName(ref);
        }

        ClassDescriptor superclassDescriptor = DescriptorUtilsKt.getSuperClassOrAny(classDescriptor);
        JsExpression superConstructorRef = context().getInnerReference(superclassDescriptor);
        JsInvocation call = new JsInvocation(Namer.getFunctionCallRef(superConstructorRef));
        call.setSource(psi);
        call.getArguments().add(new JsThisRef());
        call.getArguments().addAll(arguments);
        initFunction.getBody().getStatements().add(call.makeStmt());
    }

    private void addCallToSuperSecondaryConstructor(@NotNull List<JsExpression> arguments, @NotNull ConstructorDescriptor descriptor) {
        JsExpression reference = context.getInnerReference(descriptor);
        JsInvocation call = new JsInvocation(reference);
        call.getArguments().addAll(arguments);
        call.getArguments().add(new JsThisRef());
        initFunction.getBody().getStatements().add(call.makeStmt());
    }

    @NotNull
    private List<JsParameter> translatePrimaryConstructorParameters() {
        List<KtParameter> parameterList = getPrimaryConstructorParameters(classDeclaration);
        List<JsParameter> result = new ArrayList<>();
        for (KtParameter jetParameter : parameterList) {
            result.add(translateParameter(jetParameter));
        }
        return result;
    }

    @NotNull
    private JsParameter translateParameter(@NotNull KtParameter jetParameter) {
        DeclarationDescriptor parameterDescriptor = getDescriptorForElement(bindingContext(), jetParameter);
        JsName parameterName = context().getNameForDescriptor(parameterDescriptor);
        JsParameter jsParameter = new JsParameter(parameterName);
        mayBeAddInitializerStatementForProperty(jsParameter, jetParameter);
        return jsParameter;
    }

    private void mayBeAddInitializerStatementForProperty(@NotNull JsParameter jsParameter,
            @NotNull KtParameter jetParameter) {
        PropertyDescriptor propertyDescriptor = getPropertyDescriptorForConstructorParameter(bindingContext(), jetParameter);
        if (propertyDescriptor == null) {
            return;
        }
        JsNameRef initialValueForProperty = jsParameter.getName().makeRef();
        addInitializerOrPropertyDefinition(initialValueForProperty, propertyDescriptor);
    }

    private void addInitializerOrPropertyDefinition(@NotNull JsNameRef initialValue, @NotNull PropertyDescriptor propertyDescriptor) {
        initFunction.getBody().getStatements().add(
                InitializerUtils.generateInitializerForProperty(context(), propertyDescriptor, initialValue));
    }

    private void addThrowableCall() {
        if (!JsDescriptorUtils.isExceptionClass(classDescriptor)) return;

        if (JsDescriptorUtils.isImmediateSubtypeOfError(classDescriptor)) {
            ClassDescriptor superClass = DescriptorUtilsKt.getSuperClassOrAny(classDescriptor);
            JsExpression invocation = new JsInvocation(
                    pureFqn("captureStack", Namer.kotlinObject()),
                    ReferenceTranslator.translateAsTypeReference(superClass, context()),
                    new JsThisRef());
            initFunction.getBody().getStatements().add(JsAstUtils.asSyntheticStatement(invocation));
        }

        JsExpression nameLiteral = new JsStringLiteral(context.getInnerNameForDescriptor(classDescriptor).getIdent());
        JsExpression nameAssignment = JsAstUtils.assignment(pureFqn("name", new JsThisRef()), nameLiteral);
        initFunction.getBody().getStatements().add(JsAstUtils.asSyntheticStatement(nameAssignment));
    }
}
