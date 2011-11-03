package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * @author ignatov
 */
public class Constructor extends Function {
  public Constructor(Identifier identifier, Set<String> modifiers, Type type, List<Element> typeParameters, Element params, Block block) {
    super(identifier, modifiers, type, typeParameters, params, block);
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