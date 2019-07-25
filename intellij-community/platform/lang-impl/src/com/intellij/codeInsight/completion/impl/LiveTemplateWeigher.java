// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.completion.impl;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementWeigher;
import com.intellij.codeInsight.template.impl.LiveTemplateLookupElement;
import com.intellij.openapi.util.registry.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
* @author peter
*/
public class LiveTemplateWeigher extends LookupElementWeigher {
  public LiveTemplateWeigher() {
    super("templates", Registry.is("ide.completion.show.live.templates.on.top"), false);
  }

  @Nullable
  @Override
  public Comparable weigh(@NotNull LookupElement element) {
    return element instanceof LiveTemplateLookupElement;
  }
}
