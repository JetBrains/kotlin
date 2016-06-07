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

package org.jetbrains.kotlin.js.translate.initializer;

import com.google.dart.compiler.backend.js.ast.*;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.context.UsageTracker;
import org.jetbrains.kotlin.js.translate.declaration.DelegationTranslator;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.reference.CallArgumentTranslator;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.jsAstUtils.AstUtilsKt;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtEnumEntry;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.ArrayList;
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
    private final List<JsStatement> initializerStatements = new SmartList<JsStatement>();
    private final JsFunction initFunction;
    private final TranslationContext context;

    public ClassInitializerTranslator(
            @NotNull KtClassOrObject classDeclaration,
            @NotNull TranslationContext context
    ) {
        super(context);
        this.classDeclaration = classDeclaration;
        this.initFunction = createInitFunction(classDeclaration, context);
        this.context = context.contextWithScope(initFunction);
    }

    @NotNull
    @Override
    protected TranslationContext context() {
        return context;
    }

    @NotNull
    private static JsFunction createInitFunction(KtClassOrObject declaration, TranslationContext context) {
        //TODO: it's inconsistent that we have scope for class and function for constructor, currently have problems implementing better way
        ClassDescriptor classDescriptor = getClassDescriptor(context.bindingContext(), declaration);
        ConstructorDescriptor primaryConstructor = classDescriptor.getUnsubstitutedPrimaryConstructor();

        if (primaryConstructor != null) {
            return context.getFunctionObject(primaryConstructor);
        }
        else {
            return new JsFunction(context.scope(), new JsBlock(), "fake constructor for " + classDescriptor.getName().asString());
        }
    }

    @NotNull
    public JsFunction generateInitializeMethod(DelegationTranslator delegationTranslator) {
        ClassDescriptor classDescriptor = getClassDescriptor(bindingContext(), classDeclaration);
        addOuterClassReference(classDescriptor);
        ConstructorDescriptor primaryConstructor = classDescriptor.getUnsubstitutedPrimaryConstructor();

        if (primaryConstructor != null) {
            initFunction.getBody().getStatements().addAll(setDefaultValueForArguments(primaryConstructor, context()));

            //NOTE: while we translate constructor parameters we also add property initializer statements
            // for properties declared as constructor parameters
            initFunction.getParameters().addAll(translatePrimaryConstructorParameters());

            mayBeAddCallToSuperMethod(initFunction, classDescriptor);
        }

        delegationTranslator.addInitCode(initializerStatements);
        new InitializerVisitor(initializerStatements).traverseContainer(classDeclaration, context());

        List<JsStatement> statements = initFunction.getBody().getStatements();

        for (JsStatement statement : initializerStatements) {
            if (statement instanceof JsBlock) {
                statements.addAll(((JsBlock) statement).getStatements());
            }
            else {
                statements.add(statement);
            }
        }

        return initFunction;
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
    public JsExpression generateEnumEntryInstanceCreation(@NotNull KotlinType enumClassType) {
        ResolvedCall<FunctionDescriptor> superCall = getSuperCall(bindingContext(), classDeclaration);

        if (superCall == null) {
            ClassDescriptor classDescriptor = getClassDescriptorForType(enumClassType);
            JsNameRef reference = context().getQualifiedReference(classDescriptor);
            return new JsNew(reference);
        }

        return CallTranslator.translate(context(), superCall);
    }

    private void mayBeAddCallToSuperMethod(JsFunction initializer, @NotNull ClassDescriptor descriptor) {
        if (classDeclaration.hasModifier(KtTokens.ENUM_KEYWORD)) {
            addCallToSuperMethod(Collections.<JsExpression>emptyList(), initializer);
        }
        else if (hasAncestorClass(bindingContext(), classDeclaration)) {
            ResolvedCall<FunctionDescriptor> superCall = getSuperCall(bindingContext(), classDeclaration);
            if (superCall == null) {
                if (DescriptorUtils.isEnumEntry(descriptor)) {
                    addCallToSuperMethod(Collections.<JsExpression>emptyList(), initializer);
                }
                return;
            }

            if (classDeclaration instanceof KtEnumEntry) {
                JsExpression expression = CallTranslator.translate(context(), superCall, null);
                JsExpression fixedInvocation = AstUtilsKt.toInvocationWith(expression, Collections.<JsExpression>emptyList(),
                                                                           JsLiteral.THIS);
                initializerStatements.add(0, fixedInvocation.makeStmt());
            }
            else {
                List<JsExpression> arguments = new ArrayList<JsExpression>();

                ConstructorDescriptor superDescriptor = (ConstructorDescriptor) superCall.getResultingDescriptor();

                List<DeclarationDescriptor> superclassClosure = context.getClassOrConstructorClosure(superDescriptor);
                if (superclassClosure != null) {
                    UsageTracker tracker = context.usageTracker();
                    assert tracker != null : "Closure exists, therefore UsageTracker must exist too. Translating constructor of " +
                                             descriptor;
                    for (DeclarationDescriptor capturedValue : superclassClosure) {
                        tracker.used(capturedValue);
                        arguments.add(tracker.getCapturedDescriptorToJsName().get(capturedValue).makeRef());
                    }
                }

                if (superDescriptor.getContainingDeclaration().isInner() && descriptor.isInner()) {
                    arguments.add(pureFqn(Namer.OUTER_FIELD_NAME, JsLiteral.THIS));
                }

                if (!DescriptorUtils.isAnonymousObject(descriptor)) {
                    arguments.addAll(CallArgumentTranslator.translate(superCall, null, context()).getTranslateArguments());
                }
                else {
                    for (ValueParameterDescriptor parameter : superDescriptor.getValueParameters()) {
                        JsName parameterName = context.getNameForDescriptor(parameter);
                        arguments.add(parameterName.makeRef());
                        initializer.getParameters().add(new JsParameter(parameterName));
                    }
                }

                addCallToSuperMethod(arguments, initializer);
            }
        }
    }

    private void addCallToSuperMethod(@NotNull List<JsExpression> arguments, JsFunction initializer) {
        JsName ref = context().scope().declareName(Namer.CALLEE_NAME);
        initializer.setName(ref);
        JsInvocation call = new JsInvocation(Namer.getFunctionCallRef(Namer.superMethodNameRef(ref)));
        call.getArguments().add(JsLiteral.THIS);
        call.getArguments().addAll(arguments);
        initializerStatements.add(0, call.makeStmt());
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
        DeclarationDescriptor parameterDescriptor =
                getDescriptorForElement(bindingContext(), jetParameter);
        JsName parameterName = context().getNameForDescriptor(parameterDescriptor);
        JsParameter jsParameter = new JsParameter(parameterName);
        mayBeAddInitializerStatementForProperty(jsParameter, jetParameter);
        return jsParameter;
    }

    private void mayBeAddInitializerStatementForProperty(@NotNull JsParameter jsParameter,
            @NotNull KtParameter jetParameter) {
        PropertyDescriptor propertyDescriptor =
                getPropertyDescriptorForConstructorParameter(bindingContext(), jetParameter);
        if (propertyDescriptor == null) {
            return;
        }
        JsNameRef initialValueForProperty = jsParameter.getName().makeRef();
        addInitializerOrPropertyDefinition(initialValueForProperty, propertyDescriptor);
    }

    private void addInitializerOrPropertyDefinition(@NotNull JsNameRef initialValue, @NotNull PropertyDescriptor propertyDescriptor) {
        initializerStatements.add(InitializerUtils.generateInitializerForProperty(context(), propertyDescriptor, initialValue));
    }
}
