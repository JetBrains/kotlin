/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.translate.declaration;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.backend.common.DataClassMethodGenerator;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.UtilsKt;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.resolve.source.KotlinSourceElementKt;

import java.util.List;

import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.and;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.or;

abstract class JsEqualsHashcodeToStringGenerator extends DataClassMethodGenerator {
    protected final TranslationContext context;

    protected JsEqualsHashcodeToStringGenerator(KtClassOrObject klass, TranslationContext context) {
        super(klass, context.bindingContext());
        this.context = context;
    }

    @Override
    public void generateToStringMethod(@NotNull FunctionDescriptor function, @NotNull List<? extends PropertyDescriptor> classProperties) {
        // TODO: relax this limitation, with the data generation logic fixed.
        assert !classProperties.isEmpty();
        JsFunction functionObj = generateJsMethod(function);

        JsExpression result = null;
        for (int i = 0; i < classProperties.size(); i++) {
            String printName = classProperties.get(i).getName().asString();
            JsName name = context.getNameForDescriptor(classProperties.get(i));
            JsExpression literal = new JsStringLiteral((i == 0 ? (getClassDescriptor().getName() + "(") : ", ") + printName + "=");
            JsExpression expr = new JsInvocation(context.namer().kotlin("toString"), new JsNameRef(name, new JsThisRef()));
            PsiElement source = KotlinSourceElementKt.getPsi(classProperties.get(i).getSource());
            JsExpression component = JsAstUtils.sum(literal, expr).source(source);
            if (result == null) {
                result = component;
            }
            else {
                result = JsAstUtils.sum(result, component);
            }
        }
        assert result != null;
        result = JsAstUtils.sum(result, new JsStringLiteral(")"));

        JsReturn returnStatement = new JsReturn(result);
        returnStatement.setSource(getDeclaration());
        functionObj.getBody().getStatements().add(returnStatement);
    }

    @Override
    public void generateHashCodeMethod(@NotNull FunctionDescriptor function, @NotNull List<? extends PropertyDescriptor> classProperties) {
        JsFunction functionObj = generateJsMethod(function);

        List<JsStatement> statements = functionObj.getBody().getStatements();

        JsName varName = functionObj.getScope().declareName("result");

        JsVars resultVar = new JsVars(new JsVars.JsVar(varName, new JsIntLiteral(0)));
        resultVar.setSource(getDeclaration());
        statements.add(resultVar);

        for (PropertyDescriptor prop : classProperties) {
            // TODO: we should statically check that we can call hashCode method directly.
            JsName name = context.getNameForDescriptor(prop);
            JsExpression component = new JsInvocation(context.namer().kotlin("hashCode"), new JsNameRef(name, new JsThisRef()));
            JsExpression newHashValue = JsAstUtils.sum(JsAstUtils.mul(new JsNameRef(varName), new JsIntLiteral(31)), component);
            JsExpression assignment = JsAstUtils.assignment(new JsNameRef(varName),
                                                            new JsBinaryOperation(JsBinaryOperator.BIT_OR, newHashValue,
                                                                                  new JsIntLiteral(0)));
            statements.add(assignment.source(KotlinSourceElementKt.getPsi(prop.getSource())).makeStmt());
        }

        JsReturn returnStatement = new JsReturn(new JsNameRef(varName));
        returnStatement.setSource(getDeclaration());
        statements.add(returnStatement);
    }

    @Override
    public void generateEqualsMethod(@NotNull FunctionDescriptor function, @NotNull List<? extends PropertyDescriptor> classProperties) {
        assert !classProperties.isEmpty();
        JsFunction functionObj = generateJsMethod(function);
        JsFunctionScope funScope = functionObj.getScope();

        JsName paramName = funScope.declareName("other");
        functionObj.getParameters().add(new JsParameter(paramName));

        JsExpression referenceEqual = JsAstUtils.equality(new JsThisRef(), new JsNameRef(paramName));
        JsExpression isNotNull = JsAstUtils.inequality(new JsNameRef(paramName), new JsNullLiteral());
        JsExpression otherIsObject = JsAstUtils.typeOfIs(paramName.makeRef(), new JsStringLiteral("object"));
        JsExpression prototypeEqual =
                JsAstUtils.equality(new JsInvocation(new JsNameRef("getPrototypeOf", new JsNameRef("Object")), new JsThisRef()),
                                    new JsInvocation(new JsNameRef("getPrototypeOf", new JsNameRef("Object")), new JsNameRef(paramName)));

        JsExpression fieldChain = null;
        for (PropertyDescriptor prop : classProperties) {
            JsName name = context.getNameForDescriptor(prop);
            PsiElement source = KotlinSourceElementKt.getPsi(prop.getSource());
            JsExpression next = new JsInvocation(context.namer().kotlin("equals"),
                                                 new JsNameRef(name, new JsThisRef()),
                                                 new JsNameRef(name, new JsNameRef(paramName))).source(source);
            if (fieldChain == null) {
                fieldChain = next;
            }
            else {
                fieldChain = and(fieldChain, next);
            }
        }
        assert fieldChain != null;

        JsExpression returnExpression = or(referenceEqual, and(isNotNull, and(otherIsObject, and(prototypeEqual, fieldChain))));
        JsReturn returnStatement = new JsReturn(returnExpression);
        returnStatement.setSource(getDeclaration());
        functionObj.getBody().getStatements().add(returnStatement);
    }

    @Override
    protected void generateComponentFunction(
            @NotNull FunctionDescriptor function, @NotNull ValueParameterDescriptor parameter
    ) {
        // Do nothing
    }

    @Override
    protected void generateCopyFunction(
            @NotNull FunctionDescriptor function, @NotNull List<? extends KtParameter> constructorParameters
    ) {
        // Do nothing
    }

    protected JsFunction generateJsMethod(@NotNull FunctionDescriptor functionDescriptor) {
        JsFunction functionObject = context.createRootScopedFunction(functionDescriptor);
        functionObject.setSource(getDeclaration());
        ClassDescriptor containingClass = (ClassDescriptor) functionDescriptor.getContainingDeclaration();
        context.addDeclarationStatement(UtilsKt.addFunctionToPrototype(context, containingClass, functionDescriptor, functionObject));
        return functionObject;
    }
}
