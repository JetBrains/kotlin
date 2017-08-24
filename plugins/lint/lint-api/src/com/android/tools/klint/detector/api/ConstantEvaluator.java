/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.klint.detector.api;

import static com.android.tools.klint.client.api.JavaParser.TYPE_BOOLEAN;
import static com.android.tools.klint.client.api.JavaParser.TYPE_BYTE;
import static com.android.tools.klint.client.api.JavaParser.TYPE_CHAR;
import static com.android.tools.klint.client.api.JavaParser.TYPE_DOUBLE;
import static com.android.tools.klint.client.api.JavaParser.TYPE_FLOAT;
import static com.android.tools.klint.client.api.JavaParser.TYPE_INT;
import static com.android.tools.klint.client.api.JavaParser.TYPE_LONG;
import static com.android.tools.klint.client.api.JavaParser.TYPE_OBJECT;
import static com.android.tools.klint.client.api.JavaParser.TYPE_SHORT;
import static com.android.tools.klint.client.api.JavaParser.TYPE_STRING;
import static com.android.tools.klint.detector.api.JavaContext.getParentOfType;
import static org.jetbrains.uast.UastBinaryExpressionWithTypeKind.TYPE_CAST;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.client.api.JavaParser.ResolvedField;
import com.android.tools.klint.client.api.JavaParser.ResolvedNode;
import com.android.tools.klint.client.api.UastLintUtils;
import com.google.common.collect.Lists;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiArrayInitializerExpression;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiBinaryExpression;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiConditionalExpression;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiParenthesizedExpression;
import com.intellij.psi.PsiPrefixExpression;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeCastExpression;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.uast.*;
import org.jetbrains.uast.UReferenceExpression;
import org.jetbrains.uast.util.UastExpressionUtils;
import org.jetbrains.uast.visitor.AbstractUastVisitor;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import lombok.ast.ArrayCreation;
import lombok.ast.ArrayInitializer;
import lombok.ast.BinaryExpression;
import lombok.ast.BinaryOperator;
import lombok.ast.BooleanLiteral;
import lombok.ast.Cast;
import lombok.ast.CharLiteral;
import lombok.ast.Expression;
import lombok.ast.ExpressionStatement;
import lombok.ast.FloatingPointLiteral;
import lombok.ast.InlineIfExpression;
import lombok.ast.IntegralLiteral;
import lombok.ast.Node;
import lombok.ast.NullLiteral;
import lombok.ast.Select;
import lombok.ast.Statement;
import lombok.ast.StrictListAccessor;
import lombok.ast.StringLiteral;
import lombok.ast.TypeReference;
import lombok.ast.UnaryExpression;
import lombok.ast.UnaryOperator;
import lombok.ast.VariableDeclaration;
import lombok.ast.VariableDefinition;
import lombok.ast.VariableDefinitionEntry;
import lombok.ast.VariableReference;

/** Evaluates constant expressions */
public class ConstantEvaluator {
    private final JavaContext mContext;
    private boolean mAllowUnknown;

    /**
     * Creates a new constant evaluator
     *
     * @param context the context to use to resolve field references, if any
     */
    public ConstantEvaluator(@Nullable JavaContext context) {
        mContext = context;
    }

    /**
     * Whether we allow computing values where some terms are unknown. For example, the expression
     * {@code "foo" + x + "bar"} would return {@code null} without and {@code "foobar"} with.
     *
     * @return this for constructor chaining
     */
    public ConstantEvaluator allowUnknowns() {
        mAllowUnknown = true;
        return this;
    }

