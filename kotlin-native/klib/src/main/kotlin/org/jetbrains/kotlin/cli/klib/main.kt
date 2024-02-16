/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.cli.klib

// TODO: Extract `library` package as a shared jar?
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializer
import org.jetbrains.kotlin.backend.konan.serialization.KonanIdSignaturer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.dump
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
import java.io.File
import org.jetbrains.kotlin.konan.file.File as KFile

private val defaultRepository = KFile(DependencyDirectories.localKonanDir.resolve("klib").absolutePath)

private class KlibRepoDeprecationWarning {
    private var alreadyLogged = false

    fun logOnceIfNecessary() {
        if (!alreadyLogged) {
            alreadyLogged = true
            logWarning("Local KLIB repositories to be dropped soon. See https://youtrack.jetbrains.com/issue/KT-61098")
        }
    }
}

internal class Library(val libraryNameOrPath: String, requestedRepository: String?) {

    private val klibRepoDeprecationWarning = KlibRepoDeprecationWarning()

    private val repository = requestedRepository?.let {
        klibRepoDeprecationWarning.logOnceIfNecessary() // Due to use of "-repository" option.
        KFile(it)
    } ?: defaultRepository

    fun info() {
        val library = libraryInRepoOrCurrentDir(repository, libraryNameOrPath)
        val headerAbiVersion = library.versions.abiVersion
        val headerCompilerVersion = library.versions.compilerVersion
        val headerLibraryVersion = library.versions.libraryVersion
        val headerMetadataVersion = library.versions.metadataVersion
        val moduleName = parseModuleHeader(library.moduleHeaderData).moduleName

        println("")
        println("Resolved to: ${KFile(library.libraryName).absolutePath}")
        println("Module name: $moduleName")
        println("ABI version: $headerAbiVersion")
        println("Compiler version: $headerCompilerVersion")
        println("Library version: $headerLibraryVersion")
        println("Metadata version: $headerMetadataVersion")

        if (library is KonanLibrary) {
            val targets = library.targetList.joinToString(", ")
            print("Available targets: $targets\n")
        }
    }

    fun install() {
        klibRepoDeprecationWarning.logOnceIfNecessary()

        if (!repository.exists) {
            logWarning("Repository does not exist: $repository. Creating...")
            repository.mkdirs()
        }

        val libraryTrueName = KFile(libraryNameOrPath).name.removeSuffixIfPresent(KLIB_FILE_EXTENSION_WITH_DOT)
        val library = libraryInCurrentDir(libraryNameOrPath)

        val installLibDir = KFile(repository, libraryTrueName)

        if (installLibDir.exists) installLibDir.deleteRecursively()

        library.libraryFile.unpackZippedKonanLibraryTo(installLibDir)
    }

