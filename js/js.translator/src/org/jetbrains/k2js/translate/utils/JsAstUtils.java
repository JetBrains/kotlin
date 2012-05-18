/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.utils;

import com.google.common.collect.Lists;
import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.k2js.translate.context.TranslationContext;

import java.util.*;

/**
 * @author Pavel Talanov
 */
public final class JsAstUtils {
    private static final JsNameRef VALUE = new JsNameRef("value");
    private static final JsPropertyInitializer WRITABLE = new JsPropertyInitializer(new JsNameRef("writable"), null);
    private static final JsNameRef DEFINE_PROPERTIES = new JsNameRef("defineProperties");
    private static final JsNameRef DEFINE_PROPERTY = new JsNameRef("defineProperty");
    private static final JsNameRef CREATE = new JsNameRef("create");
    private static final JsNameRef EMPTY_REF = new JsNameRef("");

    static {
        JsNameRef globalObjectReference = new JsNameRef("Object");
        DEFINE_PROPERTIES.setQualifier(globalObjectReference);
        DEFINE_PROPERTY.setQualifier(globalObjectReference);
        CREATE.setQualifier(globalObjectReference);
    }

    private JsAstUtils() {
    }

    @NotNull
    public static JsPropertyInitializer newNamedMethod(@NotNull JsName name, @NotNull JsFunction function) {
        JsNameRef methodName = name.makeRef();
        return new JsPropertyInitializer(methodName, function);
    }

    @NotNull
    public static JsStatement convertToStatement(@NotNull JsNode jsNode) {
        assert (jsNode instanceof JsExpression) || (jsNode instanceof JsStatement)
                : "Unexpected node of type: " + jsNode.getClass().toString();
        if (jsNode instanceof JsExpression) {
            return new JsExprStmt((JsExpression) jsNode);
        }
        return (JsStatement) jsNode;
    }

    @NotNull
    public static JsBlock convertToBlock(@NotNull JsNode jsNode) {
        if (jsNode instanceof JsBlock) {
            return (JsBlock) jsNode;
        }
        JsStatement jsStatement = convertToStatement(jsNode);
        return new JsBlock(jsStatement);
    }

    @NotNull
    public static JsExpression convertToExpression(@NotNull JsNode jsNode) {
        assert jsNode instanceof JsExpression : "Unexpected node of type: " + jsNode.getClass().toString();
        return (JsExpression) jsNode;
    }

    public static JsNameRef thisQualifiedReference(@NotNull JsName name) {
        JsNameRef result = name.makeRef();
        result.setQualifier(new JsThisRef());
        return result;
    }

    @NotNull
    public static JsBlock newBlock(List<JsStatement> statements) {
        JsBlock result = new JsBlock();
        setStatements(result, statements);
        return result;
    }

    @NotNull
    public static JsPrefixOperation negated(@NotNull JsExpression expression) {
        return new JsPrefixOperation(JsUnaryOperator.NOT, expression);
    }

    @NotNull
    public static JsBinaryOperation and(@NotNull JsExpression op1, @NotNull JsExpression op2) {
        return new JsBinaryOperation(JsBinaryOperator.AND, op1, op2);
    }

    @NotNull
    public static JsBinaryOperation or(@NotNull JsExpression op1, @NotNull JsExpression op2) {
        return new JsBinaryOperation(JsBinaryOperator.OR, op1, op2);
    }

    public static void setQualifier(@NotNull JsExpression selector, @Nullable JsExpression receiver) {
        assert (selector instanceof JsInvocation || selector instanceof JsNameRef);
        if (selector instanceof JsInvocation) {
            setQualifier(((JsInvocation) selector).getQualifier(), receiver);
            return;
        }
        setQualifierForNameRef((JsNameRef) selector, receiver);
    }

    private static void setQualifierForNameRef(@NotNull JsNameRef selector, @Nullable JsExpression receiver) {
        JsExpression qualifier = selector.getQualifier();
        if (qualifier == null) {
            selector.setQualifier(receiver);
        }
        else {
            setQualifier(qualifier, receiver);
        }
    }

    public static JsNameRef qualified(@NotNull JsName selector, @Nullable JsExpression qualifier) {
        JsNameRef reference = selector.makeRef();
        setQualifier(reference, qualifier);
        return reference;
    }

