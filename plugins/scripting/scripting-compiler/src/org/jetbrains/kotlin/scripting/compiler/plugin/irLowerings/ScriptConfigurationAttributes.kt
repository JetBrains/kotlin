/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.irLowerings

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrReplSnippet
import org.jetbrains.kotlin.ir.declarations.IrScript
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.script.experimental.api.ScriptCompilationConfiguration

data class ScriptResultFieldData(
    val scriptClassName: FqName,
    val fieldName: Name,
    val fieldTypeName: String,
)

var IrClass.scriptResultFieldDataAttr: ScriptResultFieldData? by irAttribute(copyByDefault = true)

var IrScript.scriptCompilationConfiguration: ScriptCompilationConfiguration? by irAttribute(copyByDefault = true)

/**
 * Protobuf-wire-encoded `SnippetArtifactSidecar` bytes assembled from the frontend in
 * `Fir2IrReplSnippetConfiguratorExtensionImpl.prepareSnippet` and consumed by
 * `ReplSnippetsToClassesLowering.finalizeReplSnippetClass`, which embeds them into the snippet
 * wrapper class's `.kotlin_metadata`.
 *
 * Set only when a sidecar was assembled for this snippet; otherwise left `null`.
 */
var IrReplSnippet.replSidecarMetadataAttr: ByteArray? by irAttribute(copyByDefault = false)
