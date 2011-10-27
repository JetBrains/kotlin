package org.jetbrains.jet.j2k.util;

import org.jetbrains.jet.j2k.ast.INode;

import java.util.LinkedList;
import java.util.List;

/**
 * @author ignatov
 */
public class AstUtil {
  private static String join(final String array[], final String delimiter) {
    StringBuilder buffer = new StringBuilder();
    boolean haveDelimiter = (delimiter != null);

    for (int i = 0; i < array.length; i++) {
      buffer.append(array[i]);

      if (haveDelimiter && (i + 1) < array.length)
        buffer.append(delimiter);
    }

    return buffer.toString();
  }

  public static String joinNodes(final List<? extends INode> nodes, final String delimiter) {
    return join(nodesToKotlin(nodes), delimiter);
  }

  public static String join(final List<String> array, final String delimiter) {
    return join(array.toArray(new String[array.size()]), delimiter);
  }

  public static List<String> nodesToKotlin(List<? extends INode> nodes) {
    List<String> result = new LinkedList<String>();
    for (INode n : nodes)
      result.add(n.toKotlin());
    return result;
  }

  public static String upperFirstCharacter(String string) {
    return string.substring(0, 1).toUpperCase() + string.substring(1);
  }
}
