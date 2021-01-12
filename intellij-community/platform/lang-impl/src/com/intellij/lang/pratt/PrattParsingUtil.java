// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.lang.pratt;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author peter
 */
public final class PrattParsingUtil {
  private PrattParsingUtil() {
  }

  public static void searchFor(PrattBuilder builder, PrattTokenType @NotNull ... types) {
    searchFor(builder, true, types);
  }

  public static boolean searchFor(final PrattBuilder builder, final boolean consume, final PrattTokenType... types) {
    final TokenSet set = TokenSet.create(types);
    if (!set.contains(builder.getTokenType())) {
      builder.assertToken(types[0]);
      while (!set.contains(builder.getTokenType()) && !builder.isEof()) {
        builder.advance();
      }
    }
    if (consume) {
      builder.advance();
    }
    return !builder.isEof();
  }

  @Nullable
  public static IElementType parseOption(final PrattBuilder builder, int rightPriority) {
    final MutableMarker marker = builder.mark();
    final IElementType type = builder.createChildBuilder(rightPriority).parse();
    if (type == null) {
      marker.rollback();
    } else {
      marker.finish();
    }
    return type;
  }
}
