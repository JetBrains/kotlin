/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

interface SubTargetProvider {
    fun availableSubTarget(genericName: String): List<String>

    object NoSubTargets : SubTargetProvider {
        override fun availableSubTarget(genericName: String): List<String> = emptyList()
    }
}