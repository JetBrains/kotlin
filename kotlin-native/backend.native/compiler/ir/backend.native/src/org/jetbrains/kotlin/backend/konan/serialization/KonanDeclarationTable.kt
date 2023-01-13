package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.GlobalDeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.signature.DescToIrIdSignatureComputer
import org.jetbrains.kotlin.backend.konan.descriptors.isFromInteropLibrary
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.resolve.descriptorUtil.module

class KonanGlobalDeclarationTable(builtIns: IrBuiltIns) : GlobalDeclarationTable(KonanManglerIr) {
    init {
        loadKnownBuiltins(builtIns)
    }
}

class KonanDeclarationTable(
        globalDeclarationTable: GlobalDeclarationTable
) : DeclarationTable(globalDeclarationTable) {

    private val signatureIdComposer = DescToIrIdSignatureComputer(KonanIdSignaturer(KonanManglerDesc))

    // TODO: We should get rid of this extension point in favor of proper support in IR-based mangler.
    override fun tryComputeBackendSpecificSignature(declaration: IrDeclaration): IdSignature? =
            if (declaration.descriptor.isFromInteropLibrary()) {
                signatureIdComposer.computeSignature(declaration)
            } else null
}