    fun remove(blind: Boolean = false) {
        klibRepoDeprecationWarning.logOnceIfNecessary()

        if (!repository.exists) logError("Repository does not exist: $repository")

        val library = try {
            val library = libraryInRepo(repository, libraryNameOrPath)
            if (blind) logWarning("Removing The previously installed $libraryNameOrPath from $repository")
            library

        } catch (e: Throwable) {
            if (!blind) println(e.message)
            null

        }
        library?.libraryFile?.deleteRecursively()
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    fun dumpIr(output: Appendable, printSignatures: Boolean, signatureVersion: KotlinIrSignatureVersion?) {
        val library = libraryInRepoOrCurrentDir(repository, libraryNameOrPath)
        checkLibraryHasIr(library)

        if (signatureVersion != null && signatureVersion != KotlinIrSignatureVersion.V2) {
            // TODO: support passing any signature version through `DumpIrTreeOptions`, KT-62828
            logWarning("using a non-default signature version in \"dump-ir\" is not supported yet")
        }

        val module = ModuleDescriptorLoader.load(library)

        val idSignaturer = KonanIdSignaturer(KonanManglerDesc)
        val symbolTable = SymbolTable(idSignaturer, IrFactoryImpl)
        val typeTranslator = TypeTranslatorImpl(symbolTable, ModuleDescriptorLoader.languageVersionSettings, module)
        val irBuiltIns = IrBuiltInsOverDescriptors(module.builtIns, typeTranslator, symbolTable)

        val linker = KlibToolIrLinker(module, irBuiltIns, symbolTable)
        module.allDependencyModules.forEach {
            linker.deserializeOnlyHeaderModule(it, it.kotlinLibrary)
            linker.resolveModuleDeserializer(it, null).init()
        }
        val irFragment = linker.deserializeFullModule(module, library)
        linker.resolveModuleDeserializer(module, null).init()
        linker.modulesWithReachableTopLevels.forEach(IrModuleDeserializer::deserializeReachableDeclarations)

        output.append(irFragment.dump(DumpIrTreeOptions(printSignatures = printSignatures)))
    }

    @OptIn(ExperimentalLibraryAbiReader::class)
    fun dumpAbi(output: Appendable, signatureVersion: KotlinIrSignatureVersion?) {
        val library = libraryInCurrentDir(libraryNameOrPath)
        checkLibraryHasIr(library)

        val abiSignatureVersion = if (signatureVersion != null) {
            signatureVersion.checkSupportedInLibrary(library)

            val abiSignatureVersion = AbiSignatureVersion.resolveByVersionNumber(signatureVersion.number)
            if (!abiSignatureVersion.isSupportedByAbiReader)
                logError(
                        "Signature version ${signatureVersion.number} is not supported by the KLIB ABI reader." +
                                " Supported versions: ${AbiSignatureVersion.allSupportedByAbiReader.joinToString { it.versionNumber.toString() }}"
                )

            abiSignatureVersion
        } else {
            val versionsSupportedByAbiReader: Map<Int, AbiSignatureVersion> = AbiSignatureVersion.allSupportedByAbiReader
                    .associateBy { it.versionNumber }

            val abiSignatureVersion = library.versions.irSignatureVersions
                    .map { it.number }
                    .sortedDescending()
                    .firstNotNullOfOrNull { versionsSupportedByAbiReader[it] }

            if (abiSignatureVersion == null)
                logError(
                        "There is no signature version that would be both supported in library ${library.libraryFile}" +
                                " and by the KLIB ABI reader. Supported versions in the library:" +
                                " ${library.versions.irSignatureVersions.joinToString { it.number.toString() }}" +
                                ". Supported versions by the KLIB ABI reader: ${AbiSignatureVersion.allSupportedByAbiReader.joinToString { it.versionNumber.toString() }}"
                )

            abiSignatureVersion
        }

        LibraryAbiRenderer.render(
                libraryAbi = LibraryAbiReader.readAbiInfo(File(library.libraryFile.absolutePath)),
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
    fun contents(output: Appendable, printSignatures: Boolean, signatureVersion: KotlinIrSignatureVersion?) {
        logWarning("\"contents\" has been renamed to \"dump-metadata\". Please, use new command name.")
        val module = ModuleDescriptorLoader.load(libraryInRepoOrCurrentDir(repository, libraryNameOrPath))
        val signatureRenderer = if (printSignatures)
            DefaultKlibSignatureRenderer(signatureVersion, "// Signature: ")
        else
            KlibSignatureRenderer.NO_SIGNATURE
        val printer = DeclarationPrinter(output, signatureRenderer)

        printer.print(module)
    }

    /**
     * @param testMode if `true` then a special pre-processing is performed towards the metadata before rendering:
     *        - empty package fragments are removed
     *        - package fragments with the same package FQN are merged
     *        - classes are sorted in alphabetical order
     */
    fun dumpMetadata(output: Appendable, printSignatures: Boolean, signatureVersion: KotlinIrSignatureVersion?, testMode: Boolean) {
        KotlinpBasedMetadataDumper(output, printSignatures, signatureVersion).dumpLibrary(libraryInCurrentDir(libraryNameOrPath), testMode)
    }

    fun signatures(output: Appendable, signatureVersion: KotlinIrSignatureVersion?) {
        logWarning("\"signatures\" has been renamed to \"dump-metadata-signatures\". Please, use new command name.")
        dumpMetadataSignatures(output, signatureVersion)
    }

    fun dumpMetadataSignatures(output: Appendable, signatureVersion: KotlinIrSignatureVersion?) {
        val module = ModuleDescriptorLoader.load(libraryInRepoOrCurrentDir(repository, libraryNameOrPath))
        // Don't call `checkSupportedInLibrary()` - the signatures are anyway generated on the fly.
        val printer = SignaturePrinter(output, DefaultKlibSignatureRenderer(signatureVersion))

        printer.print(module)
    }

    fun dumpIrSignatures(output: Appendable, signatureVersion: KotlinIrSignatureVersion?) {
        val library = libraryInCurrentDir(libraryNameOrPath)
        checkLibraryHasIr(library)
        signatureVersion?.checkSupportedInLibrary(library)

        val signatures = IrSignaturesExtractor(library).extract()
        IrSignaturesRenderer(output, signatureVersion).render(signatures)
    }

    private fun checkLibraryHasIr(library: KotlinLibrary) {
        if (!library.hasIr) logError("Library ${library.libraryFile} is an IR-less library")
    }

    private fun KotlinIrSignatureVersion.checkSupportedInLibrary(library: KotlinLibrary) {
        val supportedSignatureVersions = library.versions.irSignatureVersions
        if (this !in supportedSignatureVersions)
            logError("Signature version ${this.number} is not supported in library ${library.libraryFile}." +
                    " Supported versions: ${supportedSignatureVersions.joinToString { it.number.toString() }}")
    }
}

private fun libraryInRepo(repository: KFile, name: String) =
        resolverByName(listOf(repository.absolutePath), skipCurrentDir = true, logger = KlibToolLogger).resolve(name)

private fun libraryInCurrentDir(name: String) = resolverByName(emptyList(), logger = KlibToolLogger).resolve(name)

private fun libraryInRepoOrCurrentDir(repository: KFile, name: String) =
        resolverByName(listOf(repository.absolutePath), logger = KlibToolLogger).resolve(name)

fun main(rawArgs: Array<String>) {
    val args = KlibToolArgumentsParser().parseArguments(rawArgs)
    val library = Library(args.libraryNameOrPath, args.repository)

    when (args.commandName) {
        "dump-abi" -> library.dumpAbi(System.out, args.signatureVersion)
        "dump-ir" -> library.dumpIr(System.out, args.printSignatures, args.signatureVersion)
        "dump-ir-signatures" -> library.dumpIrSignatures(System.out, args.signatureVersion)
        "dump-metadata" -> library.dumpMetadata(System.out, args.printSignatures, args.signatureVersion, testMode = false)
        "dump-metadata-signatures" -> library.dumpMetadataSignatures(System.out, args.signatureVersion)
        "contents" -> library.contents(System.out, args.printSignatures, args.signatureVersion)
        "signatures" -> library.signatures(System.out, args.signatureVersion)
        "info" -> library.info()
        "install" -> library.install()
        "remove" -> library.remove()
        else -> logError("Unknown command: ${args.commandName}")
    }
}
