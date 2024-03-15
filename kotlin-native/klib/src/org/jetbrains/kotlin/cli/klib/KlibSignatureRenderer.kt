package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor

internal interface KlibSignatureRenderer {
    fun render(descriptor: DeclarationDescriptor): String?

    companion object {
        val NO_SIGNATURE = object : KlibSignatureRenderer {
            override fun render(descriptor: DeclarationDescriptor): String? = null
        }
    }
}
