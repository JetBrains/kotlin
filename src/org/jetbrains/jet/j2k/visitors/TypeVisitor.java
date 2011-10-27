package org.jetbrains.jet.j2k.visitors;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.ast.*;
import org.jetbrains.jet.j2k.util.AstUtil;

/**
 * @author ignatov
 */
public class TypeVisitor extends PsiTypeVisitor<Type> implements Visitor {
  private Type myResult = new EmptyType();

  @NotNull
  public Type getResult() {
    return myResult;
  }

  @Override
  public Type visitType(PsiType type) {
    return super.visitType(type);
  }

  @Override
  public Type visitPrimitiveType(PsiPrimitiveType primitiveType) {
    final String name = primitiveType.getCanonicalText();
    final IdentifierImpl identifier = new IdentifierImpl(name);

    if (name.equals("void"))
      myResult = new PrimitiveType(new IdentifierImpl("Unit"));
    else if (Node.PRIMITIVE_TYPES.contains(name))
      myResult = new PrimitiveType(new IdentifierImpl(AstUtil.upperFirstCharacter(name)));
    else myResult = new PrimitiveType(identifier);
    return super.visitPrimitiveType(primitiveType);
  }

  @Override
  public Type visitArrayType(PsiArrayType arrayType) {
    return super.visitArrayType(arrayType);
  }

  @Override
  public Type visitClassType(PsiClassType classType) {
    myResult = new ClassType(new IdentifierImpl(classType.getClassName()));
    return super.visitClassType(classType);
  }

  @Override
  public Type visitCapturedWildcardType(PsiCapturedWildcardType capturedWildcardType) {
    return super.visitCapturedWildcardType(capturedWildcardType);
  }

  @Override
  public Type visitWildcardType(PsiWildcardType wildcardType) {
    return super.visitWildcardType(wildcardType);
  }

  @Override
  public Type visitEllipsisType(PsiEllipsisType ellipsisType) {
    return super.visitEllipsisType(ellipsisType);
  }

  @Override
  public Type visitDisjunctionType(PsiDisjunctionType disjunctionType) {
    return super.visitDisjunctionType(disjunctionType);
  }
}
