package org.jetbrains.jet.j2k.visitors;

import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.ast.*;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.LinkedList;
import java.util.List;

import static org.jetbrains.jet.j2k.Converter.typeToType;
import static org.jetbrains.jet.j2k.Converter.typesToTypeList;

/**
 * @author ignatov
 */
public class TypeVisitor extends PsiTypeVisitor<Type> {
  public static final String JAVA_LANG_BYTE = "java.lang.Byte";
  public static final String JAVA_LANG_CHARACTER = "java.lang.Character";
  public static final String JAVA_LANG_DOUBLE = "java.lang.Double";
  public static final String JAVA_LANG_FLOAT = "java.lang.Float";
  public static final String JAVA_LANG_INTEGER = "java.lang.Integer";
  public static final String JAVA_LANG_LONG = "java.lang.Long";
  public static final String JAVA_LANG_SHORT = "java.lang.Short";
  private static final String JAVA_LANG_BOOLEAN = "java.lang.Boolean";
  public static final String JAVA_LANG_OBJECT = "java.lang.Object";
  public static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String JAVA_LANG_ITERABLE = "java.lang.Iterable";
  private static final String JAVA_UTIL_ITERATOR = "java.util.Iterator";
  private Type myResult = Type.EMPTY_TYPE;

  @NotNull
  public Type getResult() {
    return myResult;
  }

  @Override
  public Type visitPrimitiveType(@NotNull PsiPrimitiveType primitiveType) {
    final String name = primitiveType.getCanonicalText();
    final IdentifierImpl identifier = new IdentifierImpl(name);

    if (name.equals("void"))
      myResult = new PrimitiveType(new IdentifierImpl("Unit"));
    else if (Node.PRIMITIVE_TYPES.contains(name))
      myResult = new PrimitiveType(new IdentifierImpl(AstUtil.upperFirstCharacter(name)));
    else
      myResult = new PrimitiveType(identifier);
    return super.visitPrimitiveType(primitiveType);
  }

  @Override
  public Type visitArrayType(@NotNull PsiArrayType arrayType) {
    if (myResult == Type.EMPTY_TYPE)
      myResult = new ArrayType(typeToType(arrayType.getComponentType()));
    return super.visitArrayType(arrayType);
  }

  @Override
  public Type visitClassType(@NotNull PsiClassType classType) {
    final IdentifierImpl identifier = constructClassTypeIdentifier(classType);
    final List<Type> resolvedClassTypeParams = createRawTypesForResolvedReference(classType);

    if (classType.getParameterCount() == 0 && resolvedClassTypeParams.size() > 0)
      myResult = new ClassType(identifier, resolvedClassTypeParams);
    else
      myResult = new ClassType(identifier, typesToTypeList(classType.getParameters()));
    return super.visitClassType(classType);
  }

  @NotNull
  private static IdentifierImpl constructClassTypeIdentifier(@NotNull PsiClassType classType) {
    final PsiClass psiClass = classType.resolve();
    if (psiClass != null) {
      String qualifiedName = psiClass.getQualifiedName();
      if (qualifiedName != null) {
        if (qualifiedName.equals(JAVA_LANG_ITERABLE))
          return new IdentifierImpl(JAVA_LANG_ITERABLE);
        if (qualifiedName.equals(JAVA_UTIL_ITERATOR))
          return new IdentifierImpl(JAVA_UTIL_ITERATOR);
      }
    }
    final String classTypeName = createQualifiedName(classType);

    if (classTypeName.isEmpty())
      return new IdentifierImpl(getClassTypeName(classType));

    return new IdentifierImpl(classTypeName);
  }

  @NotNull
  private static String createQualifiedName(@NotNull PsiClassType classType) {
    String classTypeName = "";
    if (classType instanceof PsiClassReferenceType) {
      final PsiJavaCodeReferenceElement reference = ((PsiClassReferenceType) classType).getReference();
      if (reference.isQualified()) {
        String result = new IdentifierImpl(reference.getReferenceName()).toKotlin();
        PsiElement qualifier = reference.getQualifier();
        while (qualifier != null) {
          final PsiJavaCodeReferenceElement p = (PsiJavaCodeReferenceElement) qualifier;
          result = new IdentifierImpl(p.getReferenceName()).toKotlin() + "." + result; // TODO: maybe need to replace by safe call?
          qualifier = p.getQualifier();
        }
        classTypeName = result;
      }
    }
    return classTypeName;
  }

  @NotNull
  private static List<Type> createRawTypesForResolvedReference(@NotNull PsiClassType classType) {
    final List<Type> typeParams = new LinkedList<Type>();
    if (classType instanceof PsiClassReferenceType) {
      final PsiJavaCodeReferenceElement reference = ((PsiClassReferenceType) classType).getReference();
      final PsiElement resolve = reference.resolve();
      if (resolve != null) {
        if (resolve instanceof PsiClass)
          //noinspection UnusedDeclaration
          for (PsiTypeParameter p : ((PsiClass) resolve).getTypeParameters())
            typeParams.add(new StarProjectionType());
      }
    }
    return typeParams;
  }

  @NotNull
  private static String getClassTypeName(@NotNull PsiClassType classType) {
    String canonicalTypeStr = classType.getCanonicalText();
    if (canonicalTypeStr.equals(JAVA_LANG_OBJECT)) return "Any";
    if (canonicalTypeStr.equals(JAVA_LANG_BYTE)) return "Byte";
    if (canonicalTypeStr.equals(JAVA_LANG_CHARACTER)) return "Char";
    if (canonicalTypeStr.equals(JAVA_LANG_DOUBLE)) return "Double";
    if (canonicalTypeStr.equals(JAVA_LANG_FLOAT)) return "Float";
    if (canonicalTypeStr.equals(JAVA_LANG_INTEGER)) return "Int";
    if (canonicalTypeStr.equals(JAVA_LANG_LONG)) return "Long";
    if (canonicalTypeStr.equals(JAVA_LANG_SHORT)) return "Short";
    if (canonicalTypeStr.equals(JAVA_LANG_BOOLEAN)) return "Boolean";
    return classType.getClassName() != null ? classType.getClassName() : classType.getCanonicalText();
  }

  @Override
  public Type visitWildcardType(@NotNull PsiWildcardType wildcardType) {
    if (wildcardType.isExtends())
      myResult = new OutProjectionType(typeToType(wildcardType.getExtendsBound()));
    else if (wildcardType.isSuper())
      myResult = new InProjectionType(typeToType(wildcardType.getSuperBound()));
    else
      myResult = new StarProjectionType();
    return super.visitWildcardType(wildcardType);
  }

  @Override
  public Type visitEllipsisType(@NotNull PsiEllipsisType ellipsisType) {
    myResult = new VarArg(typeToType(ellipsisType.getComponentType()));
    return super.visitEllipsisType(ellipsisType);
  }

  @Override
  public Type visitCapturedWildcardType(PsiCapturedWildcardType capturedWildcardType) {
    return super.visitCapturedWildcardType(capturedWildcardType);
  }

  @Override
  public Type visitDisjunctionType(PsiDisjunctionType disjunctionType) {
    return super.visitDisjunctionType(disjunctionType);
  }
}

