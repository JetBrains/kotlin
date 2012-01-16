package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author ignatov
 */
public abstract class Node implements INode {
  @NotNull
  @Override
  public Kind getKind() {
    return Kind.UNDEFINED;
  }

  @NotNull
  final static Set<String> ONLY_KOTLIN_KEYWORDS = new HashSet<String>(Arrays.asList(
    "package", "as", "type", "val", "var", "fun", "is", "in", "object", "when", "trait", "This"
  ));

  @NotNull
  public final static Set<String> PRIMITIVE_TYPES = new HashSet<String>(Arrays.asList(
    "double", "float", "long", "int", "short", "byte", "boolean", "char"
  ));

  static final String N = System.getProperty("line.separator");
  @NotNull
  static final String N2 = N + N;
  @NotNull
  static final String SPACE = " ";
  @NotNull
  static final String EQUAL = "=";
  @NotNull
  static final String EMPTY = "";
  @NotNull
  static final String DOT = ".";
  @NotNull
  static final String QUESTDOT = "?.";
  @NotNull
  static final String COLON = ":";
  @NotNull
  static final String IN = "in";
  @NotNull
  static final String AT = "@";
  @NotNull
  static final String BACKTICK = "`";
  @NotNull
  static final String QUEST = "?";
  @NotNull
  static final String COMMA_WITH_SPACE = "," + SPACE;
  @NotNull
  static final String STAR = "*";
  @NotNull
  protected static final String ZERO = "0";
}