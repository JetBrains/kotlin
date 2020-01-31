// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl.jdkDownloader

import com.google.common.hash.Hashing
import com.google.common.io.Files
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.Urls
import com.intellij.util.io.HttpRequests
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.math.absoluteValue

data class JdkInstallRequest(
  val item: JdkItem,
  val targetDir: File
)

object JdkInstaller {
  private val LOG = logger<JdkInstaller>()

  fun defaultInstallDir(newVersion: JdkItem) : String {
    val installFolderName = newVersion.installFolderName

    val home = FileUtil.toCanonicalPath(System.getProperty("user.home") ?: ".")
    val targetDir = when {
      SystemInfo.isLinux ->  "$home/.jdks/$installFolderName"
      //see https://youtrack.jetbrains.com/issue/IDEA-206163#focus=streamItem-27-3270022.0-0
      SystemInfo.isMac ->  "$home/Library/Java/JavaVirtualMachines/$installFolderName"
      SystemInfo.isWindows -> "$home\\.jdks\\${installFolderName}"
      else -> error("Unsupported OS")
    }

    var count = 1
    var uniqueDir = targetDir
    while(File(uniqueDir).exists()) {
      uniqueDir = targetDir + "-" + count++
    }
    return FileUtil.toSystemDependentName(uniqueDir)
  }


  fun validateInstallDir(selectedPath: String): Pair<File?, String?> {
    if (selectedPath.isBlank()) return null to "Target path is empty"

    val targetDir = runCatching { File(FileUtil.expandUserHome(selectedPath)) }.getOrElse { t ->
      LOG.warn("Failed to resolve user path: $selectedPath. ${t.message}", t)
      return null to (t.message ?: "Failed to resolve path")
    }

    if (targetDir.isFile) return null to "Target path is an existing file"
    if (targetDir.isDirectory && targetDir.listFiles()?.isNotEmpty() == true) {
      return null to "Target path is an existing non-empty directory: $targetDir"
    }

    return targetDir to null
  }

  fun installJdk(request: JdkInstallRequest, indicator: ProgressIndicator?) {
    val item = request.item
    indicator?.text = "Installing ${item.fullPresentationText}..."

    val targetDir = request.targetDir
    val url = Urls.parse(item.url, false) ?: error("Cannot parse download URL: ${item.url}")
    if (!url.scheme.equals("https", ignoreCase = true)) error("URL must use https:// protocol, but was: $url")

    indicator?.text2 = "Downloading"
    val downloadFile = File(PathManager.getTempPath(), "jdk-${item.archiveFileName}")
    try {
      try {
        HttpRequests.request(item.url)
          .productNameAsUserAgent()
          .saveToFile(downloadFile, indicator)
      }
      catch (t: IOException) {
        throw RuntimeException("Failed to download JDK from $url. ${t.message}", t)
      }

      val sizeDiff = downloadFile.length() - item.archiveSize
      if (sizeDiff != 0L) {
        throw RuntimeException("Downloaded JDK distribution has incorrect size, difference is ${sizeDiff.absoluteValue} bytes")
      }

      val actualHashCode = Files.asByteSource(downloadFile).hash(Hashing.sha256()).toString()
      if (!actualHashCode.equals(item.sha256, ignoreCase = true)) {
        throw RuntimeException("SHA-256 checksums does not match. Actual value is $actualHashCode, expected ${item.sha256}")
      }

      indicator?.isIndeterminate = true
      indicator?.text2 = "Unpacking"

      val decompressor = item.packageType.openDecompressor(downloadFile)
      //handle cancellation via postProcessor (instead of inheritance)
      decompressor.postprocessor { indicator?.checkCanceled() }

      val fullMatchPath = item.unpackPrefixFilter.trim('/')
      if (!fullMatchPath.isBlank()) {
        decompressor.removePrefixPath(fullMatchPath)
      }
      decompressor.extract(targetDir)

      writeMarkerFile(request)
    }
    catch (t: Throwable) {
      //if we were cancelled in the middle or failed, let's clean up
      FileUtil.delete(targetDir)
      if (t is ProcessCanceledException) throw t
      if (t is IOException) throw RuntimeException("Failed to extract JDK package", t)
      throw t
    }
    finally {
      FileUtil.delete(downloadFile)
    }
  }

  /**
   * executed synchronously to prepare Jdk installation process, that would run in the future
   */
  fun prepareJdkInstallation(jdkItem: JdkItem, targetPath: String): JdkInstallRequest {
    val (home, error) = validateInstallDir(targetPath)
    if (home == null || error != null) throw RuntimeException(error ?: "Invalid Target Directory")

    FileUtil.createDirectory(home)
    if (!home.isDirectory) {
      throw IOException("Failed to create home directory: $home")
    }

    val request = JdkInstallRequest(jdkItem, home)
    writeMarkerFile(request)
    return request
  }

  private fun writeMarkerFile(request: JdkInstallRequest) {
    val markerFile = File(request.targetDir, "intellij-downloader-info.txt")
    markerFile.writeText("Download started on ${Date()}\n${request.item}")
  }
}

