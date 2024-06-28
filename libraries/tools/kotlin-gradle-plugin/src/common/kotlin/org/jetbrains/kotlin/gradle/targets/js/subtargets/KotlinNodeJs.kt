/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.subtargets

import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsNodeDsl

@Suppress("DEPRECATION")
@Deprecated("The Kotlin/JS legacy target is deprecated and its support completely discontinued", level = DeprecationLevel.HIDDEN)
abstract class KotlinNodeJs :
    KotlinJsSubTarget(),
    KotlinJsNodeDsl