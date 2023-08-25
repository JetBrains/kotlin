/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")
package org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util

import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.utils.targets

@Deprecated("Scheduled for removal in Kotlin 2.1", ReplaceWith("targets", "org.jetbrains.kotlin.gradle.utils.targets"))
val KotlinProjectExtension.targets: Iterable<KotlinTarget> get() = targets
