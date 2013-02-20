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

package org.jetbrains.jet.j2k;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.j2k.ast.*;
import org.jetbrains.jet.j2k.ast.Class;
import org.jetbrains.jet.j2k.ast.Enum;
import org.jetbrains.jet.j2k.util.AstUtil;
import org.jetbrains.jet.j2k.visitors.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.*;

import static org.jetbrains.jet.j2k.ConverterUtil.countWritingAccesses;
import static org.jetbrains.jet.j2k.ConverterUtil.createMainFunction;
import static org.jetbrains.jet.j2k.visitors.TypeVisitor.*;
import static org.jetbrains.jet.lang.types.expressions.OperatorConventions.*;

public class Converter {
    @NotNull
    public static final Set<String> NOT_NULL_ANNOTATIONS = ImmutableSet.of(
        "org.jetbrains.annotations.NotNull",
        "com.sun.istack.internal.NotNull",
        "javax.annotation.Nonnull"
    );

    @NotNull
    private Set<String> classIdentifiers = Sets.newHashSet();

    @NotNull
    private final Dispatcher dispatcher = new Dispatcher(this);

    @Nullable
    private PsiType methodReturnType = null;

    @NotNull
    private final Set<J2KConverterFlags> flags = Sets.newHashSet();

    public Converter(@NotNull Project project) {
        KotlinBuiltIns.initialize(project);
    }

    public boolean addFlag(@NotNull J2KConverterFlags flag) {
        return flags.add(flag);
    }

    public boolean hasFlag(@NotNull J2KConverterFlags flag) {
        return flags.contains(flag);
    }

    public void setClassIdentifiers(@NotNull Set<String> identifiers) {
        classIdentifiers = identifiers;
    }

    @NotNull
    public Set<String> getClassIdentifiers() {
        return Collections.unmodifiableSet(classIdentifiers);
    }

    @Nullable
    public PsiType getMethodReturnType() {
        return methodReturnType;
    }

    public void clearClassIdentifiers() {
        classIdentifiers.clear();
    }

    @NotNull
    public String elementToKotlin(@NotNull PsiElement element) {
        if (element instanceof PsiJavaFile) {
            return fileToFile((PsiJavaFile) element).toKotlin();
        }

        if (element instanceof PsiClass) {
            return classToClass((PsiClass) element).toKotlin();
        }

        if (element instanceof PsiMethod) {
            return methodToFunction((PsiMethod) element).toKotlin();
        }

        if (element instanceof PsiField) {
            PsiField field = (PsiField) element;
            return fieldToField(field, field.getContainingClass()).toKotlin();
        }

        if (element instanceof PsiStatement) {
            return statementToStatement((PsiStatement) element).toKotlin();
        }

        if (element instanceof PsiExpression) {
            return expressionToExpression((PsiExpression) element).toKotlin();
        }

        return "";
    }

    @NotNull
    public File fileToFile(@NotNull PsiJavaFile javaFile) {
        return fileToFile(javaFile, Collections.<String>emptyList());
    }

    @NotNull
    public File fileToFileWithCompatibilityImport(@NotNull PsiJavaFile javaFile) {
        return fileToFile(javaFile, Collections.singletonList("kotlin.compatibility.*"));
    }

    @NotNull
    private File fileToFile(PsiJavaFile javaFile, List<String> additionalImports) {
        final PsiImportList importList = javaFile.getImportList();
        List<Import> imports = importList == null
                               ? Collections.<Import>emptyList()
                               : importsToImportList(importList.getAllImportStatements());
        for (String i : additionalImports)
            imports.add(new Import(i));
        return new File(quoteKeywords(javaFile.getPackageName()), imports, classesToClassList(javaFile.getClasses()), createMainFunction(javaFile));
    }

    @NotNull
    private static String quoteKeywords(@NotNull String packageName) {
        List<String> result = new LinkedList<String>();
        for (String part : packageName.split("\\."))
            result.add(new IdentifierImpl(part).toKotlin());
        return AstUtil.join(result, ".");
    }

    @NotNull
    private List<Class> classesToClassList(@NotNull PsiClass[] classes) {
        List<Class> result = new LinkedList<Class>();
        for (PsiClass t : classes) result.add(classToClass(t));
        return result;
    }

