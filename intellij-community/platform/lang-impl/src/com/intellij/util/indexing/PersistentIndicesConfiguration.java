// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.application.PathManager;
import com.intellij.util.io.DataInputOutputUtil;

import java.io.*;

final class PersistentIndicesConfiguration {
  private static final int BASE_INDICES_CONFIGURATION_VERSION = 1;

  static void saveConfiguration() {
    try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(indicesConfigurationFile())))) {
      DataInputOutputUtil.writeINT(out, getIndexesConfigurationVersion());
      IndexingStamp.savePersistentIndexStamp(out);
    }
    catch (IOException ignored) {
    }
  }

  static void loadConfiguration() {
    try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(indicesConfigurationFile())))) {
      if (DataInputOutputUtil.readINT(in) == getIndexesConfigurationVersion()) {
        IndexingStamp.initPersistentIndexStamp(in);
      }
    }
    catch (IOException ignored) {
    }
  }

  private static int getIndexesConfigurationVersion() {
    int version = BASE_INDICES_CONFIGURATION_VERSION;
    for (FileBasedIndexInfrastructureExtension ex : FileBasedIndexInfrastructureExtension.EP_NAME.getExtensions()) {
      version = 31 * version + ex.getVersion();
    }
    return version;
  }

  private static File indicesConfigurationFile() {
    return new File(PathManager.getIndexRoot(), "indices.config");
  }
}
