/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

/**
 * Generates additional properties based on a setup file and local environment. Should be stateless.
 */
internal interface SyntheticPropertiesGenerator {
    fun generate(setupFile: SetupFile): Map<String, String>
}