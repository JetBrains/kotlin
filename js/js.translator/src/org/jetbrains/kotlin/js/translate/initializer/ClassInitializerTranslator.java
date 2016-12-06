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

package org.jetbrains.kotlin.js.translate.initializer;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor;
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.context.UsageTracker;
import org.jetbrains.kotlin.js.translate.declaration.DelegationTranslator;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.reference.CallArgumentTranslator;
import org.jetbrains.kotlin.js.translate.utils.BindingUtils;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.jsAstUtils.AstUtilsKt;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtEnumEntry;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.*;
import static org.jetbrains.kotlin.js.translate.utils.FunctionBodyTranslator.setDefaultValueForArguments;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.pureFqn;
import static org.jetbrains.kotlin.js.translate.utils.PsiUtils.getPrimaryConstructorParameters;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.getClassDescriptorForType;

public final class ClassInitializerTranslator extends AbstractTranslator {
    @NotNull
    private final KtClassOrObject classDeclaration;
    @NotNull
    private final JsFunction initFunction;
    @NotNull
    private final TranslationContext context;
    @NotNull
    private final ClassDescriptor classDescriptor;

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
        ConstructorDescriptor primaryConstructor = classDescriptor.getUnsubstitutedPrimaryConstructor();

        if (primaryConstructor != null) {
            initFunction.getBody().getStatements().addAll(setDefaultValueForArguments(primaryConstructor, context()));

            mayBeAddCallToSuperMethod(initFunction);

            //NOTE: while we translate constructor parameters we also add property initializer statements
            // for properties declared as constructor parameters
            initFunction.getParameters().addAll(translatePrimaryConstructorParameters());

            // Initialize enum 'name' and 'ordinal' before translating property initializers.
            if (classDescriptor.getKind() == ClassKind.ENUM_CLASS) {
                addEnumClassParameters(initFunction);
            }
        }

