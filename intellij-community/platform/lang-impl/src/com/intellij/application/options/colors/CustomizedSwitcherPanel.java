/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.application.options.colors;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorSchemeAttributeDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import com.intellij.openapi.options.colors.RainbowColorSettingsPage;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class CustomizedSwitcherPanel extends CompositeColorDescriptionPanel {
  private final ColorSettingsPage myPage;
  private final PreviewPanel myPreviewPanel;

  CustomizedSwitcherPanel(@Nullable PreviewPanel previewPanel,
                                 @Nullable ColorSettingsPage page) {
    myPage = page;
    myPreviewPanel = previewPanel;

    addDescriptionPanel(new ColorAndFontDescriptionPanel(), it -> it instanceof ColorAndFontDescription);
    addDescriptionPanel(new RainbowDescriptionPanel(), it -> it instanceof RainbowAttributeDescriptor);
  }

  @Override
  public void reset(@NotNull EditorSchemeAttributeDescriptor descriptor) {
    super.reset(descriptor);
    updatePreviewPanel(descriptor);
  }

  @Override
  public void apply(@NotNull EditorSchemeAttributeDescriptor descriptor, EditorColorsScheme scheme) {
    super.apply(descriptor, scheme);
    updatePreviewPanel(descriptor);
  }

  protected void updatePreviewPanel(@NotNull EditorSchemeAttributeDescriptor descriptor) {
    if (!(myPreviewPanel instanceof SimpleEditorPreview && myPage instanceof RainbowColorSettingsPage)) return;
    UIUtil.invokeAndWaitIfNeeded((Runnable)() -> ApplicationManager.getApplication().runWriteAction(() -> {
      SimpleEditorPreview simpleEditorPreview = (SimpleEditorPreview)myPreviewPanel;
      simpleEditorPreview.setupRainbow(descriptor.getScheme(), (RainbowColorSettingsPage)myPage);
      simpleEditorPreview.updateView();
    }));
  }
}
