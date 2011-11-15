package org.jetbrains.jet.j2k;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.j2k.ast.*;
import org.jetbrains.jet.j2k.ast.Class;
import org.jetbrains.jet.j2k.ast.Enum;
import org.jetbrains.jet.j2k.ast.Modifier;
import org.jetbrains.jet.j2k.util.AstUtil;
import org.jetbrains.jet.j2k.visitors.*;

import java.util.*;

/**
 * @author ignatov
 */
public class Converter {
  @NotNull
  public static File fileToFile(PsiJavaFile javaFile) {
    final PsiImportList importList = javaFile.getImportList();
    List<Import> imports = importList == null ?
      new LinkedList<Import>() :
      importsToImportList(importList.getAllImportStatements());
    return new File(javaFile.getPackageName(), imports, classesToClassList(javaFile.getClasses())); // TODO: use identifier
  }

  @NotNull
  private static List<Class> classesToClassList(PsiClass[] classes) {
    List<Class> result = new LinkedList<Class>();
    for (PsiClass t : classes) result.add(classToClass(t));
    return result;
  }

  public static AnonymousClass anonymousClassToAnonymousClass(PsiAnonymousClass anonymousClass) {
    // TODO: replace by Block, use class.getChild() method
    return new AnonymousClass(
      classesToClassList(anonymousClass.getAllInnerClasses()),
      methodsToFunctionList(anonymousClass.getMethods()),
      fieldsToFieldList(anonymousClass.getAllFields())
    );
  }

  private static List<Field> getFinalOrWithEmptyInitializer(List<? extends Field> fields) {
    List<Field> result = new LinkedList<Field>();
    for (Field f : fields)
      if (f.isFinal() || f.getInitializer().toKotlin().isEmpty())
        result.add(f);
    return result;
  }

  private static List<Parameter> createParametersFromFields(List<? extends Field> fields) {
    List<Parameter> result = new LinkedList<Parameter>();
    for (Field f : fields) result.add(new Parameter(f.getIdentifier(), f.getType()));
    return result;
  }

  private static List<Statement> createInitStatementsFromFields(List<? extends Field> fields) {
    List<Statement> result = new LinkedList<Statement>();
    for (Field f : fields) {
      final String identifierToKotlin = f.getIdentifier().toKotlin();
      result.add(new DummyStringStatement("$" + identifierToKotlin + " = " + identifierToKotlin));
    }
    return result;
  }

  private static String createPrimaryConstructorInvocation(String s, List<? extends Field> fields, Map<String, String> initializers) {
    List<String> result = new LinkedList<String>();
    for (Field f : fields) {
      final String id = f.getIdentifier().toKotlin();
      result.add(initializers.get(id));
    }
    return s + "(" + AstUtil.join(result, ", ") + ")";
  }