    /**
     * Evaluates the given node and returns the constant value it resolves to, if any
     *
     * @param node the node to compute the constant value for
     * @return the corresponding constant value - a String, an Integer, a Float, and so on
     * @deprecated Use {@link #evaluate(PsiElement)} instead
     */
    @Deprecated
    @Nullable
    public Object evaluate(@NonNull Node node) {
        if (node instanceof NullLiteral) {
            return null;
        } else if (node instanceof BooleanLiteral) {
            return ((BooleanLiteral)node).astValue();
        } else if (node instanceof StringLiteral) {
            StringLiteral string = (StringLiteral) node;
            return string.astValue();
        } else if (node instanceof CharLiteral) {
            return ((CharLiteral)node).astValue();
        } else if (node instanceof IntegralLiteral) {
            IntegralLiteral literal = (IntegralLiteral) node;
            // Don't combine to ?: since that will promote astIntValue to a long
            if (literal.astMarkedAsLong()) {
                return literal.astLongValue();
            } else {
                return literal.astIntValue();
            }
        } else if (node instanceof FloatingPointLiteral) {
            FloatingPointLiteral literal = (FloatingPointLiteral) node;
            // Don't combine to ?: since that will promote astFloatValue to a double
            if (literal.astMarkedAsFloat()) {
                return literal.astFloatValue();
            } else {
                return literal.astDoubleValue();
            }
        } else if (node instanceof UnaryExpression) {
            UnaryOperator operator = ((UnaryExpression) node).astOperator();
            Object operand = evaluate(((UnaryExpression) node).astOperand());
            if (operand == null) {
                return null;
            }
            switch (operator) {
                case LOGICAL_NOT:
                    if (operand instanceof Boolean) {
                        return !(Boolean) operand;
                    }
                    break;
                case UNARY_PLUS:
                    return operand;
                case BINARY_NOT:
                    if (operand instanceof Integer) {
                        return ~(Integer) operand;
                    } else if (operand instanceof Long) {
                        return ~(Long) operand;
                    } else if (operand instanceof Short) {
                        return ~(Short) operand;
                    } else if (operand instanceof Character) {
                        return ~(Character) operand;
                    } else if (operand instanceof Byte) {
                        return ~(Byte) operand;
                    }
                    break;
                case UNARY_MINUS:
                    if (operand instanceof Integer) {
                        return -(Integer) operand;
                    } else if (operand instanceof Long) {
                        return -(Long) operand;
                    } else if (operand instanceof Double) {
                        return -(Double) operand;
                    } else if (operand instanceof Float) {
                        return -(Float) operand;
                    } else if (operand instanceof Short) {
                        return -(Short) operand;
                    } else if (operand instanceof Character) {
                        return -(Character) operand;
                    } else if (operand instanceof Byte) {
                        return -(Byte) operand;
                    }
                    break;
            }
        } else if (node instanceof InlineIfExpression) {
            InlineIfExpression expression = (InlineIfExpression) node;
            Object known = evaluate(expression.astCondition());
            if (known == Boolean.TRUE && expression.astIfTrue() != null) {
                return evaluate(expression.astIfTrue());
            } else if (known == Boolean.FALSE && expression.astIfFalse() != null) {
                return evaluate(expression.astIfFalse());
            }
        } else if (node instanceof BinaryExpression) {
            BinaryOperator operator = ((BinaryExpression) node).astOperator();
            Object operandLeft = evaluate(((BinaryExpression) node).astLeft());
            Object operandRight = evaluate(((BinaryExpression) node).astRight());
            if (operandLeft == null || operandRight == null) {
                if (mAllowUnknown) {
                    if (operandLeft == null) {
                        return operandRight;
                    } else {
                        return operandLeft;
                    }
                }
                return null;
            }
            if (operandLeft instanceof String && operandRight instanceof String) {
                if (operator == BinaryOperator.PLUS) {
                    return operandLeft.toString() + operandRight.toString();
                }
                return null;
            } else if (operandLeft instanceof Boolean && operandRight instanceof Boolean) {
                boolean left = (Boolean) operandLeft;
                boolean right = (Boolean) operandRight;
                switch (operator) {
                    case LOGICAL_OR:
                        return left || right;
                    case LOGICAL_AND:
                        return left && right;
                    case BITWISE_OR:
                        return left | right;
                    case BITWISE_XOR:
                        return left ^ right;
                    case BITWISE_AND:
                        return left & right;
                    case EQUALS:
                        return left == right;
                    case NOT_EQUALS:
                        return left != right;
                }
            } else if (operandLeft instanceof Number && operandRight instanceof Number) {
                Number left = (Number) operandLeft;
                Number right = (Number) operandRight;
                boolean isInteger =
                        !(left instanceof Float || left instanceof Double
                                || right instanceof Float || right instanceof Double);
                boolean isWide =
                        isInteger ? (left instanceof Long || right instanceof Long)
                                : (left instanceof Double || right instanceof Double);

                switch (operator) {
                    case BITWISE_OR:
                        if (isWide) {
                            return left.longValue() | right.longValue();
                        } else {
                            return left.intValue() | right.intValue();
                        }
                    case BITWISE_XOR:
                        if (isWide) {
                            return left.longValue() ^ right.longValue();
                        } else {
                            return left.intValue() ^ right.intValue();
                        }
                    case BITWISE_AND:
                        if (isWide) {
                            return left.longValue() & right.longValue();
                        } else {
                            return left.intValue() & right.intValue();
                        }
                    case EQUALS:
                        if (isInteger) {
                            return left.longValue() == right.longValue();
                        } else {
                            return left.doubleValue() == right.doubleValue();
                        }
                    case NOT_EQUALS:
                        if (isInteger) {
                            return left.longValue() != right.longValue();
                        } else {
                            return left.doubleValue() != right.doubleValue();
                        }
                    case GREATER:
                        if (isInteger) {
                            return left.longValue() > right.longValue();
                        } else {
                            return left.doubleValue() > right.doubleValue();
                        }
                    case GREATER_OR_EQUAL:
                        if (isInteger) {
                            return left.longValue() >= right.longValue();
                        } else {
                            return left.doubleValue() >= right.doubleValue();
                        }
                    case LESS:
                        if (isInteger) {
                            return left.longValue() < right.longValue();
                        } else {
                            return left.doubleValue() < right.doubleValue();
                        }
                    case LESS_OR_EQUAL:
                        if (isInteger) {
                            return left.longValue() <= right.longValue();
                        } else {
                            return left.doubleValue() <= right.doubleValue();
                        }
                    case SHIFT_LEFT:
                        if (isWide) {
                            return left.longValue() << right.intValue();
                        } else {
                            return left.intValue() << right.intValue();
                        }
                    case SHIFT_RIGHT:
                        if (isWide) {
                            return left.longValue() >> right.intValue();
                        } else {
                            return left.intValue() >> right.intValue();
                        }
                    case BITWISE_SHIFT_RIGHT:
                        if (isWide) {
                            return left.longValue() >>> right.intValue();
                        } else {
                            return left.intValue() >>> right.intValue();
                        }
                    case PLUS:
                        if (isInteger) {
                            if (isWide) {
                                return left.longValue() + right.longValue();
                            } else {
                                return left.intValue() + right.intValue();
                            }
                        } else {
                            if (isWide) {
                                return left.doubleValue() + right.doubleValue();
                            } else {
                                return left.floatValue() + right.floatValue();
                            }
                        }
                    case MINUS:
                        if (isInteger) {
                            if (isWide) {
                                return left.longValue() - right.longValue();
                            } else {
                                return left.intValue() - right.intValue();
                            }
                        } else {
                            if (isWide) {
                                return left.doubleValue() - right.doubleValue();
                            } else {
                                return left.floatValue() - right.floatValue();
                            }
                        }
                    case MULTIPLY:
                        if (isInteger) {
                            if (isWide) {
                                return left.longValue() * right.longValue();
                            } else {
                                return left.intValue() * right.intValue();
                            }
                        } else {
                            if (isWide) {
                                return left.doubleValue() * right.doubleValue();
                            } else {
                                return left.floatValue() * right.floatValue();
                            }
                        }
                    case DIVIDE:
                        if (isInteger) {
                            if (isWide) {
                                return left.longValue() / right.longValue();
                            } else {
                                return left.intValue() / right.intValue();
                            }
                        } else {
                            if (isWide) {
                                return left.doubleValue() / right.doubleValue();
                            } else {
                                return left.floatValue() / right.floatValue();
                            }
                        }
                    case REMAINDER:
                        if (isInteger) {
                            if (isWide) {
                                return left.longValue() % right.longValue();
                            } else {
                                return left.intValue() % right.intValue();
                            }
                        } else {
                            if (isWide) {
                                return left.doubleValue() % right.doubleValue();
                            } else {
                                return left.floatValue() % right.floatValue();
                            }
                        }
                    default:
                        return null;
                }
            }
        } else if (node instanceof Cast) {
            Cast cast = (Cast)node;
            Object operandValue = evaluate(cast.astOperand());
            if (operandValue instanceof Number) {
                Number number = (Number)operandValue;
                String typeName = cast.astTypeReference().getTypeName();
                if (typeName.equals("float")) {
                    return number.floatValue();
                } else if (typeName.equals("double")) {
                    return number.doubleValue();
                } else if (typeName.equals("int")) {
                    return number.intValue();
                } else if (typeName.equals("long")) {
                    return number.longValue();
                } else if (typeName.equals("short")) {
                    return number.shortValue();
                } else if (typeName.equals("byte")) {
                    return number.byteValue();
                }
            }
            return operandValue;
        } else if (mContext != null && (node instanceof VariableReference ||
                node instanceof Select)) {
            ResolvedNode resolved = mContext.resolve(node);
            if (resolved instanceof ResolvedField) {
                ResolvedField field = (ResolvedField) resolved;
                Object value = field.getValue();
                if (value != null) {
                    return value;
                }
                Node astNode = field.findAstNode();
                if (astNode instanceof VariableDeclaration) {
                    VariableDeclaration declaration = (VariableDeclaration) astNode;
                    VariableDefinition definition = declaration.astDefinition();
                    if (definition != null && definition.astModifiers().isFinal()) {
                        StrictListAccessor<VariableDefinitionEntry, VariableDefinition> variables =
                                definition.astVariables();
                        if (variables.size() == 1) {
                            VariableDefinitionEntry first = variables.first();
                            if (first.astInitializer() != null) {
                                return evaluate(first.astInitializer());
                            }
                        }
                    }
                }
                return null;
            } else if (node instanceof VariableReference) {
                Statement statement = getParentOfType(node, Statement.class, false);
                if (statement != null) {
                    ListIterator<Node> iterator = statement.getParent().getChildren().listIterator();
                    while (iterator.hasNext()) {
                        if (iterator.next() == statement) {
                            if (iterator.hasPrevious()) { // should always be true
                                iterator.previous();
                            }
                            break;
                        }
                    }

                    String targetName = ((VariableReference)node).astIdentifier().astValue();
                    while (iterator.hasPrevious()) {
                        Node previous = iterator.previous();
                        if (previous instanceof VariableDeclaration) {
                            VariableDeclaration declaration = (VariableDeclaration) previous;
                            VariableDefinition definition = declaration.astDefinition();
                            for (VariableDefinitionEntry entry : definition
                                    .astVariables()) {
                                if (entry.astInitializer() != null
                                        && entry.astName().astValue().equals(targetName)) {
                                    return evaluate(entry.astInitializer());
                                }
                            }
                        } else if (previous instanceof ExpressionStatement) {
                            ExpressionStatement expressionStatement = (ExpressionStatement) previous;
                            Expression expression = expressionStatement.astExpression();
                            if (expression instanceof BinaryExpression &&
                                    ((BinaryExpression) expression).astOperator()
                                            == BinaryOperator.ASSIGN) {
                                BinaryExpression binaryExpression = (BinaryExpression) expression;
                                if (targetName.equals(binaryExpression.astLeft().toString())) {
                                    return evaluate(binaryExpression.astRight());
                                }
                            }
                        }
                    }
                }
            }
        } else if (node instanceof ArrayCreation) {
            ArrayCreation creation = (ArrayCreation) node;
            ArrayInitializer initializer = creation.astInitializer();
            if (initializer != null) {
                TypeReference typeReference = creation.astComponentTypeReference();
                StrictListAccessor<Expression, ArrayInitializer> expressions = initializer
                        .astExpressions();
                List<Object> values = Lists.newArrayListWithExpectedSize(expressions.size());
                Class<?> commonType = null;
                for (Expression expression : expressions) {
                    Object value = evaluate(expression);
                    if (value != null) {
                        values.add(value);
                        if (commonType == null) {
                            commonType = value.getClass();
                        } else {
                            while (!commonType.isAssignableFrom(value.getClass())) {
                                commonType = commonType.getSuperclass();
                            }
                        }
                    } else if (!mAllowUnknown) {
                        // Inconclusive
                        return null;
                    }
                }
                if (!values.isEmpty()) {
                    Object o = Array.newInstance(commonType, values.size());
                    return values.toArray((Object[]) o);
                } else if (mContext != null) {
                    ResolvedNode type = mContext.resolve(typeReference);
                    System.out.println(type);
                    // TODO: return new array of this type
                }
            } else {
                // something like "new byte[3]" but with no initializer.
                String type = creation.astComponentTypeReference().toString();
                // TODO: Look up the size and only if small, use it. E.g. if it was byte[3]
                // we could return a byte[3] array, but if it's say byte[1024*1024] we don't
                // want to do that.
                int size = 0;
                if (TYPE_BYTE.equals(type)) {
                    return new byte[size];
                }
                if (TYPE_BOOLEAN.equals(type)) {
                    return new boolean[size];
                }
                if (TYPE_INT.equals(type)) {
                    return new int[size];
                }
                if (TYPE_LONG.equals(type)) {
                    return new long[size];
                }
                if (TYPE_CHAR.equals(type)) {
                    return new char[size];
                }
                if (TYPE_FLOAT.equals(type)) {
                    return new float[size];
                }
                if (TYPE_DOUBLE.equals(type)) {
                    return new double[size];
                }
                if (TYPE_STRING.equals(type)) {
                    //noinspection SSBasedInspection
                    return new String[size];
                }
                if (TYPE_SHORT.equals(type)) {
                    return new short[size];
                }
                if (TYPE_OBJECT.equals(type)) {
                    //noinspection SSBasedInspection
                    return new Object[size];
                }
            }
        }

        // TODO: Check for MethodInvocation and perform some common operations -
        // Math.* methods, String utility methods like notNullize, etc

        return null;
    }

