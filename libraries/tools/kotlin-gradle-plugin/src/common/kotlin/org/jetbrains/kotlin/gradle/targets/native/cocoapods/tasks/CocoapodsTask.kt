/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("LeakingThis", "PackageDirectoryMismatch") // All tasks should be inherited only by Gradle, Old package for compatibility

package org.jetbrains.kotlin.gradle.targets.native.tasks

import org.gradle.api.DefaultTask
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.CocoapodsDependency
import org.jetbrains.kotlin.konan.target.HostManager

val CocoapodsDependency.schemeName: String
    get() = name.split("/")[0]

open class CocoapodsTask : DefaultTask() {
    init {
        onlyIf {
            HostManager.hostIsMac
        }
    }
}
