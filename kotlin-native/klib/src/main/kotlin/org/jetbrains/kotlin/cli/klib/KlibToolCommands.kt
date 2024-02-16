/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializer
import org.jetbrains.kotlin.backend.konan.serialization.KonanIdSignaturer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions
import org.jetbrains.kotlin.ir.util.IdSignatureRenderer
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.resolverByName
import org.jetbrains.kotlin.konan.util.DependencyDirectories
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION_WITH_DOT
import org.jetbrains.kotlin.library.KotlinIrSignatureVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.abi.*
import org.jetbrains.kotlin.library.metadata.kotlinLibrary
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.library.unpackZippedKonanLibraryTo
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import org.jetbrains.kotlin.utils.addToStdlib.runIf

internal class KlibToolCommand(private val output: KlibToolOutput, private val args: KlibToolArguments) {

    private val klibRepoDeprecationWarning = KlibRepoDeprecationWarning(output)

    private val repository = args.repository?.let {
        klibRepoDeprecationWarning.logOnceIfNecessary() // Due to use of "-repository" option.
        File(it)
    } ?: defaultRepository

    fun info() {
        val library = libraryInRepoOrCurrentDir(repository, args.libraryNameOrPath)
        val headerAbiVersion = library.versions.abiVersion
        val headerCompilerVersion = library.versions.compilerVersion
        val headerLibraryVersion = library.versions.libraryVersion
        val headerMetadataVersion = library.versions.metadataVersion
        val moduleName = parseModuleHeader(library.moduleHeaderData).moduleName

        output.appendLine()
        output.appendLine("Resolved to: ${File(library.libraryName).absolutePath}")
        output.appendLine("Module name: $moduleName")
        output.appendLine("ABI version: $headerAbiVersion")
        output.appendLine("Compiler version: $headerCompilerVersion")
        output.appendLine("Library version: $headerLibraryVersion")
        output.appendLine("Metadata version: $headerMetadataVersion")

        if (library is KonanLibrary) {
            output.appendLine("Available targets: ${library.targetList.joinToString()}")
        }
    }

    fun install() {
        klibRepoDeprecationWarning.logOnceIfNecessary()

        if (!repository.exists) {
            output.logWarning("Repository does not exist: $repository. Creating...")
            repository.mkdirs()
        }

        val libraryTrueName = File(args.libraryNameOrPath).name.removeSuffixIfPresent(KLIB_FILE_EXTENSION_WITH_DOT)
        val library = libraryInCurrentDir(args.libraryNameOrPath)

        val installLibDir = File(repository, libraryTrueName)

        if (installLibDir.exists) installLibDir.deleteRecursively()

        library.libraryFile.unpackZippedKonanLibraryTo(installLibDir)
    }

