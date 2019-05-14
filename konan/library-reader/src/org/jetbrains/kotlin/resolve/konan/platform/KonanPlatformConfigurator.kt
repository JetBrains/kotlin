/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.platform

import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.jvm.checkers.SuperCallWithDefaultArgumentsChecker

object KonanPlatformConfigurator : PlatformConfiguratorBase(
    additionalDeclarationCheckers = listOf(ExpectedActualDeclarationChecker()),
    additionalCallCheckers = listOf(SuperCallWithDefaultArgumentsChecker())
) {
    override fun configureModuleComponents(container: StorageComponentContainer) {
    }
}
