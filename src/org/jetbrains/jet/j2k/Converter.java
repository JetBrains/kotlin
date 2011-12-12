package org.jetbrains.jet.j2k;

import com.intellij.psi.*;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiUtil;
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
  private final static Set<String> NOT_NULL_ANNOTATIONS = new HashSet<String>() {
    {
      add("org.jetbrains.annotations.NotNull");
      add("com.sun.istack.internal.NotNull");
      add("javax.annotation.Nonnull");
    }
  };
  private static Set<String> ourClassIdentifiers = new HashSet<String>();
  private static final Dispatcher ourDispatcher = new Dispatcher();

  public static void setClassIdentifiers(Set<String> identifiers) {
    ourClassIdentifiers = identifiers;
  }

  public static Set<String> getClassIdentifiers() {
    return new HashSet<String>(ourClassIdentifiers);
  }

  public static void clearClassIdentifiers() {
    ourClassIdentifiers.clear();
  }

  @NotNull
  public static File fileToFile(@NotNull PsiJavaFile javaFile) {
    final PsiImportList importList = javaFile.getImportList();
    List<Import> imports = importList == null ? Collections.<Import>emptyList() : importsToImportList(importList.getAllImportStatements());
    return new File(quoteKeywords(javaFile.getPackageName()), imports, classesToClassList(javaFile.getClasses()));
  }

  @NotNull
  public static File fileToFileWithCompatibilityImport(@NotNull PsiJavaFile javaFile) {
    final PsiImportList importList = javaFile.getImportList();
    List<Import> imports = importList == null ? Collections.<Import>emptyList() : importsToImportList(importList.getAllImportStatements());
    imports.add(new Import("std.compatibility.*"));
    return new File(quoteKeywords(javaFile.getPackageName()), imports, classesToClassList(javaFile.getClasses()));
  }

  @NotNull
  private static String quoteKeywords(@NotNull String packageName) {
    List<String> result = new LinkedList<String>();
    for (String part : packageName.split("\\."))
      result.add(new IdentifierImpl(part).toKotlin());
    return AstUtil.join(result, ".");
  }

  @NotNull
  private static List<Class> classesToClassList(@NotNull PsiClass[] classes) {
    List<Class> result = new LinkedList<Class>();
    for (PsiClass t : classes) result.add(classToClass(t));
    return result;
  }

  @NotNull
  public static AnonymousClass anonymousClassToAnonymousClass(@NotNull PsiAnonymousClass anonymousClass) {
    return new AnonymousClass(getMembers(anonymousClass));
  }

  private static List<Member> getMembers(PsiClass psiClass) {
    List<Member> members = new LinkedList<Member>();
    for (PsiElement e : psiClass.getChildren()) {
      if (e instanceof PsiMethod) members.add(methodToFunction((PsiMethod) e, true));
      else if (e instanceof PsiField) members.add(fieldToField((PsiField) e));
      else if (e instanceof PsiClass) members.add(classToClass((PsiClass) e));
      else if (e instanceof PsiClassInitializer) members.add(initializerToInitializer((PsiClassInitializer) e));
      else if (e instanceof PsiMember) System.out.println(e.getClass() + " " + e.getText());
    }
    return members;
  }

  @NotNull
  private static List<Field> getFinalOrWithEmptyInitializer(@NotNull List<? extends Field> fields) {
    List<Field> result = new LinkedList<Field>();
    for (Field f : fields)
      if (f.isFinal() || f.getInitializer().toKotlin().isEmpty())
        result.add(f);
    return result;
  }

  @NotNull
  private static List<Parameter> createParametersFromFields(@NotNull List<? extends Field> fields) {
    List<Parameter> result = new LinkedList<Parameter>();
    for (Field f : fields) result.add(new Parameter(f.getIdentifier(), f.getType()));
    return result;
  }

  @NotNull
  private static List<Statement> createInitStatementsFromFields(@NotNull List<? extends Field> fields) {
    List<Statement> result = new LinkedList<Statement>();
    for (Field f : fields) {
      final String identifierToKotlin = f.getIdentifier().toKotlin();
      result.add(new DummyStringExpression("$" + identifierToKotlin + " = " + identifierToKotlin));
    }
    return result;
  }

  @NotNull
  private static String createPrimaryConstructorInvocation(@NotNull String s, @NotNull List<? extends Field> fields, @NotNull Map<String, String> initializers) {
    List<String> result = new LinkedList<String>();
    for (Field f : fields) {
      final String id = f.getIdentifier().toKotlin();
      result.add(initializers.get(id));
    }
    return s + "(" + AstUtil.join(result, ", ") + ")";
  }

  @NotNull
  private static Class classToClass(@NotNull PsiClass psiClass) {
    final Set<String> modifiers = modifiersListToModifiersSet(psiClass.getModifierList());
    final List<Field> fields = fieldsToFieldList(psiClass.getFields());
    final List<Element> typeParameters = elementsToElementList(psiClass.getTypeParameters());
    final List<Type> implementsTypes = typesToNotNullableTypeList(psiClass.getImplementsListTypes());
    final List<Type> extendsTypes = typesToNotNullableTypeList(psiClass.getExtendsListTypes());
    final IdentifierImpl name = new IdentifierImpl(psiClass.getName());
    final List<Expression> baseClassParams = new LinkedList<Expression>();

    List<Member> members = getMembers(psiClass);

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
    if (!psiClass.isEnum() && !psiClass.isInterface() && psiClass.getConstructors().length > 1 && getPrimaryConstructorForThisCase(psiClass) == null) {
      final List<Field> finalOrWithEmptyInitializer = getFinalOrWithEmptyInitializer(fields);
      final Map<String, String> initializers = new HashMap<String, String>();

      for (final Member m : members) {
        // and modify secondaries
        if (m.getKind() == INode.Kind.CONSTRUCTOR) {
          Function f = (Function) m;
          if (!((Constructor) f).isPrimary()) {
            for (Field fo : finalOrWithEmptyInitializer) {
              String init = getDefaultInitializer(fo);
              initializers.put(fo.getIdentifier().toKotlin(), init);
            }

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
              new DummyStringExpression(
                "val __ = " + createPrimaryConstructorInvocation(
                  name.toKotlin(),
                  finalOrWithEmptyInitializer,
                  initializers)));

            f.setBlock(new Block(newStatements));
          }
        }
      }

      members.add(
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
      return new Trait(name, modifiers, typeParameters, extendsTypes, Collections.<Expression>emptyList(), implementsTypes, members);
    if (psiClass.isEnum())
      return new Enum(name, modifiers, typeParameters, Collections.<Type>emptyList(), Collections.<Expression>emptyList(), implementsTypes, members);
    return new Class(name, modifiers, typeParameters, extendsTypes, baseClassParams, implementsTypes, members);
  }

  @NotNull
  private static Initializer initializerToInitializer(PsiClassInitializer i) {
    return new Initializer(
      blockToBlock(i.getBody(), true),
      modifiersListToModifiersSet(i.getModifierList())
    );
  }

  @NotNull
  public static String getDefaultInitializer(@NotNull Field f) {
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

  @NotNull
  private static List<Field> fieldsToFieldList(@NotNull PsiField[] fields) {
    List<Field> result = new LinkedList<Field>();
    for (PsiField f : fields) result.add(fieldToField(f));
    return result;
  }

  @NotNull
  private static Field fieldToField(@NotNull PsiField field) {
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

  @Nullable
  private static PsiMethod getPrimaryConstructorForThisCase(@NotNull PsiClass psiClass) {
    ThisVisitor tv = new ThisVisitor();
    psiClass.accept(tv);
    return tv.getPrimaryConstructor();
  }

  public static boolean isConstructorPrimary(@NotNull PsiMethod constructor) {
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

  @NotNull
  private static List<Statement> removeEmpty(@NotNull List<Statement> statements) {
    List<Statement> result = new LinkedList<Statement>();
    for (Statement s : statements)
      if (s != Statement.EMPTY_STATEMENT && s != Expression.EMPTY_EXPRESSION)
        result.add(s);
    return result;
  }

  @NotNull
  private static Function methodToFunction(@NotNull PsiMethod method, boolean notEmpty) {
    if (isOverrideObjectDirect(method))
      ourDispatcher.setExpressionVisitor(new ExpressionVisitorForDirectObjectInheritors());
    else
      ourDispatcher.setExpressionVisitor(new ExpressionVisitor());

    final IdentifierImpl identifier = new IdentifierImpl(method.getName());
    final Type returnType = typeToType(method.getReturnType(), isNotNull(method.getModifierList()));
    final Block body = blockToBlock(method.getBody(), notEmpty);
    final Element params = elementToElement(method.getParameterList());
    final List<Element> typeParameters = elementsToElementList(method.getTypeParameters());

    final Set<String> modifiers = modifiersListToModifiersSet(method.getModifierList());
    if (isOverrideAnyMethodExceptMethodsFromObject(method))
      modifiers.add(Modifier.OVERRIDE);
    if (method.getParent() instanceof PsiClass && ((PsiClass) method.getParent()).isInterface())
      modifiers.remove(Modifier.ABSTRACT);
    if (isNotOpenMethod(method))
      modifiers.add(Modifier.NOT_OPEN);

    if (method.isConstructor()) { // TODO: simplify
      boolean isPrimary = isConstructorPrimary(method);
      return new Constructor(
        identifier,
        modifiers,
        returnType,
        typeParameters,
        params,
        new Block(removeEmpty(body.getStatements()), false),
        isPrimary
      );
    }
    return new Function(
      identifier,
      modifiers,
      returnType,
      typeParameters,
      params,
      body
    );
  }

  private static boolean isNotOpenMethod(final PsiMethod method) {
    if (method.getParent() instanceof PsiClass) {
      final PsiModifierList parentModifierList = ((PsiClass) method.getParent()).getModifierList();
      if ((parentModifierList != null && parentModifierList.hasExplicitModifier(Modifier.FINAL)) || ((PsiClass) method.getParent()).isEnum())
        return true;
    }
    return false;
  }

  private static boolean isOverrideAnyMethodExceptMethodsFromObject(@NotNull PsiMethod method) {
    int counter = 0;
    for (HierarchicalMethodSignature s : method.getHierarchicalMethodSignature().getSuperSignatures()) {
      PsiClass containingClass = s.getMethod().getContainingClass();
      String qualifiedName = containingClass != null ? containingClass.getQualifiedName() : "";
      if (qualifiedName != null && !qualifiedName.equals("java.lang.Object"))
        counter++;
    }
    return counter > 0;
  }

  private static boolean isOverrideObjectDirect(@NotNull final PsiMethod method) {
    List<HierarchicalMethodSignature> superSignatures = method.getHierarchicalMethodSignature().getSuperSignatures();
    if (superSignatures.size() == 1) {
      final PsiClass containingClass = superSignatures.get(0).getMethod().getContainingClass();
      final String qualifiedName = containingClass != null ? containingClass.getQualifiedName() : "";
      if (qualifiedName != null && qualifiedName.equals("java.lang.Object"))
        return true;
    }
    return false;
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
  public static List<Statement> statementsToStatementList(@NotNull PsiStatement[] statements) {
    List<Statement> result = new LinkedList<Statement>();
    for (PsiStatement t : statements) result.add(statementToStatement(t));
    return result;
  }

  @NotNull
  public static List<Statement> statementsToStatementList(@NotNull List<PsiStatement> statements) {
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
  public static List<Expression> expressionsToExpressionList(@NotNull PsiExpression[] expressions) {
    List<Expression> result = new LinkedList<Expression>();
    for (PsiExpression e : expressions) result.add(expressionToExpression(e));
    return result;
  }

  @NotNull
  public static Expression expressionToExpression(@Nullable PsiExpression e) {
    if (e == null) return Expression.EMPTY_EXPRESSION;
    final ExpressionVisitor expressionVisitor = ourDispatcher.getExpressionVisitor();
    e.accept(expressionVisitor);
    return expressionVisitor.getResult();
  }

  @NotNull
  public static Element elementToElement(@Nullable PsiElement e) {
    if (e == null) return Element.EMPTY_ELEMENT;
    final ElementVisitor elementVisitor = new ElementVisitor();
    e.accept(elementVisitor);
    return elementVisitor.getResult();
  }

  @NotNull
  public static List<Element> elementsToElementList(@NotNull PsiElement[] elements) {
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
  public static List<Type> typesToTypeList(@NotNull PsiType[] types) {
    List<Type> result = new LinkedList<Type>();
    for (PsiType t : types) result.add(typeToType(t));
    return result;
  }

  public static Type typeToType(PsiType type, boolean notNull) {
    Type result = typeToType(type);
    if (notNull)
      result.convertedToNotNull();
    return result;
  }

  @NotNull
  private static List<Type> typesToNotNullableTypeList(@NotNull PsiType[] types) {
    List<Type> result = new LinkedList<Type>(typesToTypeList(types));
    for (Type p : result) p.convertedToNotNull();
    return result;
  }

  @NotNull
  private static List<Import> importsToImportList(@NotNull PsiImportStatementBase[] imports) {
    List<Import> result = new LinkedList<Import>();
    for (PsiImportStatementBase i : imports) {
      Import anImport = importToImport(i);
      String name = anImport.getName();
      if (!name.isEmpty() && !NOT_NULL_ANNOTATIONS.contains(name))
        result.add(anImport);
    }
    return result;
  }

  @NotNull
  private static Import importToImport(@NotNull PsiImportStatementBase i) {
    final PsiJavaCodeReferenceElement reference = i.getImportReference();
    if (reference != null)
      return new Import(quoteKeywords(reference.getQualifiedName()) + (i.isOnDemand() ? ".*" : ""));
    return new Import("");
  }

  @NotNull
  public static List<Parameter> parametersToParameterList(@NotNull PsiParameter[] parameters) {
    List<Parameter> result = new LinkedList<Parameter>();
    for (PsiParameter t : parameters) result.add(parameterToParameter(t));
    return result;
  }

  @NotNull
  public static Parameter parameterToParameter(@NotNull PsiParameter parameter) {
    return new Parameter(
      new IdentifierImpl(parameter.getName()),
      typeToType(parameter.getType(), isNotNull(parameter.getModifierList())),
      isReadOnly(parameter)
    );
  }

  public static boolean isNotNull(@Nullable PsiModifierList modifierList) {
    if (modifierList != null) {
      PsiAnnotation[] annotations = modifierList.getAnnotations();
      for (PsiAnnotation a : annotations) {
        String qualifiedName = a.getQualifiedName();
        if (qualifiedName != null && NOT_NULL_ANNOTATIONS.contains(qualifiedName))
          return true;
      }
    }
    return false;
  }

  private static boolean isReadOnly(PsiParameter parameter) {
    for (PsiReference r : (ReferencesSearch.search(parameter))) {
      if (r instanceof PsiExpression && PsiUtil.isAccessedForWriting((PsiExpression) r)) {
        return false;
      }
    }
    return true;
  }

  @NotNull
  public static Identifier identifierToIdentifier(@Nullable PsiIdentifier identifier) {
    if (identifier == null) return Identifier.EMPTY_IDENTIFIER;
    return new IdentifierImpl(identifier.getText());
  }

  @NotNull
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