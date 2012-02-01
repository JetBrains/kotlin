package org.jetbrains.jet.j2k.ast;

import org.jetbrains.jet.j2k.Converter;

import java.util.List;
import java.util.Set;

/**
 * @author ignatov
 */
public class Trait extends Class {
    public Trait(Converter converter, Identifier name, Set<String> modifiers, List<Element> typeParameters, List<Type> extendsTypes,
                 List<Expression> baseClassParams, List<Type> implementsTypes, List<Member> members) {
        super(converter, name, modifiers, typeParameters, extendsTypes, baseClassParams, implementsTypes, getMembers(members, converter));
        TYPE = "trait";
    }

    @Override
    String primaryConstructorSignatureToKotlin() {
        return EMPTY;
    }

    boolean needOpenModifier() {
        return false;
    }
}