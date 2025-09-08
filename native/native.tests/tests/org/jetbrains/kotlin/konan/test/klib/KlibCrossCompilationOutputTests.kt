/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.group.ClassicPipeline

@ClassicPipeline()
@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
@EnforcedProperty(ClassLevelProperty.TEST_TARGET, "ios_arm64")
class ClassicFEKlibCrossCompilationOutputTest : KlibCrossCompilationOutputTest()

@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
@EnforcedProperty(ClassLevelProperty.TEST_TARGET, "ios_arm64")
class FirKlibCrossCompilationOutputTest : KlibCrossCompilationOutputTest()
