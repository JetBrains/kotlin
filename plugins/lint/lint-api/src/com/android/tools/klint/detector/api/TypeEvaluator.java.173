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
import static com.android.tools.klint.client.api.JavaParser.TYPE_CHAR;
import static com.android.tools.klint.client.api.JavaParser.TYPE_DOUBLE;
import static com.android.tools.klint.client.api.JavaParser.TYPE_FLOAT;
import static com.android.tools.klint.client.api.JavaParser.TYPE_INT;
import static com.android.tools.klint.client.api.JavaParser.TYPE_LONG;
import static com.android.tools.klint.client.api.JavaParser.TYPE_STRING;
import static com.android.tools.klint.detector.api.JavaContext.getParentOfType;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.client.api.JavaParser.DefaultTypeDescriptor;
import com.android.tools.klint.client.api.JavaParser.ResolvedClass;
import com.android.tools.klint.client.api.JavaParser.ResolvedField;
import com.android.tools.klint.client.api.JavaParser.ResolvedMethod;
import com.android.tools.klint.client.api.JavaParser.ResolvedNode;
import com.android.tools.klint.client.api.JavaParser.ResolvedVariable;
import com.android.tools.klint.client.api.JavaParser.TypeDescriptor;
import com.android.tools.klint.client.api.UastLintUtils;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UVariable;
import org.jetbrains.uast.UastUtils;
import org.jetbrains.uast.UReferenceExpression;
import org.jetbrains.uast.util.UastExpressionUtils;

import java.util.ListIterator;

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
import lombok.ast.Literal;
import lombok.ast.Node;
import lombok.ast.NullLiteral;
import lombok.ast.Statement;
import lombok.ast.StringLiteral;
import lombok.ast.UnaryExpression;
import lombok.ast.VariableDeclaration;
import lombok.ast.VariableDefinition;
import lombok.ast.VariableDefinitionEntry;
import lombok.ast.VariableReference;

/**
 * Evaluates the types of nodes. This goes deeper than
 * {@link JavaContext#getType(Node)} in that it analyzes the
 * flow and for example figures out that if you ask for the type of {@code var}
 * in this code snippet:
 * <pre>
 *     Object o = new StringBuilder();
 *     Object var = o;
 * </pre>
 * it will return "java.lang.StringBuilder".
 * <p>
 * <b>NOTE:</b> This type evaluator does not (yet) compute the correct
 * types when involving implicit type conversions, so be careful
 * if using this for primitives; e.g. for "int * long" it might return
 * the type "int".
 */
public class TypeEvaluator {
    private final JavaContext mContext;

    /**
     * Creates a new constant evaluator
     *
     * @param context the context to use to resolve field references, if any
     */
    public TypeEvaluator(@Nullable JavaContext context) {
        mContext = context;
    }