    /**
     * Evaluates the given node and returns the constant value it resolves to, if any
     *
     * @param node the node to compute the constant value for
     * @return the corresponding constant value - a String, an Integer, a Float, and so on
     */
    @Nullable
    public Object evaluate(@Nullable UElement node) {
        if (node == null) {
            return null;
        }

        if (node instanceof ULiteralExpression) {
            return ((ULiteralExpression) node).getValue();
        } else if (node instanceof UPrefixExpression) {
            UastPrefixOperator operator = ((UPrefixExpression) node).getOperator();
            Object operand = evaluate(((UPrefixExpression) node).getOperand());
            if (operand == null) {
                return null;
            }
            if (operator == UastPrefixOperator.LOGICAL_NOT) {
                if (operand instanceof Boolean) {
                    return !(Boolean) operand;
                }
            } else if (operator == UastPrefixOperator.UNARY_PLUS) {
                return operand;
            } else if (operator == UastPrefixOperator.BITWISE_NOT) {
                if (operand instanceof Integer) {
                    return ~(Integer) operand;
                } else if (operand instanceof Long) {
                    return ~(Long) operand;
                } else if (operand instanceof Short) {
                    return ~(Short) operand;
                } else if (operand instanceof Character) {
                    return ~(Character) operand;
                } else if (operand instanceof Byte) {
                    return ~(Byte) operand;
                }
            } else if (operator == UastPrefixOperator.UNARY_MINUS) {
                if (operand instanceof Integer) {
                    return -(Integer) operand;
                } else if (operand instanceof Long) {
                    return -(Long) operand;
                } else if (operand instanceof Double) {
                    return -(Double) operand;
                } else if (operand instanceof Float) {
                    return -(Float) operand;
                } else if (operand instanceof Short) {
                    return -(Short) operand;
                } else if (operand instanceof Character) {
                    return -(Character) operand;
                } else if (operand instanceof Byte) {
                    return -(Byte) operand;
                }
            }
        } else if (node instanceof UIfExpression
                && ((UIfExpression) node).getExpressionType() != null) {
            UIfExpression expression = (UIfExpression) node;
            Object known = evaluate(expression.getCondition());
            if (known == Boolean.TRUE && expression.getThenExpression() != null) {
                return evaluate(expression.getThenExpression());
            } else if (known == Boolean.FALSE && expression.getElseExpression() != null) {
                return evaluate(expression.getElseExpression());
            }
        } else if (node instanceof UParenthesizedExpression) {
            UParenthesizedExpression parenthesizedExpression = (UParenthesizedExpression) node;
            UExpression expression = parenthesizedExpression.getExpression();
            return evaluate(expression);
        } else if (node instanceof UBinaryExpression) {
            UastBinaryOperator operator = ((UBinaryExpression) node).getOperator();
            Object operandLeft = evaluate(((UBinaryExpression) node).getLeftOperand());
            Object operandRight = evaluate(((UBinaryExpression) node).getRightOperand());
            if (operandLeft == null || operandRight == null) {
                if (mAllowUnknown) {
                    if (operandLeft == null) {
                        return operandRight;
                    } else {
                        return operandLeft;
                    }
                }
                return null;
            }
            if (operandLeft instanceof String && operandRight instanceof String) {
                if (operator == UastBinaryOperator.PLUS) {
                    return operandLeft.toString() + operandRight.toString();
                }
                return null;
            } else if (operandLeft instanceof Boolean && operandRight instanceof Boolean) {
                boolean left = (Boolean) operandLeft;
                boolean right = (Boolean) operandRight;
                if (operator == UastBinaryOperator.LOGICAL_OR) {
                    return left || right;
                } else if (operator == UastBinaryOperator.LOGICAL_AND) {
                    return left && right;
                } else if (operator == UastBinaryOperator.BITWISE_OR) {
                    return left | right;
                } else if (operator == UastBinaryOperator.BITWISE_XOR) {
                    return left ^ right;
                } else if (operator == UastBinaryOperator.BITWISE_AND) {
                    return left & right;
                } else if (operator == UastBinaryOperator.IDENTITY_EQUALS
                        || operator == UastBinaryOperator.EQUALS) {
                    return left == right;
                } else if (operator == UastBinaryOperator.IDENTITY_NOT_EQUALS
                        || operator == UastBinaryOperator.NOT_EQUALS) {
                    return left != right;
                }
            } else if (operandLeft instanceof Number && operandRight instanceof Number) {
                Number left = (Number) operandLeft;
                Number right = (Number) operandRight;
                boolean isInteger =
                        !(left instanceof Float || left instanceof Double
                                || right instanceof Float || right instanceof Double);
                boolean isWide =
                        isInteger ? (left instanceof Long || right instanceof Long)
                                : (left instanceof Double || right instanceof Double);

                if (operator == UastBinaryOperator.BITWISE_OR) {
                    if (isWide) {
                        return left.longValue() | right.longValue();
                    } else {
                        return left.intValue() | right.intValue();
                    }
                } else if (operator == UastBinaryOperator.BITWISE_XOR) {
                    if (isWide) {
                        return left.longValue() ^ right.longValue();
                    } else {
                        return left.intValue() ^ right.intValue();
                    }
                } else if (operator == UastBinaryOperator.BITWISE_AND) {
                    if (isWide) {
                        return left.longValue() & right.longValue();
                    } else {
                        return left.intValue() & right.intValue();
                    }
                } else if (operator == UastBinaryOperator.EQUALS
                        || operator == UastBinaryOperator.IDENTITY_EQUALS) {
                    if (isInteger) {
                        return left.longValue() == right.longValue();
                    } else {
                        return left.doubleValue() == right.doubleValue();
                    }
                } else if (operator == UastBinaryOperator.NOT_EQUALS
                        || operator == UastBinaryOperator.IDENTITY_NOT_EQUALS) {
                    if (isInteger) {
                        return left.longValue() != right.longValue();
                    } else {
                        return left.doubleValue() != right.doubleValue();
                    }
                } else if (operator == UastBinaryOperator.GREATER) {
                    if (isInteger) {
                        return left.longValue() > right.longValue();
                    } else {
                        return left.doubleValue() > right.doubleValue();
                    }
                } else if (operator == UastBinaryOperator.GREATER_OR_EQUALS) {
                    if (isInteger) {
                        return left.longValue() >= right.longValue();
                    } else {
                        return left.doubleValue() >= right.doubleValue();
                    }
                } else if (operator == UastBinaryOperator.LESS) {
                    if (isInteger) {
                        return left.longValue() < right.longValue();
                    } else {
                        return left.doubleValue() < right.doubleValue();
                    }
                } else if (operator == UastBinaryOperator.LESS_OR_EQUALS) {
                    if (isInteger) {
                        return left.longValue() <= right.longValue();
                    } else {
                        return left.doubleValue() <= right.doubleValue();
                    }
                } else if (operator == UastBinaryOperator.SHIFT_LEFT) {
                    if (isWide) {
                        return left.longValue() << right.intValue();
                    } else {
                        return left.intValue() << right.intValue();
                    }
                } else if (operator == UastBinaryOperator.SHIFT_RIGHT) {
                    if (isWide) {
                        return left.longValue() >> right.intValue();
                    } else {
                        return left.intValue() >> right.intValue();
                    }
                } else if (operator == UastBinaryOperator.UNSIGNED_SHIFT_RIGHT) {
                    if (isWide) {
                        return left.longValue() >>> right.intValue();
                    } else {
                        return left.intValue() >>> right.intValue();
                    }
                } else if (operator == UastBinaryOperator.PLUS) {
                    if (isInteger) {
                        if (isWide) {
                            return left.longValue() + right.longValue();
                        } else {
                            return left.intValue() + right.intValue();
                        }
                    } else {
                        if (isWide) {
                            return left.doubleValue() + right.doubleValue();
                        } else {
                            return left.floatValue() + right.floatValue();
                        }
                    }
                } else if (operator == UastBinaryOperator.MINUS) {
                    if (isInteger) {
                        if (isWide) {
                            return left.longValue() - right.longValue();
                        } else {
                            return left.intValue() - right.intValue();
                        }
                    } else {
                        if (isWide) {
                            return left.doubleValue() - right.doubleValue();
                        } else {
                            return left.floatValue() - right.floatValue();
                        }
                    }
                } else if (operator == UastBinaryOperator.MULTIPLY) {
                    if (isInteger) {
                        if (isWide) {
                            return left.longValue() * right.longValue();
                        } else {
                            return left.intValue() * right.intValue();
                        }
                    } else {
                        if (isWide) {
                            return left.doubleValue() * right.doubleValue();
                        } else {
                            return left.floatValue() * right.floatValue();
                        }
                    }
                } else if (operator == UastBinaryOperator.DIV) {
                    if (isInteger) {
                        if (isWide) {
                            return left.longValue() / right.longValue();
                        } else {
                            return left.intValue() / right.intValue();
                        }
                    } else {
                        if (isWide) {
                            return left.doubleValue() / right.doubleValue();
                        } else {
                            return left.floatValue() / right.floatValue();
                        }
                    }
                } else if (operator == UastBinaryOperator.MOD) {
                    if (isInteger) {
                        if (isWide) {
                            return left.longValue() % right.longValue();
                        } else {
                            return left.intValue() % right.intValue();
                        }
                    } else {
                        if (isWide) {
                            return left.doubleValue() % right.doubleValue();
                        } else {
                            return left.floatValue() % right.floatValue();
                        }
                    }
                } else {
                    return null;
                }
            }
        } else if (node instanceof UBinaryExpressionWithType &&
                ((UBinaryExpressionWithType) node).getOperationKind() == TYPE_CAST) {
            UBinaryExpressionWithType cast = (UBinaryExpressionWithType) node;
            Object operandValue = evaluate(cast.getOperand());
            if (operandValue instanceof Number) {
                Number number = (Number) operandValue;
                PsiType type = cast.getType();
                if (PsiType.FLOAT.equals(type)) {
                    return number.floatValue();
                } else if (PsiType.DOUBLE.equals(type)) {
                    return number.doubleValue();
                } else if (PsiType.INT.equals(type)) {
                    return number.intValue();
                } else if (PsiType.LONG.equals(type)) {
                    return number.longValue();
                } else if (PsiType.SHORT.equals(type)) {
                    return number.shortValue();
                } else if (PsiType.BYTE.equals(type)) {
                    return number.byteValue();
                }
            }
            return operandValue;
        } else if (mContext != null && node instanceof UReferenceExpression) {
            PsiElement resolved = ((UReferenceExpression) node).resolve();
            if (resolved instanceof PsiVariable) {
                PsiVariable variable = (PsiVariable) resolved;
                Object value = UastLintUtils.findLastValue(variable, node, mContext, this);

                if (value != null) {
                    return value;
                }
                if (variable.getInitializer() != null) {
                    return evaluate(variable.getInitializer());
                }
                return null;
            }
        } else if (UastExpressionUtils.isNewArrayWithDimensions((UExpression) node)) {
            UCallExpression call = (UCallExpression) node;
            PsiType arrayType = call.getExpressionType();
            if (arrayType instanceof PsiArrayType) {
                PsiType componentType = ((PsiArrayType) arrayType).getComponentType();
                // Single-dimension array
                if (!(componentType instanceof PsiArrayType)
                        && call.getValueArgumentCount() == 1) {
                    Object lengthObj = evaluate(call.getValueArguments().get(0));
                    if (lengthObj instanceof Number) {
                        int length = ((Number) lengthObj).intValue();
                        if (length > 30) {
                            length = 30;
                        }
                        if (componentType == PsiType.BOOLEAN) {
                            return new boolean[length];
                        } else if (isObjectType(componentType)) {
                            return new Object[length];
                        } else if (componentType == PsiType.CHAR) {
                            return new char[length];
                        } else if (componentType == PsiType.BYTE) {
                            return new byte[length];
                        } else if (componentType == PsiType.DOUBLE) {
                            return new double[length];
                        } else if (componentType == PsiType.FLOAT) {
                            return new float[length];
                        } else if (componentType == PsiType.INT) {
                            return new int[length];
                        } else if (componentType == PsiType.SHORT) {
                            return new short[length];
                        } else if (componentType == PsiType.LONG) {
                            return new long[length];
                        } else if (isStringType(componentType)) {
                            return new String[length];
                        }
                    }
                }
            }
        } else if (UastExpressionUtils.isNewArrayWithInitializer(node)) {
            UCallExpression call = (UCallExpression) node;
            PsiType arrayType = call.getExpressionType();
            if (arrayType instanceof PsiArrayType) {
                PsiType componentType = ((PsiArrayType) arrayType).getComponentType();
                // Single-dimension array
                if (!(componentType instanceof PsiArrayType)) {
                    int length = call.getValueArgumentCount();
                    List<Object> evaluatedArgs = new ArrayList<Object>(length);
                    for (UExpression arg : call.getValueArguments()) {
                        Object evaluatedArg = evaluate(arg);
                        if (!mAllowUnknown && evaluatedArg == null) {
                            // Inconclusive
                            return null;
                        }
                        evaluatedArgs.add(evaluatedArg);
                    }

                    if (componentType == PsiType.BOOLEAN) {
                        boolean[] arr = new boolean[length];
                        for (int i = 0; i < length; ++i) {
                            Object o = evaluatedArgs.get(i);
                            if (o instanceof Boolean) {
                                arr[i] = (Boolean) o;
                            }
                        }
                        return arr;
                    } else if (isObjectType(componentType)) {
                        Object[] arr = new Object[length];
                        for (int i = 0; i < length; ++i) {
                            arr[i] = evaluatedArgs.get(i);
                        }
                        return arr;
                    } else if (componentType.equals(PsiType.CHAR)) {
                        char[] arr = new char[length];
                        for (int i = 0; i < length; ++i) {
                            Object o = evaluatedArgs.get(i);
                            if (o instanceof Character) {
                                arr[i] = (Character) o;
                            }
                        }
                        return arr;
                    } else if (componentType.equals(PsiType.BYTE)) {
                        byte[] arr = new byte[length];
                        for (int i = 0; i < length; ++i) {
                            Object o = evaluatedArgs.get(i);
                            if (o instanceof Byte) {
                                arr[i] = (Byte) o;
                            }
                        }
                        return arr;
                    } else if (componentType.equals(PsiType.DOUBLE)) {
                        double[] arr = new double[length];
                        for (int i = 0; i < length; ++i) {
                            Object o = evaluatedArgs.get(i);
                            if (o instanceof Double) {
                                arr[i] = (Double) o;
                            }
                        }
                        return arr;
                    } else if (componentType.equals(PsiType.FLOAT)) {
                        float[] arr = new float[length];
                        for (int i = 0; i < length; ++i) {
                            Object o = evaluatedArgs.get(i);
                            if (o instanceof Float) {
                                arr[i] = (Float) o;
                            }
                        }
                        return arr;
                    } else if (componentType.equals(PsiType.INT)) {
                        int[] arr = new int[length];
                        for (int i = 0; i < length; ++i) {
                            Object o = evaluatedArgs.get(i);
                            if (o instanceof Integer) {
                                arr[i] = (Integer) o;
                            }
                        }
                        return arr;
                    } else if (componentType.equals(PsiType.SHORT)) {
                        short[] arr = new short[length];
                        for (int i = 0; i < length; ++i) {
                            Object o = evaluatedArgs.get(i);
                            if (o instanceof Short) {
                                arr[i] = (Short) o;
                            }
                        }
                        return arr;
                    } else if (componentType.equals(PsiType.LONG)) {
                        long[] arr = new long[length];
                        for (int i = 0; i < length; ++i) {
                            Object o = evaluatedArgs.get(i);
                            if (o instanceof Long) {
                                arr[i] = (Long) o;
                            }
                        }
                        return arr;
                    } else if (isStringType(componentType)) {
                        String[] arr = new String[length];
                        for (int i = 0; i < length; ++i) {
                            Object o = evaluatedArgs.get(i);
                            if (o instanceof String) {
                                arr[i] = (String) o;
                            }
                        }
                        return arr;
                    }
                }
            }
        }

        if (node instanceof UExpression) {
            Object evaluated = ((UExpression) node).evaluate();
            if (evaluated != null) {
                return evaluated;
            }
        }

        // TODO: Check for MethodInvocation and perform some common operations -
        // Math.* methods, String utility methods like notNullize, etc

        return null;
    }
    
