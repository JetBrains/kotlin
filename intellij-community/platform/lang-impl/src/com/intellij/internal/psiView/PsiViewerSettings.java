// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.psiView;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Konstantin Bulenkov
 */
@State(
  name = "PsiViewerSettings",
  storages = @Storage("other.xml")
)
public class PsiViewerSettings implements PersistentStateComponent<PsiViewerSettings> {
  public boolean showWhiteSpaces = true;
  public boolean showTreeNodes = true;
  public String type = "JAVA file";
  public String text = "";
  public String dialect = "";
  public int textDividerLocation = 250;
  public int treeDividerLocation = 400;
  public int lastSelectedTabIndex = 0;

  public static PsiViewerSettings getSettings() {
    return ServiceManager.getService(PsiViewerSettings.class);
  }

  @Override
  public PsiViewerSettings getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull PsiViewerSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
