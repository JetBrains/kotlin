// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl.jdkDownloader

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.*
import com.intellij.openapi.projectRoots.SimpleJavaSdkType.notSimpleJavaSdkTypeIfAlternativeExistsAndNotDependentSdkType
import com.intellij.openapi.roots.ui.configuration.*
import com.intellij.openapi.roots.ui.configuration.UnknownSdkResolver.*
import com.intellij.openapi.roots.ui.configuration.SdkDetector.DetectedSdkListener
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTask
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.lang.JavaVersion
import org.jetbrains.jps.model.java.JdkVersionDetector

class JdkAuto : UnknownSdkResolver, JdkDownloaderBase {
  private val LOG = logger<JdkAuto>()

  override fun supportsResolution(sdkTypeId: SdkTypeId) = notSimpleJavaSdkTypeIfAlternativeExistsAndNotDependentSdkType().value(sdkTypeId)

  override fun createResolver(project: Project?, indicator: ProgressIndicator): UnknownSdkLookup? {
    if (!Registry.`is`("jdk.auto.setup")) return null
    if (ApplicationManager.getApplication().isUnitTestMode) return null

    val sdkType = SdkType.getAllTypes()
                    .singleOrNull(notSimpleJavaSdkTypeIfAlternativeExistsAndNotDependentSdkType()::value) ?: return null

    return object : UnknownSdkLookup {
      val lazyDownloadModel by lazy {
        indicator.text = "Downloading JDK list..."
        JdkListDownloader.downloadModelForJdkInstaller(indicator)
      }

      override fun proposeDownload(sdk: UnknownSdk, indicator: ProgressIndicator): UnknownSdkDownloadableSdkFix? {
        if (sdk.sdkType != sdkType) return null

        val req = JdkRequirements.parseRequirement(sdk) ?: return null
        LOG.info("Looking for a possible download for ${sdk.sdkType.presentableName} with name ${sdk}")

        //we select the newest matching version for a possible fix
        val jdkToDownload = lazyDownloadModel
                              .filter { req.matches(it) }
                              .mapNotNull {
                                val v = JavaVersion.tryParse(it.versionString)
                                if (v != null) {
                                  it to v
                                }
                                else null
                              }.maxBy { it.second }
                              ?.first ?: return null

        return object: UnknownSdkDownloadableSdkFix {
          override fun getVersionString() = jdkToDownload.versionString
          override fun getDownloadDescription() = jdkToDownload.fullPresentationText

          override fun createTask(indicator: ProgressIndicator): SdkDownloadTask {
            val homeDir = JdkInstaller.defaultInstallDir(jdkToDownload)
            val request = JdkInstaller.prepareJdkInstallation(jdkToDownload, homeDir)
            return newDownloadTask(request)
          }
        }
      }

      val lazyLocalJdks by lazy {
        indicator.text = "Detecting local JDKs..."
        val result = mutableListOf<JavaLocalSdkFix>()

        SdkDetector.getInstance().detectSdks(sdkType, indicator, object : DetectedSdkListener {
          override fun onSdkDetected(type: SdkType, version: String, home: String) {
            val javaVersion = JavaVersion.tryParse(version) ?: return
            val suggestedName = JdkUtil.suggestJdkName(version) ?: return
            result += JavaLocalSdkFix(home, javaVersion, suggestedName)
          }
        })

        result
      }

      override fun proposeLocalFix(sdk: UnknownSdk, indicator: ProgressIndicator): UnknownSdkLocalSdkFix? {
        if (sdk.sdkType != sdkType) return null

        val req = JdkRequirements.parseRequirement(sdk) ?: return null
        LOG.info("Looking for a local SDK for ${sdk.sdkType.presentableName} with name ${sdk}")

        fun List<JavaLocalSdkFix>.pickBestMatch() = this.minBy { it.version }

        return tryUsingExistingSdk(req, sdk.sdkType, indicator).pickBestMatch()
               ?: lazyLocalJdks.filter { req.matches(it) }.pickBestMatch()
      }

      private fun tryUsingExistingSdk(req: JdkRequirement, sdkType: SdkType, indicator: ProgressIndicator): List<JavaLocalSdkFix> {
        indicator.text = "Checking existing SDKs..."
        return runReadAction { ProjectJdkTable.getInstance().allJdks }
          .filter { it.sdkType == sdkType }
          .filter { runCatching { req.matches(it) }.getOrNull() == true }
          //TODO: Existing SDK can be still downloading, thus we will skip it here :(
          .filter { runCatching { sdkType.isValidSdkHome(it.homePath) }.getOrNull() == true }
          .filter { runCatching { it.versionString != null }.getOrNull() == true }
          .mapNotNull {
            val homeDir = it.homePath ?: return@mapNotNull null
            val versionString = it.versionString ?: return@mapNotNull null
            val version = JavaVersion.tryParse(versionString) ?: return@mapNotNull null
            val suggestedName = JdkUtil.suggestJdkName(versionString) ?: return@mapNotNull null
            JavaLocalSdkFix(homeDir, version, suggestedName)
          }
      }
    }
  }

  private class JavaLocalSdkFix(val homeDir: String,
                                val version: JavaVersion,
                                val suggestedName: String) : UnknownSdkLocalSdkFix {
    override fun getExistingSdkHome() = homeDir
    override fun getVersionString() = JdkVersionDetector.formatVersionString(version)
    override fun getSuggestedSdkName() : String = suggestedName
  }
}
