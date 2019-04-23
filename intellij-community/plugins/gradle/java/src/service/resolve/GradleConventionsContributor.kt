// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.*

@Deprecated("to attract attention, this is going to be removed")
class GradleConventionsContributor {

  companion object {
    val conventions = arrayOf(
      GRADLE_API_BASE_PLUGIN_CONVENTION,
      GRADLE_API_JAVA_PLUGIN_CONVENTION,
      GRADLE_API_APPLICATION_PLUGIN_CONVENTION,
      GRADLE_API_WAR_CONVENTION
    )
  }
}
