// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.lang.cacheBuilder;

import com.intellij.openapi.fileTypes.FileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 */
public class CacheBuilderRegistryImpl extends CacheBuilderRegistry {
  @Override
  @Nullable
  public WordsScanner getCacheBuilder(@NotNull FileType fileType) {
    for(CacheBuilderEP ep: CacheBuilderEP.EP_NAME.getExtensionList()) {
      if (ep.getFileType().equals(fileType.getName())) {
        return ep.getWordsScanner();
      }
    }
    return null;
  }
}
