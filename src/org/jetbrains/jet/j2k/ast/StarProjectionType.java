package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class StarProjectionType extends Type {
  @NotNull
  @Override
  public String toKotlin() {
    G g = new G("");
    G<String> g2 = new G<String>("");
    return STAR;
  }
}

class G <T extends String> {
  public <T> G(T t) {
  }
}