    private static boolean isStringType(PsiType type) {
        if (!(type instanceof PsiClassType)) {
            return false;
        }
        
        PsiClass resolvedClass = ((PsiClassType) type).resolve();
        return resolvedClass != null && TYPE_STRING.equals(resolvedClass.getQualifiedName());
    }

    private static boolean isObjectType(PsiType type) {
        if (!(type instanceof PsiClassType)) {
            return false;
        }

        PsiClass resolvedClass = ((PsiClassType) type).resolve();
        return resolvedClass != null && TYPE_OBJECT.equals(resolvedClass.getQualifiedName());
    }

    public static class LastAssignmentFinder extends AbstractUastVisitor {
        private final PsiVariable mVariable;
        private final UElement mEndAt;

        private final ConstantEvaluator mConstantEvaluator;

        private boolean mDone = false;
        private int mCurrentLevel = 0;
        private int mVariableLevel = -1;
        private Object mCurrentValue;
        private UElement mLastAssignment;

        public LastAssignmentFinder(
                @NonNull PsiVariable variable,
                @NonNull UElement endAt,
                @NonNull JavaContext context,
                @Nullable ConstantEvaluator constantEvaluator,
                int variableLevel) {
            mVariable = variable;
            mEndAt = endAt;
            UExpression initializer = context.getUastContext().getInitializerBody(variable);
            mLastAssignment = initializer;
            mConstantEvaluator = constantEvaluator;
            if (initializer != null && constantEvaluator != null) {
                mCurrentValue = constantEvaluator.evaluate(initializer);
            }
            this.mVariableLevel = variableLevel;
        }

