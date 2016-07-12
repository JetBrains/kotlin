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

package org.jetbrains.kotlin.js.translate.utils;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.backend.js.ast.metadata.MetadataProperties;
import com.google.dart.compiler.backend.js.ast.metadata.SideEffectKind;
import com.intellij.util.SmartList;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.types.expressions.OperatorConventions;
import org.jetbrains.kotlin.util.OperatorNameConventions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class JsAstUtils {
    private static final JsNameRef DEFINE_PROPERTY = pureFqn("defineProperty", null);
    public static final JsNameRef CREATE_OBJECT = pureFqn("create", null);

    private static final JsNameRef VALUE = new JsNameRef("value");
    private static final JsPropertyInitializer WRITABLE = new JsPropertyInitializer(pureFqn("writable", null), JsLiteral.TRUE);
    private static final JsPropertyInitializer ENUMERABLE = new JsPropertyInitializer(pureFqn("enumerable", null), JsLiteral.TRUE);

    public static final String LENDS_JS_DOC_TAG = "lends";

    static {
        JsNameRef globalObjectReference = new JsNameRef("Object");
        DEFINE_PROPERTY.setQualifier(globalObjectReference);
        CREATE_OBJECT.setQualifier(globalObjectReference);
    }

    private JsAstUtils() {
    }

    @NotNull
    public static JsStatement convertToStatement(@NotNull JsNode jsNode) {
        assert (jsNode instanceof JsExpression) || (jsNode instanceof JsStatement)
                : "Unexpected node of type: " + jsNode.getClass().toString();
        if (jsNode instanceof JsExpression) {
            return ((JsExpression) jsNode).makeStmt();
        }
        return (JsStatement) jsNode;
    }

    @NotNull
    public static JsBlock convertToBlock(@NotNull JsNode jsNode) {
        if (jsNode instanceof JsBlock) {
            return (JsBlock) jsNode;
        }
        JsBlock block = new JsBlock();
        block.getStatements().add(convertToStatement(jsNode));
        return block;
    }

    @NotNull
    private static JsStatement deBlockIfPossible(@NotNull JsStatement statement) {
        if (statement instanceof JsBlock && ((JsBlock)statement).getStatements().size() == 1) {
            return ((JsBlock)statement).getStatements().get(0);
        }
        else {
            return statement;
        }
    }

    @NotNull
    public static JsIf newJsIf(
            @NotNull JsExpression ifExpression,
            @NotNull JsStatement thenStatement,
            @Nullable JsStatement elseStatement
    ) {
        elseStatement = elseStatement != null ? deBlockIfPossible(elseStatement) : null;
        return new JsIf(ifExpression, deBlockIfPossible(thenStatement), elseStatement);
    }

    @NotNull
    public static JsIf newJsIf(@NotNull JsExpression ifExpression, @NotNull JsStatement thenStatement) {
        return newJsIf(ifExpression, thenStatement, null);
    }

    @Nullable
    public static JsExpression extractExpressionFromStatement(@Nullable JsStatement statement) {
        return statement instanceof JsExpressionStatement ? ((JsExpressionStatement) statement).getExpression() : null;
    }

    @NotNull
    public static JsStatement mergeStatementInBlockIfNeeded(@NotNull JsStatement statement, @NotNull JsBlock block) {
        if (block.isEmpty()) {
            return statement;
        } else {
            if (isEmptyStatement(statement)) {
                return deBlockIfPossible(block);
            }
            block.getStatements().add(statement);
            return block;
        }
    }

    public static boolean isEmptyStatement(@NotNull JsStatement statement) {
        return statement instanceof JsEmpty;
    }

    public static boolean isEmptyExpression(@NotNull JsExpression expression) {
        return expression instanceof JsEmptyExpression;
    }

    @NotNull
    public static JsInvocation invokeKotlinFunction(@NotNull String name, @NotNull JsExpression... argument) {
        return invokeMethod(Namer.kotlinObject(), name, argument);
    }

    @NotNull
    public static JsInvocation invokeMethod(@NotNull JsExpression thisObject, @NotNull String name, @NotNull JsExpression... arguments) {
        return new JsInvocation(pureFqn(name, thisObject), arguments);
    }

    @NotNull
    public static JsExpression toInt32(@NotNull JsExpression expression) {
        return new JsBinaryOperation(JsBinaryOperator.BIT_OR, expression, JsNumberLiteral.ZERO);
    }

    @NotNull
    public static JsExpression charToInt(@NotNull JsExpression expression) {
        return invokeMethod(expression, "charCodeAt", JsNumberLiteral.ZERO);
    }

    @NotNull
    public static JsExpression toShort(@NotNull JsExpression expression) {
        return invokeKotlinFunction(OperatorConventions.SHORT.getIdentifier(), expression);
    }

    @NotNull
    public static JsExpression toByte(@NotNull JsExpression expression) {
        return invokeKotlinFunction(OperatorConventions.BYTE.getIdentifier(), expression);
    }

    @NotNull
    public static JsExpression toLong(@NotNull JsExpression expression) {
        return invokeKotlinFunction(OperatorConventions.LONG.getIdentifier(), expression);
    }

    @NotNull
    public static JsExpression toChar(@NotNull JsExpression expression) {
        return invokeKotlinFunction(OperatorConventions.CHAR.getIdentifier(), expression);
    }

    @NotNull
    public static JsExpression compareTo(@NotNull JsExpression left, @NotNull JsExpression right) {
        return invokeKotlinFunction(OperatorNameConventions.COMPARE_TO.getIdentifier(), left, right);
    }

    @NotNull
    public static JsExpression primitiveCompareTo(@NotNull JsExpression left, @NotNull JsExpression right) {
        return invokeKotlinFunction(Namer.PRIMITIVE_COMPARE_TO, left, right);
    }

    @NotNull
    private static JsExpression rangeTo(@NotNull String rangeClassName, @NotNull JsExpression rangeStart, @NotNull JsExpression rangeEnd) {
        JsNameRef expr = pureFqn(rangeClassName, Namer.kotlinObject());
        JsNew numberRangeConstructorInvocation = new JsNew(expr);
        setArguments(numberRangeConstructorInvocation, rangeStart, rangeEnd);
        return numberRangeConstructorInvocation;
    }

    @NotNull
    public static JsExpression numberRangeTo(@NotNull JsExpression rangeStart, @NotNull JsExpression rangeEnd) {
        return rangeTo(Namer.NUMBER_RANGE, rangeStart, rangeEnd);
    }

    @NotNull
    public static JsExpression charRangeTo(@NotNull JsExpression rangeStart, @NotNull JsExpression rangeEnd) {
        return rangeTo(Namer.CHAR_RANGE, rangeStart, rangeEnd);
    }

    public static JsExpression newLong(long value, @NotNull TranslationContext context) {
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            int low = (int) value;
            int high = (int) (value >> 32);
            List<JsExpression> args = new SmartList<JsExpression>();
            args.add(context.program().getNumberLiteral(low));
            args.add(context.program().getNumberLiteral(high));
            return new JsNew(Namer.kotlinLong(), args);
        }
        else {
            if (value == 0) {
                return new JsNameRef(Namer.LONG_ZERO, Namer.kotlinLong());
            }
            else if (value == 1) {
                return new JsNameRef(Namer.LONG_ONE, Namer.kotlinLong());
            }
            else if (value == -1) {
                return new JsNameRef(Namer.LONG_NEG_ONE, Namer.kotlinLong());
            }
            return longFromInt(context.program().getNumberLiteral((int) value));
        }
    }

    @NotNull
    public static JsExpression longFromInt(@NotNull JsExpression expression) {
        return invokeMethod(Namer.kotlinLong(), Namer.LONG_FROM_INT, expression);
    }

    @NotNull
    public static JsExpression longFromNumber(@NotNull JsExpression expression) {
        return invokeMethod(Namer.kotlinLong(), Namer.LONG_FROM_NUMBER, expression);
    }

    @NotNull
    public static JsExpression equalsForObject(@NotNull JsExpression left, @NotNull JsExpression right) {
        return invokeMethod(left, Namer.EQUALS_METHOD_NAME, right);
    }

    @NotNull
    public static JsExpression compareForObject(@NotNull JsExpression left, @NotNull JsExpression right) {
        return invokeMethod(left, Namer.COMPARE_TO_METHOD_NAME, right);
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

    @NotNull
    public static JsBinaryOperation equality(@NotNull JsExpression arg1, @NotNull JsExpression arg2) {
        return new JsBinaryOperation(JsBinaryOperator.REF_EQ, arg1, arg2);
    }

    @NotNull
    public static JsBinaryOperation inequality(@NotNull JsExpression arg1, @NotNull JsExpression arg2) {
        return new JsBinaryOperation(JsBinaryOperator.REF_NEQ, arg1, arg2);
    }

    @NotNull
    public static JsBinaryOperation lessThanEq(@NotNull JsExpression arg1, @NotNull JsExpression arg2) {
        return new JsBinaryOperation(JsBinaryOperator.LTE, arg1, arg2);
    }

    @NotNull
    public static JsBinaryOperation lessThan(@NotNull JsExpression arg1, @NotNull JsExpression arg2) {
        return new JsBinaryOperation(JsBinaryOperator.LT, arg1, arg2);
    }

    @NotNull
    public static JsBinaryOperation greaterThan(@NotNull JsExpression arg1, @NotNull JsExpression arg2) {
        return new JsBinaryOperation(JsBinaryOperator.GT, arg1, arg2);
    }

    @NotNull
    public static JsBinaryOperation greaterThanEq(@NotNull JsExpression arg1, @NotNull JsExpression arg2) {
        return new JsBinaryOperation(JsBinaryOperator.GTE, arg1, arg2);
    }

    @NotNull
    public static JsBinaryOperation assignment(@NotNull JsExpression left, @NotNull JsExpression right) {
        return new JsBinaryOperation(JsBinaryOperator.ASG, left, right);
    }

    @Nullable
    public static Pair<JsExpression, JsExpression> decomposeAssignment(@NotNull JsExpression expr) {
        if (!(expr instanceof JsBinaryOperation)) return null;

        JsBinaryOperation binary = (JsBinaryOperation) expr;
        if (binary.getOperator() != JsBinaryOperator.ASG) return null;

        return new Pair<JsExpression, JsExpression>(binary.getArg1(), binary.getArg2());
    }

    @Nullable
    public static Pair<JsName, JsExpression> decomposeAssignmentToVariable(@NotNull JsExpression expr) {
        Pair<JsExpression, JsExpression> assignment = decomposeAssignment(expr);
        if (assignment == null || !(assignment.getFirst() instanceof JsNameRef)) return null;

        JsNameRef nameRef = (JsNameRef) assignment.getFirst();
        if (nameRef.getName() == null || nameRef.getQualifier() != null) return null;

        return new Pair<JsName, JsExpression>(nameRef.getName(), assignment.getSecond());
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
    public static JsBinaryOperation mul(@NotNull JsExpression left, @NotNull JsExpression right) {
        return new JsBinaryOperation(JsBinaryOperator.MUL, left, right);
    }

    @NotNull
    public static JsBinaryOperation div(@NotNull JsExpression left, @NotNull JsExpression right) {
        return new JsBinaryOperation(JsBinaryOperator.DIV, left, right);
    }

    @NotNull
    public static JsBinaryOperation mod(@NotNull JsExpression left, @NotNull JsExpression right) {
        return new JsBinaryOperation(JsBinaryOperator.MOD, left, right);
    }

    @NotNull
    public static JsPrefixOperation not(@NotNull JsExpression expression) {
        return new JsPrefixOperation(JsUnaryOperator.NOT, expression);
    }

    @NotNull
    public static JsBinaryOperation typeOfIs(@NotNull JsExpression expression, @NotNull JsStringLiteral string) {
        return equality(new JsPrefixOperation(JsUnaryOperator.TYPEOF, expression), string);
    }

    @NotNull
    public static JsVars newVar(@NotNull JsName name, @Nullable JsExpression expr) {
        return new JsVars(new JsVars.JsVar(name, expr));
    }

    public static void setArguments(@NotNull HasArguments invocation, @NotNull List<JsExpression> newArgs) {
        List<JsExpression> arguments = invocation.getArguments();
        assert arguments.isEmpty() : "Arguments already set.";
        arguments.addAll(newArgs);
    }

    public static void setArguments(@NotNull HasArguments invocation, JsExpression... arguments) {
        setArguments(invocation, Arrays.asList(arguments));
    }

    public static void setParameters(@NotNull JsFunction function, @NotNull List<JsParameter> newParams) {
        List<JsParameter> parameters = function.getParameters();
        assert parameters.isEmpty() : "Arguments already set.";
        parameters.addAll(newParams);
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
        return new JsFunction(parent, new JsBlock(), "<anonymous>");
    }

    @NotNull
    public static List<JsExpression> toStringLiteralList(@NotNull List<String> strings, @NotNull JsProgram program) {
        if (strings.isEmpty()) {
            return Collections.emptyList();
        }

        List<JsExpression> result = new SmartList<JsExpression>();
        for (String str : strings) {
            result.add(program.getStringLiteral(str));
        }
        return result;
    }

    @NotNull
    public static JsInvocation defineProperty(
            @NotNull String name,
            @NotNull JsObjectLiteral value,
            @NotNull TranslationContext context
    ) {
        return new JsInvocation(DEFINE_PROPERTY, JsLiteral.THIS, context.program().getStringLiteral(name), value);
    }

    @NotNull
    public static JsStatement defineSimpleProperty(@NotNull String name, @NotNull JsExpression value) {
        return assignment(new JsNameRef(name, JsLiteral.THIS), value).makeStmt();
    }

    @NotNull
    public static JsObjectLiteral createDataDescriptor(@NotNull JsExpression value, boolean writable, boolean enumerable) {
        JsObjectLiteral dataDescriptor = new JsObjectLiteral();
        dataDescriptor.getPropertyInitializers().add(new JsPropertyInitializer(VALUE, value));
        if (writable) {
            dataDescriptor.getPropertyInitializers().add(WRITABLE);
        }
        if (enumerable) {
            dataDescriptor.getPropertyInitializers().add(ENUMERABLE);
        }
        return dataDescriptor;
    }

    @NotNull
    public static JsObjectLiteral wrapValue(@NotNull JsExpression label, @NotNull JsExpression value) {
        return new JsObjectLiteral(Collections.singletonList(new JsPropertyInitializer(label, value)));
    }

    public static JsExpression replaceRootReference(@NotNull JsNameRef fullQualifier, @NotNull JsExpression newQualifier) {
        if (fullQualifier.getQualifier() == null) {
            assert Namer.getRootPackageName().equals(fullQualifier.getIdent()) : "Expected root package, but: " + fullQualifier.getIdent();
            return newQualifier;
        }

        fullQualifier = fullQualifier.deepCopy();
        JsNameRef qualifier = fullQualifier;
        while (true) {
            JsExpression parent = qualifier.getQualifier();
            assert parent instanceof JsNameRef : "unexpected qualifier: " + parent + ", original: " + fullQualifier;
            if (((JsNameRef) parent).getQualifier() == null) {
                assert Namer.getRootPackageName().equals(((JsNameRef) parent).getIdent());
                qualifier.setQualifier(newQualifier);
                return fullQualifier;
            }
            qualifier = (JsNameRef) parent;
        }
    }

    @NotNull
    public static List<JsStatement> flattenStatement(@NotNull JsStatement statement) {
        if (statement instanceof JsBlock) {
            return ((JsBlock) statement).getStatements();
        }

        return new SmartList<JsStatement>(statement);
    }

    @NotNull
    public static JsNameRef pureFqn(@NotNull String identifier, @Nullable JsExpression qualifier) {
        JsNameRef result = new JsNameRef(identifier, qualifier);
        MetadataProperties.setSideEffects(result, SideEffectKind.PURE);
        return result;
    }

    @NotNull
    public static JsNameRef pureFqn(@NotNull JsName identifier, @Nullable JsExpression qualifier) {
        JsNameRef result = new JsNameRef(identifier, qualifier);
        MetadataProperties.setSideEffects(result, SideEffectKind.PURE);
        return result;
    }

    @NotNull
    public static JsInvocation invokeBind(@NotNull JsExpression receiver, @NotNull JsExpression method) {
        return invokeMethod(method, "bind", receiver);
    }

    public static boolean isUndefinedExpression(JsExpression expression) {
        if (!(expression instanceof JsUnaryOperation)) return false;

        JsUnaryOperation unary = (JsUnaryOperation) expression;
        return unary.getOperator() == JsUnaryOperator.VOID;
    }
}
