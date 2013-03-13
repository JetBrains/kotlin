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

package org.jetbrains.jet.j2k.visitors;

import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.j2k.Converter;
import org.jetbrains.jet.j2k.ast.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.jetbrains.jet.j2k.Converter.identifierToIdentifier;
import static org.jetbrains.jet.j2k.ConverterUtil.countWritingAccesses;

public class StatementVisitor extends ElementVisitor {
    private Statement myResult = Statement.EMPTY_STATEMENT;

    public StatementVisitor(@NotNull Converter converter) {
        super(converter);
    }

    @NotNull
    @Override
    public Statement getResult() {
        return myResult;
    }

    @Override
    public void visitAssertStatement(@NotNull PsiAssertStatement statement) {
        super.visitAssertStatement(statement);
        myResult = new AssertStatement(
                getConverter().expressionToExpression(statement.getAssertCondition()),
                getConverter().expressionToExpression(statement.getAssertDescription())
        );
    }

    @Override
    public void visitBlockStatement(@NotNull PsiBlockStatement statement) {
        super.visitBlockStatement(statement);
        myResult = new Block(
                getConverter().statementsToStatementList(statement.getCodeBlock().getStatements()),
                true
        );
    }

    @Override
    public void visitBreakStatement(@NotNull PsiBreakStatement statement) {
        super.visitBreakStatement(statement);
        if (statement.getLabelIdentifier() == null) {
            myResult = new BreakStatement();
        }
        else {
            myResult = new BreakStatement(
                    identifierToIdentifier(statement.getLabelIdentifier())
            );
        }
    }

    @Override
    public void visitContinueStatement(@NotNull PsiContinueStatement statement) {
        super.visitContinueStatement(statement);
        if (statement.getLabelIdentifier() == null) {
            myResult = new ContinueStatement();
        }
        else {
            myResult = new ContinueStatement(
                    identifierToIdentifier(statement.getLabelIdentifier())
            );
        }
    }

    @Override
    public void visitDeclarationStatement(@NotNull PsiDeclarationStatement statement) {
        super.visitDeclarationStatement(statement);
        myResult = new DeclarationStatement(
                getConverter().elementsToElementList(statement.getDeclaredElements())
        );
    }

    @Override
    public void visitDoWhileStatement(@NotNull PsiDoWhileStatement statement) {
        super.visitDoWhileStatement(statement);
        PsiExpression condition = statement.getCondition();
        @SuppressWarnings("ConstantConditions")
        Expression expression = condition != null && condition.getType() != null ?
                                getConverter().createSureCallOnlyForChain(condition, condition.getType()) :
                                getConverter().expressionToExpression(condition);
        myResult = new DoWhileStatement(
                expression,
                getConverter().statementToStatement(statement.getBody())
        );
    }

    @Override
    public void visitExpressionStatement(@NotNull PsiExpressionStatement statement) {
        super.visitExpressionStatement(statement);
        myResult = getConverter().expressionToExpression(statement.getExpression());
    }

    @Override
    public void visitExpressionListStatement(@NotNull PsiExpressionListStatement statement) {
        super.visitExpressionListStatement(statement);
        myResult =
                new ExpressionListStatement(getConverter().expressionsToExpressionList(statement.getExpressionList().getExpressions()));
    }

    @Override
    public void visitForStatement(@NotNull PsiForStatement statement) {
        super.visitForStatement(statement);

        PsiStatement initialization = statement.getInitialization();
        PsiStatement update = statement.getUpdate();
        PsiExpression condition = statement.getCondition();
        PsiStatement body = statement.getBody();

        PsiLocalVariable firstChild = initialization != null && initialization.getFirstChild() instanceof PsiLocalVariable
                                            ?
                                            (PsiLocalVariable) initialization.getFirstChild()
                                            : null;

        int bodyWriteCount = countWritingAccesses(firstChild, body);
        int conditionWriteCount = countWritingAccesses(firstChild, condition);
        int updateWriteCount = countWritingAccesses(firstChild, update);
        boolean onceWritableIterator = updateWriteCount == 1 && bodyWriteCount + conditionWriteCount == 0;

        IElementType operationTokenType = condition != null && condition instanceof PsiBinaryExpression ?
                                                ((PsiBinaryExpression) condition).getOperationTokenType() : null;
        if (
                initialization != null &&
                initialization instanceof PsiDeclarationStatement
                && initialization.getFirstChild() == initialization.getLastChild()
                && condition != null
                && update != null
                && update.getChildren().length == 1
                && isPlusPlusExpression(update.getChildren()[0])
                && operationTokenType != null
                && (operationTokenType == JavaTokenType.LT || operationTokenType == JavaTokenType.LE)
                && initialization.getFirstChild() != null
                && initialization.getFirstChild() instanceof PsiLocalVariable
                && firstChild != null
                && firstChild.getNameIdentifier() != null
                && onceWritableIterator
                ) {
            Expression end = getConverter().expressionToExpression(((PsiBinaryExpression) condition).getROperand());
            Expression endExpression = operationTokenType == JavaTokenType.LT ?
                                             new BinaryExpression(end, new IdentifierImpl("1"), "-") :
                                             end;
            myResult = new ForeachWithRangeStatement(
                    new IdentifierImpl(firstChild.getName()),
                    getConverter().expressionToExpression(firstChild.getInitializer()),
                    endExpression,
                    getConverter().statementToStatement(body)
            );
        }
        else { // common case: while loop instead of for loop
            List<Statement> forStatements = new LinkedList<Statement>();
            forStatements.add(getConverter().statementToStatement(initialization));
            forStatements.add(new WhileStatement(
                    getConverter().expressionToExpression(condition),
                    new Block(
                            Arrays.asList(getConverter().statementToStatement(body),
                                          new Block(Arrays.asList(getConverter().statementToStatement(update)))))));
            myResult = new Block(forStatements);
        }
    }

