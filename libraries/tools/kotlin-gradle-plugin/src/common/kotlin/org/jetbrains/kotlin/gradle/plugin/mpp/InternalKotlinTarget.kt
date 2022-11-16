/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

internal interface InternalKotlinTarget : KotlinTarget

internal val KotlinTarget.internal: InternalKotlinTarget
    get() = (this as? InternalKotlinTarget) ?: throw IllegalArgumentException(
        "KotlinTarget($name) ${this::class} does not implement ${InternalKotlinTarget::class}"
    )