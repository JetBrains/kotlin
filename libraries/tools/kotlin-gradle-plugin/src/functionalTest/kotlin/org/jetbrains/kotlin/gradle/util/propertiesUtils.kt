/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.extraProperties

// TODO(Dmitrii Krasnov): we can remove this, when downloading konan from maven local will be possible KT-63198
internal fun disableDownloadingKonanFromMavenCentral(project: Project) {
    project.extraProperties.set("kotlin.native.distribution.downloadFromMaven", "false")
}