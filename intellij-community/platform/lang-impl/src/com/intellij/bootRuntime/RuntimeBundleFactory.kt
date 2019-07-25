// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bootRuntime

import com.intellij.bootRuntime.bundles.Local
import com.intellij.bootRuntime.bundles.Remote
import com.intellij.bootRuntime.bundles.Runtime
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import java.io.File
import java.net.URL
import java.util.regex.Pattern

private val WINDOWS_X64_JVM_LOCATION = File.listRoots().map { r ->  File(r.absolutePath + "Program Files/Java") }.toList()
private val WINDOWS_X86_JVM_LOCATION = File.listRoots().map { r ->  File(r.absolutePath + "Program Files (x86)/Java") }.toList()
private val MAC_OS_JVM_LOCATIONS = File.listRoots().map { r ->  File(r.absolutePath + "/Library/Java/JavaVirtualMachines") }.toList()
private val LINUX_JVM_LOCATIONS = File.listRoots().flatMap { r -> listOf(File(r.absolutePath + "/usr/lib/jvm"),
                                                                         File(r.absolutePath + "/usr/java"))}.toList()

enum class OperationSystem {
  Windows32, Windows64, Linux, MacOSX
}

fun javaHomeFromFile(file:File): File {
  return file.parentFile.parentFile
}

fun javaHomeToInstallationLocation(file:File): File {
  if (SystemInfo.isMac) {
    return file.parentFile.parentFile
  } else {
    return file
  }
}

class RuntimeLocationsFactory {

  fun runtimesFrom (locations: List<File>) : List<File> {
    return locations.flatMap{ l -> l.walk().filter {
      file -> file.name == "tools.jar" ||
              file.name == "jrt-fs.jar"
    }.map{ file -> javaHomeToInstallationLocation(javaHomeFromFile(file)) }.toList()}.toList()
  }

  fun runtimeLocations(operationSystem: OperationSystem): List<File> {
    return when (operationSystem) {
      OperationSystem.Windows32, OperationSystem.Windows64 -> WINDOWS_X86_JVM_LOCATION + WINDOWS_X64_JVM_LOCATION
      OperationSystem.Linux -> LINUX_JVM_LOCATIONS
      OperationSystem.MacOSX -> MAC_OS_JVM_LOCATIONS
    }
  }

  fun bundlesFromLocations(project: Project, locations: List<File>): List<Runtime> {
      return locations.map { location -> Local(project, location) }.toList()
  }

  fun operationSystem() : OperationSystem {
    return when  {
      SystemInfo.is64Bit && SystemInfo.isWindows -> OperationSystem.Windows64
      SystemInfo.is32Bit && SystemInfo.isWindows -> OperationSystem.Windows32
      SystemInfo.isMac -> OperationSystem.MacOSX
      else -> OperationSystem.Linux
    }
  }

  fun localBundles(project: Project) : List<Runtime> {
    return bundlesFromLocations(project, runtimesFrom(runtimeLocations(operationSystem())))
  }

  fun bintrayBundles(project: Project): List<Runtime> {

    val subject = BinTrayConfig.subject
    val repoName = BinTrayConfig.repoName
    val jbrRepoName = BinTrayConfig.jbrRepoName
    val linkTemplate = "https://dl.bintray.com/%s/%s";

    val runtimes = collectRuntimes(String.format(linkTemplate, subject, repoName), project, ".*\"(jbsdk.*%s.*?)\"")
    runtimes.addAll((collectRuntimes(String.format(linkTemplate, subject, jbrRepoName), project, ".*\".*?(jbrsdk.*%s.*?)\"")))

    return runtimes
  }

  private fun collectRuntimes(link: String,
                              project: Project,
                              bundleNamePattern: String): MutableList<Runtime> {
    val response = URL(link).readText()

    var osFilter = ""
    if (SystemInfo.isMac) {
      osFilter = "osx"
    }
    else if (SystemInfo.isLinux) {
      osFilter = "linux"
    }
    else if (SystemInfo.isWindows) {
      osFilter = "win"
    }

    val r = Pattern.compile(String.format(bundleNamePattern, osFilter))
    val m = r.matcher(response)

    val list = mutableListOf<Runtime>()

    while (m.find()) {
      list.add(Remote(project, m.group(1)))
    }

    return list
  }
}


