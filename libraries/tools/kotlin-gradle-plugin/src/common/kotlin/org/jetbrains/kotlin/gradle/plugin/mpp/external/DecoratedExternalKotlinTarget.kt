/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.external

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

@ExternalKotlinTargetApi
abstract class DecoratedExternalKotlinTarget(
    internal val delegate: ExternalKotlinTarget
) : KotlinTarget by delegate {
    internal val logger: Logger = Logging.getLogger("${ExternalKotlinTarget::class.qualifiedName}: $name")
}