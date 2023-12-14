/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin

/**
 * Built-in function used to mark a boolean predicate to be verified in Viper.
 * This function hooks-in in the `formver` plugin, its invocation in a Kotlin
 * program does not do anything.
 */
fun verify(@Suppress("UNUSED_PARAMETER") predicate: Boolean) = Unit