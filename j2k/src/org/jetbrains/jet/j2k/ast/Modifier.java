package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public abstract class Modifier {
  @NotNull
  public static final String PUBLIC = "public";
  @NotNull
  public static final String PROTECTED = "protected";
  @NotNull
  public static final String PRIVATE = "private";
  @NotNull
  public static final String INTERNAL = "internal";
  @NotNull
  public static final String STATIC = "static";
  @NotNull
  public static final String ABSTRACT = "abstract";
  @NotNull
  public static final String FINAL = "final";
  @NotNull
  public static final String OPEN = "open";
  @NotNull
  public static final String NOT_OPEN = "not open";
  @NotNull
  public static final String OVERRIDE = "override";
}
