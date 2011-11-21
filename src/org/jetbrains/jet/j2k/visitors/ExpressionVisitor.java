package org.jetbrains.jet.j2k.visitors;

import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.j2k.Converter;
import org.jetbrains.jet.j2k.ast.*;

import java.util.List;

import static org.jetbrains.jet.j2k.Converter.*;

/**
 * @author ignatov
 */
public class ExpressionVisitor extends StatementVisitor {
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

    String secondOp = "";
    if (tokenType == JavaTokenType.GTGTEQ) secondOp = "shr";
    if (tokenType == JavaTokenType.LTLTEQ) secondOp = "shl";
    if (tokenType == JavaTokenType.XOREQ) secondOp = "xor";
    if (tokenType == JavaTokenType.ANDEQ) secondOp = "and";
    if (tokenType == JavaTokenType.OREQ) secondOp = "or";
    if (tokenType == JavaTokenType.GTGTGTEQ) secondOp = "cyclicShiftRight";

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
    if (tokenType == JavaTokenType.GTGTGT) return "cyclicShiftRight";
    if (tokenType == JavaTokenType.GT) return ">";
    if (tokenType == JavaTokenType.LT) return "<";
    if (tokenType == JavaTokenType.GE) return ">=";
    if (tokenType == JavaTokenType.LE) return "<=";
    if (tokenType == JavaTokenType.EQEQ) return "==";
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
    myResult = new ClassObjectAccessExpression(elementToElement(expression.getOperand()));
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
    if (!SuperVisitor.isSuper(expression.getMethodExpression()) || !isInsidePrimaryConstructor(expression))
      myResult = // TODO: not resolved
        new MethodCallExpression(
          expressionToExpression(expression.getMethodExpression()),
          elementToElement(expression.getArgumentList()),
          typeToType(expression.getType()).isNullable(),
          typesToTypeList(expression.getTypeArguments())
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
      myResult = createNewEmptyArray(expression);
    else if (expression.getArrayDimensions().length > 0) { // new Foo[5]
      myResult = createNewNonEmptyArray(expression);
    } else { // new Class(): common case
      myResult = createNewClassExpression(expression);
    }
  }

  @NotNull
  private Expression createNewClassExpression(@NotNull PsiNewExpression expression) {
    final PsiAnonymousClass anonymousClass = expression.getAnonymousClass();
    final PsiMethod constructor = expression.resolveMethod();
    if (constructor == null || isConstructorPrimary(constructor)) {
      return new NewClassExpression(
        expressionToExpression(expression.getQualifier()),
        elementToElement(expression.getClassOrAnonymousClassReference()),
        elementToElement(expression.getArgumentList()),
        anonymousClass != null ? anonymousClassToAnonymousClass(anonymousClass) : null
      );
    }
    // is constructor secondary
    return new CallChainExpression(
      new IdentifierImpl(constructor.getName(), false),
      new MethodCallExpression(new IdentifierImpl("init"), elementToElement(expression.getArgumentList()), false, typesToTypeList(expression.getTypeArguments())));
  }

  @NotNull
  private NewClassExpression createNewNonEmptyArray(@NotNull PsiNewExpression expression) {
    final List<Expression> callExpression = expressionsToExpressionList(expression.getArrayDimensions());
    callExpression.add(new IdentifierImpl("{null}")); // TODO: remove
    return new NewClassExpression(
      typeToType(expression.getType()),
      new ExpressionList(callExpression)
    );
  }

