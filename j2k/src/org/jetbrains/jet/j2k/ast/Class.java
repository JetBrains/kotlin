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

package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.j2k.Converter;
import org.jetbrains.jet.j2k.J2KConverterFlags;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.j2k.util.AstUtil.*;

public class Class extends Member {
    @NotNull
    String TYPE = "class";
    final Identifier myName;
    private final List<Expression> myBaseClassParams;
    private final List<Member> myMembers;
    private final List<Element> myTypeParameters;
    private final List<Type> myExtendsTypes;
    private final List<Type> myImplementsTypes;

    public Class(Converter converter, Identifier name, Set<String> modifiers, List<Element> typeParameters, List<Type> extendsTypes,
                 List<Expression> baseClassParams, List<Type> implementsTypes, List<Member> members) {
        myName = name;
        myBaseClassParams = baseClassParams;
        myModifiers = modifiers;
        myTypeParameters = typeParameters;
        myExtendsTypes = extendsTypes;
        myImplementsTypes = implementsTypes;
        myMembers = getMembers(members, converter);
    }

    /*package*/ static List<Member> getMembers(List<Member> members, Converter converter) {
        List<Member> withoutPrivate = new LinkedList<Member>();
        if (converter.hasFlag(J2KConverterFlags.SKIP_NON_PUBLIC_MEMBERS)) {
            for (Member m : members) {
                if (m.accessModifier().equals("public") || m.accessModifier().equals("protected")) {
                    withoutPrivate.add(m);
                }
            }
        }
        else {
            withoutPrivate = members;
        }
        return withoutPrivate;
    }


    @Nullable
    private Constructor getPrimaryConstructor() {
        for (Member m : myMembers)
            if (m.getKind() == Kind.CONSTRUCTOR) {
                if (((Constructor) m).isPrimary()) {
                    return (Constructor) m;
                }
            }
        return null;
    }

    String primaryConstructorSignatureToKotlin() {
        Constructor maybeConstructor = getPrimaryConstructor();
        if (maybeConstructor != null) {
            return maybeConstructor.primarySignatureToKotlin();
        }
        return "(" + ")";
    }

    String primaryConstructorBodyToKotlin() {
        Constructor maybeConstructor = getPrimaryConstructor();
        if (maybeConstructor != null && !maybeConstructor.getBlock().isEmpty()) {
            return maybeConstructor.primaryBodyToKotlin();
        }
        return EMPTY;
    }

    private boolean hasWhere() {
        for (Element t : myTypeParameters)
            if (t instanceof TypeParameter && ((TypeParameter) t).hasWhere()) {
                return true;
            }
        return false;
    }

    @NotNull
    String typeParameterWhereToKotlin() {
        if (hasWhere()) {
            List<String> wheres = new LinkedList<String>();
            for (Element t : myTypeParameters)
                if (t instanceof TypeParameter) {
                    wheres.add(((TypeParameter) t).getWhereToKotlin());
                }
            return SPACE + "where" + SPACE + join(wheres, COMMA_WITH_SPACE) + SPACE;
        }
        return EMPTY;
    }

    @NotNull
    LinkedList<Member> membersExceptConstructors() {
        LinkedList<Member> result = new LinkedList<Member>();
        for (Member m : myMembers)
            if (m.getKind() != Kind.CONSTRUCTOR) {
                result.add(m);
            }
        return result;
    }

    @NotNull
    List<Function> secondaryConstructorsAsStaticInitFunction() {
        LinkedList<Function> result = new LinkedList<Function>();
        for (Member m : myMembers)
            if (m.getKind() == Kind.CONSTRUCTOR && !((Constructor) m).isPrimary()) {
                Function f = (Function) m;
                Set<String> modifiers = new HashSet<String>(m.myModifiers);
                modifiers.add(Modifier.STATIC);

                List<Statement> statements = f.getBlock().getStatements();
                statements.add(new ReturnStatement(new IdentifierImpl("__"))); // TODO: move to one place, find other __ usages
                Block block = new Block(statements);

                List<Element> typeParameters = new LinkedList<Element>();
                if (f.getTypeParameters().size() == 0) {
                    typeParameters.addAll(myTypeParameters);
                }
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
        }
        else {
            return nodesToKotlin(myExtendsTypes);
        }
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

        modifierList.add(accessModifier());

        if (needAbstractModifier()) {
            modifierList.add(Modifier.ABSTRACT);
        }

        if (needOpenModifier()) {
            modifierList.add(Modifier.OPEN);
        }

        if (modifierList.size() > 0) {
            return join(modifierList, SPACE) + SPACE;
        }

        return EMPTY;
    }

    boolean needOpenModifier() {
        return !myModifiers.contains(Modifier.FINAL) && !myModifiers.contains(Modifier.ABSTRACT);
    }

    boolean needAbstractModifier() {
        return isAbstract();
    }

    @NotNull
    String bodyToKotlin() {
        return SPACE + "{" + N +
               AstUtil.joinNodes(getNonStatic(membersExceptConstructors()), N) + N +
               primaryConstructorBodyToKotlin() + N +
               classObjectToKotlin() + N +
               "}";
    }

    @NotNull
    private static List<Member> getStatic(@NotNull List<? extends Member> members) {
        List<Member> result = new LinkedList<Member>();
        for (Member m : members)
            if (m.isStatic()) {
                result.add(m);
            }
        return result;
    }

    @NotNull
    private static List<Member> getNonStatic(@NotNull List<? extends Member> members) {
        List<Member> result = new LinkedList<Member>();
        for (Member m : members)
            if (!m.isStatic()) {
                result.add(m);
            }
        return result;
    }

    @NotNull
    private String classObjectToKotlin() {
        List<Member> staticMembers = new LinkedList<Member>(secondaryConstructorsAsStaticInitFunction());
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
