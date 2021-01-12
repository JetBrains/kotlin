// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.ide.util.DelegatingProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;

class PoweredProgressIndicator extends DelegatingProgressIndicator {
  private final double myPower;

  static ProgressIndicator wrap(@NotNull ProgressIndicator indicator, double power) {
    return new PoweredProgressIndicator(indicator, power);
  }

  private PoweredProgressIndicator(@NotNull ProgressIndicator indicator, double power) {
    super(indicator);
    myPower = power;
  }

  @Override
  public void setFraction(double fraction) {
    double poweredFraction = Math.pow(fraction, myPower);
    super.setFraction(poweredFraction);
  }
}