    private static boolean isPlusPlusExpression(@NotNull PsiElement psiElement) {
        return (psiElement instanceof PsiPostfixExpression && ((PsiPostfixExpression) psiElement).getOperationTokenType() == JavaTokenType.PLUSPLUS)
               || (psiElement instanceof PsiPrefixExpression && ((PsiPrefixExpression) psiElement).getOperationTokenType() == JavaTokenType.PLUSPLUS);
    }

    @Override
    public void visitForeachStatement(@NotNull PsiForeachStatement statement) {
        super.visitForeachStatement(statement);
        myResult = new ForeachStatement(
                getConverter().parameterToParameter(statement.getIterationParameter()),
                getConverter().expressionToExpression(statement.getIteratedValue()),
                getConverter().statementToStatement(statement.getBody())
        );
    }

    @Override
    public void visitIfStatement(@NotNull PsiIfStatement statement) {
        super.visitIfStatement(statement);
        PsiExpression condition = statement.getCondition();
        @SuppressWarnings("ConstantConditions")
        Expression expression = condition != null && condition.getType() != null ?
                                getConverter().createSureCallOnlyForChain(condition, condition.getType()) :
                                getConverter().expressionToExpression(condition);
        myResult = new IfStatement(
                expression,
                getConverter().statementToStatement(statement.getThenBranch()),
                getConverter().statementToStatement(statement.getElseBranch())
        );
    }

    @Override
    public void visitLabeledStatement(@NotNull PsiLabeledStatement statement) {
        super.visitLabeledStatement(statement);
        myResult = new LabelStatement(
                identifierToIdentifier(statement.getLabelIdentifier()),
                getConverter().statementToStatement(statement.getStatement())
        );
    }

    @Override
    public void visitSwitchLabelStatement(@NotNull PsiSwitchLabelStatement statement) {
        super.visitSwitchLabelStatement(statement);
        myResult = statement.isDefaultCase() ?
                   new DefaultSwitchLabelStatement() :
                   new SwitchLabelStatement(getConverter().expressionToExpression(statement.getCaseValue()));
    }

    @Override
    public void visitSwitchStatement(@NotNull PsiSwitchStatement statement) {
        super.visitSwitchStatement(statement);
        myResult = new SwitchContainer(
                getConverter().expressionToExpression(statement.getExpression()),
                switchBodyToCases(statement.getBody())
        );
    }

    @NotNull
    private List<CaseContainer> switchBodyToCases(@Nullable PsiCodeBlock body) {
        List<List<PsiStatement>> cases = splitToCases(body);
        List<PsiStatement> allSwitchStatements = body != null
                                                       ? Arrays.asList(body.getStatements())
                                                       : Collections.<PsiStatement>emptyList();

        List<CaseContainer> result = new LinkedList<CaseContainer>();
        List<Statement> pendingLabels = new LinkedList<Statement>();
        int i = 0;
        for (List<PsiStatement> ls : cases) {
            assert ls.size() > 0;
            PsiStatement label = ls.get(0);
            assert label instanceof PsiSwitchLabelStatement;

            assert allSwitchStatements.get(i) == label : "not a right index";

            if (ls.size() > 1) {
                pendingLabels.add(getConverter().statementToStatement(label));
                List<PsiStatement> slice = ls.subList(1, ls.size());

                if (!containsBreak(slice)) {
                    List<Statement> statements = getConverter().statementsToStatementList(slice);
                    statements.addAll(
                            getConverter().statementsToStatementList(getAllToNextBreak(allSwitchStatements, i + ls.size()))
                    );
                    result.add(new CaseContainer(pendingLabels, statements));
                    pendingLabels = new LinkedList<Statement>();
                }
                else {
                    result.add(new CaseContainer(pendingLabels, getConverter().statementsToStatementList(slice)));
                    pendingLabels = new LinkedList<Statement>();
                }
            }
            else // ls.size() == 1
            {
                pendingLabels.add(getConverter().statementToStatement(label));
            }
            i += ls.size();
        }
        return result;
    }

