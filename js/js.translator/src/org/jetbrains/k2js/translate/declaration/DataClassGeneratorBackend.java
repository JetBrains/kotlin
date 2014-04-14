package org.jetbrains.k2js.translate.declaration;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegencommon.DataClassMethodGenerator;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.utils.JsAstUtils;

import java.util.ArrayList;
import java.util.List;

class DataClassGeneratorBackend implements DataClassMethodGenerator.Backend {
    private final TranslationContext context;
    private final List<? super JsPropertyInitializer> output;

    DataClassGeneratorBackend(TranslationContext context, List<? super JsPropertyInitializer> output) {
        this.context = context;
        this.output = output;
    }

    @Override
    public void generateComponentFunction(@NotNull FunctionDescriptor function, @NotNull ValueParameterDescriptor parameter) {
        JsFunction functionObject = generateJsMethod(function, false);
        JsExpression returnExpression = propertyAccessor(JsLiteral.THIS, parameter.getName().toString());
        functionObject.getBody().getStatements().add(new JsReturn(returnExpression));
    }

    @Override
    public void generateCopyFunction(@NotNull FunctionDescriptor function, @NotNull List<JetParameter> constructorParameters) {
        JsFunction functionObj = generateJsMethod(function, false);
        JsScope funScope = functionObj.getScope();

        assert function.getValueParameters().size() == constructorParameters.size();

        List<JsExpression> constructorArguments = new ArrayList<JsExpression>(constructorParameters.size());
        for (int i = 0; i < constructorParameters.size(); i++) {
            JetParameter constructorParam = constructorParameters.get(i);
            JsName paramName = funScope.declareName(constructorParam.getName());

            functionObj.getParameters().add(new JsParameter(paramName));

            JsExpression argumentValue;
            JsExpression parameterValue = new JsNameRef(paramName);
            if (constructorParam.getValOrVarNode() == null) {
                assert !function.getValueParameters().get(i).hasDefaultValue();
                // Caller cannot rely on default value and pass undefined here.
                argumentValue = parameterValue;
            } else {
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
        assert !classProperties.isEmpty();
        FunctionDescriptor prototypeFun = DataClassMethodGenerator.getAnyToStringMethod(KotlinBuiltIns.getInstance());
        JsFunction functionObj = generateJsMethod(prototypeFun, true);

        JsProgram jsProgram = context.program();
        JsExpression result = null;
        for (int i = 0; i < classProperties.size(); i++) {
            String name = classProperties.get(i).getName().toString();
            JsExpression literal = jsProgram.getStringLiteral((i == 0 ? "(" : ", ") + name + "=");
            JsExpression expr = new JsInvocation(new JsNameRef("String"), propertyAccessor(JsLiteral.THIS, name));
            JsExpression component = JsAstUtils.sum(literal, expr);
            if (result == null) {
                result = component;
            } else {
                result = JsAstUtils.sum(result, component);
            }
        }
        result = JsAstUtils.sum(result, jsProgram.getStringLiteral(")"));
        functionObj.getBody().getStatements().add(new JsReturn(result));
    }

    @Override
    public void generateHashCodeMethod(@NotNull List<PropertyDescriptor> classProperties) {
        FunctionDescriptor prototypeFun = DataClassMethodGenerator.getAnyHashCodeMethod(KotlinBuiltIns.getInstance());
        JsFunction functionObj = generateJsMethod(prototypeFun, true);
        // TODO: implement this.
        functionObj.getBody().getStatements().add(new JsThrow(context.program().getStringLiteral("Not implemented")));
    }

    @Override
    public void generateEqualsMethod(@NotNull List<PropertyDescriptor> classProperties) {
        assert !classProperties.isEmpty();
        FunctionDescriptor prototypeFun = DataClassMethodGenerator.getAnyEqualsMethod(KotlinBuiltIns.getInstance());
        JsFunction functionObj = generateJsMethod(prototypeFun, true);
        JsScope funScope = functionObj.getScope();

        JsName paramName = funScope.declareName("other");
        functionObj.getParameters().add(new JsParameter(paramName));

        JsExpression referenceEqual = JsAstUtils.equality(JsLiteral.THIS, new JsNameRef(paramName));
        JsExpression isNotNull = JsAstUtils.inequality(new JsNameRef(paramName), JsLiteral.NULL);
        JsExpression prototypeEqual = JsAstUtils.equality(new JsNameRef("prototype", JsLiteral.THIS),
                                                          new JsNameRef("prototype", new JsNameRef(paramName)));

        JsExpression fieldChain = null;
        for (PropertyDescriptor prop : classProperties) {
            String name = prop.getName().toString();
            JsExpression next = new JsInvocation(new JsNameRef("equals", new JsNameRef("Kotlin")),
                                                propertyAccessor(JsLiteral.THIS, name),
                                                propertyAccessor(new JsNameRef(paramName), name));
            if (fieldChain == null) {
                fieldChain = next;
            } else {
                fieldChain = JsAstUtils.and(fieldChain, next);
            }
        }

        JsExpression returnExpression = JsAstUtils.or(referenceEqual, JsAstUtils.and(isNotNull, JsAstUtils.and(prototypeEqual, fieldChain)));
        functionObj.getBody().getStatements().add(new JsReturn(returnExpression));
    }

    private static JsExpression propertyAccessor(JsExpression object, String propertyName) {
        // Might be not accurate enough.
        return new JsNameRef(propertyName, object);
    }

    private JsFunction generateJsMethod(@NotNull FunctionDescriptor functionDescriptor, boolean isPrototype) {
        JsName functionName = context.getNameForDescriptor(functionDescriptor);
        JsFunction functionObject;
        if (isPrototype) {
            JsScope enclosingScope = context.scope();
            functionObject = JsAstUtils.createFunctionWithEmptyBody(enclosingScope);
        } else {
            functionObject = context.getFunctionObject(functionDescriptor);
        }
        JsPropertyInitializer initializer = new JsPropertyInitializer(functionName.makeRef(), functionObject);
        output.add(initializer);
        return functionObject;
    }
}