    @NotNull
    public AnonymousClass anonymousClassToAnonymousClass(@NotNull PsiAnonymousClass anonymousClass) {
        return new AnonymousClass(this, getMembers(anonymousClass));
    }

    @NotNull
    private List<Member> getMembers(@NotNull PsiClass psiClass) {
        List<Member> members = new LinkedList<Member>();
        for (PsiElement e : psiClass.getChildren()) {
            if (e instanceof PsiMethod) {
                members.add(methodToFunction((PsiMethod) e, true));
            }
            else if (e instanceof PsiField) {
                members.add(fieldToField((PsiField) e, psiClass));
            }
            else if (e instanceof PsiClass) {
                members.add(classToClass((PsiClass) e));
            }
            else if (e instanceof PsiClassInitializer) {
                members.add(initializerToInitializer((PsiClassInitializer) e));
            }
            else if (e instanceof PsiMember) {
                // System.out.println(e.getClass() + " " + e.getText());
            }
        }
        return members;
    }

    @NotNull
    private static List<Field> getFinalOrWithEmptyInitializer(@NotNull List<? extends Field> fields) {
        List<Field> result = new LinkedList<Field>();
        for (Field f : fields)
            if (f.isVal() || f.getInitializer().toKotlin().isEmpty()) {
                result.add(f);
            }
        return result;
    }

    @NotNull
    private static List<Parameter> createParametersFromFields(@NotNull List<? extends Field> fields) {
        List<Parameter> result = new LinkedList<Parameter>();
        for (Field f : fields)
            result.add(new Parameter(new IdentifierImpl("_" + f.getIdentifier().getName()), f.getType()));
        return result;
    }

