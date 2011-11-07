package org.jetbrains.jet.j2k.ast;

import java.util.Set;

/**
 * @author ignatov
 */
public abstract class Member extends Node implements IMember {
  Set<String> myModifiers;

  String accessModifier() {
    for (String m : myModifiers)
      if (m.equals(Modifier.PUBLIC) || m.equals(Modifier.PROTECTED) || m.equals(Modifier.PRIVATE))
        return m;
    return EMPTY; // package local converted to internal, but we use internal by default
  }

  @Override
  public boolean isAbstract() {
    return myModifiers.contains(Modifier.ABSTRACT);
  }

  @Override
  public boolean isStatic() {
    return myModifiers.contains(Modifier.STATIC);
  }
}
