package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor

interface DeclarationHeaderRenderer {
    fun render(descriptor: DeclarationDescriptor): String
}
