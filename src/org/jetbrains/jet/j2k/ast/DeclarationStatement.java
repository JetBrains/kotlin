package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * @author ignatov
 */
public class DeclarationStatement extends Statement {
  private final List<Element> myElements;

  public DeclarationStatement(List<Element> elements) {
    myElements = elements;
  }

  private List<String> toStringList(List<Element> elements) {
    List<String> result = new LinkedList<String>();
    for (String n : AstUtil.nodesToKotlin(elements))
      result.add(
        "var" + SPACE + n
      );
    return result;
  }

  @NotNull
  @Override
  public String toKotlin() {
    return AstUtil.join(toStringList(myElements), N);
  }
}
