package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public interface Visitor {
  @NotNull
  Element getResult();
}
