package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * @author ignatov
 */
public class Constructor extends Function {
  private final boolean myIsPrimary;

  public Constructor(Identifier identifier, Set<String> modifiers, Type type, List<Element> typeParameters, Element params, Block block, boolean isPrimary) {
    super(identifier, modifiers, type, typeParameters, params, block);
    myIsPrimary = isPrimary;
  }

  public String primarySignatureToKotlin() {
    return "(" + myParams.toKotlin() + ")";
  }

  public String primaryBodyToKotlin() {
    return myBlock.toKotlin();
  }

  public boolean isPrimary() {
    return myIsPrimary;
  }

  @NotNull
  @Override
  public Kind getKind() {
    return Kind.CONSTRUCTOR;
  }
}