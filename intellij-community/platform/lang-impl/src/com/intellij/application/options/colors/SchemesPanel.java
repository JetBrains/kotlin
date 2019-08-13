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

import com.intellij.application.options.SkipSelfSearchComponent;
import com.intellij.application.options.schemes.AbstractSchemeActions;
import com.intellij.application.options.schemes.SimpleSchemesPanel;
import com.intellij.application.options.schemes.SchemesModel;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SchemesPanel extends SimpleSchemesPanel<EditorColorsScheme> implements SkipSelfSearchComponent {
  private final ColorAndFontOptions myOptions;

  private final EventDispatcher<ColorAndFontSettingsListener> myDispatcher = EventDispatcher.create(ColorAndFontSettingsListener.class);

  public SchemesPanel(@NotNull ColorAndFontOptions options) {
    this(options, DEFAULT_VGAP);
  }

  SchemesPanel(@NotNull ColorAndFontOptions options, int vGap) {
    super(vGap);
    myOptions = options;
  }

  private boolean myListLoaded;

  public boolean areSchemesLoaded() {
    return myListLoaded;
  }


  void resetSchemesCombo(final Object source) {
    if (this != source) {
      setListLoaded(false);
      EditorColorsScheme selectedSchemeBackup = myOptions.getSelectedScheme();
      resetSchemes(myOptions.getOrderedSchemes());
      selectScheme(selectedSchemeBackup);
      setListLoaded(true);
      myDispatcher.getMulticaster().schemeChanged(this);
    }
  }
  

  private void setListLoaded(final boolean b) {
    myListLoaded = b;
  }

  public void addListener(@NotNull ColorAndFontSettingsListener listener) {
    myDispatcher.addListener(listener);
  }

  @NotNull
  @Override
  protected AbstractSchemeActions<EditorColorsScheme> createSchemeActions() {
    return new ColorSchemeActions(this) {
        @NotNull
        @Override
        protected ColorAndFontOptions getOptions() {
          return myOptions;
        }

        @Override
        protected void onSchemeChanged(@Nullable EditorColorsScheme scheme) {
          if (scheme != null) {
            myOptions.selectScheme(scheme.getName());
            if (areSchemesLoaded()) {
              myDispatcher.getMulticaster().schemeChanged(SchemesPanel.this);
            }
          }
        }

        @Override
        protected void renameScheme(@NotNull EditorColorsScheme scheme, @NotNull String newName) {
          if (myOptions.saveSchemeAs(scheme, newName)) {
            myOptions.removeScheme(scheme);
            myOptions.selectScheme(newName);
          }
        }
      };
  }

  @NotNull
  @Override
  public SchemesModel<EditorColorsScheme> getModel() {
    return myOptions;
  }

  @Override
  protected boolean supportsProjectSchemes() {
    return false;
  }

  @Override
  protected boolean highlightNonDefaultSchemes() {
    return true;
  }

  @Override
  public boolean useBoldForNonRemovableSchemes() {
    return true;
  }
}
