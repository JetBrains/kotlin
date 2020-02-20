// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl

import com.google.common.collect.MultimapBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager.checkCanceled
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.SdkType
import com.intellij.openapi.roots.ModuleJdkOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.UnknownSdk
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.*
import java.util.function.Consumer

private val EP_NAME = ExtensionPointName.create<UnknownSdkContributor>("com.intellij.unknownSdkContributor")

interface UnknownSdkContributor {
  fun contributeUnknownSdks(project: Project): List<UnknownSdk>
}

data class UnknownSdkSnapshot(
  val totallyUnknownSdks: Set<String>,
  val resolvableSdks: List<UnknownSdk>
)

private data class MissingSdkInfo(
  private val mySdkName: String,
  private val mySdkType: SdkType
) : UnknownSdk {
  override fun getSdkName() = mySdkName
  override fun getSdkType() = mySdkType
}

class UnknownSdkCollector(private val myProject: Project) {
  private val LOG = logger<UnknownSdkCollector>()

  fun collectSdksPromise(onCompleted: Consumer<UnknownSdkSnapshot>) {
    ReadAction.nonBlocking<UnknownSdkSnapshot> { collectSdksUnderReadAction() }
      .expireWith(myProject)
      .coalesceBy(myProject, UnknownSdkCollector::class)
      .finishOnUiThread(ApplicationManager.getApplication().noneModalityState, onCompleted)
      .submit(AppExecutorUtil.getAppExecutorService())
  }

  private fun collectSdksUnderReadAction(): UnknownSdkSnapshot {

    val sdkToTypes = MultimapBuilder.treeKeys(java.lang.String.CASE_INSENSITIVE_ORDER)
      .hashSetValues()
      .build<String, String>()

    checkCanceled()

    val rootManager = ProjectRootManager.getInstance(myProject)
    if (rootManager.projectSdk == null) {
      val sdkName = rootManager.projectSdkName
      val sdkTypeName = rootManager.projectSdkTypeName

      if (sdkName != null) {
        sdkToTypes.put(sdkName, sdkTypeName)
      }
    }

    for (module in ModuleManager.getInstance(myProject).modules) {
      checkCanceled()

      val moduleRootManager = ModuleRootManager.getInstance(module)
      if (moduleRootManager.externalSource != null) {
        //we do not need to check external modules (e.g. which are imported from Maven or Gradle)
        continue
      }

      val jdkEntry = moduleRootManager.orderEntries
                       .filterIsInstance<ModuleJdkOrderEntry>()
                       .firstOrNull() ?: continue

      if (jdkEntry.jdk == null) {
        val jdkName = jdkEntry.jdkName
        val jdkTypeName = jdkEntry.jdkTypeName

        if (jdkName != null) {
          sdkToTypes.put(jdkName, jdkTypeName)
        }
      }
    }

    val totallyUnknownSdks = TreeSet(String.CASE_INSENSITIVE_ORDER)
    val resolvableSdks = mutableListOf<UnknownSdk>()
    for ((sdkName, sdkTypes) in sdkToTypes.asMap()) {
      val singleSdkTypeName = sdkTypes.filterNotNull().distinct().singleOrNull()
      if (singleSdkTypeName == null) {
        totallyUnknownSdks.add(sdkName)
        continue
      }

      val sdkType = SdkType.findByName(singleSdkTypeName)
      if (sdkType == null) {
        // that seems like one has removed a plugin that was used
        // and there is no requested SDK too.
        // it makes less sense to suggest any SDK here (we will fail)
        // just skipping it for now
        continue
      }

      resolvableSdks.add(MissingSdkInfo(sdkName, sdkType))
    }

    val detectedUnknownSdkNames = resolvableSdks.mapNotNull { it.sdkName }.toMutableSet()

    EP_NAME.forEachExtensionSafe {
      val contrib = try {
        it.contributeUnknownSdks(myProject)
      } catch (e: ProcessCanceledException) {
        throw e
      } catch (t: Throwable) {
        LOG.warn("Failed to contribute SDKs with ${it.javaClass.name}. ${t.message}", t)
        listOf<UnknownSdk>()
      }

      for (unknownSdk in contrib) {
        val name = unknownSdk.sdkName ?: continue
        if (!detectedUnknownSdkNames.add(name)) continue
        resolvableSdks += unknownSdk
      }
    }

    return UnknownSdkSnapshot(totallyUnknownSdks, resolvableSdks)
  }
}
