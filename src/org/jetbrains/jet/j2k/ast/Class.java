package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.j2k.util.AstUtil.*;

/**
 * @author ignatov
 */
public class Class extends Member {
  @NotNull
  String TYPE = "class";
  final Identifier myName;
  private final List<Expression> myBaseClassParams;
  private final List<? extends Member> myMembers;
  private final List<Element> myTypeParameters;
  private final List<Type> myExtendsTypes;
  private final List<Type> myImplementsTypes;

  public Class(Identifier name, Set<String> modifiers, List<Element> typeParameters, List<Type> extendsTypes,
               List<Expression> baseClassParams, List<Type> implementsTypes, List<? extends Member> members) {
    myName = name;
    myBaseClassParams = baseClassParams;
    myModifiers = modifiers;
    myTypeParameters = typeParameters;
    myExtendsTypes = extendsTypes;
    myImplementsTypes = implementsTypes;
    myMembers = members;
  }


  @Nullable
  private Constructor getPrimaryConstructor() {
    for (Member m : myMembers)
      if (m.getKind() == Kind.CONSTRUCTOR)
        if (((Constructor) m).isPrimary())
          return (Constructor) m;
    return null;
  }

  String primaryConstructorSignatureToKotlin() {
    Constructor maybeConstructor = getPrimaryConstructor();
    if (maybeConstructor != null)
      return maybeConstructor.primarySignatureToKotlin();
    return "(" + ")";
  }

  String primaryConstructorBodyToKotlin() {
    Constructor maybeConstructor = getPrimaryConstructor();
    if (maybeConstructor != null && !maybeConstructor.getBlock().isEmpty())
      return maybeConstructor.primaryBodyToKotlin();
    return EMPTY;
  }

  private boolean hasWhere() {
    for (Element t : myTypeParameters)
      if (t instanceof TypeParameter && ((TypeParameter) t).hasWhere())
        return true;
    return false;
  }

  @NotNull
  String typeParameterWhereToKotlin() {
    if (hasWhere()) {
      List<String> wheres = new LinkedList<String>();
      for (Element t : myTypeParameters)
        if (t instanceof TypeParameter)
          wheres.add(((TypeParameter) t).getWhereToKotlin());
      return SPACE + "where" + SPACE + join(wheres, COMMA_WITH_SPACE) + SPACE;
    }
    return EMPTY;
  }

  @NotNull
  LinkedList<Member> membersExceptConstructors() {
    final LinkedList<Member> result = new LinkedList<Member>();
    for (Member m : myMembers)
      if (m.getKind() != Kind.CONSTRUCTOR)
        result.add(m);
    return result;
  }

  @NotNull
  List<Function> secondaryConstructorsAsStaticInitFunction() {
    final LinkedList<Function> result = new LinkedList<Function>();
    for (Member m : myMembers)
      if (m.getKind() == Kind.CONSTRUCTOR && !((Constructor) m).isPrimary()) {
        Function f = (Function) m;
        Set<String> modifiers = new HashSet<String>(m.myModifiers);
        modifiers.add(Modifier.STATIC);

        final List<Statement> statements = f.getBlock().getStatements();
        statements.add(new ReturnStatement(new IdentifierImpl("__"))); // TODO: move to one place, find other __ usages
        final Block block = new Block(statements);

        final List<Element> typeParameters = new LinkedList<Element>();
        if (f.getTypeParameters().size() == 0)
          typeParameters.addAll(myTypeParameters);
        else {
          typeParameters.addAll(myTypeParameters);
          typeParameters.addAll(f.getTypeParameters());
        }

        result.add(new Function(
          new IdentifierImpl("init"),
          modifiers,
          new ClassType(myName, typeParameters, false),
          typeParameters,
          f.getParams(),
          block
        ));
      }
    return result;
  }

  @NotNull
  String typeParametersToKotlin() {
    return myTypeParameters.size() > 0 ? "<" + AstUtil.joinNodes(myTypeParameters, COMMA_WITH_SPACE) + ">" : EMPTY;
  }

  List<String> baseClassSignatureWithParams() {
    if (TYPE.equals("class") && myExtendsTypes.size() == 1) {
      LinkedList<String> result = new LinkedList<String>();
      result.add(myExtendsTypes.get(0).toKotlin() + "(" + joinNodes(myBaseClassParams, COMMA_WITH_SPACE) + ")");
      return result;
    } else
      return nodesToKotlin(myExtendsTypes);
  }

  @NotNull
  String implementTypesToKotlin() {
    List<String> allTypes = new LinkedList<String>() {
      {
        addAll(baseClassSignatureWithParams());
        addAll(nodesToKotlin(myImplementsTypes));
      }
    };
    return allTypes.size() == 0 ? EMPTY : SPACE + COLON + SPACE + join(allTypes, COMMA_WITH_SPACE);
  }

  @NotNull
  String modifiersToKotlin() {
    List<String> modifierList = new LinkedList<String>();

    if (needAbstractModifier())
      modifierList.add(Modifier.ABSTRACT);

    modifierList.add(accessModifier());

    if (needOpenModifier())
      modifierList.add(Modifier.OPEN);

    if (modifierList.size() > 0)
      return join(modifierList, SPACE) + SPACE;

    return EMPTY;
  }

  boolean needOpenModifier() {
    return !myModifiers.contains(Modifier.FINAL);
  }

  boolean needAbstractModifier() {
    return isAbstract();
  }

  @NotNull
  String bodyToKotlin() {
    return SPACE + "{" + N +
      primaryConstructorBodyToKotlin() + N +
      AstUtil.joinNodes(getNonStatic(membersExceptConstructors()), N) + N +
      classObjectToKotlin() + N +
      "}";
  }

  @NotNull
  private static List<Member> getStatic(@NotNull List<? extends Member> members) {
    List<Member> result = new LinkedList<Member>();
    for (Member m : members)
      if (m.isStatic())
        result.add(m);
    return result;
  }

  @NotNull
  private static List<Member> getNonStatic(@NotNull List<? extends Member> members) {
    List<Member> result = new LinkedList<Member>();
    for (Member m : members)
      if (!m.isStatic())
        result.add(m);
    return result;
  }

  @NotNull
  private String classObjectToKotlin() {
    final List<Member> staticMembers = new LinkedList<Member>(secondaryConstructorsAsStaticInitFunction());
    staticMembers.addAll(getStatic(membersExceptConstructors()));
    if (staticMembers.size() > 0) {
      return "class" + SPACE + "object" + SPACE + "{" + N +
        AstUtil.joinNodes(staticMembers, N) + N +
        "}";
    }
    return EMPTY;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return modifiersToKotlin() + TYPE + SPACE + myName.toKotlin() + typeParametersToKotlin() + primaryConstructorSignatureToKotlin() +
      implementTypesToKotlin() +
      typeParameterWhereToKotlin() +
      bodyToKotlin();
  }
}