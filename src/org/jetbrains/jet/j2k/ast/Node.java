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

  final static Set<String> ONLY_KOTLIN_KEYWORDS = new HashSet<String>(Arrays.asList(
    "namespace", "as", "type", "val", "var", "fun", "is", "in", "object", "when", "trait", "This"
  ));

  public final static Set<String> PRIMITIVE_TYPES = new HashSet<String>(Arrays.asList(
    "double", "float", "long", "int", "short", "byte", "boolean", "char"
  ));

  static final String N = System.getProperty("line.separator");
  static final String N2 = N + N;
  static final String SPACE = " ";
  static final String EQUAL = "=";
  static final String EMPTY = "";
  static final String DOT = ".";
  static final String QUESTDOT = "?.";
  static final String COLON = ":";
  static final String IN = "in";
  static final String AT = "@";
  static final String DOLLAR = "$";
  static final String BACKTICK = "`";
  static final String QUEST = "?";
  static final String COMMA_WITH_SPACE = "," + SPACE;
  static final String STAR = "*";
  public static final String ZERO = "0";
}