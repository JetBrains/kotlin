/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport

import org.jetbrains.kotlin.test.model.BinaryKind
import org.jetbrains.kotlin.test.model.ResultingArtifact
import java.io.File

internal data class SwiftExportArtifact(
    val swift: File,
    val cHeader: File,
    val ktBridge: File,
) : ResultingArtifact.Binary<SwiftExportArtifact>() {
    object Kind : BinaryKind<SwiftExportArtifact>("SwiftExportArtifact")

    override val kind: BinaryKind<SwiftExportArtifact>
        get() = Kind
}