        @Nullable
        public Object getCurrentValue() {
            return mCurrentValue;
        }

        @Nullable
        public UElement getLastAssignment() {
            return mLastAssignment;
        }

        @Override
        public boolean visitElement(UElement node) {
            if (elementHasLevel(node)) {
                mCurrentLevel++;
            }
            if (node.equals(mEndAt)) {
                mDone = true;
            }
            return mDone || super.visitElement(node);
        }

        @Override
        public boolean visitVariable(UVariable node) {
            if (mVariableLevel < 0 && node.getPsi().isEquivalentTo(mVariable)) {
                mVariableLevel = mCurrentLevel;
            }

            return super.visitVariable(node);
        }

        @Override
        public void afterVisitBinaryExpression(UBinaryExpression node) {
            if (!mDone
                && node.getOperator() instanceof UastBinaryOperator.AssignOperator
                && mVariableLevel >= 0) {
                UExpression leftOperand = node.getLeftOperand();
                UastBinaryOperator operator = node.getOperator();

                if (!(operator instanceof UastBinaryOperator.AssignOperator)
                    || !(leftOperand instanceof UResolvable)) {
                    return;
                }

                PsiElement resolved = ((UResolvable) leftOperand).resolve();
                if (!mVariable.equals(resolved)) {
                    return;
                }

                // Last assignment is unknown if we see an assignment inside
                // some conditional or loop statement.
                if (mCurrentLevel > mVariableLevel + 1) {
                    mLastAssignment = null;
                    mCurrentValue = null;
                    return;
                }

                UExpression rightOperand = node.getRightOperand();
                ConstantEvaluator constantEvaluator = mConstantEvaluator;

                mCurrentValue = (constantEvaluator != null)
                                ? constantEvaluator.evaluate(rightOperand)
                                : null;
                mLastAssignment = rightOperand;
            }

            super.afterVisitBinaryExpression(node);
        }

        @Override
        public void afterVisitElement(UElement node) {
            if (elementHasLevel(node)) {
                mCurrentLevel--;
            }
            super.afterVisitElement(node);
        }

        private static boolean elementHasLevel(UElement node) {
            return !(node instanceof UBlockExpression
                     || node instanceof UDeclarationsExpression);
        }
    }

