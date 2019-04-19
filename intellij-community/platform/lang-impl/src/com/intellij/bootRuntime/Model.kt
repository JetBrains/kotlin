// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bootRuntime

import com.intellij.bootRuntime.bundles.Remote
import com.intellij.bootRuntime.bundles.Runtime
import com.intellij.openapi.util.io.FileUtil

enum class BundleState {
  REMOTE,
  DOWNLOADED,
  EXTRACTED,
  INSTALLED,
  UNINSTALLED
}

class Model(var selectedBundle: Runtime, val bundles:MutableList<Runtime>) {

  fun updateBundle(newBundle:Runtime) {
    selectedBundle = newBundle
  }

  fun currentState () : BundleState {
     return when {
       isInstalled(selectedBundle) -> BundleState.INSTALLED
       isExtracted(selectedBundle) -> BundleState.EXTRACTED
       isDownloaded(selectedBundle) -> BundleState.DOWNLOADED
       isRemote(selectedBundle) -> BundleState.REMOTE
       else -> BundleState.UNINSTALLED
     }
  }

  fun isInstalled(bundle:Runtime):Boolean = bundle.installationPath.exists() &&
                                            BinTrayUtil.getJdkConfigFilePath().exists() &&
                                            FileUtil.loadFile(BinTrayUtil.getJdkConfigFilePath()).startsWith(bundle.installationPath.absolutePath)

  private fun isExtracted(bundle:Runtime):Boolean = bundle.transitionPath.exists()

  private fun isDownloaded(bundle:Runtime):Boolean = bundle.downloadPath.exists()

  fun isRemote(bundle:Runtime):Boolean = bundle is Remote
}
