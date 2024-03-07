/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources

import org.gradle.util.GradleVersion

internal interface GradleVersionProvider {
    val current: GradleVersion

    companion object {
        const val EXTENSION_NAME = "_kgpGradleVersionProvider"
    }
}

internal class GradleVersionProviderImpl : GradleVersionProvider {
    override val current: GradleVersion get() = GradleVersion.current()
}

