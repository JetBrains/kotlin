package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;

/**
 * @author ignatov
 */
public class ReferenceElement extends Element {
  private Identifier myReference;
  private List<Type> myTypes;

  public ReferenceElement(@NotNull Identifier reference, @NotNull List<Type> types) {
    myReference = reference;
    myTypes = types;
  }

  @NotNull
  @Override
  public String toKotlin() {
    String typesIfNeeded = myTypes.size() > 0 ? "<" + AstUtil.joinNodes(myTypes, COMMA_WITH_SPACE) + ">" : EMPTY;
    return myReference.toKotlin() + typesIfNeeded;
  }
}
