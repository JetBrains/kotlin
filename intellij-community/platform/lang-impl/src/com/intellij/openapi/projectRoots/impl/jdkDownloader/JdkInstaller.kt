// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl.jdkDownloader

import com.google.common.hash.Hashing
import com.google.common.io.Files
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectBundle
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.Urls
import com.intellij.util.io.HttpRequests
import org.jetbrains.annotations.Nls
import java.io.File
import java.io.IOException
import kotlin.math.absoluteValue

interface JdkInstallRequest {
  val item: JdkItem

  /**
   * The path where JDK is installed.
   * On macOS it is likely (depending on the JDK package)
   * to contain Contents/Home folders
   */
  val installDir: File

  /**
   * The path on the disk where the installed JDK
   * would have the bin/java and bin/javac files.
   *
   * On macOs this path may differ from the [installDir]
   * if the JDK package follows the macOS Bundle layout
   */
  val javaHome: File
}

private val JDK_INSTALL_LISTENER_EP_NAME = ExtensionPointName.create<JdkInstallerListener>("com.intellij.jdkDownloader.jdkInstallerListener")

interface JdkInstallerListener {
  /**
   * Executed at the moment, when a download process for
   * a given [request] is started
   */
  @JvmDefault
  fun onJdkDownloadStarted(request: JdkInstallRequest, project: Project?) { }

  /**
   * This event is executed when download process is finished,
   * for all possible outcomes, no matter it was a success or a failure
   */
  @JvmDefault
  fun onJdkDownloadFinished(request: JdkInstallRequest, project: Project?) { }
}

class JdkInstaller {
  companion object {
    @JvmStatic
    fun getInstance() = service<JdkInstaller>()
  }

  private val LOG = logger<JdkInstaller>()

  private operator fun File.div(path: String) = File(this, path).absoluteFile

  fun defaultInstallDir() : File {
    val home = File(FileUtil.toCanonicalPath(System.getProperty("user.home") ?: "."))
    return when {
      SystemInfo.isLinux   -> home / ".jdks"
      //see https://youtrack.jetbrains.com/issue/IDEA-206163#focus=streamItem-27-3270022.0-0
      SystemInfo.isMac     -> home / "Library" / "Java" / "JavaVirtualMachines"
      SystemInfo.isWindows -> home / ".jdks"
      else -> error("Unsupported OS: ${SystemInfo.getOsNameAndVersion()}")
    }
  }

  fun defaultInstallDir(newVersion: JdkItem) : File {
    val targetDir = defaultInstallDir() / newVersion.installFolderName

    var count = 1
    var uniqueDir = targetDir
    while(uniqueDir.exists()) {
      uniqueDir = File(targetDir.path + "-" + count++)
    }
    return uniqueDir.absoluteFile
  }

  fun validateInstallDir(selectedPath: String): Pair<File?, @Nls String?> {
    if (selectedPath.isBlank()) return null to ProjectBundle.message("dialog.message.error.target.path.empty")

    val targetDir = runCatching { File(FileUtil.expandUserHome(selectedPath)) }.getOrElse { t ->
      LOG.warn("Failed to resolve user path: $selectedPath. ${t.message}", t)
      return null to ProjectBundle.message("dialog.message.error.resolving.path")
    }

    if (targetDir.isFile) return null to ProjectBundle.message("dialog.message.error.target.path.exists.file")
    if (targetDir.isDirectory && targetDir.listFiles()?.isNotEmpty() == true) {
      return null to ProjectBundle.message("dialog.message.error.target.path.exists.nonEmpty.dir")
    }

    return targetDir to null
  }