  private static Class classToClass(PsiClass psiClass) {
    final Set<String> modifiers = modifiersListToModifiersSet(psiClass.getModifierList());
    final List<Class> innerClasses = classesToClassList(psiClass.getAllInnerClasses());
    final List<Function> methods = methodsToFunctionList(psiClass.getMethods());
    final List<Field> fields = fieldsToFieldList(psiClass.getFields());
    final List<Element> typeParameters = elementsToElementList(psiClass.getTypeParameters());
    final List<Type> implementsTypes = typesToNotNullableTypeList(psiClass.getImplementsListTypes());
    final List<Type> extendsTypes = typesToNotNullableTypeList(psiClass.getExtendsListTypes());
    final IdentifierImpl name = new IdentifierImpl(psiClass.getName());
    final List<Expression> baseClassParams = new LinkedList<Expression>();

    // we try to find super() call and generate class declaration like that: class A(name: String, i : Int) : Base(name)
    final SuperVisitor visitor = new SuperVisitor();
    psiClass.accept(visitor);
    final HashSet<PsiExpressionList> resolvedSuperCallParameters = visitor.getResolvedSuperCallParameters();
    if (resolvedSuperCallParameters.size() == 1)
      baseClassParams.addAll(
        expressionsToExpressionList(
          resolvedSuperCallParameters.toArray(new PsiExpressionList[1])[0].getExpressions()
        )
      );

    // we create primary constructor from all non final fields and fields without initializers
    if (!psiClass.isEnum() && !psiClass.isInterface() && getPrimaryConstructorForThisCase(psiClass) == null) {
      final List<Field> finalOrWithEmptyInitializer = getFinalOrWithEmptyInitializer(fields);
      final Map<String, String> initializers = new HashMap<String, String>();

      for (final Function f : methods) {
        for (Field fo : finalOrWithEmptyInitializer) {
          String init = getDefaultInitializer(fo);
          initializers.put(fo.getIdentifier().toKotlin(), init);
        }

        // and modify secondaries
        if (f.getKind() == INode.Kind.CONSTRUCTOR && !((Constructor) f).isPrimary()) {
          final List<Statement> newStatements = new LinkedList<Statement>();

          for (Statement s : f.getBlock().getStatements()) {
            boolean isRemoved = false;

            if (s.getKind() == INode.Kind.ASSIGNMENT_EXPRESSION) {
              final AssignmentExpression assignmentExpression = (AssignmentExpression) s;
              if (assignmentExpression.getLeft().getKind() == INode.Kind.CALL_CHAIN) {
                for (Field fo : finalOrWithEmptyInitializer) {
                  final String id = fo.getIdentifier().toKotlin();
                  if (((CallChainExpression) assignmentExpression.getLeft()).getIdentifier().toKotlin().endsWith("." + id)) {
                    initializers.put(id, assignmentExpression.getRight().toKotlin());
                    isRemoved = true;
                  }
                }
              }
            }
            if (!isRemoved) {
              newStatements.add(s);
            }
          }

          newStatements.add(
            0,
            new DummyStringStatement(
              "val __ = " + createPrimaryConstructorInvocation(
                name.toKotlin(),
                finalOrWithEmptyInitializer,
                initializers)));

          f.setBlock(new Block(newStatements));
        }
      }

      methods.add(
        new Constructor(
          Identifier.EMPTY_IDENTIFIER,
          Collections.<String>emptySet(),
          new ClassType(name),
          Collections.<Element>emptyList(),
          new ParameterList(createParametersFromFields(finalOrWithEmptyInitializer)),
          new Block(createInitStatementsFromFields(finalOrWithEmptyInitializer)),
          true
        )
      );
    }

    if (psiClass.isInterface())
      return new Trait(name, modifiers, typeParameters, extendsTypes, Collections.<Expression>emptyList(), implementsTypes, innerClasses, methods, fields);
    if (psiClass.isEnum())
      return new Enum(name, modifiers, typeParameters, Collections.<Type>emptyList(), Collections.<Expression>emptyList(), implementsTypes,
        innerClasses, methods, fieldsToFieldListForEnums(psiClass.getAllFields()));
    return new Class(name, modifiers, typeParameters, extendsTypes, baseClassParams, implementsTypes, innerClasses, methods, fields);
  }

  private static String getDefaultInitializer(Field f) {
    if (f.getType().isNullable())
      return "null";
    else {
      final String typeToKotlin = f.getType().toKotlin();
      if (typeToKotlin.equals("Boolean")) return "false";
      if (typeToKotlin.equals("Char")) return "' '";
      if (typeToKotlin.equals("Double")) return "0.dbl";
      if (typeToKotlin.equals("Float")) return "0.flt";
      return "0";
    }
  }

  // hack for enums, we remove methods:
  // private int ordinal() { ... }
  // private String name() { ... }
  // those methods any enum class inherits from java.lang.Enum
  private static List<Field> fieldsToFieldListForEnums(PsiField[] fields) {
    List<Field> result = new LinkedList<Field>();
    for (PsiField f : fields) {
      if ((f.getName().equals("ordinal")
        && f.getType().getCanonicalText().equals("int")
        && f.hasModifierProperty(PsiModifier.PRIVATE)
        && f.hasModifierProperty(PsiModifier.FINAL)
      ) ||
        (f.getName().equals("name")
          && f.getType().getCanonicalText().equals("java.lang.String")
          && f.hasModifierProperty(PsiModifier.PRIVATE)
          && f.hasModifierProperty(PsiModifier.FINAL)
        ))
        continue;

      result.add(fieldToField(f));
    }
    return result;
  }

