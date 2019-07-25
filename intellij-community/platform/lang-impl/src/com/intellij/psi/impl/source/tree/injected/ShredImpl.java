/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.psi.impl.source.tree.injected;

import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.smartPointers.SmartPointerManagerImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class ShredImpl implements PsiLanguageInjectionHost.Shred {
  private final SmartPsiFileRange relevantRangeInHost;
  private final SmartPsiElementPointer<PsiLanguageInjectionHost> hostElementPointer;
  private final TextRange rangeInDecodedPSI;
  private final String prefix;
  private final String suffix;
  private final boolean usePsiRange;
  private final boolean isOneLine;

  ShredImpl(@NotNull SmartPsiFileRange relevantRangeInHost,
            @NotNull SmartPsiElementPointer<PsiLanguageInjectionHost> hostElementPointer,
            @NotNull String prefix,
            @NotNull String suffix,
            @NotNull TextRange rangeInDecodedPSI,
            boolean usePsiRange, boolean isOneLine) {
    this.hostElementPointer = hostElementPointer;
    this.relevantRangeInHost = relevantRangeInHost;
    this.prefix = prefix;
    this.suffix = suffix;
    this.rangeInDecodedPSI = rangeInDecodedPSI;
    this.usePsiRange = usePsiRange;
    this.isOneLine = isOneLine;

    assertValid();
  }

  private void assertValid() {
    Segment hostRange = relevantRangeInHost.getPsiRange();
    assert hostRange != null : "invalid host range: " + relevantRangeInHost;

    PsiLanguageInjectionHost host = hostElementPointer.getElement();
    assert host != null && host.isValid() : "no host: " + hostElementPointer;
  }

  @NotNull
  ShredImpl withPsiRange() {
    return new ShredImpl(relevantRangeInHost, hostElementPointer, prefix, suffix, rangeInDecodedPSI, true, isOneLine);
  }
  @NotNull
  ShredImpl withRange(@NotNull TextRange rangeInDecodedPSI,
                      @NotNull TextRange rangeInHostElementPSI,
                      @NotNull PsiLanguageInjectionHost newHost) {
    SmartPsiFileRange rangeMarker = relevantRangeInHost;
    Segment oldRangeInHostElementPSI = calcRangeInsideHostElement(false);
    SmartPointerManagerImpl pointerManager = (SmartPointerManagerImpl)SmartPointerManager.getInstance(rangeMarker.getProject());
    SmartPsiElementPointer<PsiLanguageInjectionHost> newHostPointer = pointerManager.createSmartPsiElementPointer(newHost, newHost.getContainingFile(), true);

    if (!rangeInHostElementPSI.equals(TextRange.create(oldRangeInHostElementPSI))) {
      Segment hostElementRange = newHostPointer.getRange();
      rangeMarker = pointerManager
        .createSmartPsiFileRangePointer(rangeMarker.getContainingFile(), rangeInHostElementPSI.shiftRight(hostElementRange.getStartOffset()), true);
    }
    return new ShredImpl(rangeMarker, newHostPointer, prefix, suffix, rangeInDecodedPSI, usePsiRange, isOneLine);
  }

  @NotNull
  SmartPsiElementPointer<PsiLanguageInjectionHost> getSmartPointer() {
    return hostElementPointer;
  }

  @Override
  @Nullable("returns null when the host document marker is invalid")
  public Segment getHostRangeMarker() {
    return usePsiRange ? relevantRangeInHost.getPsiRange() : relevantRangeInHost.getRange();
  }

  @Override
  @NotNull
  public TextRange getRangeInsideHost() {
    return calcRangeInsideHostElement(true);
  }

  @NotNull
  private TextRange calcRangeInsideHostElement(boolean usePsiRange) {
    PsiLanguageInjectionHost host = getHost();
    Segment psiRange = usePsiRange ? relevantRangeInHost.getPsiRange() : relevantRangeInHost.getRange();
    TextRange textRange = psiRange == null ? null : TextRange.create(psiRange);
    if (host == null) {
      if (textRange != null) return textRange;
      Segment fromSP = usePsiRange ? hostElementPointer.getPsiRange() : hostElementPointer.getRange();
      if (fromSP != null) return TextRange.create(fromSP);
      return new TextRange(0,0);
    }
    TextRange hostTextRange = host.getTextRange();
    textRange = textRange == null ? null : textRange.intersection(hostTextRange);
    if (textRange == null) return new ProperTextRange(0, hostTextRange.getLength());

    return textRange.shiftLeft(hostTextRange.getStartOffset());
  }

  @Override
  @SuppressWarnings("HardCodedStringLiteral")
  public String toString() {
    PsiLanguageInjectionHost host = getHost();
    Segment hostRange = getHostRangeMarker();
    return "Shred " + (host == null ? null : host.getTextRange()) + ": " + host +
           " In host range: " + (hostRange != null ? "(" + hostRange.getStartOffset() + "," + hostRange.getEndOffset() + ");" : "invalid;") +
           " PSI range: " + rangeInDecodedPSI;
  }

  @Override
  public boolean isValid() {
    return getHostRangeMarker() != null && getHost() != null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PsiLanguageInjectionHost.Shred shred = (PsiLanguageInjectionHost.Shred)o;

    PsiLanguageInjectionHost host = getHost();
    Segment hostRangeMarker = getHostRangeMarker();
    Segment hostRangeMarker2 = shred.getHostRangeMarker();
    return host != null &&
           host.equals(shred.getHost()) &&
           prefix.equals(shred.getPrefix()) &&
           suffix.equals(shred.getSuffix()) &&
           rangeInDecodedPSI.equals(shred.getRange()) &&
           hostRangeMarker != null &&
           hostRangeMarker2 != null &&
           TextRange.areSegmentsEqual(hostRangeMarker, hostRangeMarker2);
  }

  @Override
  public int hashCode() {
    return rangeInDecodedPSI.hashCode();
  }

  @Override
  public void dispose() {
  }

  @Override
  @Nullable
  public PsiLanguageInjectionHost getHost() {
    return hostElementPointer.getElement();
  }

  @NotNull
  @Override
  public TextRange getRange() {
    return rangeInDecodedPSI;
  }

  @NotNull
  @Override
  public String getPrefix() {
    return prefix;
  }

  @NotNull
  @Override
  public String getSuffix() {
    return suffix;
  }

  boolean isOneLine() {
    return isOneLine;
  }
}
