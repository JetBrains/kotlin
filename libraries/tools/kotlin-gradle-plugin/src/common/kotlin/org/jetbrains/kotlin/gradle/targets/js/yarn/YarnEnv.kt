/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.gradle.targets.js.AbstractEnv
import java.io.File

  class YarnEnv internal constructor(
      override val download: Boolean,
      override val downloadBaseUrl: String?,
      override val allowInsecureProtocol: Boolean,
      override val dir: File,
      override val executable: String,
      override val ivyDependency: String,
      val ignoreScripts: Boolean,
      val yarnLockMismatchReport: YarnLockMismatchReport,
      val reportNewYarnLock: Boolean,
      val yarnLockAutoReplace: Boolean,
//    val yarnResolutions: List<YarnResolution>,
      val yarnResolutions: NamedDomainObjectContainer<YarnResolutionSpec>,
) : AbstractEnv {
    val standalone: Boolean
        get() = !download
}