    /**
     * Evaluates the given node and returns the constant value it resolves to, if any
     *
     * @param node the node to compute the constant value for
     * @return the corresponding constant value - a String, an Integer, a Float, and so on
     */
    @Nullable
    public Object evaluate(@Nullable PsiElement node) {
        if (node == null) {
            return null;
        }
        if (node instanceof PsiLiteral) {
            return ((PsiLiteral)node).getValue();
        } else if (node instanceof PsiPrefixExpression) {
            IElementType operator = ((PsiPrefixExpression) node).getOperationTokenType();
            Object operand = evaluate(((PsiPrefixExpression) node).getOperand());
            if (operand == null) {
                return null;
            }
            if (operator == JavaTokenType.EXCL) {
                if (operand instanceof Boolean) {
                    return !(Boolean) operand;
                }
            } else if (operator == JavaTokenType.PLUS) {
                return operand;
            } else if (operator == JavaTokenType.TILDE) {
                if (operand instanceof Integer) {
                    return ~(Integer) operand;
                } else if (operand instanceof Long) {
                    return ~(Long) operand;
                } else if (operand instanceof Short) {
                    return ~(Short) operand;
                } else if (operand instanceof Character) {
                    return ~(Character) operand;
                } else if (operand instanceof Byte) {
                    return ~(Byte) operand;
                }
            } else if (operator == JavaTokenType.MINUS) {
                if (operand instanceof Integer) {
                    return -(Integer) operand;
                } else if (operand instanceof Long) {
                    return -(Long) operand;
                } else if (operand instanceof Double) {
                    return -(Double) operand;
                } else if (operand instanceof Float) {
                    return -(Float) operand;
                } else if (operand instanceof Short) {
                    return -(Short) operand;
                } else if (operand instanceof Character) {
                    return -(Character) operand;
                } else if (operand instanceof Byte) {
                    return -(Byte) operand;
                }
            }
        } else if (node instanceof PsiConditionalExpression) {
            PsiConditionalExpression expression = (PsiConditionalExpression) node;
            Object known = evaluate(expression.getCondition());
            if (known == Boolean.TRUE && expression.getThenExpression() != null) {
                return evaluate(expression.getThenExpression());
            } else if (known == Boolean.FALSE && expression.getElseExpression() != null) {
                return evaluate(expression.getElseExpression());
            }
        } else if (node instanceof PsiParenthesizedExpression) {
            PsiParenthesizedExpression parenthesizedExpression = (PsiParenthesizedExpression) node;
            PsiExpression expression = parenthesizedExpression.getExpression();
            if (expression != null) {
                return evaluate(expression);
            }
        } else if (node instanceof PsiBinaryExpression) {
            IElementType operator = ((PsiBinaryExpression) node).getOperationTokenType();
            Object operandLeft = evaluate(((PsiBinaryExpression) node).getLOperand());
            Object operandRight = evaluate(((PsiBinaryExpression) node).getROperand());
            if (operandLeft == null || operandRight == null) {
                if (mAllowUnknown) {
                    if (operandLeft == null) {
                        return operandRight;
                    } else {
                        return operandLeft;
                    }
                }
                return null;
            }
            if (operandLeft instanceof String && operandRight instanceof String) {
                if (operator == JavaTokenType.PLUS) {
                    return operandLeft.toString() + operandRight.toString();
                }
                return null;
            } else if (operandLeft instanceof Boolean && operandRight instanceof Boolean) {
                boolean left = (Boolean) operandLeft;
                boolean right = (Boolean) operandRight;
                if (operator == JavaTokenType.OROR) {
                    return left || right;
                } else if (operator == JavaTokenType.ANDAND) {
                    return left && right;
                } else if (operator == JavaTokenType.OR) {
                    return left | right;
                } else if (operator == JavaTokenType.XOR) {
                    return left ^ right;
                } else if (operator == JavaTokenType.AND) {
                    return left & right;
                } else if (operator == JavaTokenType.EQEQ) {
                    return left == right;
                } else if (operator == JavaTokenType.NE) {
                    return left != right;
                }
            } else if (operandLeft instanceof Number && operandRight instanceof Number) {
                Number left = (Number) operandLeft;
                Number right = (Number) operandRight;
                boolean isInteger =
                        !(left instanceof Float || left instanceof Double
                                || right instanceof Float || right instanceof Double);
                boolean isWide =
                        isInteger ? (left instanceof Long || right instanceof Long)
                                : (left instanceof Double || right instanceof Double);

                if (operator == JavaTokenType.OR) {
                    if (isWide) {
                        return left.longValue() | right.longValue();
                    } else {
                        return left.intValue() | right.intValue();
                    }
                } else if (operator == JavaTokenType.XOR) {
                    if (isWide) {
                        return left.longValue() ^ right.longValue();
                    } else {
                        return left.intValue() ^ right.intValue();
                    }
                } else if (operator == JavaTokenType.AND) {
                    if (isWide) {
                        return left.longValue() & right.longValue();
                    } else {
                        return left.intValue() & right.intValue();
                    }
                } else if (operator == JavaTokenType.EQEQ) {
                    if (isInteger) {
                        return left.longValue() == right.longValue();
                    } else {
                        return left.doubleValue() == right.doubleValue();
                    }
                } else if (operator == JavaTokenType.NE) {
                    if (isInteger) {
                        return left.longValue() != right.longValue();
                    } else {
                        return left.doubleValue() != right.doubleValue();
                    }
                } else if (operator == JavaTokenType.GT) {
                    if (isInteger) {
                        return left.longValue() > right.longValue();
                    } else {
                        return left.doubleValue() > right.doubleValue();
                    }
                } else if (operator == JavaTokenType.GE) {
                    if (isInteger) {
                        return left.longValue() >= right.longValue();
                    } else {
                        return left.doubleValue() >= right.doubleValue();
                    }
                } else if (operator == JavaTokenType.LT) {
                    if (isInteger) {
                        return left.longValue() < right.longValue();
                    } else {
                        return left.doubleValue() < right.doubleValue();
                    }
                } else if (operator == JavaTokenType.LE) {
                    if (isInteger) {
                        return left.longValue() <= right.longValue();
                    } else {
                        return left.doubleValue() <= right.doubleValue();
                    }
                } else if (operator == JavaTokenType.LTLT) {
                    if (isWide) {
                        return left.longValue() << right.intValue();
                    } else {
                        return left.intValue() << right.intValue();
                    }
                } else if (operator == JavaTokenType.GTGT) {
                    if (isWide) {
                        return left.longValue() >> right.intValue();
                    } else {
                        return left.intValue() >> right.intValue();
                    }
                } else if (operator == JavaTokenType.GTGTGT) {
                    if (isWide) {
                        return left.longValue() >>> right.intValue();
                    } else {
                        return left.intValue() >>> right.intValue();
                    }
                } else if (operator == JavaTokenType.PLUS) {
                    if (isInteger) {
                        if (isWide) {
                            return left.longValue() + right.longValue();
                        } else {
                            return left.intValue() + right.intValue();
                        }
                    } else {
                        if (isWide) {
                            return left.doubleValue() + right.doubleValue();
                        } else {
                            return left.floatValue() + right.floatValue();
                        }
                    }
                } else if (operator == JavaTokenType.MINUS) {
                    if (isInteger) {
                        if (isWide) {
                            return left.longValue() - right.longValue();
                        } else {
                            return left.intValue() - right.intValue();
                        }
                    } else {
                        if (isWide) {
                            return left.doubleValue() - right.doubleValue();
                        } else {
                            return left.floatValue() - right.floatValue();
                        }
                    }
                } else if (operator == JavaTokenType.ASTERISK) {
                    if (isInteger) {
                        if (isWide) {
                            return left.longValue() * right.longValue();
                        } else {
                            return left.intValue() * right.intValue();
                        }
                    } else {
                        if (isWide) {
                            return left.doubleValue() * right.doubleValue();
                        } else {
                            return left.floatValue() * right.floatValue();
                        }
                    }
                } else if (operator == JavaTokenType.DIV) {
                    if (isInteger) {
                        if (isWide) {
                            return left.longValue() / right.longValue();
                        } else {
                            return left.intValue() / right.intValue();
                        }
                    } else {
                        if (isWide) {
                            return left.doubleValue() / right.doubleValue();
                        } else {
                            return left.floatValue() / right.floatValue();
                        }
                    }
                } else if (operator == JavaTokenType.PERC) {
                    if (isInteger) {
                        if (isWide) {
                            return left.longValue() % right.longValue();
                        } else {
                            return left.intValue() % right.intValue();
                        }
                    } else {
                        if (isWide) {
                            return left.doubleValue() % right.doubleValue();
                        } else {
                            return left.floatValue() % right.floatValue();
                        }
                    }
                } else {
                    return null;
                }
            }
        } else if (node instanceof PsiTypeCastExpression) {
            PsiTypeCastExpression cast = (PsiTypeCastExpression) node;
            Object operandValue = evaluate(cast.getOperand());
            if (operandValue instanceof Number) {
                Number number = (Number) operandValue;
                PsiTypeElement typeElement = cast.getCastType();
                if (typeElement != null) {
                    PsiType type = typeElement.getType();
                    if (PsiType.FLOAT.equals(type)) {
                        return number.floatValue();
                    } else if (PsiType.DOUBLE.equals(type)) {
                        return number.doubleValue();
                    } else if (PsiType.INT.equals(type)) {
                        return number.intValue();
                    } else if (PsiType.LONG.equals(type)) {
                        return number.longValue();
                    } else if (PsiType.SHORT.equals(type)) {
                        return number.shortValue();
                    } else if (PsiType.BYTE.equals(type)) {
                        return number.byteValue();
                    }
                }
            }
            return operandValue;
        } else if (node instanceof PsiReference) {
            PsiElement resolved = ((PsiReference) node).resolve();
            if (resolved instanceof PsiField) {
                PsiField field = (PsiField) resolved;
                Object value = field.computeConstantValue();
                if (value != null) {
                    return value;
                }
                if (field.getInitializer() != null) {
                    return evaluate(field.getInitializer());
                }
                return null;
            } else if (resolved instanceof PsiLocalVariable) {
                PsiLocalVariable variable = (PsiLocalVariable) resolved;
                PsiStatement statement = PsiTreeUtil.getParentOfType(node, PsiStatement.class,
                        false);
                if (statement != null) {
                    PsiStatement prev = PsiTreeUtil.getPrevSiblingOfType(statement,
                            PsiStatement.class);
                    String targetName = variable.getName();
                    if (targetName == null) {
                        return null;
                    }
                    while (prev != null) {
                        if (prev instanceof PsiDeclarationStatement) {
                            for (PsiElement element : ((PsiDeclarationStatement) prev)
                                    .getDeclaredElements()) {
                                if (variable.equals(element)) {
                                    return evaluate(variable.getInitializer());
                                }
                            }
                        } else if (prev instanceof PsiExpressionStatement) {
                            PsiExpression expression = ((PsiExpressionStatement) prev)
                                    .getExpression();
                            if (expression instanceof PsiAssignmentExpression) {
                                PsiAssignmentExpression assign
                                        = (PsiAssignmentExpression) expression;
                                PsiExpression lhs = assign.getLExpression();
                                if (lhs instanceof PsiReferenceExpression) {
                                    PsiReferenceExpression reference = (PsiReferenceExpression) lhs;
                                    if (targetName.equals(reference.getReferenceName()) &&
                                            reference.getQualifier() == null) {
                                        return evaluate(assign.getRExpression());
                                    }
                                }
                            }
                        }
                        prev = PsiTreeUtil.getPrevSiblingOfType(prev,
                                PsiStatement.class);
                    }
                }
            }
        } else if (node instanceof PsiNewExpression) {
            PsiNewExpression creation = (PsiNewExpression) node;
            PsiArrayInitializerExpression initializer = creation.getArrayInitializer();
            PsiType type = creation.getType();
            if (type instanceof PsiArrayType) {
                if (initializer != null) {
                    PsiExpression[] initializers = initializer.getInitializers();
                    Class<?> commonType = null;
                    List<Object> values = Lists.newArrayListWithExpectedSize(initializers.length);
                    int count = 0;
                    for (PsiExpression expression : initializers) {
                        Object value = evaluate(expression);
                        if (value != null) {
                            values.add(value);
                            if (commonType == null) {
                                commonType = value.getClass();
                            } else {
                                while (!commonType.isAssignableFrom(value.getClass())) {
                                    commonType = commonType.getSuperclass();
                                }
                            }
                        } else if (!mAllowUnknown) {
                            // Inconclusive
                            return null;
                        }
                        count++;
                        if (count == 20) { // avoid large initializers
                            break;
                        }
                    }
                    type = type.getDeepComponentType();
                    if (type == PsiType.INT) {
                        if (!values.isEmpty()) {
                            int[] array = new int[values.size()];
                            for (int i = 0; i < values.size(); i++) {
                                Object o = values.get(i);
                                if (o instanceof Integer) {
                                    array[i] = (Integer) o;
                                }
                            }
                            return array;
                        }
                        return new int[0];
                    } else if (type == PsiType.BOOLEAN) {
                        if (!values.isEmpty()) {
                            boolean[] array = new boolean[values.size()];
                            for (int i = 0; i < values.size(); i++) {
                                Object o = values.get(i);
                                if (o instanceof Boolean) {
                                    array[i] = (Boolean) o;
                                }
                            }
                            return array;
                        }
                        return new boolean[0];
                    } else if (type == PsiType.DOUBLE) {
                        if (!values.isEmpty()) {
                            double[] array = new double[values.size()];
                            for (int i = 0; i < values.size(); i++) {
                                Object o = values.get(i);
                                if (o instanceof Double) {
                                    array[i] = (Double) o;
                                }
                            }
                            return array;
                        }
                        return new double[0];
                    } else if (type == PsiType.LONG) {
                        if (!values.isEmpty()) {
                            long[] array = new long[values.size()];
                            for (int i = 0; i < values.size(); i++) {
                                Object o = values.get(i);
                                if (o instanceof Long) {
                                    array[i] = (Long) o;
                                }
                            }
                            return array;
                        }
                        return new long[0];
                    } else if (type == PsiType.FLOAT) {
                        if (!values.isEmpty()) {
                            float[] array = new float[values.size()];
                            for (int i = 0; i < values.size(); i++) {
                                Object o = values.get(i);
                                if (o instanceof Float) {
                                    array[i] = (Float) o;
                                }
                            }
                            return array;
                        }
                        return new float[0];
                    } else if (type == PsiType.CHAR) {
                        if (!values.isEmpty()) {
                            char[] array = new char[values.size()];
                            for (int i = 0; i < values.size(); i++) {
                                Object o = values.get(i);
                                if (o instanceof Character) {
                                    array[i] = (Character) o;
                                }
                            }
                            return array;
                        }
                        return new char[0];
                    } else if (type == PsiType.BYTE) {
                        if (!values.isEmpty()) {
                            byte[] array = new byte[values.size()];
                            for (int i = 0; i < values.size(); i++) {
                                Object o = values.get(i);
                                if (o instanceof Byte) {
                                    array[i] = (Byte) o;
                                }
                            }
                            return array;
                        }
                        return new byte[0];
                    } else if (type == PsiType.SHORT) {
                        if (!values.isEmpty()) {
                            short[] array = new short[values.size()];
                            for (int i = 0; i < values.size(); i++) {
                                Object o = values.get(i);
                                if (o instanceof Short) {
                                    array[i] = (Short) o;
                                }
                            }
                            return array;
                        }
                        return new short[0];
                    } else {
                        if (!values.isEmpty()) {
                            Object o = Array.newInstance(commonType, values.size());
                            return values.toArray((Object[]) o);
                        }
                        return null;
                    }
                } else {
                    // something like "new byte[3]" but with no initializer.
                    // Look up the size and only if small, use it. E.g. if it was byte[3]
                    // we return a byte[3] array, but if it's say byte[1024*1024] we don't
                    // want to do that.
                    PsiExpression[] arrayDimensions = creation.getArrayDimensions();
                    int size = 0;
                    if (arrayDimensions.length == 1) {
                        Object fixedSize = evaluate(arrayDimensions[0]);
                        if (fixedSize instanceof Number) {
                            size = ((Number)fixedSize).intValue();
                            if (size > 30) {
                                size = 30;
                            }
                        }
                    }
                    type = type.getDeepComponentType();
                    if (type instanceof PsiPrimitiveType) {
                        if (PsiType.BYTE.equals(type)) {
                            return new byte[size];
                        }
                        if (PsiType.BOOLEAN.equals(type)) {
                            return new boolean[size];
                        }
                        if (PsiType.INT.equals(type)) {
                            return new int[size];
                        }
                        if (PsiType.LONG.equals(type)) {
                            return new long[size];
                        }
                        if (PsiType.CHAR.equals(type)) {
                            return new char[size];
                        }
                        if (PsiType.FLOAT.equals(type)) {
                            return new float[size];
                        }
                        if (PsiType.DOUBLE.equals(type)) {
                            return new double[size];
                        }
                        if (PsiType.SHORT.equals(type)) {
                            return new short[size];
                        }
                    } else if (type instanceof PsiClassType) {
                        String className = type.getCanonicalText();
                        if (TYPE_STRING.equals(className)) {
                            //noinspection SSBasedInspection
                            return new String[size];
                        }
                        if (TYPE_OBJECT.equals(className)) {
                            //noinspection SSBasedInspection
                            return new Object[size];
                        }
                    }
                }
            }
        }

        // TODO: Check for MethodInvocation and perform some common operations -
        // Math.* methods, String utility methods like notNullize, etc

        return null;
    }

