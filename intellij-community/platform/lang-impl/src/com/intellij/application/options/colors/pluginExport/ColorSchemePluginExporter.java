/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.application.options.colors.pluginExport;

import com.intellij.application.options.schemes.SerializableSchemeExporter;
import com.intellij.configurationStore.SerializableScheme;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.impl.AbstractColorsScheme;
import com.intellij.openapi.editor.colors.impl.ReadOnlyColorsScheme;
import com.intellij.openapi.options.ConfigurableSchemeExporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ColorSchemePluginExporter extends ConfigurableSchemeExporter<PluginExportData,EditorColorsScheme> {
  @Override
  public void exportScheme(@NotNull EditorColorsScheme scheme, @NotNull OutputStream outputStream, @Nullable PluginExportData exportData)
    throws Exception {
    if (exportData != null) {
      try (ZipOutputStream zipStream = new ZipOutputStream(outputStream)) {
        zipStream.putNextEntry(new ZipEntry("META-INF/plugin.xml"));
        writePluginXml(scheme, zipStream, exportData);
        zipStream.putNextEntry(new ZipEntry("colors/" + scheme.getName() + ".xml"));
        SerializableSchemeExporter.writeToStream((SerializableScheme)scheme, zipStream);
      }
    }
  }

  @Override
  public String getExtension() {
    return "jar";
  }

  @Nullable
  @Override
  public PluginExportData getConfiguration(@NotNull Component parent, @NotNull EditorColorsScheme scheme) {
    PluginExportData exportData = getPluginExportData(scheme);
    EditorColorsScheme schemeToUpdate = getSchemeToUpdate(scheme);
    PluginInfoDialog infoDialog = new PluginInfoDialog(parent, exportData);
    if (infoDialog.showAndGet()) {
      infoDialog.apply();
      // Save metadata
      exportData.saveToProperties(schemeToUpdate.getMetaProperties());
      if (schemeToUpdate instanceof AbstractColorsScheme) ((AbstractColorsScheme)schemeToUpdate).setSaveNeeded(true);
      return exportData;
    }
    return null;
  }

  private static void writePluginXml(
    @NotNull EditorColorsScheme scheme,
    @NotNull OutputStream outputStream,
    @NotNull PluginExportData exportData
  ) throws IOException {
    ColorSchemePluginTemplate template = new ColorSchemePluginTemplate(scheme, exportData);
    @SuppressWarnings("IOResourceOpenedButNotSafelyClosed") // Don't close the stream, there will be more content!
      OutputStreamWriter writer = new OutputStreamWriter(outputStream);
    writer.write(template.getText());
    writer.flush();
  }

  @NotNull
  private static EditorColorsScheme getSchemeToUpdate(@NotNull EditorColorsScheme scheme) {
    if (scheme instanceof AbstractColorsScheme) {
      EditorColorsScheme original = ((AbstractColorsScheme)scheme).getOriginal();
      if (original != null) return original;
    }
    return scheme;
  }

  @NotNull
  private static PluginExportData getPluginExportData(@NotNull EditorColorsScheme scheme) {
    PluginExportData data = new PluginExportData(scheme.getMetaProperties());
    if (data.isEmpty() && scheme instanceof AbstractColorsScheme && !(scheme instanceof ReadOnlyColorsScheme)) {
      EditorColorsScheme original = ((AbstractColorsScheme)scheme).getOriginal();
      if (original != null) {
        return getPluginExportData(original);
      }
    }
    return data;
  }
}
