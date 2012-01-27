package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public interface INode {
  @NotNull
  String toKotlin();

  @NotNull
  Kind getKind();

  enum Kind {
    UNDEFINED, TYPE, CONSTRUCTOR, BREAK, CONTINUE, VARARG, TRAIT, ASSIGNMENT_EXPRESSION, CALL_CHAIN, LITERAL, ARRAY_TYPE,
  }
}
