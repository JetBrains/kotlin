// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.fileTemplates.impl;

import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

/**
 * @author Dmitry Avdeev
 */
@State(
  name = "ExportableFileTemplateSettings",
  storages = @Storage(FileTemplateSettings.EXPORTABLE_SETTINGS_FILE),
  additionalExportFile = FileTemplatesLoader.TEMPLATES_DIR
)
final class ExportableFileTemplateSettings extends FileTemplateSettings {
  ExportableFileTemplateSettings() {
    super(null);
  }
}