  /**
   * @see [JdkInstallRequest.javaHome] for the actual java home, it may not match the [JdkInstallRequest.installDir]
   */
  fun installJdk(request: JdkInstallRequest, indicator: ProgressIndicator?, project: Project?) {
    JDK_INSTALL_LISTENER_EP_NAME.forEachExtensionSafe { it.onJdkDownloadStarted(request, project) }

    val item = request.item
    indicator?.text = ProjectBundle.message("progress.text.installing.jdk.1", item.fullPresentationText)

    val targetDir = request.installDir
    val url = Urls.parse(item.url, false) ?: error("Cannot parse download URL: ${item.url}")
    if (!url.scheme.equals("https", ignoreCase = true)) error("URL must use https:// protocol, but was: $url")

    indicator?.text2 = ProjectBundle.message("progress.text2.downloading.jdk")
    val downloadFile = File(PathManager.getTempPath(), "jdk-${item.archiveFileName}")
    try {
      try {
        HttpRequests.request(item.url)
          .productNameAsUserAgent()
          .saveToFile(downloadFile, indicator)
      }
      catch (t: Throwable) {
        if (t is ControlFlowException) throw t
        throw RuntimeException("Failed to download ${item.fullPresentationText} from $url. ${t.message}", t)
      }

      val sizeDiff = downloadFile.length() - item.archiveSize
      if (sizeDiff != 0L) {
        throw RuntimeException("The downloaded ${item.fullPresentationText} has incorrect file size,\n" +
                               "the difference is ${sizeDiff.absoluteValue} bytes.\n" +
                               "Check your internet connection and try again later")
      }

      val actualHashCode = Files.asByteSource(downloadFile).hash(Hashing.sha256()).toString()
      if (!actualHashCode.equals(item.sha256, ignoreCase = true)) {
        throw RuntimeException("Failed to verify SHA-256 checksum for ${item.fullPresentationText}\n\n" +
                               "The actual value is $actualHashCode,\n" +
                               "but expected ${item.sha256} was expected\n" +
                               "Check your internet connection and try again later")
      }

      indicator?.isIndeterminate = true
      indicator?.text2 = ProjectBundle.message("progress.text2.unpacking.jdk")

      try {
        val decompressor = item.packageType.openDecompressor(downloadFile)
        //handle cancellation via postProcessor (instead of inheritance)
        decompressor.postprocessor { indicator?.checkCanceled() }

        val fullMatchPath = item.packageRootPrefix.trim('/')
        if (!fullMatchPath.isBlank()) {
          decompressor.removePrefixPath(fullMatchPath)
        }
        decompressor.extract(targetDir)

        writeMarkerFile(request)
      } catch (t: Throwable) {
        if (t is ControlFlowException) throw t
        throw RuntimeException("Failed to extract ${item.fullPresentationText}. ${t.message}", t)
      }
    }
    catch (t: Throwable) {
      //if we were cancelled in the middle or failed, let's clean up
      FileUtil.delete(targetDir)
      FileUtil.delete(markerFile(targetDir))
      throw t
    }
    finally {
      runCatching { FileUtil.delete(downloadFile) }
      JDK_INSTALL_LISTENER_EP_NAME.forEachExtensionSafe { it.onJdkDownloadFinished(request, project) }
    }
  }

  /**
   * executed synchronously to prepare Jdk installation process, that would run in the future
   */
  fun prepareJdkInstallation(jdkItem: JdkItem, targetPath: File): JdkInstallRequest {
    val (home, error) = validateInstallDir(targetPath.path)
    if (home == null || error != null) throw RuntimeException(error ?: "Invalid Target Directory")

    FileUtil.createDirectory(home)
    if (!home.isDirectory) {
      throw IOException("Failed to create home directory: $home")
    }

    val javaHome = when {
      jdkItem.packageToBinJavaPrefix.isBlank() -> targetPath
      else -> File(targetPath, jdkItem.packageToBinJavaPrefix).absoluteFile
    }

    FileUtil.createDirectory(javaHome)
    if (!javaHome.isDirectory) {
      throw IOException("Failed to create home directory: $javaHome")
    }

    val request = object: JdkInstallRequest {
      override val item = jdkItem
      override val installDir = targetPath
      override val javaHome = javaHome
    }
    writeMarkerFile(request)
    return request
  }

  private fun markerFile(installDir: File) = File(installDir.parent, ".${installDir.name}.intellij")

  private fun writeMarkerFile(request: JdkInstallRequest) {
    val installDir = request.installDir
    val markerFile = markerFile(installDir)
    try {
      request.item.writeMarkerFile(markerFile)
    } catch (t: Throwable) {
      if (t is ControlFlowException) throw t
      LOG.warn("Failed to write marker file to $markerFile. ${t.message}", t)
    }
  }

  fun findJdkItemForInstalledJdk(jdkHome: String) = findJdkItem(File(jdkHome))

  private fun findJdkItem(jdkHome: File): JdkItem? {
    // Java package install dir have several folders up from it, e.g. Contents/Home on macOS
    val markerFile = generateSequence(jdkHome, { file -> file.parentFile })
                       .takeWhile { it.isDirectory }
                       .take(5)
                       .map { markerFile(it) }
                       .firstOrNull { it.isFile } ?: return null
    try {
      val json = JdkListParser.readTree(markerFile.readBytes())
      return JdkListParser.parseJdkItem(json, JdkPredicate.createInstance())
    } catch (e: Throwable) {
      return null
    }
  }
}