  @NotNull
  private static Expression createNewEmptyArray(@NotNull PsiNewExpression expression) {
    return expressionToExpression(expression.getArrayInitializer());
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

    final boolean isFieldReference = isFieldReference(expression, getContainingClass(expression));
    final boolean hasDollar = isFieldReference && isInsidePrimaryConstructor(expression);
    final boolean insideSecondaryConstructor = isInsideSecondaryConstructor(expression);
    final boolean hasReceiver = isFieldReference && insideSecondaryConstructor;
    final boolean isThis = isThisExpression(expression);
    final boolean isNullable = typeToType(expression.getType()).isNullable();
    final String className = getClassName(expression);

    Expression identifier = new IdentifierImpl(expression.getReferenceName(), isNullable);

    if (hasDollar)
      identifier = new IdentifierImpl(expression.getReferenceName(), hasDollar, isNullable);
    else {
      final String temporaryObject = "__";
      if (hasReceiver)
        identifier = new CallChainExpression(new IdentifierImpl(temporaryObject, false), new IdentifierImpl(expression.getReferenceName(), isNullable));
      else if (insideSecondaryConstructor && isThis)
        identifier = new IdentifierImpl("val " + temporaryObject + " = " + className); // TODO: hack
    }

    myResult = new CallChainExpression(
      expressionToExpression(expression.getQualifierExpression()),
      identifier // TODO: if type exists so identifier is nullable
    );
  }

  @NotNull
  private String getClassName(PsiReferenceExpression expression) {
    PsiElement context = expression.getContext();
    while (context != null) {
      if (context instanceof PsiMethod && ((PsiMethod) context).isConstructor()) {
        final PsiClass containingClass = ((PsiMethod) context).getContainingClass();
        if (containingClass != null) {
          final PsiIdentifier identifier = containingClass.getNameIdentifier();
          if (identifier != null)
            return identifier.getText();
        }
      }
      context = context.getContext();
    }
    return "";
  }

  private boolean isFieldReference(PsiReferenceExpression expression, PsiClass currentClass) {
    final PsiReference reference = expression.getReference();
    if (reference != null) {
      final PsiElement resolvedReference = reference.resolve();
      if (resolvedReference != null) {
        if (resolvedReference instanceof PsiField) {
          return ((PsiField) resolvedReference).getContainingClass() == currentClass;
        }
      }
    }
    return false;
  }

  private boolean isInsideSecondaryConstructor(PsiReferenceExpression expression) {
    PsiElement context = expression.getContext();
    while (context != null) {
      if (context instanceof PsiMethod && ((PsiMethod) context).isConstructor())
        return !Converter.isConstructorPrimary((PsiMethod) context);
      context = context.getContext();
    }
    return false;
  }

  private boolean isInsidePrimaryConstructor(PsiExpression expression) {
    PsiElement context = expression.getContext();
    while (context != null) {
      if (context instanceof PsiMethod && ((PsiMethod) context).isConstructor())
        return Converter.isConstructorPrimary((PsiMethod) context);
      context = context.getContext();
    }
    return false;
  }

  @Nullable
  private PsiClass getContainingClass(@NotNull PsiExpression expression) {
    PsiElement context = expression.getContext();
    while (context != null) {
      if (context instanceof PsiMethod && ((PsiMethod) context).isConstructor())
        return ((PsiMethod) context).getContainingClass();
      context = context.getContext();
    }
    return null;
  }

  private boolean isThisExpression(PsiReferenceExpression expression) {
    for (PsiReference r : expression.getReferences())
      if (r.getCanonicalText().equals("this")) {
        final PsiElement res = r.resolve();
        if (res != null && res instanceof PsiMethod && ((PsiMethod) res).isConstructor())
          return true;
      }
    return false;
  }

  @Override
  public void visitSuperExpression(PsiSuperExpression expression) {
    super.visitSuperExpression(expression);
    final PsiJavaCodeReferenceElement qualifier = expression.getQualifier();
    myResult = new SuperExpression(
      qualifier != null ?
        new IdentifierImpl(qualifier.getQualifiedName()) :
        Identifier.EMPTY_IDENTIFIER
    );
  }

  @Override
  public void visitThisExpression(PsiThisExpression expression) {
    super.visitThisExpression(expression);
    final PsiJavaCodeReferenceElement qualifier = expression.getQualifier();
    myResult = new ThisExpression(
      qualifier != null ?
        new IdentifierImpl(qualifier.getQualifiedName()) :
        Identifier.EMPTY_IDENTIFIER
    );
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
    myResult = new PolyadicExpression(
      expressionsToExpressionList(expression.getOperands()),
      getOperatorString(expression.getOperationTokenType())
    );
  }
}