    @NotNull
    public static JsBinaryOperation equality(@NotNull JsExpression arg1, @NotNull JsExpression arg2) {
        return new JsBinaryOperation(JsBinaryOperator.EQ, arg1, arg2);
    }

    @NotNull
    public static JsBinaryOperation inequality(@NotNull JsExpression arg1, @NotNull JsExpression arg2) {
        return new JsBinaryOperation(JsBinaryOperator.NEQ, arg1, arg2);
    }

    @NotNull
    public static JsExpression assignment(@NotNull JsExpression left, @NotNull JsExpression right) {
        return new JsBinaryOperation(JsBinaryOperator.ASG, left, right);
    }

    @NotNull
    public static JsBinaryOperation sum(@NotNull JsExpression left, @NotNull JsExpression right) {
        return new JsBinaryOperation(JsBinaryOperator.ADD, left, right);
    }

    @NotNull
    public static JsBinaryOperation addAssign(@NotNull JsExpression left, @NotNull JsExpression right) {
        return new JsBinaryOperation(JsBinaryOperator.ASG_ADD, left, right);
    }

    @NotNull
    public static JsBinaryOperation subtract(@NotNull JsExpression left, @NotNull JsExpression right) {
        return new JsBinaryOperation(JsBinaryOperator.SUB, left, right);
    }

    @NotNull
    public static JsPrefixOperation not(@NotNull JsExpression expression) {
        return new JsPrefixOperation(JsUnaryOperator.NOT, expression);
    }

    @NotNull
    public static JsBinaryOperation typeof(@NotNull JsExpression expression, @NotNull JsStringLiteral string) {
        return equality(new JsPrefixOperation(JsUnaryOperator.TYPEOF, expression), string);
    }

    @NotNull
    public static JsFor generateForExpression(@NotNull JsVars initExpression,
            @NotNull JsExpression condition,
            @NotNull JsExpression incrExpression,
            @NotNull JsStatement body) {
        JsFor result = new JsFor();
        result.setInitVars(initExpression);
        result.setCondition(condition);
        result.setIncrExpr(incrExpression);
        result.setBody(body);
        return result;
    }

