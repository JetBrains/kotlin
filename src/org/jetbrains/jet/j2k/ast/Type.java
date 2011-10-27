package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public abstract class Type extends Element {
  @NotNull
  @Override
  public Kind getKind() {
    return Kind.TYPE;
  }
}
