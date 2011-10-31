package org.jetbrains.jet.j2k.visitors;

import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.ast.*;

import static org.jetbrains.jet.j2k.Converter.*;

/**
 * @author ignatov
 */
public class ExpressionVisitor extends StatementVisitor implements Visitor {
  private Expression myResult = new EmptyExpression();

  @NotNull
  public Expression getResult() {
    return myResult;
  }

  @Override
  public void visitArrayAccessExpression(PsiArrayAccessExpression expression) {
    super.visitArrayAccessExpression(expression);
  }

  @Override
  public void visitArrayInitializerExpression(PsiArrayInitializerExpression expression) {
    super.visitArrayInitializerExpression(expression);
  }

  @Override
  public void visitAssignmentExpression(PsiAssignmentExpression expression) {
    super.visitAssignmentExpression(expression);

    // TODO: simplify

    final IElementType tokenType = expression.getOperationSign().getTokenType();

    if (tokenType == JavaTokenType.GTGTGTEQ) { // because it's transform to the method call
      myResult = new AssignmentExpression(
        expressionToExpression(expression.getLExpression()),
        new DummyMethodCallExpression(
          expressionToExpression(expression.getLExpression()),
          "cyclicShiftRight",
          expressionToExpression(expression.getRExpression())
        ),
        "="
      );
    } else {

      String secondOp = "";
      if (tokenType == JavaTokenType.GTGTEQ) secondOp = "shr";
      if (tokenType == JavaTokenType.LTLTEQ) secondOp = "shl";
      if (tokenType == JavaTokenType.XOREQ) secondOp = "xor";
      if (tokenType == JavaTokenType.ANDEQ) secondOp = "and";
      if (tokenType == JavaTokenType.OREQ) secondOp = "or";

      if (!secondOp.equals("")) // if not Kotlin operators
        myResult = new AssignmentExpression(
          expressionToExpression(expression.getLExpression()),
          new BinaryExpression(
            expressionToExpression(expression.getLExpression()),
            expressionToExpression(expression.getRExpression()),
            secondOp
          ),
          "="
        );
      else
        myResult = new AssignmentExpression(
          expressionToExpression(expression.getLExpression()),
          expressionToExpression(expression.getRExpression()),
          expression.getOperationSign().getText() // TODO
        );
    }
  }

  private String getOperatorString(PsiJavaToken op) {
    if (op.getTokenType() == JavaTokenType.GTGT) return "shr";
    if (op.getTokenType() == JavaTokenType.LTLT) return "shl";
    if (op.getTokenType() == JavaTokenType.XOR) return "xor";
    if (op.getTokenType() == JavaTokenType.AND) return "and";
    if (op.getTokenType() == JavaTokenType.OR) return "or";
    if (op.getTokenType() == JavaTokenType.OR) return "or";

    return op.getText();
  }

  @Override
  public void visitBinaryExpression(PsiBinaryExpression expression) {
    super.visitBinaryExpression(expression);

    if (expression.getOperationSign().getTokenType() == JavaTokenType.GTGTGT)
      myResult = new DummyMethodCallExpression(
        expressionToExpression(expression.getLOperand()),
        "cyclicShiftRight",
        expressionToExpression(expression.getROperand()));
    else
      myResult =
        new BinaryExpression(
          expressionToExpression(expression.getLOperand()),
          expressionToExpression(expression.getROperand()),
          getOperatorString(expression.getOperationSign())
        );
  }

  @Override
  public void visitClassObjectAccessExpression(PsiClassObjectAccessExpression expression) {
    super.visitClassObjectAccessExpression(expression);
  }

  @Override
  public void visitConditionalExpression(PsiConditionalExpression expression) {
    super.visitConditionalExpression(expression);
  }

  @Override
  public void visitExpressionStatement(PsiExpressionStatement statement) {
    super.visitExpressionStatement(statement);
  }

  @Override
  public void visitExpressionList(PsiExpressionList list) {
    super.visitExpressionList(list);
    myResult =
      new ExpressionList(expressionsToExpressionList(list.getExpressions()));
  }

  @Override
  public void visitInstanceOfExpression(PsiInstanceOfExpression expression) {
    super.visitInstanceOfExpression(expression);
  }

  @Override
  public void visitLiteralExpression(PsiLiteralExpression expression) {
    super.visitLiteralExpression(expression);
    myResult = new LiteralExpression(
      new IdentifierImpl(
        ((PsiLiteralExpressionImpl) expression).getCanonicalText() // TODO
      )
    );
  }

  @Override
  public void visitMethodCallExpression(PsiMethodCallExpression expression) {
    super.visitMethodCallExpression(expression);
    // TODO: not resolved
    myResult =
      new MethodCallExpression(
        expressionToExpression(expression.getMethodExpression()),
        elementToElement(expression.getArgumentList())
      );
  }

  @Override
  public void visitCallExpression(PsiCallExpression callExpression) {
    super.visitCallExpression(callExpression);
  }

  @Override
  public void visitNewExpression(PsiNewExpression expression) {
    super.visitNewExpression(expression);
  }

  @Override
  public void visitParenthesizedExpression(PsiParenthesizedExpression expression) {
    super.visitParenthesizedExpression(expression);
  }

  @Override
  public void visitPostfixExpression(PsiPostfixExpression expression) {
    super.visitPostfixExpression(expression);

    String op = "";
    if (expression.getOperationSign().getTokenType() == JavaTokenType.PLUSPLUS) op = "++";
    if (expression.getOperationSign().getTokenType() == JavaTokenType.MINUSMINUS) op = "--";

    myResult = new PostfixOperator(
      op,
      expressionToExpression(expression.getOperand())
    );
  }

  @Override
  public void visitPrefixExpression(PsiPrefixExpression expression) {
    super.visitPrefixExpression(expression);
  }

  @Override
  public void visitReferenceExpression(PsiReferenceExpression expression) {
    super.visitReferenceExpression(expression);
    myResult = new IdentifierImpl(expression.getQualifiedName()); // TODO
  }

  @Override
  public void visitSuperExpression(PsiSuperExpression expression) {
    super.visitSuperExpression(expression);
  }

  @Override
  public void visitThisExpression(PsiThisExpression expression) {
    super.visitThisExpression(expression);
  }

  @Override
  public void visitTypeCastExpression(PsiTypeCastExpression expression) {
    super.visitTypeCastExpression(expression);
  }

  @Override
  public void visitPolyadicExpression(PsiPolyadicExpression expression) {
    super.visitPolyadicExpression(expression);
  }
}
