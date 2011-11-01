package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author ignatov
 */
public class Constructor extends Function {
  public Constructor(Identifier identifier, Type type, List<Element> typeParameters, Element params, Block block) {
    super(identifier, type, typeParameters, params, block);
  }

  public String primary() {
    return "(" + myParams.toKotlin() + ")";
  }

  @NotNull
  @Override
  public Kind getKind() {
    return Kind.CONSTRUCTOR;
  }
}