    fun remove() {
        klibRepoDeprecationWarning.logOnceIfNecessary()

        if (!repository.exists) {
            output.logError("Repository does not exist: $repository")
            return
        }

        runCatching {
            libraryInRepo(repository, args.libraryNameOrPath).libraryFile.deleteRecursively()
        }
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    fun dumpIr() {
        val library = libraryInRepoOrCurrentDir(repository, args.libraryNameOrPath)

        if (!checkLibraryHasIr(library)) return

        if (args.signatureVersion != null && args.signatureVersion != KotlinIrSignatureVersion.V2) {
            // TODO: support passing any signature version through `DumpIrTreeOptions`, KT-62828
            output.logWarning("using a non-default signature version in \"dump-ir\" is not supported yet")
        }

        val module = ModuleDescriptorLoader(output).load(library)

        val idSignaturer = KonanIdSignaturer(KonanManglerDesc)
        val symbolTable = SymbolTable(idSignaturer, IrFactoryImpl)
        val typeTranslator = TypeTranslatorImpl(symbolTable, ModuleDescriptorLoader.languageVersionSettings, module)
        val irBuiltIns = IrBuiltInsOverDescriptors(module.builtIns, typeTranslator, symbolTable)

        val linker = KlibToolIrLinker(output, module, irBuiltIns, symbolTable)
        module.allDependencyModules.forEach {
            linker.deserializeOnlyHeaderModule(it, it.kotlinLibrary)
            linker.resolveModuleDeserializer(it, null).init()
        }
        val irFragment = linker.deserializeFullModule(module, library)
        linker.resolveModuleDeserializer(module, null).init()
        linker.modulesWithReachableTopLevels.forEach(IrModuleDeserializer::deserializeReachableDeclarations)

        output.append(irFragment.dump(DumpIrTreeOptions(printSignatures = args.printSignatures)))
    }

    @OptIn(ExperimentalLibraryAbiReader::class)
    fun dumpAbi() {
        val library = libraryInCurrentDir(args.libraryNameOrPath)

        if (!checkLibraryHasIr(library)) return

        val abiSignatureVersion = args.signatureVersion?.let { signatureVersion ->
            if (!signatureVersion.checkSupportedInLibrary(library)) return

            val abiSignatureVersion = AbiSignatureVersion.resolveByVersionNumber(signatureVersion.number)
            if (!abiSignatureVersion.isSupportedByAbiReader) {
                output.logError(
                        "Signature version ${signatureVersion.number} is not supported by the KLIB ABI reader." +
                                " Supported versions: ${AbiSignatureVersion.allSupportedByAbiReader.joinToString { it.versionNumber.toString() }}"
                )
                return
            }

            abiSignatureVersion
        } ?: run {
            val versionsSupportedByAbiReader: Map<Int, AbiSignatureVersion> = AbiSignatureVersion.allSupportedByAbiReader
                    .associateBy { it.versionNumber }

            val abiSignatureVersion = library.versions.irSignatureVersions
                    .map { it.number }
                    .sortedDescending()
                    .firstNotNullOfOrNull { versionsSupportedByAbiReader[it] }

            if (abiSignatureVersion == null) {
                output.logError(
                        "There is no signature version that would be both supported in library ${library.libraryFile}" +
                                " and by the KLIB ABI reader. Supported versions in the library:" +
                                " ${library.versions.irSignatureVersions.joinToString { it.number.toString() }}" +
                                ". Supported versions by the KLIB ABI reader: ${AbiSignatureVersion.allSupportedByAbiReader.joinToString { it.versionNumber.toString() }}"
                )
                return
            }

            abiSignatureVersion
        }

        LibraryAbiRenderer.render(
                libraryAbi = LibraryAbiReader.readAbiInfo(java.io.File(library.libraryFile.absolutePath)),
                output = output,
                settings = AbiRenderingSettings(
                        renderedSignatureVersion = abiSignatureVersion,
                        renderManifest = false,
                        renderDeclarations = true,
                        indentationString = "    ",

                        )
        )
    }

    // TODO: This command is deprecated. Drop it after 2.0. KT-65380
    fun contents() {
        output.logWarning("\"contents\" has been renamed to \"dump-metadata\". Please, use new command name.")

        val idSignatureRenderer = args.signatureVersion.getMostSuitableSignatureRenderer() ?: return

        val module = ModuleDescriptorLoader(output).load(libraryInRepoOrCurrentDir(repository, args.libraryNameOrPath))
        val klibSignatureRenderer = if (args.printSignatures)
            DefaultKlibSignatureRenderer(idSignatureRenderer, "// Signature: ")
        else
            KlibSignatureRenderer.NO_SIGNATURE
        val printer = DeclarationPrinter(output, klibSignatureRenderer)

        printer.print(module)
    }

    fun dumpMetadata() {
        val idSignatureRenderer: IdSignatureRenderer? = runIf(args.printSignatures) {
            args.signatureVersion.getMostSuitableSignatureRenderer() ?: return
        }
        KotlinpBasedMetadataDumper(output, idSignatureRenderer).dumpLibrary(libraryInCurrentDir(args.libraryNameOrPath), args.testMode)
    }

    fun signatures() {
        output.logWarning("\"signatures\" has been renamed to \"dump-metadata-signatures\". Please, use new command name.")
        dumpMetadataSignatures()
    }

    fun dumpMetadataSignatures() {
        // Don't call `checkSupportedInLibrary()` - the signatures are anyway generated on the fly.

        val idSignatureRenderer = args.signatureVersion.getMostSuitableSignatureRenderer() ?: return

        val module = ModuleDescriptorLoader(output).load(libraryInRepoOrCurrentDir(repository, args.libraryNameOrPath))
        val printer = SignaturePrinter(output, DefaultKlibSignatureRenderer(idSignatureRenderer))

        printer.print(module)
    }

    fun dumpIrSignatures() {
        val library = libraryInCurrentDir(args.libraryNameOrPath)

        if (!checkLibraryHasIr(library) || !args.signatureVersion.checkSupportedInLibrary(library)) return

        val idSignatureRenderer = args.signatureVersion.getMostSuitableSignatureRenderer() ?: return

        val signatures = IrSignaturesExtractor(library).extract()
        IrSignaturesRenderer(output, idSignatureRenderer).render(signatures)
    }

    private fun checkLibraryHasIr(library: KotlinLibrary): Boolean {
        if (!library.hasIr) {
            output.logError("Library ${library.libraryFile} is an IR-less library")
            return false
        }
        return true
    }

    private fun KotlinIrSignatureVersion?.checkSupportedInLibrary(library: KotlinLibrary): Boolean {
        if (this != null) {
            val supportedSignatureVersions = library.versions.irSignatureVersions
            if (this !in supportedSignatureVersions) {
                output.logError("Signature version ${this.number} is not supported in library ${library.libraryFile}." +
                        " Supported versions: ${supportedSignatureVersions.joinToString { it.number.toString() }}")
                return false
            }
        }
        return true
    }

    private fun KotlinIrSignatureVersion?.getMostSuitableSignatureRenderer(): IdSignatureRenderer? = when (this) {
        KotlinIrSignatureVersion.V1 -> IdSignatureRenderer.LEGACY
        null, KotlinIrSignatureVersion.V2 -> IdSignatureRenderer.DEFAULT
        else -> {
            output.logError("Unsupported signature version: $number")
            null
        }
    }

    private fun libraryInRepo(repository: File, name: String) =
            resolverByName(listOf(repository.absolutePath), skipCurrentDir = true, logger = KlibToolLogger(output)).resolve(name)

    private fun libraryInCurrentDir(name: String) = resolverByName(emptyList(), logger = KlibToolLogger(output)).resolve(name)

    private fun libraryInRepoOrCurrentDir(repository: File, name: String) =
            resolverByName(listOf(repository.absolutePath), logger = KlibToolLogger(output)).resolve(name)
}


private val defaultRepository = File(DependencyDirectories.localKonanDir.resolve("klib").absolutePath)

private class KlibRepoDeprecationWarning(private val output: KlibToolOutput) {
    private var alreadyLogged = false

    fun logOnceIfNecessary() {
        if (!alreadyLogged) {
            alreadyLogged = true
            output.logWarning("Local KLIB repositories to be dropped soon. See https://youtrack.jetbrains.com/issue/KT-61098")
        }
    }
}
