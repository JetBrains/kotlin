// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.ide.util.treeView.TreeState;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Transient;
import org.jdom.Element;

@Tag("serviceView")
final class ServiceViewState {
  private static final float DEFAULT_CONTENT_PROPORTION = 0.3f;

  @Attribute("id")
  public String id = "";
  public float contentProportion = DEFAULT_CONTENT_PROPORTION;
  @Tag("treeState")
  public Element treeStateElement;

  @Transient
  public TreeState treeState = TreeState.createFrom(null);
}
