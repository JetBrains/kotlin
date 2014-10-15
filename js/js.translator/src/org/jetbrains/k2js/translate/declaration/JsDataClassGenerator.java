/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.declaration;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.backend.common.CodegenUtil;
import org.jetbrains.jet.backend.common.DataClassMethodGenerator;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.utils.JsAstUtils;

import java.util.ArrayList;
import java.util.List;

class JsDataClassGenerator extends DataClassMethodGenerator {
    private final TranslationContext context;
    private final List<? super JsPropertyInitializer> output;

    JsDataClassGenerator(JetClassOrObject klass, TranslationContext context, List<? super JsPropertyInitializer> output) {
        super(klass, context.bindingContext());
        this.context = context;
        this.output = output;
    }

    @Override
    public void generateComponentFunction(@NotNull FunctionDescriptor function, @NotNull ValueParameterDescriptor parameter) {
        JsFunction functionObject = generateJsMethod(function);
        JsExpression returnExpression = propertyAccessor(JsLiteral.THIS, context.getNameForDescriptor(parameter).toString());
        functionObject.getBody().getStatements().add(new JsReturn(returnExpression));
    }

    @Override
    public void generateCopyFunction(@NotNull FunctionDescriptor function, @NotNull List<JetParameter> constructorParameters) {
        JsFunction functionObj = generateJsMethod(function);
        JsFunctionScope funScope = functionObj.getScope();

        assert function.getValueParameters().size() == constructorParameters.size();

        List<JsExpression> constructorArguments = new ArrayList<JsExpression>(constructorParameters.size());
        for (int i = 0; i < constructorParameters.size(); i++) {
            JetParameter constructorParam = constructorParameters.get(i);
            JsName paramName = funScope.declareName(constructorParam.getName());

            functionObj.getParameters().add(new JsParameter(paramName));

            JsExpression argumentValue;
            JsExpression parameterValue = new JsNameRef(paramName);
            if (!constructorParam.hasValOrVarNode()) {
                assert !function.getValueParameters().get(i).hasDefaultValue();
                // Caller cannot rely on default value and pass undefined here.
                argumentValue = parameterValue;
            }
            else {
                JsExpression defaultCondition = JsAstUtils.equality(new JsNameRef(paramName), context.namer().getUndefinedExpression());
                argumentValue = new JsConditional(defaultCondition,
                                                  propertyAccessor(JsLiteral.THIS, constructorParam.getName()),
                                                  parameterValue);
            }
            constructorArguments.add(argumentValue);
        }

        ClassDescriptor classDescriptor = (ClassDescriptor) function.getContainingDeclaration();
        ConstructorDescriptor constructor = classDescriptor.getConstructors().iterator().next();

        JsExpression constructorRef = context.getQualifiedReference(constructor);

        JsExpression returnExpression = new JsNew(constructorRef, constructorArguments);
        functionObj.getBody().getStatements().add(new JsReturn(returnExpression));
    }

