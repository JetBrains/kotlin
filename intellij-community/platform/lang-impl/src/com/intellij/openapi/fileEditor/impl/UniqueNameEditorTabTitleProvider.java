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
package com.intellij.openapi.fileEditor.impl;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.fileEditor.UniqueVFilePathBuilder;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author yole
 */
public class UniqueNameEditorTabTitleProvider implements EditorTabTitleProvider {
  @Override
  public String getEditorTabTitle(@NotNull Project project, @NotNull VirtualFile file) {
    UISettings uiSettings = UISettings.getInstanceOrNull();
    if (uiSettings == null || !uiSettings.getShowDirectoryForNonUniqueFilenames() || DumbService.isDumb(project)) {
      return null;
    }

    // Even though this is a 'tab title provider' it is used also when tabs are not shown, namely for building IDE frame title.
    String uniqueName = uiSettings.getEditorTabPlacement() == UISettings.TABS_NONE ?
                        UniqueVFilePathBuilder.getInstance().getUniqueVirtualFilePath(project, file) :
                        UniqueVFilePathBuilder.getInstance().getUniqueVirtualFilePathWithinOpenedFileEditors(project, file);
    uniqueName = getEditorTabText(uniqueName, File.separator, uiSettings.getHideKnownExtensionInTabs());
    return uniqueName.equals(file.getName()) ? null : uniqueName;
  }

  public static String getEditorTabText(String result, String separator, boolean hideKnownExtensionInTabs) {
    if (hideKnownExtensionInTabs) {
      String withoutExtension = FileUtil.getNameWithoutExtension(result);
      if (StringUtil.isNotEmpty(withoutExtension) && !withoutExtension.endsWith(separator)) {
        return withoutExtension;
      }
    }
    return result;
  }
}
