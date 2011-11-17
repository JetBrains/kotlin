package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * @author ignatov
 */
public class AnonymousClass extends Class {
  public AnonymousClass(List<Member> members) {
    super(new IdentifierImpl("anonClass"),
      Collections.<String>emptySet(),
      Collections.<Element>emptyList(),
      Collections.<Type>emptyList(),
      Collections.<Expression>emptyList(),
      Collections.<Type>emptyList(),
      members
    );
  }

  @NotNull
  @Override
  public String toKotlin() {
    return bodyToKotlin();
  }
}