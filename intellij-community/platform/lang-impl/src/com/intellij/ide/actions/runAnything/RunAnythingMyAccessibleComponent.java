// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.*;
import java.awt.*;

class RunAnythingMyAccessibleComponent extends JPanel {
  private Accessible myAccessible;

  RunAnythingMyAccessibleComponent(LayoutManager layout) {
    super(layout);
    setOpaque(false);
  }

  void setAccessible(Accessible comp) {
    myAccessible = comp;
  }

  @Override
  public AccessibleContext getAccessibleContext() {
    return accessibleContext = (myAccessible != null ? myAccessible.getAccessibleContext() : super.getAccessibleContext());
  }
}