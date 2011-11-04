package org.jetbrains.jet.j2k.visitors;

import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.ast.*;

import java.util.List;

import static org.jetbrains.jet.j2k.Converter.*;

/**
 * @author ignatov
 */
public class ExpressionVisitor extends StatementVisitor implements Visitor {
  private Expression myResult = Expression.EMPTY_EXPRESSION;

  @NotNull
  @Override
  public Expression getResult() {
    return myResult;
  }

  @Override
  public void visitArrayAccessExpression(PsiArrayAccessExpression expression) {
    super.visitArrayAccessExpression(expression);
    myResult = new ArrayAccessExpression(
      expressionToExpression(expression.getArrayExpression()),
      expressionToExpression(expression.getIndexExpression())
    );
  }

  @Override
  public void visitArrayInitializerExpression(PsiArrayInitializerExpression expression) {
    super.visitArrayInitializerExpression(expression);
    myResult = new ArrayInitializerExpression(
      expressionsToExpressionList(expression.getInitializers())
    );
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

  @NotNull
  private String getOperatorString(@NotNull IElementType tokenType) {
    if (tokenType == JavaTokenType.PLUS) return "+";
    if (tokenType == JavaTokenType.MINUS) return "-";
    if (tokenType == JavaTokenType.ASTERISK) return "*";
    if (tokenType == JavaTokenType.DIV) return "/";
    if (tokenType == JavaTokenType.PERC) return "%";
    if (tokenType == JavaTokenType.GTGT) return "shr";
    if (tokenType == JavaTokenType.LTLT) return "shl";
    if (tokenType == JavaTokenType.XOR) return "xor";
    if (tokenType == JavaTokenType.AND) return "and";
    if (tokenType == JavaTokenType.OR) return "or";
    if (tokenType == JavaTokenType.OR) return "or";
    if (tokenType == JavaTokenType.GT) return ">";
    if (tokenType == JavaTokenType.LT) return "<";
    if (tokenType == JavaTokenType.GE) return ">=";
    if (tokenType == JavaTokenType.LE) return "<=";
    if (tokenType == JavaTokenType.NE) return "!=";
    if (tokenType == JavaTokenType.ANDAND) return "&&";
    if (tokenType == JavaTokenType.OROR) return "||";
    if (tokenType == JavaTokenType.PLUSPLUS) return "++";
    if (tokenType == JavaTokenType.MINUSMINUS) return "--";

    System.out.println("UNSUPPORTED TOKEN TYPE: " + tokenType.toString());
    return "";
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
          getOperatorString(expression.getOperationSign().getTokenType())
        );
  }

  @Override
  public void visitClassObjectAccessExpression(PsiClassObjectAccessExpression expression) {
    super.visitClassObjectAccessExpression(expression);
  }

  @Override
  public void visitConditionalExpression(PsiConditionalExpression expression) {
    super.visitConditionalExpression(expression);
    myResult = new ParenthesizedExpression(
      new IfStatement(
        expressionToExpression(expression.getCondition()),
        expressionToExpression(expression.getThenExpression()),
        expressionToExpression(expression.getElseExpression())
      )
    );
  }

  @Override
  public void visitExpressionList(PsiExpressionList list) {
    super.visitExpressionList(list);
    myResult = new ExpressionList(expressionsToExpressionList(list.getExpressions()));
  }

  @Override
  public void visitInstanceOfExpression(PsiInstanceOfExpression expression) {
    super.visitInstanceOfExpression(expression);
    myResult = new IsOperator(
      expressionToExpression(expression.getOperand()),
      elementToElement(expression.getCheckType()));
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
    myResult = // TODO: not resolved
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

    if (expression.getArrayInitializer() != null) // new Foo[] {}
      myResult = expressionToExpression(expression.getArrayInitializer());
    else if (expression.getArrayDimensions().length > 0) { // new Foo[5]
      final List<Expression> callExpression = expressionsToExpressionList(expression.getArrayDimensions());
      callExpression.add(new IdentifierImpl("{null}")); // TODO: remove

      myResult = new NewClassExpression(
        typeToType(expression.getType()), // TODO: remove
        new ExpressionList(callExpression)
      );
    } else {
      myResult = new NewClassExpression(
        elementToElement(expression.getClassOrAnonymousClassReference()),
        elementToElement(expression.getArgumentList())
      );
    }
  }

  @Override
  public void visitParenthesizedExpression(PsiParenthesizedExpression expression) {
    super.visitParenthesizedExpression(expression);
    myResult = new ParenthesizedExpression(
      expressionToExpression(expression.getExpression())
    );
  }

  @Override
  public void visitPostfixExpression(PsiPostfixExpression expression) {
    super.visitPostfixExpression(expression);
    myResult = new PostfixOperator(
      getOperatorString(expression.getOperationSign().getTokenType()),
      expressionToExpression(expression.getOperand())
    );
  }

  @Override
  public void visitPrefixExpression(PsiPrefixExpression expression) {
    super.visitPrefixExpression(expression);
    myResult = new PrefixOperator(
      getOperatorString(expression.getOperationSign().getTokenType()),
      expressionToExpression(expression.getOperand())
    );
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

    final PsiTypeElement castType = expression.getCastType();
    if (castType != null) {
      myResult = new TypeCastExpression(
        typeToType(castType.getType()),
        expressionToExpression(expression.getOperand())
      );
    }
  }

  @Override
  public void visitPolyadicExpression(PsiPolyadicExpression expression) {
    super.visitPolyadicExpression(expression);
    if (expression.getOperationTokenType() != JavaTokenType.GTGTGT)
      myResult = new PolyadicExpression(
        expressionsToExpressionList(expression.getOperands()),
        getOperatorString(expression.getOperationTokenType())
      );
    else {
      // TODO: support GTGTGT
    }
  }
  }
