// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.richcopy.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Denis Zhdanov
 */
@State(name = "EditorRichCopySettings", storages = @Storage("editor.rich.copy.xml"))
public class RichCopySettings implements PersistentStateComponent<RichCopySettings> {

  @NotNull public static final String ACTIVE_GLOBAL_SCHEME_MARKER = "__ACTIVE_GLOBAL_SCHEME__";

  private boolean myEnabled = true;
  private String  mySchemeName = ACTIVE_GLOBAL_SCHEME_MARKER;

  @NotNull
  public static RichCopySettings getInstance() {
    return ServiceManager.getService(RichCopySettings.class);
  }

  @NotNull
  public EditorColorsScheme getColorsScheme(@NotNull EditorColorsScheme editorColorsScheme) {
    EditorColorsScheme result = null;
    if (mySchemeName != null && !ACTIVE_GLOBAL_SCHEME_MARKER.equals(mySchemeName)) {
      result = EditorColorsManager.getInstance().getScheme(mySchemeName);
    }
    return result == null ? editorColorsScheme : result;
  }

  @Nullable
  @Override
  public RichCopySettings getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull RichCopySettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  @NotNull
  public String getSchemeName() {
    return mySchemeName == null ? ACTIVE_GLOBAL_SCHEME_MARKER : mySchemeName;
  }

  public void setSchemeName(@Nullable String schemeName) {
    mySchemeName = schemeName;
  }

  public boolean isEnabled() {
    return myEnabled;
  }

  public void setEnabled(boolean enabled) {
    myEnabled = enabled;
  }
}
