/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib.impl

import kotlinx.metadata.impl.ReadContextExtension
import kotlinx.metadata.klib.KlibSourceFile

class SourceFileIndexReadExtension(
    private val files: List<KlibSourceFile>
) : ReadContextExtension {
    fun getSourceFile(index: Int): KlibSourceFile = files[index]
}