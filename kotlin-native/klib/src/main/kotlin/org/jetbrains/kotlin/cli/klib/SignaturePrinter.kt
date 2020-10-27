package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.backend.konan.descriptors.getPackageFragments
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies
import org.jetbrains.kotlin.utils.Printer

class SignaturePrinter(
        out: Appendable,
        private val signatureRenderer: IdSignatureRenderer
) {
    private val printer = Printer(out)

    fun print(module: ModuleDescriptor) {
        module.accept(PrinterVisitor(), Unit)
    }

    private fun Printer.printlnIfNotNull(line: String?) {
        if (line != null) println(line)
    }

    private inner class PrinterVisitor : DeclarationDescriptorVisitorEmptyBodies<Unit, Unit>() {
        override fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: Unit) {
            descriptor.getPackageFragments().forEach { it.accept(this, data) }
        }

        override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: Unit) {
            descriptor.getMemberScope().getContributedDescriptors().forEach { it.accept(this, data) }
        }

        override fun visitClassDescriptor(descriptor: ClassDescriptor, data: Unit) {
            printer.printlnIfNotNull(signatureRenderer.render(descriptor))
            descriptor.constructors.forEach { it.accept(this, data) }
            descriptor.unsubstitutedMemberScope.getContributedDescriptors().forEach { it.accept(this, data) }
        }

        override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: Unit) {
            printer.printlnIfNotNull(signatureRenderer.render(descriptor))
        }

        override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: Unit) {
            printer.printlnIfNotNull(signatureRenderer.render(descriptor))
            descriptor.getter?.let { printer.printlnIfNotNull(signatureRenderer.render(it)) }
            descriptor.setter?.let { printer.printlnIfNotNull(signatureRenderer.render(it)) }
        }

        override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, data: Unit) {
            printer.printlnIfNotNull(signatureRenderer.render(descriptor))
        }

        override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, data: Unit) {
            printer.printlnIfNotNull(signatureRenderer.render(descriptor))
        }
    }
}
