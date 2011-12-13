package org.jetbrains.jet.j2k.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.j2k.ast.Expression;
import org.jetbrains.jet.j2k.ast.INode;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author ignatov
 */
public class AstUtil {
  private AstUtil() {
  }

  private static String join(@NotNull final String[] array, @Nullable final String delimiter) {
    StringBuilder buffer = new StringBuilder();
    boolean haveDelimiter = (delimiter != null);

    for (int i = 0; i < array.length; i++) {
      buffer.append(array[i]);

      if (haveDelimiter && (i + 1) < array.length)
        buffer.append(delimiter);
    }

    return buffer.toString();
  }

  public static String joinNodes(@NotNull final List<? extends INode> nodes, final String delimiter) {
    return join(nodesToKotlin(nodes), delimiter);
  }

  public static String join(@NotNull final List<String> array, final String delimiter) {
    return join(array.toArray(new String[array.size()]), delimiter);
  }

  @NotNull
  public static List<String> nodesToKotlin(@NotNull List<? extends INode> nodes) {
    List<String> result = new LinkedList<String>();
    for (INode n : nodes)
      result.add(n.toKotlin());
    return result;
  }

  @NotNull
  public static String upperFirstCharacter(@NotNull String string) {
    return string.substring(0, 1).toUpperCase() + string.substring(1);
  }

  @NotNull
  public static String lowerFirstCharacter(@NotNull String string) {
    return string.substring(0, 1).toLowerCase() + string.substring(1);
  }

  @NotNull
  public static List<String> createListWithEmptyString(@NotNull final List<Expression> arguments) {
    final List<String> conversions = new LinkedList<String>();
    //noinspection UnusedDeclaration
    for (Expression argument : arguments) conversions.add("");
    return conversions;
  }

  @NotNull
  public static List<String> applyConversions(@NotNull List<String> first, @NotNull List<String> second) {
    List<String> result = new LinkedList<String>();
    assert first.size() == second.size() : "Lists must have the same size.";
    for (int i = 0; i < first.size(); i++) {
      result.add(applyConversionForOneItem(first.get(i), second.get(i)));
    }
    return result;
  }

  @NotNull
  public static String applyConversionForOneItem(@NotNull String f, @NotNull String s) {
    if (s.isEmpty())
      return f;
    else
      return "(" + f + ")" + s;
  }

  public static <T> T getOrElse(@NotNull Map<T, T> map, T e, T orElse) {
    if (map.containsKey(e))
      return map.get(e);
    return orElse;
  }
}