  private static List<Field> fieldsToFieldList(PsiField[] fields) {
    List<Field> result = new LinkedList<Field>();
    for (PsiField f : fields) result.add(fieldToField(f));
    return result;
  }

  private static Field fieldToField(PsiField field) {
    Set<String> modifiers = modifiersListToModifiersSet(field.getModifierList());
    if (field instanceof PsiEnumConstant) // TODO: remove instanceof
      return new EnumConstant(
        new IdentifierImpl(field.getName()), // TODO
        modifiers,
        typeToType(field.getType()),
        elementToElement(((PsiEnumConstant) field).getArgumentList())
      );
    return new Field(
      new IdentifierImpl(field.getName()), // TODO
      modifiers,
      typeToType(field.getType()),
      expressionToExpression(field.getInitializer()) // TODO: add modifiers
    );
  }

  @NotNull
  private static List<Function> methodsToFunctionList(PsiMethod[] methods) {
    List<Function> result = new LinkedList<Function>();
    for (PsiMethod t : methods) result.add(methodToFunction(t, true));
    return result;
  }

  @Nullable
  private static PsiMethod getPrimaryConstructorForThisCase(PsiClass psiClass) {
    ThisVisitor tv = new ThisVisitor();
    psiClass.accept(tv);
    return tv.getPrimaryConstructor();
  }

  public static boolean isConstructorPrimary(@Nullable PsiMethod constructor) {
    if (constructor == null)
      return false;
    if (constructor.getParent() instanceof PsiClass) {
      final PsiClass parent = (PsiClass) constructor.getParent();
      if (parent.getConstructors().length == 1)
        return true;
      else {
        PsiMethod c = getPrimaryConstructorForThisCase(parent); // TODO: move up to classToClass() method
        if (c != null && c.hashCode() == constructor.hashCode())
          return true;
      }
    }
    return false;
  }

  private static List<Statement> removeEmpty(List<Statement> statements) {
    List<Statement> result = new LinkedList<Statement>();
    for (Statement s : statements)
      if (s != Statement.EMPTY_STATEMENT && s != Expression.EMPTY_EXPRESSION)
        result.add(s);
    return result;
  }

  @NotNull
  private static Function methodToFunction(PsiMethod method, boolean notEmpty) {
    final IdentifierImpl identifier = new IdentifierImpl(method.getName());
    final Type type = typeToType(method.getReturnType());
    final Block body = blockToBlock(method.getBody(), notEmpty);
    final Element params = elementToElement(method.getParameterList());
    final List<Element> typeParameters = elementsToElementList(method.getTypeParameters());

    final Set<String> modifiers = modifiersListToModifiersSet(method.getModifierList());
    if (method.getHierarchicalMethodSignature().getSuperSignatures().size() > 0)
      modifiers.add(Modifier.OVERRIDE);
    if (method.getParent() instanceof PsiClass && ((PsiClass) method.getParent()).isInterface())
      modifiers.remove(Modifier.ABSTRACT);

    if (method.isConstructor()) { // TODO: simplify
      boolean isPrimary = isConstructorPrimary(method);
      return new Constructor(
        identifier,
        modifiers,
        type,
        typeParameters,
        params,
        new Block(removeEmpty(body.getStatements()), false),
        isPrimary
      );
    }
    return new Function(
      identifier,
      modifiers,
      type,
      typeParameters,
      params,
      body
    );
  }

  @NotNull
  public static Block blockToBlock(@Nullable PsiCodeBlock block, boolean notEmpty) {
    if (block == null) return Block.EMPTY_BLOCK;
    return new Block(statementsToStatementList(block.getStatements()), notEmpty);
  }

  @NotNull
  public static Block blockToBlock(@Nullable PsiCodeBlock block) {
    return blockToBlock(block, true);
  }

  @NotNull
  public static List<Statement> statementsToStatementList(PsiStatement[] statements) {
    List<Statement> result = new LinkedList<Statement>();
    for (PsiStatement t : statements) result.add(statementToStatement(t));
    return result;
  }

  @NotNull
  public static Statement statementToStatement(@Nullable PsiStatement s) {
    if (s == null) return Statement.EMPTY_STATEMENT;
    final StatementVisitor statementVisitor = new StatementVisitor();
    s.accept(statementVisitor);
    return statementVisitor.getResult();
  }

