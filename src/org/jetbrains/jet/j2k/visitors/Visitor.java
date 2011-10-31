package org.jetbrains.jet.j2k.visitors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.ast.Element;

/**
 * @author ignatov
 */
public interface Visitor {
  @NotNull
  Element getResult();
}
