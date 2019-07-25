/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import org.jetbrains.annotations.NotNull;

import java.util.EventListener;

public interface ColorAndFontSettingsListener extends EventListener {
  void selectedOptionChanged(@NotNull Object selected);
  void schemeChanged(@NotNull Object source);
  void settingsChanged();
  void selectionInPreviewChanged(@NotNull String typeToSelect);

  void fontChanged();

  abstract class Abstract implements ColorAndFontSettingsListener {
    @Override
    public void selectedOptionChanged(@NotNull final Object selected) {

    }

    @Override
    public void schemeChanged(@NotNull final Object source) {
    }

    @Override
    public void settingsChanged() {
    }

    @Override
    public void selectionInPreviewChanged(@NotNull final String typeToSelect) {
    }

    @Override
    public void fontChanged() {
      
    }
  }
}