  @NotNull
  public static List<Expression> expressionsToExpressionList(PsiExpression[] expressions) {
    List<Expression> result = new LinkedList<Expression>();
    for (PsiExpression e : expressions) result.add(expressionToExpression(e));
    return result;
  }

  @NotNull
  public static Expression expressionToExpression(@Nullable PsiExpression e) {
    if (e == null) return Expression.EMPTY_EXPRESSION;
    final ExpressionVisitor expressionVisitor = new ExpressionVisitor();
    e.accept(expressionVisitor);
    return expressionVisitor.getResult();
  }

  @NotNull
  public static Element elementToElement(PsiElement e) {
    if (e == null) return Element.EMPTY_ELEMENT;
    final ElementVisitor elementVisitor = new ElementVisitor();
    e.accept(elementVisitor);
    return elementVisitor.getResult();
  }

  @NotNull
  public static List<Element> elementsToElementList(PsiElement[] elements) {
    List<Element> result = new LinkedList<Element>();
    for (PsiElement e : elements) result.add(elementToElement(e));
    return result;
  }

  @NotNull
  public static Type typeToType(@Nullable PsiType type) {
    if (type == null) return Type.EMPTY_TYPE;
    TypeVisitor typeVisitor = new TypeVisitor();
    type.accept(typeVisitor);
    return typeVisitor.getResult();
  }

  @NotNull
  public static List<Type> typesToTypeList(PsiType[] types) {
    List<Type> result = new LinkedList<Type>();
    for (PsiType t : types) result.add(typeToType(t));
    return result;
  }

  @NotNull
  private static List<Type> typesToNotNullableTypeList(PsiType[] types) {
    List<Type> result = new LinkedList<Type>(typesToTypeList(types));
    for (Type p : result) p.convertToNotNull();
    return result;
  }

  @NotNull
  private static List<Import> importsToImportList(PsiImportStatementBase[] imports) {
    List<Import> result = new LinkedList<Import>();
    for (PsiImportStatementBase t : imports) result.add(importToImport(t));
    return result;
  }

  @NotNull
  private static Import importToImport(PsiImportStatementBase t) {
    if (t != null) {
      final PsiJavaCodeReferenceElement reference = t.getImportReference();
      if (reference != null) {
        return new Import(reference.getQualifiedName()); // TODO: use identifier
      }
    }
    return new Import("");
  }

  @NotNull
  public static List<Parameter> parametersToParameterList(PsiParameter[] parameters) {
    List<Parameter> result = new LinkedList<Parameter>();
    for (PsiParameter t : parameters) result.add(parameterToParameter(t));
    return result;
  }

  @NotNull
  public static Parameter parameterToParameter(PsiParameter parameter) {
    return new Parameter(
      new IdentifierImpl(parameter.getName()), // TODO: remove
      typeToType(parameter.getType())
    );
  }

  @NotNull
  public static Identifier identifierToIdentifier(@Nullable PsiIdentifier identifier) {
    if (identifier == null) return Identifier.EMPTY_IDENTIFIER;
    return new IdentifierImpl(identifier.getText());
  }

  public static Set<String> modifiersListToModifiersSet(PsiModifierList modifierList) {
    HashSet<String> modifiersSet = new HashSet<String>();
    if (modifierList != null) {
      if (modifierList.hasExplicitModifier(PsiModifier.ABSTRACT)) modifiersSet.add(Modifier.ABSTRACT);
      if (modifierList.hasModifierProperty(PsiModifier.FINAL)) modifiersSet.add(Modifier.FINAL);
      if (modifierList.hasModifierProperty(PsiModifier.STATIC)) modifiersSet.add(Modifier.STATIC);
      if (modifierList.hasExplicitModifier(PsiModifier.PUBLIC)) modifiersSet.add(Modifier.PUBLIC);
      if (modifierList.hasExplicitModifier(PsiModifier.PROTECTED)) modifiersSet.add(Modifier.PROTECTED);
      if (modifierList.hasExplicitModifier(PsiModifier.PACKAGE_LOCAL)) modifiersSet.add(Modifier.INTERNAL);
      if (modifierList.hasExplicitModifier(PsiModifier.PRIVATE)) modifiersSet.add(Modifier.PRIVATE);
    }
    return modifiersSet;
  }
}