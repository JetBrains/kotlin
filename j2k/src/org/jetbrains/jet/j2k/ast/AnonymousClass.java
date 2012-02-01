package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.Converter;

import java.util.Collections;
import java.util.List;

/**
 * @author ignatov
 */
public class AnonymousClass extends Class {
    public AnonymousClass(Converter converter, List<Member> members) {
        super(converter,
              new IdentifierImpl("anonClass"),
              Collections.<String>emptySet(),
              Collections.<Element>emptyList(),
              Collections.<Type>emptyList(),
              Collections.<Expression>emptyList(),
              Collections.<Type>emptyList(),
              getMembers(members, converter)
        );
    }

    @NotNull
    @Override
    public String toKotlin() {
        return bodyToKotlin();
    }
}