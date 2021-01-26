package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.backend.konan.ir.interop.IrProviderForCEnumAndCStructStubs
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IrMessageLogger

class KonanIrModuleSerializer(
    messageLogger: IrMessageLogger,
    irBuiltIns: IrBuiltIns,
    private val expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>,
    val skipExpects: Boolean
) : IrModuleSerializer<KonanIrFileSerializer>(messageLogger) {

    private val signaturer = IdSignatureSerializer(KonanManglerIr)
    private val globalDeclarationTable = KonanGlobalDeclarationTable(signaturer, irBuiltIns)

    // We skip files with IR for C structs and enums because they should be
    // generated anew.
    //
    // See [IrProviderForCEnumAndCStructStubs.kt#L31] on why we generate IR.
    // We may switch from IR generation to LazyIR later (at least for structs; enums are tricky)
    // without changing kotlin libraries that depend on interop libraries.
    override fun backendSpecificFileFilter(file: IrFile): Boolean =
            file.fileEntry.name != IrProviderForCEnumAndCStructStubs.cTypeDefinitionsFileName

    override fun createSerializerForFile(file: IrFile): KonanIrFileSerializer =
            KonanIrFileSerializer(messageLogger, KonanDeclarationTable(globalDeclarationTable), expectDescriptorToSymbol, skipExpects = skipExpects)
}
