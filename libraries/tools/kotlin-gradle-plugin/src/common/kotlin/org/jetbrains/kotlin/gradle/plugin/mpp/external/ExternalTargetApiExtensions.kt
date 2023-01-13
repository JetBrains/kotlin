/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.external

import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.tooling.core.MutableExtras

@ExternalKotlinTargetApi
val KotlinSourceSet.extras: MutableExtras get() = this.internal.extras

@ExternalKotlinTargetApi
val KotlinCompilation<*>.extras: MutableExtras get() = this.internal.extras

@ExternalKotlinTargetApi
val KotlinTarget.extras: MutableExtras get() = this.internal.extras
