package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class StarProjectionType extends Type {
  @NotNull
  @Override
  public String toKotlin() {
    return STAR;
  }
}
