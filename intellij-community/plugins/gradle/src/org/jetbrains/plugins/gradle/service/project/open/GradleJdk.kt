// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project.open

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.projectRoots.JdkUtil
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.util.lang.JavaVersion
import org.gradle.util.GradleVersion

internal data class GradleJdk(val homePath: String, val version: JavaVersion, val name: String) {

  fun isSupported(gradleVersion: GradleVersion): Boolean {
    return when {
      gradleVersion >= GradleVersion.version("5.4.1") -> version.feature in 8..12
      gradleVersion >= GradleVersion.version("5.0") -> version.feature in 8..11
      gradleVersion >= GradleVersion.version("4.1") -> version.feature in 7..9
      gradleVersion >= GradleVersion.version("4.0") -> version.feature in 7..8
      else -> version.feature in 6..8
    }
  }

  companion object {
    @JvmStatic
    fun valueOf(sdk: Sdk): GradleJdk? {
      val homePath = sdk.homePath ?: return null
      val versionString = sdk.versionString ?: return null
      return valueOf(homePath, versionString, sdk.name)
    }

    @JvmStatic
    fun valueOf(homePath: String): GradleJdk? {
      val javaSdkType = ExternalSystemJdkUtil.getJavaSdkType()
      val versionString = javaSdkType.getVersionString(homePath) ?: return null
      val name = JdkUtil.suggestJdkName(versionString) ?: return null
      return valueOf(homePath, versionString, name)
    }

    @JvmStatic
    fun valueOf(homePath: String, versionString: String, name: String): GradleJdk? {
      if (!JdkUtil.checkForJdk(homePath)) return null
      val version = JavaVersion.tryParse(versionString) ?: return null
      return GradleJdk(homePath, version, name)
    }
  }
}