    @NotNull
    private static List<Statement> createInitStatementsFromFields(@NotNull List<? extends Field> fields) {
        List<Statement> result = new LinkedList<Statement>();
        for (Field f : fields) {
            final String identifierToKotlin = f.getIdentifier().toKotlin();
            result.add(new DummyStringExpression(identifierToKotlin + " = " + "_" + identifierToKotlin));
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
    public Class classToClass(@NotNull PsiClass psiClass) {
        final Set<String> modifiers = modifiersListToModifiersSet(psiClass.getModifierList());
        final List<Field> fields = fieldsToFieldList(psiClass.getFields(), psiClass);
        final List<Element> typeParameters = elementsToElementList(psiClass.getTypeParameters());
        final List<Type> implementsTypes = typesToNotNullableTypeList(psiClass.getImplementsListTypes());
        final List<Type> extendsTypes = typesToNotNullableTypeList(psiClass.getExtendsListTypes());
        final IdentifierImpl name = new IdentifierImpl(psiClass.getName());
        final List<Expression> baseClassParams = new LinkedList<Expression>();

        List<Member> members = getMembers(psiClass);

        // we try to find super() call and generate class declaration like that: class A(name: String, i : Int) : Base(name)
        final SuperVisitor visitor = new SuperVisitor();
        psiClass.accept(visitor);
        final Collection<PsiExpressionList> resolvedSuperCallParameters = visitor.getResolvedSuperCallParameters();
        if (resolvedSuperCallParameters.size() == 1) {
            baseClassParams.addAll(
                    expressionsToExpressionList(
                            resolvedSuperCallParameters.toArray(new PsiExpressionList[1])[0].getExpressions()
                    )
            );
        }

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

        if (psiClass.isInterface()) {
            return new Trait(this, name, modifiers, typeParameters, extendsTypes, Collections.<Expression>emptyList(), implementsTypes, members);
        }
        if (psiClass.isEnum()) {
            return new Enum(this, name, modifiers, typeParameters, Collections.<Type>emptyList(), Collections.<Expression>emptyList(), implementsTypes, members);
        }
        return new Class(this, name, modifiers, typeParameters, extendsTypes, baseClassParams, implementsTypes, members);
    }

    @NotNull
    private Initializer initializerToInitializer(@NotNull PsiClassInitializer i) {
        return new Initializer(
                blockToBlock(i.getBody(), true),
                modifiersListToModifiersSet(i.getModifierList())
        );
    }

    @NotNull
    public static String getDefaultInitializer(@NotNull Field f) {
        if (f.getType().isNullable()) {
            return "null";
        }
        else {
            final String typeToKotlin = f.getType().toKotlin();
            if (typeToKotlin.equals("Boolean")) return "false";
            if (typeToKotlin.equals("Char")) return "' '";
            if (typeToKotlin.equals("Double")) return "0." + OperatorConventions.DOUBLE + "()";
            if (typeToKotlin.equals("Float")) return "0." + OperatorConventions.FLOAT + "()";
            return "0";
        }
    }

    @NotNull
    private List<Field> fieldsToFieldList(@NotNull PsiField[] fields, PsiClass psiClass) {
        List<Field> result = new LinkedList<Field>();
        for (PsiField f : fields) result.add(fieldToField(f, psiClass));
        return result;
    }

    @NotNull
    private Field fieldToField(@NotNull PsiField field, PsiClass psiClass) {
        Set<String> modifiers = modifiersListToModifiersSet(field.getModifierList());
        if (field instanceof PsiEnumConstant) // TODO: remove instanceof
        {
            return new EnumConstant(
                    new IdentifierImpl(field.getName()), // TODO
                    modifiers,
                    typeToType(field.getType()),
                    elementToElement(((PsiEnumConstant) field).getArgumentList())
            );
        }
        return new Field(
                new IdentifierImpl(field.getName()), // TODO
                modifiers,
                typeToType(field.getType(), ConverterUtil.isAnnotatedAsNotNull(field.getModifierList())),
                createSureCallOnlyForChain(field.getInitializer(), field.getType()), // TODO: add modifiers
                countWritingAccesses(field, psiClass)
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
            if (parent.getConstructors().length == 1) {
                return true;
            }
            else {
                PsiMethod c = getPrimaryConstructorForThisCase(parent); // TODO: move up to classToClass() method
                if (c != null && c.hashCode() == constructor.hashCode()) {
                    return true;
                }
            }
        }
        return false;
    }

    @NotNull
    private static List<Statement> removeEmpty(@NotNull List<Statement> statements) {
        List<Statement> result = new LinkedList<Statement>();
        for (Statement s : statements)
            if (s != Statement.EMPTY_STATEMENT && s != Expression.EMPTY_EXPRESSION) {
                result.add(s);
            }
        return result;
    }

    @NotNull
    private Function methodToFunction(@NotNull PsiMethod method) {
        return methodToFunction(method, true);
    }

    @NotNull
    private Function methodToFunction(@NotNull PsiMethod method, boolean notEmpty) {
        if (isOverrideObjectDirect(method)) {
            dispatcher.setExpressionVisitor(new ExpressionVisitorForDirectObjectInheritors(this));
        }
        else {
            dispatcher.setExpressionVisitor(new ExpressionVisitor(this));
        }

        methodReturnType = method.getReturnType();

        final IdentifierImpl identifier = new IdentifierImpl(method.getName());
        final Type returnType = typeToType(method.getReturnType(), ConverterUtil.isAnnotatedAsNotNull(method.getModifierList()));
        final Block body = hasFlag(J2KConverterFlags.SKIP_BODIES)
                           ? Block.EMPTY_BLOCK
                           : blockToBlock(method.getBody(), notEmpty); // #TODO
        final Element params = createFunctionParameters(method);
        final List<Element> typeParameters = elementsToElementList(method.getTypeParameters());

        final Set<String> modifiers = modifiersListToModifiersSet(method.getModifierList());
        if (isOverrideAnyMethodExceptMethodsFromObject(method)) {
            modifiers.add(Modifier.OVERRIDE);
        }
        if (method.getParent() instanceof PsiClass && ((PsiClass) method.getParent()).isInterface()) {
            modifiers.remove(Modifier.ABSTRACT);
        }
        if (isNotOpenMethod(method)) {
            modifiers.add(Modifier.NOT_OPEN);
        }

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

    @NotNull
    private ParameterList createFunctionParameters(@NotNull PsiMethod method) {
        List<Parameter> result = new LinkedList<Parameter>();
        for (PsiParameter parameter : method.getParameterList().getParameters()) {
            result.add(new Parameter(
                    new IdentifierImpl(parameter.getName()),
                    typeToType(parameter.getType(), ConverterUtil.isAnnotatedAsNotNull(parameter.getModifierList())),
                    ConverterUtil.isReadOnly(parameter, method.getBody())
            ));
        }
        return new ParameterList(result);
    }

    private static boolean isNotOpenMethod(@NotNull final PsiMethod method) {
        if (method.getParent() instanceof PsiClass) {
            final PsiModifierList parentModifierList = ((PsiClass) method.getParent()).getModifierList();
            if ((parentModifierList != null && parentModifierList.hasExplicitModifier(Modifier.FINAL)) || ((PsiClass) method.getParent()).isEnum()) {
                return true;
            }
        }
        return false;
    }

    private boolean isOverrideAnyMethodExceptMethodsFromObject(@NotNull PsiMethod method) {
        boolean counter = normalCase(method);
        if (counter) {
            return true;
        }
        if (isInheritFromObject(method)) {
            return caseForObject(method);
        }
        return false;
    }

    private boolean caseForObject(@NotNull PsiMethod method) {
        PsiClass containing = method.getContainingClass();
        if (containing != null) {
            for (PsiClassType s : containing.getSuperTypes()) {
                String canonicalText = s.getCanonicalText();
                if (!canonicalText.equals(JAVA_LANG_OBJECT) && !getClassIdentifiers().contains(canonicalText)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean normalCase(@NotNull PsiMethod method) {
        int counter = 0;
        for (HierarchicalMethodSignature s : method.getHierarchicalMethodSignature().getSuperSignatures()) {
            PsiClass containingClass = s.getMethod().getContainingClass();
            String qualifiedName = containingClass != null ? containingClass.getQualifiedName() : "";
            if (qualifiedName != null && !qualifiedName.equals(JAVA_LANG_OBJECT)) {
                counter++;
            }
        }
        return counter > 0;
    }

    private static boolean isInheritFromObject(@NotNull PsiMethod method) {
        List<HierarchicalMethodSignature> superSignatures = method.getHierarchicalMethodSignature().getSuperSignatures();
        for (HierarchicalMethodSignature s : superSignatures) {
            PsiClass containingClass = s.getMethod().getContainingClass();
            String qualifiedName = containingClass != null ? containingClass.getQualifiedName() : "";
            if (qualifiedName != null && qualifiedName.equals(JAVA_LANG_OBJECT)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOverrideObjectDirect(@NotNull final PsiMethod method) {
        List<HierarchicalMethodSignature> superSignatures = method.getHierarchicalMethodSignature().getSuperSignatures();
        if (superSignatures.size() == 1) {
            final PsiClass containingClass = superSignatures.get(0).getMethod().getContainingClass();
            final String qualifiedName = containingClass != null ? containingClass.getQualifiedName() : "";
            if (qualifiedName != null && qualifiedName.equals(JAVA_LANG_OBJECT)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public Block blockToBlock(@Nullable PsiCodeBlock block, boolean notEmpty) {
        if (block == null) return Block.EMPTY_BLOCK;
        return new Block(statementsToStatementList(block.getStatements()), notEmpty);
    }

    @NotNull
    public Block blockToBlock(@Nullable PsiCodeBlock block) {
        return blockToBlock(block, true);
    }

    @NotNull
    public List<Statement> statementsToStatementList(@NotNull PsiStatement[] statements) {
        List<Statement> result = new LinkedList<Statement>();
        for (PsiStatement t : statements) result.add(statementToStatement(t));
        return result;
    }

    @NotNull
    public List<Statement> statementsToStatementList(@NotNull List<PsiStatement> statements) {
        List<Statement> result = new LinkedList<Statement>();
        for (PsiStatement t : statements) result.add(statementToStatement(t));
        return result;
    }

    @NotNull
    public Statement statementToStatement(@Nullable PsiStatement s) {
        if (s == null) return Statement.EMPTY_STATEMENT;
        final StatementVisitor statementVisitor = new StatementVisitor(this);
        s.accept(statementVisitor);
        return statementVisitor.getResult();
    }

    @NotNull
    public List<Expression> expressionsToExpressionList(@NotNull PsiExpression[] expressions) {
        List<Expression> result = new LinkedList<Expression>();
        for (PsiExpression e : expressions) result.add(expressionToExpression(e));
        return result;
    }

    @NotNull
    public Expression expressionToExpression(@Nullable PsiExpression e) {
        if (e == null) return Expression.EMPTY_EXPRESSION;
        final ExpressionVisitor expressionVisitor = dispatcher.getExpressionVisitor();
        e.accept(expressionVisitor);
        return expressionVisitor.getResult();
    }

    @NotNull
    public Element elementToElement(@Nullable PsiElement e) {
        if (e == null) return Element.EMPTY_ELEMENT;
        final ElementVisitor elementVisitor = new ElementVisitor(this);
        e.accept(elementVisitor);
        return elementVisitor.getResult();
    }

    @NotNull
    public List<Element> elementsToElementList(@NotNull PsiElement[] elements) {
        List<Element> result = new LinkedList<Element>();
        for (PsiElement e : elements) result.add(elementToElement(e));
        return result;
    }

    @NotNull
    public Type typeToType(@Nullable PsiType type) {
        if (type == null) return Type.EMPTY_TYPE;
        TypeVisitor typeVisitor = new TypeVisitor(this);
        type.accept(typeVisitor);
        return typeVisitor.getResult();
    }

    @NotNull
    public List<Type> typesToTypeList(@NotNull PsiType[] types) {
        List<Type> result = new LinkedList<Type>();
        for (PsiType t : types) result.add(typeToType(t));
        return result;
    }

    @NotNull
    public Type typeToType(PsiType type, boolean notNull) {
        Type result = typeToType(type);
        if (notNull) {
            result.convertedToNotNull();
        }
        return result;
    }

    @NotNull
    private List<Type> typesToNotNullableTypeList(@NotNull PsiType[] types) {
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
            if (!name.isEmpty() && !NOT_NULL_ANNOTATIONS.contains(name)) {
                result.add(anImport);
            }
        }
        return result;
    }

    @NotNull
    private static Import importToImport(@NotNull PsiImportStatementBase i) {
        final PsiJavaCodeReferenceElement reference = i.getImportReference();
        if (reference != null) {
            return new Import(quoteKeywords(reference.getQualifiedName()) + (i.isOnDemand() ? ".*" : ""));
        }
        return new Import("");
    }

    @NotNull
    public List<Parameter> parametersToParameterList(@NotNull PsiParameter[] parameters) {
        List<Parameter> result = new LinkedList<Parameter>();
        for (PsiParameter t : parameters) result.add(parameterToParameter(t));
        return result;
    }

    @NotNull
    public Parameter parameterToParameter(@NotNull PsiParameter parameter) {
        return new Parameter(
                new IdentifierImpl(parameter.getName()),
                typeToType(parameter.getType(), ConverterUtil.isAnnotatedAsNotNull(parameter.getModifierList()))
        );
    }

    @NotNull
    public static Identifier identifierToIdentifier(@Nullable PsiIdentifier identifier) {
        if (identifier == null) return Identifier.EMPTY_IDENTIFIER;
        return new IdentifierImpl(identifier.getText());
    }

    @NotNull
    public static Set<String> modifiersListToModifiersSet(@Nullable PsiModifierList modifierList) {
        Set<String> modifiersSet = new HashSet<String>();
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

    @NotNull
    public List<String> createConversions(@NotNull PsiCallExpression expression) {
        PsiExpressionList argumentList = expression.getArgumentList();
        PsiExpression[] arguments = argumentList != null ? argumentList.getExpressions() : new PsiExpression[]{};
        List<String> conversions = new LinkedList<String>();
        //noinspection UnusedDeclaration
        for (final PsiExpression a : arguments) {
            conversions.add("");
        }

        PsiMethod resolve = expression.resolveMethod();
        if (resolve != null) {
            List<PsiType> expectedTypes = new LinkedList<PsiType>();
            List<PsiType> actualTypes = new LinkedList<PsiType>();

            for (PsiParameter p : resolve.getParameterList().getParameters())
                expectedTypes.add(p.getType());

            for (PsiExpression e : arguments)
                actualTypes.add(e.getType());

            if (conversions.size() == actualTypes.size() && actualTypes.size() == expectedTypes.size()) {
                for (int i = 0; i < actualTypes.size(); i++)
                    conversions.set(i, createConversionForExpression(arguments[i], expectedTypes.get(i)));
            }
        }
        return conversions;
    }

    @NotNull
    public List<String> createConversions(@NotNull PsiPolyadicExpression expression, PsiType expectedType) {
        PsiExpression[] arguments = expression.getOperands();
        int length = arguments.length;
        List<String> conversions = new LinkedList<String>();

        List<PsiType> expectedTypes = Collections.nCopies(length, expectedType);
        List<PsiType> actualTypes = new LinkedList<PsiType>();

        for (PsiExpression e : arguments)
            actualTypes.add(e.getType());

        assert actualTypes.size() == expectedTypes.size() : "The type list must have the same length";

        for (int i = 0; i < actualTypes.size(); i++)
            conversions.add(i, createConversionForExpression(arguments[i], expectedTypes.get(i)));

        return conversions;
    }

    @NotNull
    private String createConversionForExpression(@Nullable PsiExpression expression, @NotNull PsiType expectedType) {
        String conversion = "";
        if (expression != null) {
            PsiType actualType = expression.getType();
            boolean isPrimitiveTypeOrNull = actualType == null || Node.PRIMITIVE_TYPES.contains(actualType.getCanonicalText());
            boolean isRef = (expression instanceof PsiReferenceExpression && ((PsiReferenceExpression) expression).isQualified() || expression instanceof PsiMethodCallExpression);
            boolean containsQuestDot = expressionToExpression(expression).toKotlin().contains("?.");

            if (isPrimitiveTypeOrNull && isRef && containsQuestDot) {
                conversion += "!!";
            }

            if (actualType != null) {
                if (isConversionNeeded(actualType, expectedType)) {
                    conversion += getPrimitiveTypeConversion(expectedType.getCanonicalText());
                }
            }
        }
        return conversion;
    }

    private static boolean isConversionNeeded(@Nullable final PsiType actual, @Nullable final PsiType expected) {
        if (actual == null || expected == null) {
            return false;
        }
        Map<String, String> typeMap = new HashMap<String, String>();
        typeMap.put(JAVA_LANG_BYTE, "byte");
        typeMap.put(JAVA_LANG_SHORT, "short");
        typeMap.put(JAVA_LANG_INTEGER, "int");
        typeMap.put(JAVA_LANG_LONG, "long");
        typeMap.put(JAVA_LANG_FLOAT, "float");
        typeMap.put(JAVA_LANG_DOUBLE, "double");
        typeMap.put(JAVA_LANG_CHARACTER, "char");
        String expectedStr = expected.getCanonicalText();
        String actualStr = actual.getCanonicalText();
        boolean o1 = AstUtil.getOrElse(typeMap, actualStr, "").equals(expectedStr);
        boolean o2 = AstUtil.getOrElse(typeMap, expectedStr, "").equals(actualStr);
        return !actualStr.equals(expectedStr) && (!(o1 ^ o2));
    }

    @NotNull
    private static String getPrimitiveTypeConversion(@NotNull String type) {
        Map<String, Name> conversions = new HashMap<String, Name>();
        conversions.put("byte", BYTE);
        conversions.put("short", SHORT);
        conversions.put("int", INT);
        conversions.put("long", LONG);
        conversions.put("float", FLOAT);
        conversions.put("double", DOUBLE);
        conversions.put("char", CHAR);

        conversions.put(JAVA_LANG_BYTE, BYTE);
        conversions.put(JAVA_LANG_SHORT, SHORT);
        conversions.put(JAVA_LANG_INTEGER, INT);
        conversions.put(JAVA_LANG_LONG, LONG);
        conversions.put(JAVA_LANG_FLOAT, FLOAT);
        conversions.put(JAVA_LANG_DOUBLE, DOUBLE);
        conversions.put(JAVA_LANG_CHARACTER, CHAR);

        if (conversions.containsKey(type)) {
            return "." + conversions.get(type) + "()";
        }
        return "";
    }

//  @NotNull
//  private static String applyConversion(Expression expression, String conversion) {
//    if (conversion.isEmpty())
//      return expression.toKotlin();
//    return "(" + expression.toKotlin() + ")" + conversion;
//  }

    @NotNull
    public SureCallChainExpression createSureCallOnlyForChain(@Nullable PsiExpression expression, @NotNull PsiType type) {
        String conversion = (expression != null && (expression instanceof PsiReferenceExpression || expression instanceof PsiMethodCallExpression))
                            ?
                            createConversionForExpression(expression, type)
                            : "";
        return new SureCallChainExpression(expressionToExpression(expression), conversion);
    }

}
