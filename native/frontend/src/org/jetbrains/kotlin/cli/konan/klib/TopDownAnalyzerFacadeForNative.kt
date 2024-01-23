/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.konan.klib

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext

// TODO: The implementation is still in `kotlin-native` module. Consider moving it here.
interface TopDownAnalyzerFacadeForNative {
    fun checkForErrors(allFiles: Collection<KtFile>, bindingContext: BindingContext): Boolean
}
