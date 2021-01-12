// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.impl;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.stubs.StubUpdatingIndex;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class PushedFilePropertiesRetrieverImpl implements PushedFilePropertiesRetriever {
  @NotNull
  @Override
  public List<String> dumpSortedPushedProperties(@NotNull VirtualFile file) {
    if (file.isDirectory()) {
      throw new IllegalArgumentException("file " + file + " is expected to be a regular file");
    }
    if (StubUpdatingIndex.USE_SNAPSHOT_MAPPINGS) {
      List<FilePropertyPusher<?>> extensions = FilePropertyPusher.EP_NAME.getExtensionList();
      List<String> properties = new SmartList<>();
      for (FilePropertyPusher<?> extension : extensions) {
        Object property;
        VirtualFile vfsObject;
        if (extension.pushDirectoriesOnly()) {
          vfsObject = file.getParent();
        }
        else {
          vfsObject = file;
        }
        property = vfsObject.getUserData(extension.getFileDataKey());
        if (property != null) {
          properties.add(property.toString());
        }
      }

      ContainerUtil.sort(properties);
      return properties;
    }
    else {
      return Collections.emptyList();
    }
  }
}