    @Override
    public void generateToStringMethod(@NotNull List<PropertyDescriptor> classProperties) {
        // TODO: relax this limitation, with the data generation logic fixed.
        assert !classProperties.isEmpty();
        FunctionDescriptor prototypeFun = CodegenUtil.getAnyToStringMethod();
        JsFunction functionObj = generateJsMethod(prototypeFun);

        JsProgram jsProgram = context.program();
        JsExpression result = null;
        for (int i = 0; i < classProperties.size(); i++) {
            String name = classProperties.get(i).getName().toString();
            JsExpression literal = jsProgram.getStringLiteral((i == 0 ? (getClassDescriptor().getName() + "(") : ", ") + name + "=");
            JsExpression expr =
                    new JsInvocation(new JsNameRef("toString", Namer.KOTLIN_OBJECT_REF), propertyAccessor(JsLiteral.THIS, name));
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
    public void generateHashCodeMethod(@NotNull List<PropertyDescriptor> classProperties) {
        FunctionDescriptor prototypeFun = CodegenUtil.getAnyHashCodeMethod();
        JsFunction functionObj = generateJsMethod(prototypeFun);

        JsProgram jsProgram = context.program();
        List<JsStatement> statements = functionObj.getBody().getStatements();

        JsName varName = functionObj.getScope().declareName("result");

        statements.add(new JsVars(new JsVars.JsVar(varName, JsNumberLiteral.ZERO)));

        for (PropertyDescriptor prop : classProperties) {
            // TODO: we should statically check that we can call hashCode method directly.
            JsExpression component = new JsInvocation(new JsNameRef("hashCode", Namer.KOTLIN_OBJECT_REF),
                                                      propertyAccessor(JsLiteral.THIS, prop.getName().toString()));
            JsExpression newHashValue = JsAstUtils.sum(JsAstUtils.mul(new JsNameRef(varName), jsProgram.getNumberLiteral(31)), component);
            JsExpression assignment = JsAstUtils.assignment(new JsNameRef(varName),
                                                            new JsBinaryOperation(JsBinaryOperator.BIT_OR, newHashValue,
                                                                                  jsProgram.getNumberLiteral(0)));
            statements.add(assignment.makeStmt());
        }

        statements.add(new JsReturn(new JsNameRef(varName)));
    }

    @Override
    public void generateEqualsMethod(@NotNull List<PropertyDescriptor> classProperties) {
        assert !classProperties.isEmpty();
        FunctionDescriptor prototypeFun = CodegenUtil.getAnyEqualsMethod();
        JsFunction functionObj = generateJsMethod(prototypeFun);
        JsFunctionScope funScope = functionObj.getScope();

        JsName paramName = funScope.declareName("other");
        functionObj.getParameters().add(new JsParameter(paramName));

        JsExpression referenceEqual = JsAstUtils.equality(JsLiteral.THIS, new JsNameRef(paramName));
        JsExpression isNotNull = JsAstUtils.inequality(new JsNameRef(paramName), JsLiteral.NULL);
        JsExpression prototypeEqual =
                JsAstUtils.equality(new JsInvocation(new JsNameRef("getPrototypeOf", new JsNameRef("Object")), JsLiteral.THIS),
                                    new JsInvocation(new JsNameRef("getPrototypeOf", new JsNameRef("Object")), new JsNameRef(paramName)));

        JsExpression fieldChain = null;
        for (PropertyDescriptor prop : classProperties) {
            String name = prop.getName().toString();
            JsExpression next = new JsInvocation(new JsNameRef("equals", Namer.KOTLIN_OBJECT_REF),
                                                 propertyAccessor(JsLiteral.THIS, name),
                                                 propertyAccessor(new JsNameRef(paramName), name));
            if (fieldChain == null) {
                fieldChain = next;
            }
            else {
                fieldChain = JsAstUtils.and(fieldChain, next);
            }
        }
        assert fieldChain != null;

        JsExpression returnExpression =
                JsAstUtils.or(referenceEqual, JsAstUtils.and(isNotNull, JsAstUtils.and(prototypeEqual, fieldChain)));
        functionObj.getBody().getStatements().add(new JsReturn(returnExpression));
    }

    private static JsExpression propertyAccessor(JsExpression object, String propertyName) {
        // Might be not accurate enough.
        return new JsNameRef(propertyName, object);
    }

    private JsFunction generateJsMethod(@NotNull FunctionDescriptor functionDescriptor) {
        JsName functionName = context.getNameForDescriptor(functionDescriptor);
        JsScope enclosingScope = context.scope();
        JsFunction functionObject = JsAstUtils.createFunctionWithEmptyBody(enclosingScope);
        JsPropertyInitializer initializer = new JsPropertyInitializer(functionName.makeRef(), functionObject);
        output.add(initializer);
        return functionObject;
    }
}
