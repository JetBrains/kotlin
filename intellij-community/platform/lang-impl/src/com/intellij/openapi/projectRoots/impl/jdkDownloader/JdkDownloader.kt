// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl.jdkDownloader

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.*
import com.intellij.openapi.projectRoots.SimpleJavaSdkType.notSimpleJavaSdkTypeIfAlternativeExistsAndNotDependentSdkType
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownload
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTask
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.registry.Registry
import java.util.function.Consumer
import javax.swing.JComponent

internal class JdkDownloader : SdkDownload, JdkDownloaderBase {
  private val LOG = logger<JdkDownloader>()

  override fun supportsDownload(sdkTypeId: SdkTypeId): Boolean {
    if (!Registry.`is`("jdk.downloader")) return false
    if (ApplicationManager.getApplication().isUnitTestMode) return false
    return notSimpleJavaSdkTypeIfAlternativeExistsAndNotDependentSdkType().value(sdkTypeId)
  }

  override fun showDownloadUI(sdkTypeId: SdkTypeId,
                              sdkModel: SdkModel,
                              parentComponent: JComponent,
                              selectedSdk: Sdk?,
                              sdkCreatedCallback: Consumer<SdkDownloadTask>) {
    val project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(parentComponent))
    if (project?.isDisposed == true) return

    val items = runTaskAndReportError(project,
                                      "Downloading the list of available JDKs...",
                                      "Failed to download the list of installable JDKs") {
      JdkListDownloader.getInstance().downloadForUI(it)
    } ?: return

    if (project?.isDisposed == true) return
    val (jdkItem, jdkHome) = JdkDownloadDialog(project, parentComponent, sdkTypeId, items).selectJdkAndPath() ?: return

    /// prepare the JDK to be installed (e.g. create home dir, write marker file)
    val request = runTaskAndReportError(project, "Preparing JDK target folder...", "Failed to prepare JDK installation to $jdkHome") {
      JdkInstaller.getInstance().prepareJdkInstallation(jdkItem, jdkHome)
    } ?: return

    sdkCreatedCallback.accept(newDownloadTask(request, project))
  }

  private inline fun <T : Any> runTaskAndReportError(project: Project?,
                                                     title: String,
                                                     errorMessage: String,
                                                     crossinline action: (ProgressIndicator) -> T): T? {
    val task = object : Task.WithResult<T?, Exception>(project, title, true) {
      override fun compute(indicator: ProgressIndicator): T? {
        try {
          return action(indicator)
        }
        catch (e: ProcessCanceledException) {
          throw e
        }
        catch (e: Exception) {
          val msg = "$errorMessage. ${e.message}"
          LOG.warn(msg, e)
          invokeLater {
            Messages.showMessageDialog(project,
                                       msg,
                                       JdkDownloadDialog.DIALOG_TITLE,
                                       Messages.getErrorIcon()
            )
          }
          return null
        }
      }
    }

    return ProgressManager.getInstance().run(task)
  }
}

internal interface JdkDownloaderBase {
  fun newDownloadTask(request: JdkInstallRequest, project: Project?): SdkDownloadTask {
    return object : SdkDownloadTask {
      override fun getSuggestedSdkName() = request.item.suggestedSdkName
      override fun getPlannedHomeDir() = request.javaHome.absolutePath
      override fun getPlannedVersion() = request.item.versionString
      override fun doDownload(indicator: ProgressIndicator) {
        JdkInstaller.getInstance().installJdk(request, indicator, project)
      }
    }
  }
}