        delegationTranslator.addInitCode(initFunction.getBody().getStatements());
        new InitializerVisitor().traverseContainer(classDeclaration, context().innerBlock(initFunction.getBody()));
    }

    private static void addEnumClassParameters(JsFunction constructorFunction) {
        JsName nameParamName = constructorFunction.getScope().declareFreshName("name");
        JsName ordinalParamName = constructorFunction.getScope().declareFreshName("ordinal");
        constructorFunction.getParameters().addAll(0, Arrays.asList(new JsParameter(nameParamName), new JsParameter(ordinalParamName)));

        constructorFunction.getBody().getStatements().add(JsAstUtils.assignmentToThisField(Namer.ENUM_NAME_FIELD, nameParamName.makeRef()));
        constructorFunction.getBody().getStatements().add(JsAstUtils.assignmentToThisField(Namer.ENUM_ORDINAL_FIELD, ordinalParamName.makeRef()));
    }

    private void addOuterClassReference(ClassDescriptor classDescriptor) {
        JsName outerName = context.getOuterClassReference(classDescriptor);
        if (outerName == null) return;

        initFunction.getParameters().add(0, new JsParameter(outerName));

        JsExpression paramRef = pureFqn(outerName, null);
        JsExpression assignment = JsAstUtils.assignment(pureFqn(outerName, JsLiteral.THIS), paramRef);
        initFunction.getBody().getStatements().add(new JsExpressionStatement(assignment));
    }

    @NotNull
    public static JsExpression generateEnumEntryInstanceCreation(
            @NotNull TranslationContext context,
            @NotNull KotlinType enumClassType,
            @NotNull KtClassOrObject classDeclaration,
            int ordinal
    ) {
        ResolvedCall<FunctionDescriptor> superCall = getSuperCall(context.bindingContext(), classDeclaration);

        JsExpression nameArg = context.program().getStringLiteral(classDeclaration.getName());
        JsExpression ordinalArg = context.program().getNumberLiteral(ordinal);
        List<JsExpression> additionalArgs = Arrays.asList(nameArg, ordinalArg);

        if (superCall == null) {
            ClassDescriptor classDescriptor = getClassDescriptorForType(enumClassType);
            JsNameRef reference = context.getInnerReference(classDescriptor);
            return new JsNew(reference, additionalArgs);
        }

        JsExpression call = CallTranslator.translate(context, superCall);
        if (call instanceof JsInvocation) {
            JsInvocation invocation = (JsInvocation) call;
            invocation.getArguments().addAll(0, additionalArgs);
        }
        else if (call instanceof JsNew) {
            JsNew invocation = (JsNew) call;
            invocation.getArguments().addAll(0, additionalArgs);
        }

        return call;
    }

    private void mayBeAddCallToSuperMethod(JsFunction initializer) {
        if (classDeclaration.hasModifier(KtTokens.ENUM_KEYWORD)) {
            addCallToSuperMethod(Collections.<JsExpression>emptyList(), initializer);
        }
        else if (hasAncestorClass(bindingContext(), classDeclaration)) {
            ResolvedCall<FunctionDescriptor> superCall = getSuperCall(bindingContext(), classDeclaration);
            if (superCall == null) {
                if (DescriptorUtils.isEnumEntry(classDescriptor)) {
                    addCallToSuperMethod(getAdditionalArgumentsForEnumConstructor(), initializer);
                }
                return;
            }

            if (classDeclaration instanceof KtEnumEntry) {
                JsExpression expression = CallTranslator.translate(context(), superCall, null);

                JsExpression fixedInvocation = AstUtilsKt.toInvocationWith(
                        expression, getAdditionalArgumentsForEnumConstructor(), 0, JsLiteral.THIS);
                initFunction.getBody().getStatements().add(fixedInvocation.makeStmt());
            }
            else {
                List<JsExpression> arguments = new ArrayList<JsExpression>();

                ConstructorDescriptor superDescriptor = (ConstructorDescriptor) superCall.getResultingDescriptor();
                if (superDescriptor instanceof TypeAliasConstructorDescriptor) {
                    superDescriptor = ((TypeAliasConstructorDescriptor) superDescriptor).getUnderlyingConstructorDescriptor();
                }

                List<DeclarationDescriptor> superclassClosure = context.getClassOrConstructorClosure(superDescriptor);
                if (superclassClosure != null) {
                    UsageTracker tracker = context.usageTracker();
                    assert tracker != null : "Closure exists, therefore UsageTracker must exist too. Translating constructor of " +
                                             classDescriptor;
                    for (DeclarationDescriptor capturedValue : superclassClosure) {
                        tracker.used(capturedValue);
                        arguments.add(tracker.getCapturedDescriptorToJsName().get(capturedValue).makeRef());
                    }
                }

                if (superDescriptor.getConstructedClass().isInner() && classDescriptor.isInner()) {
                    arguments.add(pureFqn(Namer.OUTER_FIELD_NAME, JsLiteral.THIS));
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
                    addCallToSuperMethod(arguments, initializer);
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

    @NotNull
    private List<JsExpression> getAdditionalArgumentsForEnumConstructor() {
        List<JsExpression> additionalArguments = new ArrayList<JsExpression>();
        additionalArguments.add(program().getStringLiteral(classDescriptor.getName().asString()));
        additionalArguments.add(program().getNumberLiteral(ordinal));
        return additionalArguments;
    }

    private void addCallToSuperMethod(@NotNull List<JsExpression> arguments, @NotNull JsFunction initializer) {
        if (initializer.getName() == null) {
            JsName ref = context().scope().declareName(Namer.CALLEE_NAME);
            initializer.setName(ref);
        }

        ClassDescriptor superclassDescriptor = DescriptorUtilsKt.getSuperClassOrAny(classDescriptor);
        JsExpression superConstructorRef = context().getInnerReference(superclassDescriptor);
        JsInvocation call = new JsInvocation(Namer.getFunctionCallRef(superConstructorRef));
        call.getArguments().add(JsLiteral.THIS);
        call.getArguments().addAll(arguments);
        initFunction.getBody().getStatements().add(call.makeStmt());
    }

    private void addCallToSuperSecondaryConstructor(@NotNull List<JsExpression> arguments, @NotNull ConstructorDescriptor descriptor) {
        JsExpression reference = context.getInnerReference(descriptor);
        JsInvocation call = new JsInvocation(reference);
        call.getArguments().addAll(arguments);
        call.getArguments().add(JsLiteral.THIS);
        initFunction.getBody().getStatements().add(call.makeStmt());
    }

    @NotNull
    private List<JsParameter> translatePrimaryConstructorParameters() {
        List<KtParameter> parameterList = getPrimaryConstructorParameters(classDeclaration);
        List<JsParameter> result = new ArrayList<JsParameter>();
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
}
