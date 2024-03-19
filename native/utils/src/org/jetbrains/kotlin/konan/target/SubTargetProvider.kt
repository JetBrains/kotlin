/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

/**
 * Note to Kotlin team developers, why we can't remove or elevate the DeprecationLevel:
 *
 * - [HostManager] constructors should be preserved at least with the level = HIDDEN
 *   because Gradle Plugins saw this API through KGP and compiled against it.
 *   Removing constructors entirely breaks linkage of older Gradle Plugins against newer KGP.
 *   We're fine with level = HIDDEN because source-usages should be adjusted anyways (most of them
 *   actually used the default constructor so source-compatibility is preserved)
 *
 * - But these constructors use [SubTargetProvider] in its signature, so we can't put level higher
 *   than WARNING here (otherwise the kotlin.git code won't compile)
 */
@Deprecated(
    level = DeprecationLevel.WARNING,
    message = "K/N subtargets are removed. You can freely remove the usages of SubTargetProvider from your code"
)
interface SubTargetProvider {
    fun availableSubTarget(genericName: String): List<String>

    @Suppress("DEPRECATION")
    object NoSubTargets : SubTargetProvider {
        override fun availableSubTarget(genericName: String): List<String> = emptyList()
    }
}
