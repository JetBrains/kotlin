/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.api.invocation.Gradle

/**
 * Configures some default factories that are usually automatically registered in
 * [org.jetbrains.kotlin.gradle.plugin.DefaultKotlinBasePlugin.apply]
 *
 * This function can be used in some minimal tests that do not apply the full KGP plugin but still touch
 * some parts of its code
 */
fun Gradle.registerMinimalVariantImplementationFactoriesForTests() {
}
