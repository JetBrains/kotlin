/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilationFactory

@Deprecated("The Kotlin/JS legacy target is deprecated and its support completely discontinued")
typealias KotlinJsCompilationFactory = KotlinJsIrCompilationFactory