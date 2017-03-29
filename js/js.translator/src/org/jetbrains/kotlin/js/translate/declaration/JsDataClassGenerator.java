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

package org.jetbrains.kotlin.js.translate.declaration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.backend.common.DataClassMethodGenerator;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.UtilsKt;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.and;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.or;

class JsDataClassGenerator extends DataClassMethodGenerator {
    private final TranslationContext context;

    JsDataClassGenerator(KtClassOrObject klass, TranslationContext context) {
        super(klass, context.bindingContext());
        this.context = context;
    }

    @Override
    public void generateComponentFunction(@NotNull FunctionDescriptor function, @NotNull ValueParameterDescriptor parameter) {
        PropertyDescriptor propertyDescriptor = context.bindingContext().get(BindingContext.VALUE_PARAMETER_AS_PROPERTY, parameter);
        assert propertyDescriptor != null : "Property descriptor is expected to be non-null";

        JsFunction functionObject = generateJsMethod(function);
        JsExpression returnExpression = JsAstUtils.pureFqn(context.getNameForDescriptor(propertyDescriptor), JsLiteral.THIS);
        functionObject.getBody().getStatements().add(new JsReturn(returnExpression));
    }

    @Override
    public void generateCopyFunction(@NotNull FunctionDescriptor function, @NotNull List<? extends KtParameter> constructorParameters) {
        JsFunction functionObj = generateJsMethod(function);

        assert function.getValueParameters().size() == constructorParameters.size();

        List<JsExpression> constructorArguments = new ArrayList<>(constructorParameters.size());

        for (int i = 0; i < constructorParameters.size(); i++) {
            KtParameter constructorParam = constructorParameters.get(i);

            ValueParameterDescriptor parameterDescriptor = (ValueParameterDescriptor) BindingContextUtils.getNotNull(
                    context.bindingContext(), BindingContext.VALUE_PARAMETER, constructorParam);

            PropertyDescriptor propertyDescriptor = BindingContextUtils.getNotNull(
                    context.bindingContext(), BindingContext.VALUE_PARAMETER_AS_PROPERTY, parameterDescriptor);

            JsName fieldName = context.getNameForDescriptor(propertyDescriptor);
            JsName paramName = JsScope.declareTemporaryName(context.getNameForDescriptor(parameterDescriptor).getIdent());

            functionObj.getParameters().add(new JsParameter(paramName));

            JsExpression argumentValue;
            JsExpression parameterValue = new JsNameRef(paramName);
            if (!constructorParam.hasValOrVar()) {
                assert !DescriptorUtilsKt.hasDefaultValue(function.getValueParameters().get(i));
                // Caller cannot rely on default value and pass undefined here.
                argumentValue = parameterValue;
            }
            else {
                JsExpression defaultCondition = JsAstUtils.equality(new JsNameRef(paramName), Namer.getUndefinedExpression());
                argumentValue = new JsConditional(defaultCondition, new JsNameRef(fieldName, JsLiteral.THIS), parameterValue);
            }
            constructorArguments.add(argumentValue);
        }

        ClassDescriptor classDescriptor = (ClassDescriptor) function.getContainingDeclaration();
        ClassConstructorDescriptor constructor = classDescriptor.getUnsubstitutedPrimaryConstructor();
        assert constructor != null : "Data class should have primary constructor: " + classDescriptor;

        JsExpression constructorRef = context.getInnerReference(constructor);

        JsNew returnExpression = new JsNew(constructorRef, constructorArguments);
        if (context.shouldBeDeferred(constructor)) {
            context.deferConstructorCall(constructor, returnExpression.getArguments());
        }
        functionObj.getBody().getStatements().add(new JsReturn(returnExpression));
    }

