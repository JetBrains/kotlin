/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.common.serialization.extractSerializedKdocString
import org.jetbrains.kotlin.backend.common.serialization.metadata.findKDocString
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.descriptors.DeserializedDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.source.PsiSourceElement

fun ObjCExportStubOrigin(descriptor: DeclarationDescriptor?): ObjCExportStubOrigin? {
    if (descriptor == null) return null

    if (descriptor is DeclarationDescriptorWithSource) {
        return ObjCExportStubOrigin.Source(descriptor.name, descriptor.findKDocString(), (descriptor.source as? PsiSourceElement)?.psi)
    }

    assert(descriptor is DeserializedDescriptor) { "Expected '$descriptor' to implement ${DeserializedDescriptor::class.simpleName}" }
    return ObjCExportStubOrigin.Binary(descriptor.name, descriptor.extractSerializedKdocString())
}

sealed class ObjCExportStubOrigin {

    /**
     * The original 'Kotlin' name of the entity that is associated with this stub
     */
    abstract val name: Name?

    /**
     * The original 'Kotlin documentation' of the associated with this stub
     */
    abstract val kdoc: String?

    /**
     * The stub was produced from Kotlin sources
     */
    data class Source(override val name: Name?, override val kdoc: String?, val psi: PsiElement?) : ObjCExportStubOrigin()

    /**
     * The stub was produced from a compiled binary (e.g. when translating a dependency inside fleet).
     * Note: On CLI invocations, the ObjC export will only operate on binaries.
     */
    data class Binary(override val name: Name?, override val kdoc: String?) : ObjCExportStubOrigin()
}
