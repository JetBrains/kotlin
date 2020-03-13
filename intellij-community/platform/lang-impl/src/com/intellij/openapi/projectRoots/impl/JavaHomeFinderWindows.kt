// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:Suppress("ConvertSecondaryConstructorToPrimary", "UnnecessaryVariable")

package com.intellij.openapi.projectRoots.impl

import com.intellij.openapi.util.Bitness
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.WindowsRegistryUtil
import java.io.File
import java.util.*
import kotlin.text.RegexOption.IGNORE_CASE
import kotlin.text.RegexOption.MULTILINE

class JavaHomeFinderWindows : JavaHomeFinderBasic {

  companion object {

    @Suppress("SpellCheckingInspection")
    private const val regCommand = """reg query HKLM\SOFTWARE\JavaSoft\JDK /s /v JavaHome"""

    private val javaHomePattern = Regex("""^\s+JavaHome\s+REG_SZ\s+(\S.+\S)\s*$""", setOf(MULTILINE, IGNORE_CASE))

    /**
     * Whether the OS is 64-bit. Don't mix with JRE.
     * SIC! it's not the same as [SystemInfo.is64Bit].
     */
    private val os64bit: Boolean = !System.getenv("ProgramFiles(x86)").isNullOrBlank()

    fun gatherHomePaths(text: CharSequence): Set<String> {
      val paths = TreeSet<String>()
      var m: MatchResult? = javaHomePattern.find(text)
      while (m != null) {
        m.groups[1]?.run { paths += value }
        m = m.next()
      }
      return paths
    }

  }

  constructor(forceEmbeddedJava: Boolean) : super(forceEmbeddedJava) {
    registerFinder(this::findAllLocations)
  }

  private fun findAllLocations(): Set<String> {
    val paths = mutableSetOf<String>()
    if (os64bit && SystemInfo.isWin7OrNewer) {
      paths.addAll(readRegisteredLocations(Bitness.x64))
      paths.addAll(readRegisteredLocations(Bitness.x32))
    }
    else {
      paths.addAll(readRegisteredLocations(null))
    }
    paths.addAll(guessPossibleLocations())
    return paths
  }

  private fun readRegisteredLocations(b: Bitness?): Set<String> {
    val cmd =
      when (b) {
        null -> regCommand
        Bitness.x32 -> "$regCommand /reg:32"
        Bitness.x64 -> "$regCommand /reg:64"
      }
    val registryLines: CharSequence? = WindowsRegistryUtil.readRegistry(cmd)
    registryLines ?: return emptySet()
    val paths = gatherHomePaths(registryLines)
    return paths
  }

  private fun guessPossibleLocations(): Set<String> {
    val fsRoots = File.listRoots() ?: return emptySet()
    val roots: MutableSet<File> = HashSet()
    for (root in fsRoots) {
      if (!root.exists()) continue
      roots.add(File(File(root, "Program Files"), "Java"))
      roots.add(File(File(root, "Program Files (x86)"), "Java"))
      roots.add(File(root, "Java"))
    }
    return scanAll(roots, true)
  }

}