    /**
     * Returns true if the node is pointing to a an array literal
     */
    public static boolean isArrayLiteral(@Nullable UElement node, @NonNull JavaContext context) {
        if (node instanceof UReferenceExpression) {
            PsiElement resolved = ((UReferenceExpression) node).resolve();
            if (resolved instanceof PsiVariable) {
                PsiVariable variable = (PsiVariable) resolved;
                UExpression lastAssignment =
                        UastLintUtils.findLastAssignment(variable, node, context);

                if (lastAssignment != null) {
                    return isArrayLiteral(lastAssignment, context);
                }
            }
        } else if (UastExpressionUtils.isNewArrayWithDimensions(node)) {
            return true;
        } else if (UastExpressionUtils.isNewArrayWithInitializer(node)) {
            return true;
        } else if (node instanceof UParenthesizedExpression) {
            UParenthesizedExpression parenthesizedExpression = (UParenthesizedExpression) node;
            UExpression expression = parenthesizedExpression.getExpression();
            return isArrayLiteral(expression, context);
        } else if (UastExpressionUtils.isTypeCast(node)) {
            UBinaryExpressionWithType castExpression = (UBinaryExpressionWithType) node;
            assert castExpression != null;
            UExpression operand = castExpression.getOperand();
            return isArrayLiteral(operand, context);
        }

        return false;
    }

