// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle;

import com.intellij.openapi.components.*;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Rustam Vishnyakov
 */
@State(
  name = "CodeStyleSchemesUIConfiguration",
  storages = {@Storage(value = "other.xml", roamingType = RoamingType.DISABLED)}
)
public class CodeStyleSchemesUIConfiguration implements PersistentStateComponent<CodeStyleSchemesUIConfiguration> {
  public String RECENT_IMPORT_FILE_LOCATION = "";

  @Nullable
  @Override
  public CodeStyleSchemesUIConfiguration getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull CodeStyleSchemesUIConfiguration state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  public static CodeStyleSchemesUIConfiguration getInstance() {
    return ServiceManager.getService(CodeStyleSchemesUIConfiguration.class);
  }

  public static final class Util {
    @Nullable
    public static VirtualFile getRecentImportFile() {
      CodeStyleSchemesUIConfiguration configuration = getInstance();
      if (configuration != null) {
        String fileLocation = configuration.RECENT_IMPORT_FILE_LOCATION;
        if (fileLocation == null || fileLocation.trim().isEmpty()) return null;
        try {
          URL url = new URL(fileLocation);
          return VfsUtil.findFileByURL(url);
        }
        catch (MalformedURLException e) {
          // Ignore
        }
      }
      return null;
    }

    public static void setRecentImportFile(@NotNull VirtualFile recentFile) {
      CodeStyleSchemesUIConfiguration configuration = getInstance();
      if (configuration != null) {
        URL url = VfsUtilCore.convertToURL(recentFile.getUrl());
        if (url != null) {
          configuration.RECENT_IMPORT_FILE_LOCATION = url.toString();
        }
      }
    }
  }
}
