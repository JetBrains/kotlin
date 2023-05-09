package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.IrModuleSerializer
import org.jetbrains.kotlin.backend.konan.ir.interop.IrProviderForCEnumAndCStructStubs
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.IrMessageLogger

class KonanIrModuleSerializer(
        messageLogger: IrMessageLogger,
        irBuiltIns: IrBuiltIns,
        compatibilityMode: CompatibilityMode,
        normalizeAbsolutePaths: Boolean,
        sourceBaseDirs: Collection<String>,
        private val languageVersionSettings: LanguageVersionSettings,
        private val bodiesOnlyForInlines: Boolean = false,
        private val skipPrivateApi: Boolean = false,
) : IrModuleSerializer<KonanIrFileSerializer>(messageLogger, compatibilityMode, normalizeAbsolutePaths, sourceBaseDirs) {

    private val globalDeclarationTable = KonanGlobalDeclarationTable(irBuiltIns)

    // We skip files with IR for C structs and enums because they should be
    // generated anew.
    //
    // See [IrProviderForCEnumAndCStructStubs.kt#L31] on why we generate IR.
    // We may switch from IR generation to LazyIR later (at least for structs; enums are tricky)
    // without changing kotlin libraries that depend on interop libraries.
    override fun backendSpecificFileFilter(file: IrFile): Boolean =
            file.fileEntry.name != IrProviderForCEnumAndCStructStubs.cTypeDefinitionsFileName

    override fun createSerializerForFile(file: IrFile): KonanIrFileSerializer =
            KonanIrFileSerializer(messageLogger, KonanDeclarationTable(globalDeclarationTable),
                    compatibilityMode = compatibilityMode,
                    normalizeAbsolutePaths = normalizeAbsolutePaths,
                    sourceBaseDirs = sourceBaseDirs,
                    languageVersionSettings = languageVersionSettings,
                    bodiesOnlyForInlines = bodiesOnlyForInlines,
                    skipPrivateApi = skipPrivateApi)
}
