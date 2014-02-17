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

package org.jetbrains.k2js.translate.initializer;

import com.google.dart.compiler.backend.js.ast.*;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDelegationSpecifier;
import org.jetbrains.jet.lang.psi.JetDelegatorToSuperCall;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.reference.CallArgumentTranslator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getClassDescriptorForType;
import static org.jetbrains.k2js.translate.utils.BindingUtils.*;
import static org.jetbrains.k2js.translate.utils.FunctionBodyTranslator.setDefaultValueForArguments;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getPrimaryConstructorParameters;

public final class ClassInitializerTranslator extends AbstractTranslator {
    @NotNull
    private final JetClassOrObject classDeclaration;
    @NotNull
    private final List<JsStatement> initializerStatements = new SmartList<JsStatement>();

    public ClassInitializerTranslator(
            @NotNull JetClassOrObject classDeclaration,
            @NotNull TranslationContext context
    ) {
        // Note: it's important we use scope for class descriptor because anonymous function used in property initializers
        // belong to the properties themselves
        super(context.newDeclaration(getConstructor(context.bindingContext(), classDeclaration), null));
        this.classDeclaration = classDeclaration;
    }

    @NotNull
    public JsFunction generateInitializeMethod() {
        //TODO: it's inconsistent that we have scope for class and function for constructor, currently have problems implementing better way
        ConstructorDescriptor primaryConstructor = getConstructor(bindingContext(), classDeclaration);
        JsFunction result = context().getFunctionObject(primaryConstructor);
        //NOTE: while we translate constructor parameters we also add property initializer statements
        // for properties declared as constructor parameters
        result.getParameters().addAll(translatePrimaryConstructorParameters());
        mayBeAddCallToSuperMethod(result);
        new InitializerVisitor(initializerStatements).traverseContainer(classDeclaration, context());

        List<JsStatement> statements = result.getBody().getStatements();
        statements.addAll(setDefaultValueForArguments(primaryConstructor, context()));
        for (JsStatement statement : initializerStatements) {
            if (statement instanceof JsBlock) {
                statements.addAll(((JsBlock) statement).getStatements());
            }
            else {
                statements.add(statement);
            }
        }

        return result;
    }

    @NotNull
    public JsExpression generateEnumEntryInstanceCreation(@NotNull JetType enumClassType) {
        JetDelegatorToSuperCall superCall = getSuperCall();
        List<JsExpression> arguments;
        if (superCall != null) {
            arguments = translateArguments(superCall);
        } else {
            arguments = Collections.emptyList();
        }
        JsNameRef reference = context().getQualifiedReference(getClassDescriptorForType(enumClassType));
        return new JsNew(reference, arguments);
    }

    private void mayBeAddCallToSuperMethod(JsFunction initializer) {
        if (classDeclaration.hasModifier(JetTokens.ENUM_KEYWORD)) {
            addCallToSuperMethod(Collections.<JsExpression>emptyList(), initializer);
            return;
        }
        if (hasAncestorClass(bindingContext(), classDeclaration)) {
            JetDelegatorToSuperCall superCall = getSuperCall();
            if (superCall == null) {
                return;
            }
            addCallToSuperMethod(translateArguments(superCall), initializer);
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
    private List<JsExpression> translateArguments(@NotNull JetDelegatorToSuperCall superCall) {
        ResolvedCall<? extends CallableDescriptor> call = context().bindingContext().get(BindingContext.RESOLVED_CALL, superCall.getCalleeExpression());
        assert call != null : "ResolvedCall for superCall must be not null";
        return CallArgumentTranslator.translate(call, null, context()).getTranslateArguments();
    }

    @Nullable
    private JetDelegatorToSuperCall getSuperCall() {
        JetDelegatorToSuperCall result = null;
        for (JetDelegationSpecifier specifier : classDeclaration.getDelegationSpecifiers()) {
            if (specifier instanceof JetDelegatorToSuperCall) {
                result = (JetDelegatorToSuperCall) specifier;
            }
        }
        return result;
    }

    @NotNull
    List<JsParameter> translatePrimaryConstructorParameters() {
        List<JetParameter> parameterList = getPrimaryConstructorParameters(classDeclaration);
        List<JsParameter> result = new ArrayList<JsParameter>();
        for (JetParameter jetParameter : parameterList) {
            result.add(translateParameter(jetParameter));
        }
        return result;
    }

    @NotNull
    private JsParameter translateParameter(@NotNull JetParameter jetParameter) {
        DeclarationDescriptor parameterDescriptor =
                getDescriptorForElement(bindingContext(), jetParameter);
        JsName parameterName = context().getNameForDescriptor(parameterDescriptor);
        JsParameter jsParameter = new JsParameter(parameterName);
        mayBeAddInitializerStatementForProperty(jsParameter, jetParameter);
        return jsParameter;
    }

    private void mayBeAddInitializerStatementForProperty(@NotNull JsParameter jsParameter,
            @NotNull JetParameter jetParameter) {
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
