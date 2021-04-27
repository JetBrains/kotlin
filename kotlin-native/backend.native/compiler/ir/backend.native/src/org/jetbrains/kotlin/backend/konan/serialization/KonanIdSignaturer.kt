package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.backend.konan.descriptors.isFromInteropLibrary
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.resolve.descriptorUtil.module

class KonanIdSignaturer(private val mangler: KotlinMangler.DescriptorMangler) : IdSignatureDescriptor(mangler) {

    override fun createSignatureBuilder(): DescriptorBasedSignatureBuilder =
            KonanDescriptorBasedSignatureBuilder(mangler)

    private inner class KonanDescriptorBasedSignatureBuilder(
            mangler: KotlinMangler.DescriptorMangler
    ) : DescriptorBasedSignatureBuilder(mangler) {

        private fun markInteropDeclaration(descriptor: DeclarationDescriptor) {
            if (descriptor.module.isFromInteropLibrary()) {
                mask = mask or IdSignature.Flags.IS_NATIVE_INTEROP_LIBRARY.encode(true)
            }
        }

        override fun platformSpecificAlias(descriptor: TypeAliasDescriptor) {
            markInteropDeclaration(descriptor)
        }

        override fun platformSpecificClass(descriptor: ClassDescriptor) {
            markInteropDeclaration(descriptor)
        }

        override fun platformSpecificConstructor(descriptor: ConstructorDescriptor) {
            markInteropDeclaration(descriptor)
        }

        override fun platformSpecificFunction(descriptor: FunctionDescriptor) {
            markInteropDeclaration(descriptor)
        }

        override fun platformSpecificGetter(descriptor: PropertyGetterDescriptor) {
            markInteropDeclaration(descriptor)
        }

        override fun platformSpecificPackage(descriptor: PackageFragmentDescriptor) {
            markInteropDeclaration(descriptor)
        }

        override fun platformSpecificProperty(descriptor: PropertyDescriptor) {
            markInteropDeclaration(descriptor)
        }

        override fun platformSpecificSetter(descriptor: PropertySetterDescriptor) {
            markInteropDeclaration(descriptor)
        }
    }
}