    /**
     * Returns true if the node is pointing to a an array literal
     */
    public static boolean isArrayLiteral(@Nullable PsiElement node) {
        if (node instanceof PsiReference) {
            PsiElement resolved = ((PsiReference) node).resolve();
            if (resolved instanceof PsiField) {
                PsiField field = (PsiField) resolved;
                if (field.getInitializer() != null) {
                    return isArrayLiteral(field.getInitializer());
                }
            } else if (resolved instanceof PsiLocalVariable) {
                PsiLocalVariable variable = (PsiLocalVariable) resolved;
                PsiStatement statement = PsiTreeUtil.getParentOfType(node, PsiStatement.class,
                        false);
                if (statement != null) {
                    PsiStatement prev = PsiTreeUtil.getPrevSiblingOfType(statement,
                            PsiStatement.class);
                    String targetName = variable.getName();
                    if (targetName == null) {
                        return false;
                    }
                    while (prev != null) {
                        if (prev instanceof PsiDeclarationStatement) {
                            for (PsiElement element : ((PsiDeclarationStatement) prev)
                                    .getDeclaredElements()) {
                                if (variable.equals(element)) {
                                    return isArrayLiteral(variable.getInitializer());
                                }
                            }
                        } else if (prev instanceof PsiExpressionStatement) {
                            PsiExpression expression = ((PsiExpressionStatement) prev)
                                    .getExpression();
                            if (expression instanceof PsiAssignmentExpression) {
                                PsiAssignmentExpression assign
                                        = (PsiAssignmentExpression) expression;
                                PsiExpression lhs = assign.getLExpression();
                                if (lhs instanceof PsiReferenceExpression) {
                                    PsiReferenceExpression reference = (PsiReferenceExpression) lhs;
                                    if (targetName.equals(reference.getReferenceName()) &&
                                            reference.getQualifier() == null) {
                                        return isArrayLiteral(assign.getRExpression());
                                    }
                                }
                            }
                        }
                        prev = PsiTreeUtil.getPrevSiblingOfType(prev,
                                PsiStatement.class);
                    }
                }
            }
        } else if (node instanceof PsiNewExpression) {
            PsiNewExpression creation = (PsiNewExpression) node;
            if (creation.getArrayInitializer() != null) {
                return true;
            }
            PsiType type = creation.getType();
            if (type instanceof PsiArrayType) {
                return true;
            }
        } else if (node instanceof PsiParenthesizedExpression) {
            PsiParenthesizedExpression parenthesizedExpression = (PsiParenthesizedExpression) node;
            PsiExpression expression = parenthesizedExpression.getExpression();
            if (expression != null) {
                return isArrayLiteral(expression);
            }
        } else if (node instanceof PsiTypeCastExpression) {
            PsiTypeCastExpression castExpression = (PsiTypeCastExpression) node;
            PsiExpression operand = castExpression.getOperand();
            if (operand != null) {
                return isArrayLiteral(operand);
            }
        }

        return false;
    }

    /**
     * Evaluates the given node and returns the constant value it resolves to, if any. Convenience
     * wrapper which creates a new {@linkplain ConstantEvaluator}, evaluates the node and returns
     * the result.
     *
     * @param context the context to use to resolve field references, if any
     * @param node    the node to compute the constant value for
     * @return the corresponding constant value - a String, an Integer, a Float, and so on
     * @deprecated Use {@link #evaluate(JavaContext, PsiElement)} instead
     */
    @Deprecated
    @Nullable
    public static Object evaluate(@NonNull JavaContext context, @NonNull Node node) {
        return new ConstantEvaluator(context).evaluate(node);
    }

    /**
     * Evaluates the given node and returns the constant string it resolves to, if any. Convenience
     * wrapper which creates a new {@linkplain ConstantEvaluator}, evaluates the node and returns
     * the result if the result is a string.
     *
     * @param context      the context to use to resolve field references, if any
     * @param node         the node to compute the constant value for
     * @param allowUnknown whether we should construct the string even if some parts of it are
     *                     unknown
     * @return the corresponding string, if any
     * @deprecated Use {@link #evaluateString(JavaContext, PsiElement, boolean)} instead
     */
    @Deprecated
    @Nullable
    public static String evaluateString(@NonNull JavaContext context, @NonNull Node node,
            boolean allowUnknown) {
        ConstantEvaluator evaluator = new ConstantEvaluator(context);
        if (allowUnknown) {
            evaluator.allowUnknowns();
        }
        Object value = evaluator.evaluate(node);
        return value instanceof String ? (String) value : null;
    }

    /**
     * Evaluates the given node and returns the constant value it resolves to, if any. Convenience
     * wrapper which creates a new {@linkplain ConstantEvaluator}, evaluates the node and returns
     * the result.
     *
     * @param context the context to use to resolve field references, if any
     * @param node    the node to compute the constant value for
     * @return the corresponding constant value - a String, an Integer, a Float, and so on
     */
    @Nullable
    public static Object evaluate(@Nullable JavaContext context, @NonNull PsiElement node) {
        return new ConstantEvaluator(context).evaluate(node);
    }

    /**
     * Evaluates the given node and returns the constant value it resolves to, if any. Convenience
     * wrapper which creates a new {@linkplain ConstantEvaluator}, evaluates the node and returns
     * the result.
     *
     * @param context the context to use to resolve field references, if any
     * @param node    the node to compute the constant value for
     * @return the corresponding constant value - a String, an Integer, a Float, and so on
     */
    @Nullable
    public static Object evaluate(@Nullable JavaContext context, @NonNull UElement node) {
        return new ConstantEvaluator(context).evaluate(node);
    }

    /**
     * Evaluates the given node and returns the constant string it resolves to, if any. Convenience
     * wrapper which creates a new {@linkplain ConstantEvaluator}, evaluates the node and returns
     * the result if the result is a string.
     *
     * @param context      the context to use to resolve field references, if any
     * @param node         the node to compute the constant value for
     * @param allowUnknown whether we should construct the string even if some parts of it are
     *                     unknown
     * @return the corresponding string, if any
     */
    @Nullable
    public static String evaluateString(@Nullable JavaContext context, @NonNull PsiElement node,
            boolean allowUnknown) {
        ConstantEvaluator evaluator = new ConstantEvaluator(context);
        if (allowUnknown) {
            evaluator.allowUnknowns();
        }
        Object value = evaluator.evaluate(node);
        return value instanceof String ? (String) value : null;
    }

    /**
     * Evaluates the given node and returns the constant string it resolves to, if any. Convenience
     * wrapper which creates a new {@linkplain ConstantEvaluator}, evaluates the node and returns
     * the result if the result is a string.
     *
     * @param context      the context to use to resolve field references, if any
     * @param node         the node to compute the constant value for
     * @param allowUnknown whether we should construct the string even if some parts of it are
     *                     unknown
     * @return the corresponding string, if any
     */
    @Nullable
    public static String evaluateString(@Nullable JavaContext context, @NonNull UElement node,
            boolean allowUnknown) {
        ConstantEvaluator evaluator = new ConstantEvaluator(context);
        if (allowUnknown) {
            evaluator.allowUnknowns();
        }
        Object value = evaluator.evaluate(node);
        return value instanceof String ? (String) value : null;
    }
}