    /**
     * Returns the inferred type of the given node
     * @deprecated Use {@link #evaluate(PsiElement)} instead
     */
    @Deprecated
    @Nullable
    public TypeDescriptor evaluate(@NonNull Node node) {
        ResolvedNode resolved = null;
        if (mContext != null) {
            resolved = mContext.resolve(node);
        }
        if (resolved instanceof ResolvedMethod) {
            TypeDescriptor type;
            ResolvedMethod method = (ResolvedMethod) resolved;
            if (method.isConstructor()) {
                ResolvedClass containingClass = method.getContainingClass();
                type = containingClass.getType();
            } else {
                type = method.getReturnType();
            }
            return type;
        }
        if (resolved instanceof ResolvedField) {
            ResolvedField field = (ResolvedField) resolved;
            Node astNode = field.findAstNode();
            if (astNode instanceof VariableDeclaration) {
                VariableDeclaration declaration = (VariableDeclaration)astNode;
                VariableDefinition definition = declaration.astDefinition();
                if (definition != null) {
                    VariableDefinitionEntry first = definition.astVariables().first();
                    if (first != null) {
                        Expression initializer = first.astInitializer();
                        if (initializer != null) {
                            TypeDescriptor type = evaluate(initializer);
                            if (type != null) {
                                return type;
                            }
                        }
                    }
                }
            }
            return field.getType();
        }

        if (node instanceof VariableReference) {
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

                String targetName = ((VariableReference) node).astIdentifier().astValue();
                while (iterator.hasPrevious()) {
                    Node previous = iterator.previous();
                    if (previous instanceof VariableDeclaration) {
                        VariableDeclaration declaration = (VariableDeclaration) previous;
                        VariableDefinition definition = declaration.astDefinition();
                        for (VariableDefinitionEntry entry : definition.astVariables()) {
                            if (entry.astInitializer() != null && entry.astName().astValue()
                                    .equals(targetName)) {
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
        } else if (node instanceof Cast) {
            Cast cast = (Cast) node;
            if (mContext != null) {
                ResolvedNode typeReference = mContext.resolve(cast.astTypeReference());
                if (typeReference instanceof ResolvedClass) {
                    return ((ResolvedClass) typeReference).getType();
                }
            }
            TypeDescriptor viewType = evaluate(cast.astOperand());
            if (viewType != null) {
                return viewType;
            }
        } else if (node instanceof Literal) {
            if (node instanceof NullLiteral) {
                return null;
            } else if (node instanceof BooleanLiteral) {
                return new DefaultTypeDescriptor(TYPE_BOOLEAN);
            } else if (node instanceof StringLiteral) {
                return new DefaultTypeDescriptor(TYPE_STRING);
            } else if (node instanceof CharLiteral) {
                return new DefaultTypeDescriptor(TYPE_CHAR);
            } else if (node instanceof IntegralLiteral) {
                IntegralLiteral literal = (IntegralLiteral) node;
                // Don't combine to ?: since that will promote astIntValue to a long
                if (literal.astMarkedAsLong()) {
                    return new DefaultTypeDescriptor(TYPE_LONG);
                } else {
                    return new DefaultTypeDescriptor(TYPE_INT);
                }
            } else if (node instanceof FloatingPointLiteral) {
                FloatingPointLiteral literal = (FloatingPointLiteral) node;
                // Don't combine to ?: since that will promote astFloatValue to a double
                if (literal.astMarkedAsFloat()) {
                    return new DefaultTypeDescriptor(TYPE_FLOAT);
                } else {
                    return new DefaultTypeDescriptor(TYPE_DOUBLE);
                }
            }
        } else if (node instanceof UnaryExpression) {
            return evaluate(((UnaryExpression) node).astOperand());
        } else if (node instanceof InlineIfExpression) {
            InlineIfExpression expression = (InlineIfExpression) node;
            if (expression.astIfTrue() != null) {
                return evaluate(expression.astIfTrue());
            } else if (expression.astIfFalse() != null) {
                return evaluate(expression.astIfFalse());
            }
        } else if (node instanceof BinaryExpression) {
            BinaryExpression expression = (BinaryExpression) node;
            BinaryOperator operator = expression.astOperator();
            switch (operator) {
                case LOGICAL_OR:
                case LOGICAL_AND:
                case EQUALS:
                case NOT_EQUALS:
                case GREATER:
                case GREATER_OR_EQUAL:
                case LESS:
                case LESS_OR_EQUAL:
                    return new DefaultTypeDescriptor(TYPE_BOOLEAN);
            }

            TypeDescriptor type = evaluate(expression.astLeft());
            if (type != null) {
                return type;
            }
            return evaluate(expression.astRight());
        }

        if (resolved instanceof ResolvedVariable) {
            ResolvedVariable variable = (ResolvedVariable) resolved;
            return variable.getType();
        }

        return null;
    }

    /**
     * Returns the inferred type of the given node
     */
    @Nullable
    public PsiType evaluate(@Nullable PsiElement node) {
        if (node == null) {
            return null;
        }

        PsiElement resolved = null;
        if (node instanceof PsiReference) {
            resolved = ((PsiReference) node).resolve();
        }
        if (resolved instanceof PsiMethod) {
            PsiMethod method = (PsiMethod) resolved;
            if (method.isConstructor()) {
                PsiClass containingClass = method.getContainingClass();
                if (containingClass != null && mContext != null) {
                    return mContext.getEvaluator().getClassType(containingClass);
                }
            } else {
                return method.getReturnType();
            }
        }

        if (resolved instanceof PsiField) {
            PsiField field = (PsiField) resolved;
            if (field.getInitializer() != null) {
                PsiType type = evaluate(field.getInitializer());
                if (type != null) {
                    return type;
                }
            }
            return field.getType();
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
                        for (PsiElement element : ((PsiDeclarationStatement)prev).getDeclaredElements()) {
                            if (variable.equals(element)) {
                                return evaluate(variable.getInitializer());
                            }
                        }
                    } else if (prev instanceof PsiExpressionStatement) {
                        PsiExpression expression = ((PsiExpressionStatement)prev).getExpression();
                        if (expression instanceof PsiAssignmentExpression) {
                            PsiAssignmentExpression assign = (PsiAssignmentExpression) expression;
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

            return variable.getType();
        } else if (node instanceof PsiExpression) {
            PsiExpression expression = (PsiExpression) node;
            return expression.getType();
        }

        return null;
    }

    @Nullable
    public static PsiType evaluate(@NonNull JavaContext context, @Nullable UElement node) {
        if (node == null) {
            return null;
        }
        
        UElement resolved = node;
        if (resolved instanceof UReferenceExpression) {
            resolved = UastUtils.tryResolveUDeclaration(resolved, context.getUastContext());
        }
        
        if (resolved instanceof UMethod) {
            return ((UMethod) resolved).getPsi().getReturnType();
        } else if (resolved instanceof UVariable) {
            UVariable variable = (UVariable) resolved; 
            UElement lastAssignment = UastLintUtils.findLastAssignment(variable, node, context);
            if (lastAssignment != null) {
                return evaluate(context, lastAssignment);
            }
            return variable.getType();
        } else if (resolved instanceof UCallExpression) {
            if (UastExpressionUtils.isMethodCall(resolved)) {
                PsiMethod resolvedMethod = ((UCallExpression) resolved).resolve();
                return resolvedMethod != null ? resolvedMethod.getReturnType() : null;
            } else {
                return ((UCallExpression) resolved).getExpressionType();
            }
        } else if (resolved instanceof UExpression) {
            return ((UExpression) resolved).getExpressionType();
        }

        return null;
    }

    /**
     * Evaluates the given node and returns the likely type of the instance. Convenience
     * wrapper which creates a new {@linkplain TypeEvaluator}, evaluates the node and returns
     * the result.
     *
     * @param context the context to use to resolve field references, if any
     * @param node    the node to compute the type for
     * @return the corresponding type descriptor, if found
     * @deprecated Use {@link #evaluate(JavaContext, PsiElement)} instead
     */
    @Deprecated
    @Nullable
    public static TypeDescriptor evaluate(@NonNull JavaContext context, @NonNull Node node) {
        return new TypeEvaluator(context).evaluate(node);
    }

    /**
     * Evaluates the given node and returns the likely type of the instance. Convenience
     * wrapper which creates a new {@linkplain TypeEvaluator}, evaluates the node and returns
     * the result.
     *
     * @param context the context to use to resolve field references, if any
     * @param node    the node to compute the type for
     * @return the corresponding type descriptor, if found
     */
    @Nullable
    public static PsiType evaluate(@NonNull JavaContext context, @NonNull PsiElement node) {
        return new TypeEvaluator(context).evaluate(node);
    }
}