    public static boolean ownsName(@NotNull JsScope scope, @NotNull JsName name) {
        Iterator<JsName> nameIterator = scope.getAllNames();
        while (nameIterator.hasNext()) {
            if (nameIterator.next() == name) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public static JsObjectLiteral newObjectLiteral(@NotNull List<JsPropertyInitializer> propertyList) {
        JsObjectLiteral jsObjectLiteral = new JsObjectLiteral();
        jsObjectLiteral.getPropertyInitializers().addAll(propertyList);
        return jsObjectLiteral;
    }

    @NotNull
    public static JsVars newVar(@NotNull JsName name, @Nullable JsExpression expr) {
        JsVars.JsVar var = new JsVars.JsVar(name);
        if (expr != null) {
            var.setInitExpr(expr);
        }
        JsVars vars = new JsVars();
        vars.add(var);
        return vars;
    }

    public static void addVarDeclaration(@NotNull JsBlock block, @NotNull JsVars vars) {
        LinkedList<JsStatement> statementLinkedList = Lists.newLinkedList(block.getStatements());
        statementLinkedList.offer(vars);
        setStatements(block, statementLinkedList);
    }

    private static void setStatements(@NotNull JsBlock block, @NotNull List<JsStatement> statements) {
        List<JsStatement> statementList = block.getStatements();
        statementList.clear();
        statementList.addAll(statements);
    }

    public static void setArguments(@NotNull JsInvocation invocation, @NotNull List<JsExpression> newArgs) {
        List<JsExpression> arguments = invocation.getArguments();
        assert arguments.isEmpty() : "Arguments already set.";
        arguments.addAll(newArgs);
    }

    public static void setArguments(@NotNull JsNew invocation, @NotNull List<JsExpression> newArgs) {
        List<JsExpression> arguments = invocation.getArguments();
        assert arguments.isEmpty() : "Arguments already set.";
        arguments.addAll(newArgs);
    }

    public static void setArguments(@NotNull JsNew invocation, JsExpression... arguments) {
        setArguments(invocation, Arrays.asList(arguments));
    }

    public static void setParameters(@NotNull JsFunction function, @NotNull List<JsParameter> newParams) {
        List<JsParameter> parameters = function.getParameters();
        assert parameters.isEmpty() : "Arguments already set.";
        parameters.addAll(newParams);
    }

    public static void setParameters(@NotNull JsFunction function, JsParameter... arguments) {
        setParameters(function, Arrays.asList(arguments));
    }

    @NotNull
    public static JsInvocation newInvocation(@NotNull JsExpression target, List<JsExpression> params) {
        JsInvocation invoke = new JsInvocation();
        invoke.setQualifier(target);
        for (JsExpression expr : params) {
            invoke.getArguments().add(expr);
        }
        return invoke;
    }

    @NotNull
    public static JsExpression newSequence(@NotNull List<JsExpression> expressions) {
        assert !expressions.isEmpty();
        if (expressions.size() == 1) {
            return expressions.get(0);
        }
        JsExpression result = expressions.get(expressions.size() - 1);
        for (int i = expressions.size() - 2; i >= 0; i--) {
            result = new JsBinaryOperation(JsBinaryOperator.COMMA, expressions.get(i), result);
        }
        return result;
    }

    @NotNull
    public static JsFunction createFunctionWithEmptyBody(@NotNull JsScope parent) {
        JsFunction correspondingFunction = new JsFunction(parent);
        correspondingFunction.setBody(new JsBlock());
        return correspondingFunction;
    }

    @NotNull
    public static List<JsExpression> toStringLiteralList(@NotNull List<String> strings, @NotNull JsProgram program) {
        ArrayList<JsExpression> result = Lists.newArrayList();
        for (String str : strings) {
            result.add(program.getStringLiteral(str));
        }
        return result;
    }

    @NotNull
    public static JsStatement defineProperties(@NotNull JsObjectLiteral propertiesDefinition) {
        return AstUtil.newInvocation(DEFINE_PROPERTIES, new JsThisRef(), propertiesDefinition).makeStmt();
    }

    @NotNull
    public static JsPropertyInitializer propertyDescriptor(@NotNull PropertyDescriptor descriptor,
            @NotNull JsExpression value, @NotNull TranslationContext context) {
        return new JsPropertyInitializer(context.getNameForDescriptor(descriptor).makeRef(), createPropertyDescriptor(descriptor, value, context));
    }

    @NotNull
    public static JsInvocation defineProperty(@NotNull PropertyDescriptor descriptor,
            @NotNull JsExpression value, @NotNull TranslationContext context) {
        return AstUtil.newInvocation(DEFINE_PROPERTY, new JsThisRef(), context.program().getStringLiteral(context.getNameForDescriptor(descriptor).getIdent()),
                                     createPropertyDescriptor(descriptor, value, context));
    }

    @NotNull
    private static JsObjectLiteral createPropertyDescriptor(@NotNull PropertyDescriptor descriptor,
            @NotNull JsExpression value, @NotNull TranslationContext context) {
        JsObjectLiteral jsPropertyDescriptor = new JsObjectLiteral();
        List<JsPropertyInitializer> meta = jsPropertyDescriptor.getPropertyInitializers();
        meta.add(new JsPropertyInitializer(VALUE, value));
        if (descriptor.isVar()) {
            meta.add(getWritable(context));
        }
        // TODO: accessors
        return jsPropertyDescriptor;
    }

    @NotNull
    private static JsPropertyInitializer getWritable(@NotNull TranslationContext context) {
        if (WRITABLE.getValueExpr() == null) {
            WRITABLE.setValueExpr(context.program().getTrueLiteral());
        }
        return WRITABLE;
    }

    @NotNull
    public static List<JsStatement> nullableExpressionToStatementList(@Nullable JsExpression initalizerForProperty) {
        if (initalizerForProperty == null) {
            return Collections.emptyList();
        }
        return Collections.<JsStatement>singletonList(initalizerForProperty.makeStmt());
    }

    @NotNull
    public static JsFunction createPackage(List<JsStatement> to, JsRootScope scope, JsExpression arg) {
        JsFunction packageBlockFunction = new JsFunction(scope);
        packageBlockFunction.setBody(new JsBlock());

        JsInvocation packageBlockFunctionInvocation = new JsInvocation();
        packageBlockFunctionInvocation.setQualifier(EMPTY_REF);
        packageBlockFunctionInvocation.getArguments().add(packageBlockFunction);

        JsInvocation packageBlock = new JsInvocation();
        packageBlock.setQualifier(packageBlockFunctionInvocation);
        packageBlock.getArguments().add(arg);
        to.add(packageBlock.makeStmt());

        return packageBlockFunction;
    }
}
