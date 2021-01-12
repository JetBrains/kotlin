// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation.render;

import org.jetbrains.annotations.NotNull;

class DocRendererMemoryManager extends AbstractDocRenderMemoryManager<DocRenderer> {
  DocRendererMemoryManager() {
    super("doc.render.cache.size");
  }

  @Override
  void destroy(@NotNull DocRenderer renderer) {
    renderer.clearCachedComponent();
  }
}
