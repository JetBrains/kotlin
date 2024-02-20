/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.xcode

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.util.DependencyProcessor

open class XcodeOverridePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Only for macOS hosts.
        if (!HostManager.hostIsMac)
            return
        // Only if we use regular Xcode installation.
        if (DependencyProcessor.isInternalSeverAvailable)
            return
        val xcodeProvider: Provider<Xcode> = project.providers.of(XcodeValueSource::class) {}
        Xcode.xcodeOverride = xcodeProvider.get()
    }
}
