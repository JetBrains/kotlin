/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.samWithReceiver

import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.checkers.AbstractDiagnosticsTest
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor

@OptIn(ObsoleteTestInfrastructure::class)
abstract class AbstractSamWithReceiverTest : AbstractDiagnosticsTest() {
    private companion object {
        private val TEST_ANNOTATIONS = listOf("SamWithReceiver")
    }

    override fun setupEnvironment(environment: KotlinCoreEnvironment) {
        StorageComponentContainerContributor.registerExtension(
            environment.project,
            CliSamWithReceiverComponentContributor(TEST_ANNOTATIONS)
        )
    }
}
