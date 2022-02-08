package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.backend.konan.serialization.KonanIdSignaturer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor

class DefaultIdSignatureRenderer(private val prefix: String? = null) : IdSignatureRenderer {
    private val idSignaturer = KonanIdSignaturer(KonanManglerDesc)

    override fun render(descriptor: DeclarationDescriptor): String? {
        val idSignature = if (descriptor is ClassDescriptor && descriptor.kind == ClassKind.ENUM_ENTRY) {
            idSignaturer.composeEnumEntrySignature(descriptor)
        } else {
            idSignaturer.composeSignature(descriptor)
        } ?: return null

        return if (prefix != null)
            prefix + idSignature.render()
        else
            idSignature.render()
    }
}
