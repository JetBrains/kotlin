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
package com.intellij.codeInspection.export;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;

import java.io.*;
/**
 * @author Dmitry Batkovich
 */
class InspectionTreeHtmlExportResources {
  private static final Logger LOG = Logger.getInstance(InspectionTreeHtmlExportResources.class);

  static void copyInspectionReportResources(final String targetDirectory) {
    copyInspectionReportResource("styles.css", targetDirectory);
    copyInspectionReportResource("script.js", targetDirectory);
  }

  private static void copyInspectionReportResource(final String resourceName, final String targetDirectory) {
    final File resourceTargetFile = new File(targetDirectory, resourceName);
    if (!FileUtil.createIfDoesntExist(resourceTargetFile)) {
      LOG.error("Can't create file: " + resourceTargetFile.getAbsolutePath());
    }
    try (InputStream input = InspectionTreeHtmlExportResources.class.getClassLoader().getResourceAsStream("/inspectionReport/" + resourceName)) {
      try (OutputStream f = new FileOutputStream(resourceTargetFile)) {
        FileUtil.copy(input, f);
      }
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }
}
