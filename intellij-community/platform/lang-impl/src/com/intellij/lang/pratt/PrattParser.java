/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.lang.pratt;

import com.intellij.lang.ASTNode;
import com.intellij.lang.LangBundle;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/**
 * @author peter
*/
public abstract class PrattParser implements PsiParser {
  protected abstract PrattRegistry getRegistry();

  @Override
  @NotNull
  public final ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
    final PrattBuilder prattBuilder = PrattBuilderImpl.createBuilder(builder, getRegistry());
    final MutableMarker marker = prattBuilder.mark();
    parse(prattBuilder);
    marker.finish(root);
    return builder.getTreeBuilt();
  }

  protected void parse(final PrattBuilder builder) {
    builder.parse();
    if (!builder.isEof()) {
      builder.error(LangBundle.message("unexpected.token"));
      while (!builder.isEof()) builder.advance();
    }
  }
}
