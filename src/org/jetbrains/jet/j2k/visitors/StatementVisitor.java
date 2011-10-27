package org.jetbrains.jet.j2k.visitors;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.Converter;
import org.jetbrains.jet.j2k.ast.*;

/**
 * @author ignatov
 */
public class StatementVisitor extends ElementVisitor implements Visitor {
  private Statement myResult = new EmptyStatement();

  @NotNull
  public Statement getStatement() {
    return myResult;
  }

  @Override
  public void visitAssertStatement(PsiAssertStatement statement) {
    super.visitAssertStatement(statement);
  }

  @Override
  public void visitBlockStatement(PsiBlockStatement statement) {
    super.visitBlockStatement(statement);
  }

  @Override
  public void visitBreakStatement(PsiBreakStatement statement) {
    super.visitBreakStatement(statement);
  }

  @Override
  public void visitContinueStatement(PsiContinueStatement statement) {
    super.visitContinueStatement(statement);
  }

  @Override
  public void visitDeclarationStatement(PsiDeclarationStatement statement) {
    super.visitDeclarationStatement(statement);
    myResult = new DeclarationStatement(
      Converter.elementsToElementList(statement.getDeclaredElements())
    );
  }

  @Override
  public void visitDoWhileStatement(PsiDoWhileStatement statement) {
    super.visitDoWhileStatement(statement);
  }

  @Override
  public void visitEmptyStatement(PsiEmptyStatement statement) {
    super.visitEmptyStatement(statement);
  }

  @Override
  public void visitExpressionStatement(PsiExpressionStatement statement) {
    super.visitExpressionStatement(statement);
    myResult = Converter.expressionToExpression(statement.getExpression());
  }

  @Override
  public void visitExpressionListStatement(PsiExpressionListStatement statement) {
    super.visitExpressionListStatement(statement);
  }

  @Override
  public void visitForStatement(PsiForStatement statement) {
    super.visitForStatement(statement);
  }

  @Override
  public void visitForeachStatement(PsiForeachStatement statement) {
    super.visitForeachStatement(statement);
  }

  @Override
  public void visitImportStatement(PsiImportStatement statement) {
    super.visitImportStatement(statement);
  }

  @Override
  public void visitIfStatement(PsiIfStatement statement) {
    super.visitIfStatement(statement);
  }

  @Override
  public void visitImportStaticStatement(PsiImportStaticStatement statement) {
    super.visitImportStaticStatement(statement);
  }

  @Override
  public void visitLabeledStatement(PsiLabeledStatement statement) {
    super.visitLabeledStatement(statement);
  }

  @Override
  public void visitPackageStatement(PsiPackageStatement statement) {
    super.visitPackageStatement(statement);
  }

  @Override
  public void visitSwitchLabelStatement(PsiSwitchLabelStatement statement) {
    super.visitSwitchLabelStatement(statement);
  }

  @Override
  public void visitSwitchStatement(PsiSwitchStatement statement) {
    super.visitSwitchStatement(statement);
  }

  @Override
  public void visitSynchronizedStatement(PsiSynchronizedStatement statement) {
    super.visitSynchronizedStatement(statement);
  }

  @Override
  public void visitThrowStatement(PsiThrowStatement statement) {
    super.visitThrowStatement(statement);
  }

  @Override
  public void visitTryStatement(PsiTryStatement statement) {
    super.visitTryStatement(statement);
  }

  @Override
  public void visitWhileStatement(PsiWhileStatement statement) {
    super.visitWhileStatement(statement);
  }

  @Override
  public void visitReturnStatement(PsiReturnStatement statement) {
    super.visitReturnStatement(statement);

    myResult = new ReturnStatement(
      Converter.expressionToExpression(statement.getReturnValue())
    );
  }
}