    @Override
    public void generateToStringMethod(@NotNull FunctionDescriptor function, @NotNull List<? extends PropertyDescriptor> classProperties) {
        // TODO: relax this limitation, with the data generation logic fixed.
        assert !classProperties.isEmpty();
        JsFunction functionObj = generateJsMethod(function);

        JsProgram jsProgram = context.program();
        JsExpression result = null;
        for (int i = 0; i < classProperties.size(); i++) {
            String printName = classProperties.get(i).getName().asString();
            JsName name = context.getNameForDescriptor(classProperties.get(i));
            JsExpression literal = jsProgram.getStringLiteral((i == 0 ? (getClassDescriptor().getName() + "(") : ", ") + printName + "=");
            JsExpression expr = new JsInvocation(context.namer().kotlin("toString"), new JsNameRef(name, JsLiteral.THIS));
            JsExpression component = JsAstUtils.sum(literal, expr);
            if (result == null) {
                result = component;
            }
            else {
                result = JsAstUtils.sum(result, component);
            }
        }
        assert result != null;
        result = JsAstUtils.sum(result, jsProgram.getStringLiteral(")"));
        functionObj.getBody().getStatements().add(new JsReturn(result));
    }

    @Override
    public void generateHashCodeMethod(@NotNull FunctionDescriptor function, @NotNull List<? extends PropertyDescriptor> classProperties) {
        JsFunction functionObj = generateJsMethod(function);

        JsProgram jsProgram = context.program();
        List<JsStatement> statements = functionObj.getBody().getStatements();

        JsName varName = functionObj.getScope().declareName("result");

        statements.add(new JsVars(new JsVars.JsVar(varName, JsNumberLiteral.ZERO)));

        for (PropertyDescriptor prop : classProperties) {
            // TODO: we should statically check that we can call hashCode method directly.
            JsName name = context.getNameForDescriptor(prop);
            JsExpression component = new JsInvocation(context.namer().kotlin("hashCode"), new JsNameRef(name, JsLiteral.THIS));
            JsExpression newHashValue = JsAstUtils.sum(JsAstUtils.mul(new JsNameRef(varName), jsProgram.getNumberLiteral(31)), component);
            JsExpression assignment = JsAstUtils.assignment(new JsNameRef(varName),
                                                            new JsBinaryOperation(JsBinaryOperator.BIT_OR, newHashValue,
                                                                                  jsProgram.getNumberLiteral(0)));
            statements.add(assignment.makeStmt());
        }

        statements.add(new JsReturn(new JsNameRef(varName)));
    }

    @Override
    public void generateEqualsMethod(@NotNull FunctionDescriptor function, @NotNull List<? extends PropertyDescriptor> classProperties) {
        assert !classProperties.isEmpty();
        JsFunction functionObj = generateJsMethod(function);
        JsFunctionScope funScope = functionObj.getScope();

        JsName paramName = funScope.declareName("other");
        functionObj.getParameters().add(new JsParameter(paramName));

        JsExpression referenceEqual = JsAstUtils.equality(JsLiteral.THIS, new JsNameRef(paramName));
        JsExpression isNotNull = JsAstUtils.inequality(new JsNameRef(paramName), JsLiteral.NULL);
        JsExpression otherIsObject = JsAstUtils.typeOfIs(paramName.makeRef(), context.program().getStringLiteral("object"));
        JsExpression prototypeEqual =
                JsAstUtils.equality(new JsInvocation(new JsNameRef("getPrototypeOf", new JsNameRef("Object")), JsLiteral.THIS),
                                    new JsInvocation(new JsNameRef("getPrototypeOf", new JsNameRef("Object")), new JsNameRef(paramName)));

        JsExpression fieldChain = null;
        for (PropertyDescriptor prop : classProperties) {
            JsName name = context.getNameForDescriptor(prop);
            JsExpression next = new JsInvocation(context.namer().kotlin("equals"),
                                                 new JsNameRef(name, JsLiteral.THIS),
                                                 new JsNameRef(name, new JsNameRef(paramName)));
            if (fieldChain == null) {
                fieldChain = next;
            }
            else {
                fieldChain = and(fieldChain, next);
            }
        }
        assert fieldChain != null;

        JsExpression returnExpression = or(referenceEqual, and(isNotNull, and(otherIsObject, and(prototypeEqual, fieldChain))));
        functionObj.getBody().getStatements().add(new JsReturn(returnExpression));
    }

    private JsFunction generateJsMethod(@NotNull FunctionDescriptor functionDescriptor) {
        JsFunction functionObject = context.createRootScopedFunction(functionDescriptor);
        ClassDescriptor containingClass = (ClassDescriptor) functionDescriptor.getContainingDeclaration();
        context.addDeclarationStatement(UtilsKt.addFunctionToPrototype(context, containingClass, functionDescriptor, functionObject));
        return functionObject;
    }
}