    private static boolean containsBreak(@NotNull List<PsiStatement> slice) {
        for (PsiStatement s : slice)
            if (s instanceof PsiBreakStatement) {
                return true;
            }
        return false;
    }

    @NotNull
    private static List<PsiStatement> getAllToNextBreak(@NotNull List<PsiStatement> allStatements, int start) {
        List<PsiStatement> result = new LinkedList<PsiStatement>();
        for (int i = start; i < allStatements.size(); i++) {
            PsiStatement s = allStatements.get(i);
            if (s instanceof PsiBreakStatement || s instanceof PsiReturnStatement) {
                return result;
            }
            if (!(s instanceof PsiSwitchLabelStatement)) {
                result.add(s);
            }
        }
        return result;
    }

    @NotNull
    private static List<List<PsiStatement>> splitToCases(@Nullable PsiCodeBlock body) {
        List<List<PsiStatement>> cases = new LinkedList<List<PsiStatement>>();
        List<PsiStatement> currentCaseStatements = new LinkedList<PsiStatement>();
        boolean isFirst = true;
        if (body != null) {
            for (PsiStatement s : body.getStatements()) {
                if (s instanceof PsiSwitchLabelStatement) {
                    if (isFirst) {
                        isFirst = false;
                    }
                    else {
                        cases.add(currentCaseStatements);
                        currentCaseStatements = new LinkedList<PsiStatement>();
                    }
                }
                currentCaseStatements.add(s);
            }
            cases.add(currentCaseStatements);
        }
        return cases;
    }

    @Override
    public void visitSynchronizedStatement(@NotNull PsiSynchronizedStatement statement) {
        super.visitSynchronizedStatement(statement);
        myResult = new SynchronizedStatement(
                getConverter().expressionToExpression(statement.getLockExpression()),
                getConverter().blockToBlock(statement.getBody())
        );
    }

    @Override
    public void visitThrowStatement(@NotNull PsiThrowStatement statement) {
        super.visitThrowStatement(statement);
        myResult = new ThrowStatement(
                getConverter().expressionToExpression(statement.getException())
        );
    }

    @Override
    public void visitTryStatement(@NotNull PsiTryStatement statement) {
        super.visitTryStatement(statement);

        List<CatchStatement> catches = new LinkedList<CatchStatement>();
        for (int i = 0; i < statement.getCatchBlocks().length; i++) {
            catches.add(new CatchStatement(
                    getConverter().parameterToParameter(statement.getCatchBlockParameters()[i]),
                    getConverter().blockToBlock(statement.getCatchBlocks()[i], true)
            ));
        }

        myResult = new TryStatement(
                getConverter().blockToBlock(statement.getTryBlock(), true),
                catches,
                getConverter().blockToBlock(statement.getFinallyBlock(), true)
        );
    }

    @Override
    public void visitWhileStatement(@NotNull PsiWhileStatement statement) {
        super.visitWhileStatement(statement);
        PsiExpression condition = statement.getCondition();
        @SuppressWarnings("ConstantConditions")
        Expression expression = condition != null && condition.getType() != null ?
                                getConverter().createSureCallOnlyForChain(condition, condition.getType()) :
                                getConverter().expressionToExpression(condition);
        myResult = new WhileStatement(
                expression,
                getConverter().statementToStatement(statement.getBody())
        );
    }

    @Override
    public void visitReturnStatement(@NotNull PsiReturnStatement statement) {
        super.visitReturnStatement(statement);
        PsiExpression returnValue = statement.getReturnValue();
        PsiType methodReturnType = getConverter().getMethodReturnType();
        Expression expression = returnValue != null && methodReturnType != null ?
                                getConverter().createSureCallOnlyForChain(returnValue, methodReturnType) :
                                getConverter().expressionToExpression(returnValue);
        myResult = new ReturnStatement(
                expression
        );
    }
}
