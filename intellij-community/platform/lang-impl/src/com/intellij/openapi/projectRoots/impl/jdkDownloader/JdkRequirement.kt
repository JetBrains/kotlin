// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl.jdkDownloader

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.UnknownSdkLocalSdkFix
import com.intellij.util.lang.JavaVersion
import java.util.function.Predicate


interface JdkRequirement {
  fun matches(sdk: Sdk): Boolean
  fun matches(sdk: JdkItem): Boolean
  fun matches(sdk: UnknownSdkLocalSdkFix): Boolean
}

object JdkRequirements {
  private val LOG = logger<JdkRequirements>()

  private interface VersionMatcher {
    fun matchVersion(versionString: String) : Boolean
    override fun toString(): String
  }

  private fun sameMajorVersionMatcher(parsed: JavaVersion): VersionMatcher {
    return object: VersionMatcher {
      override fun toString() = "it >= $parsed && same major version"
      override fun matchVersion(versionString: String): Boolean {
        val it = JavaVersion.tryParse(versionString) ?: return false
        return it >= parsed && it.feature == parsed.feature
      }
    }
  }

  private fun strictVersionMatcher(parsed: JavaVersion): VersionMatcher {
    return object: VersionMatcher {
      override fun toString() = "it == $parsed"
      override fun matchVersion(versionString: String): Boolean {
        val it = JavaVersion.tryParse(versionString) ?: return false
        return it == parsed
      }
    }
  }

  fun parseRequirement(namePredicate: String?,
                       versionStringPredicate: Predicate<String>?,
                       homePredicate: Predicate<String>?) : JdkRequirement? {
    val nameFilter = namePredicate?.let { parseRequirement(it) }

    if (versionStringPredicate == null && nameFilter == null) {
      // if no other requirements were specified, and the name filter failed,
      // we should not attempt any other suggestions - e.g. for `IDEA jdk`
      return null
    }

    return object: JdkRequirement {
      override fun matches(sdk: Sdk): Boolean {
        if (nameFilter != null) {
          if (!nameFilter.matches(sdk)) return false
        }

        if (homePredicate != null) {
          val homePath = sdk.homePath ?: return false
          if (!homePredicate.test(homePath)) return false
        }

        if (versionStringPredicate != null) {
          val versionString = sdk.versionString ?: return false
          if (!versionStringPredicate.test(versionString)) return false
        }

        return true
      }

      override fun matches(sdk: JdkItem) : Boolean {
        if (nameFilter != null) {
          if (!nameFilter.matches(sdk)) return false
        }

        //NOTE: There is no way to test for SdkHome for an to-be-downloaded item

        if (versionStringPredicate != null) {
          val versionString = sdk.versionString
          if (!versionStringPredicate.test(versionString)) return false
        }

        return true
      }

      override fun matches(sdk: UnknownSdkLocalSdkFix): Boolean {
        if (nameFilter != null) {
          if (!nameFilter.matches(sdk)) return false
        }

        if (homePredicate != null) {
          val homePath = sdk.existingSdkHome
          if (!homePredicate.test(homePath)) return false
        }

        if (versionStringPredicate != null) {
          val versionString = sdk.versionString
          if (!versionStringPredicate.test(versionString)) return false
        }

        return true
      }

      override fun toString() =
        sequence {
          if (nameFilter != null) {
            yield("$nameFilter")
          }
          if (homePredicate != null) {
            yield("homePredicate: $homePredicate")
          }
          if (versionStringPredicate != null) {
            yield("versionPredicate: $versionStringPredicate")
          }
        }.joinToString(", ", prefix = "JdkRequirement:{ ", postfix = "}")
    }
  }

  private val JAVA_VERSION_REGEX = Regex("^" +
                                         "(9|[1-9][1-9]|)(\\.0(\\.\\d+)?)?" +
                                         "|" +
                                         "1\\.\\d(\\.0(_\\d+)?)?" +
                                         "|" +
                                         "8" +
                                         "\$")

  fun parseRequirement(request: String): JdkRequirement? {
    try {
      val versionMatcher = if (request.trim().startsWith("=")) ::strictVersionMatcher else ::sameMajorVersionMatcher
      val text = request.trimStart('=').trim()

      //case 1. <vendor>-<version>
      run {
        val idx = text.lastIndexOfAny(charArrayOf('-', ' '))
        if (idx < 0 || idx + 1 >= text.length) return@run
        val vendor = text.substring(0, idx)
        val version = text.substring(idx + 1)
        if (!version.matches(JAVA_VERSION_REGEX)) return@run
        val javaVersion = JavaVersion.tryParse(version) ?: return@run
        val matcher = versionMatcher(javaVersion)

        return object : VersionRequirement(matcher) {
          fun findJdkItem(home: String) = JdkInstaller.getInstance().findJdkItemForInstalledJdk(home)
          fun findJdkItem(sdk: Sdk) = sdk.homePath?.let { findJdkItem(it) }
          fun findJdkItem(sdk: UnknownSdkLocalSdkFix) = findJdkItem(sdk.existingSdkHome)

          override fun matches(sdk: Sdk) = super.matches(sdk) && findJdkItem(sdk)?.matchesVendor(vendor) == true
          override fun matches(sdk: UnknownSdkLocalSdkFix) = super.matches(sdk) && findJdkItem(sdk)?.matchesVendor(vendor) == true
          override fun matches(sdk: JdkItem) = super.matches(sdk) && sdk.matchesVendor(vendor)

          override fun toString() = "JdkRequirement { $vendor && $matcher }"
        }
      }

      //case 2. It is just a version
      run {
        if (!text.matches(JAVA_VERSION_REGEX)) return@run
        val javaVersion = JavaVersion.tryParse(text) ?: return@run
        val matcher = versionMatcher(javaVersion)
        return object : VersionRequirement(matcher) {
          override fun toString() = "JdkRequirement { $matcher }"
        }
      }
    }
    catch (t: Throwable) {
      LOG.warn("Failed to parse requirement $request. ${t.message}", t)
    }
    return null
  }

  private open class VersionRequirement(val matcher: VersionMatcher) : JdkRequirement {
    fun matches(version: String) = matcher.matchVersion(version)
    override fun matches(sdk: Sdk) = runCatching { sdk.versionString }.getOrNull()?.let { matches(it) } == true
    override fun matches(sdk: JdkItem) = matches(sdk.versionString)
    override fun matches(sdk: UnknownSdkLocalSdkFix) = matcher.matchVersion(sdk.versionString)
  }
}
