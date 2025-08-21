/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.test

import org.jetbrains.kotlin.kapt.test.runners.AbstractKaptStubConverterTest
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.model.AbstractTestFacade
import org.jetbrains.kotlin.test.model.FrontendKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.ResultingArtifact

open class AbstractFirKaptStubConverterTest : AbstractKaptStubConverterTest() {
    override val frontendKind: FrontendKind<*> get() = FrontendKinds.FIR

    override val kaptFacade: Constructor<AbstractTestFacade<ResultingArtifact.Source, KaptContextBinaryArtifact>>
        get() = { JvmCompilerWithKaptFacade(it) }
}
