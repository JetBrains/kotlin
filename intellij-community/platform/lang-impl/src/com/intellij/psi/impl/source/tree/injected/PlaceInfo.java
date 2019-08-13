/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.psi.impl.source.tree.injected;

import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiLanguageInjectionHost;
import org.jetbrains.annotations.NotNull;

/**
 * stores injection registration info temporarily
 * from the {@link MultiHostRegistrar#startInjecting(com.intellij.lang.Language)} call
 * to the moment of {@link MultiHostRegistrar#doneInjecting()}
 */
class PlaceInfo {
  @NotNull final String prefix;
  @NotNull final String suffix;
  @NotNull final PsiLanguageInjectionHost host;
  @NotNull final TextRange registeredRangeInsideHost;
  @NotNull final LiteralTextEscaper<? extends PsiLanguageInjectionHost> myEscaper;
  @NotNull final String myHostText;
  ProperTextRange rangeInDecodedPSI;
  TextRange rangeInHostElement;
  Segment newInjectionHostRange;

  PlaceInfo(@NotNull String prefix,
            @NotNull String suffix,
            @NotNull PsiLanguageInjectionHost host,
            @NotNull TextRange registeredRangeInsideHost) {
    this.prefix = prefix;
    this.suffix = suffix;
    this.host = host;
    this.registeredRangeInsideHost = registeredRangeInsideHost;
    myEscaper = host.createLiteralTextEscaper();
    myHostText = host.getText();
  }

  @Override
  public String toString() {
    return "Shred " +
           (prefix.isEmpty() ? "" : "prefix='"+prefix+"' ") +
           (suffix.isEmpty() ? "" : "suffix='"+suffix+"' ") +
           "in " + host + " with range " + host.getTextRange()+" ("+host.getClass()+") "+
           "inside range " + registeredRangeInsideHost;
  }

  TextRange getRelevantRangeInsideHost() {
    return myEscaper.getRelevantTextRange().intersection(registeredRangeInsideHost);
  }
}
