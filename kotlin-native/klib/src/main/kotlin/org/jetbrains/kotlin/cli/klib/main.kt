/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.cli.klib

// TODO: Extract `library` package as a shared jar?
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.overrides.IrLinkerFakeOverrideProvider
import org.jetbrains.kotlin.backend.common.serialization.BasicIrModuleDeserializer
import org.jetbrains.kotlin.backend.common.serialization.DeserializationStrategy
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializer
import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.backend.konan.serialization.KonanIdSignaturer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.TranslationPluginContext
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.resolverByName
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.util.DependencyDirectories
import org.jetbrains.kotlin.utils.KotlinNativePaths
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.abi.*
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.kotlinLibrary
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.util.Logger
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import java.io.File
import kotlin.system.exitProcess
import org.jetbrains.kotlin.konan.file.File as KFile

private val KlibFactories = KlibMetadataFactories(::KonanBuiltIns, DynamicTypeDeserializer)

fun printUsage() {
    println(
            """
            Usage: klib <command> <library> [<option>]

            where the commands are:
               info                      General information about the library
               install                   [DEPRECATED] Local KLIB repositories to be dropped soon. See https://youtrack.jetbrains.com/issue/KT-61098
                                           Install the library to the local repository.
               remove                    [DEPRECATED] Local KLIB repositories to be dropped soon. See https://youtrack.jetbrains.com/issue/KT-61098
                                           Remove the library from the local repository.
               dump-abi                  Dump the ABI snapshot of the library. Each line in the snapshot corresponds exactly to one
                                           declaration. Whenever an ABI-incompatible change happens to a declaration, this should
                                           be visible in the corresponding line of the snapshot.
               dump-ir                   Dump the intermediate representation (IR) of all declarations in the library. The output of this
                                           command is intended to be used for debugging purposes only.
               dump-ir-signatures        Dump IR signatures of all non-private declarations in the library and all non-private declarations
                                           consumed by this library (as two separate lists). This command relies purely on the data in IR.
               dump-metadata-signatures  Dump IR signatures of all non-private declarations in the library. Note, that this command renders
                                           the signatures based on the library metadata. This is different from "dump-ir-signatures",
                                           which renders signatures based on the IR. On practice, in most cases there is no difference
                                           between output of these two commands. However, if IR transforming compiler plugins
                                           (such as Compose) were used during compilation of the library, there would be different
                                           signatures for patched declarations.
               signatures                [DEPRECATED] Renamed to "dump-metadata-signatures". Please, use new command name.
               dump-metadata             Dump the metadata of all non-private declarations in the library in the form of Kotlin-alike code.
                                           The output of this command is intended to be used for debugging purposes only.
               contents                  [DEPRECATED] Renamed to "dump-metadata". Please, use new command name.

            and the options are:
               -repository <path>        [DEPRECATED] Local KLIB repositories to be dropped soon. See https://youtrack.jetbrains.com/issue/KT-61098
                                           Work with the specified repository.
               -signature-version {${KotlinIrSignatureVersion.CURRENTLY_SUPPORTED_VERSIONS.joinToString("|") { it.number.toString() }}}
                                         Render IR signatures of a specific version. By default, the most up-to-date signature version
                                           that is supported in the library is used.
               -print-signatures {true|false}
                                         Print IR signature for every declaration. Applicable only to "dump-metadata" and "dump-ir" commands.
            """.trimIndent()
    )
}

private fun parseOptions(args: Array<String>): Map<String, List<String>> {
    val options = mutableMapOf<String, MutableList<String>>()
    for (index in args.indices step 2) {
        val key = args[index]
        if (key[0] != '-') {
            logError("Expected a flag with initial dash: $key")
        }
        if (index + 1 == args.size) {
            logError("Expected an value after $key")
        }
        val value = listOf(args[index + 1])
        options[key]?.addAll(value) ?: options.put(key, value.toMutableList())
    }
    return options
}


private class Command(args: Array<String>) {
    init {
        if (args.size < 2) {
            printUsage()
            exitProcess(0)
        }
    }

    val verb = args[0]
    val library = args[1]

    val knownOptions: Map<KnownOption, List<String>> = parseOptions(args.drop(2).toTypedArray<String>())
            .entries
            .mapNotNull { (option, values) ->
                val knownOption = KnownOption.parseOrNull(option)
                if (knownOption == null) {
                    logWarning("Unrecognized command-line option: $option")
                    return@mapNotNull null
                }
                knownOption to values
            }.toMap()
}

private fun Command.parseSignatureVersion(): KotlinIrSignatureVersion? {
    val rawSignatureVersion = knownOptions[KnownOption.SIGNATURE_VERSION]?.last() ?: return null
    val signatureVersion = rawSignatureVersion.toIntOrNull()?.let(::KotlinIrSignatureVersion)
            ?: logError("Invalid signature version: $rawSignatureVersion")

    if (signatureVersion !in KotlinIrSignatureVersion.CURRENTLY_SUPPORTED_VERSIONS)
        logError("Unsupported signature version: ${signatureVersion.number}")

    return signatureVersion
}

internal fun logWarning(text: String) {
    println("warning: $text")
}

internal fun logError(text: String, withStacktrace: Boolean = false): Nothing {
    if (withStacktrace)
        error("error: $text")
    else {
        System.err.println("error: $text")
        exitProcess(1)
    }
}

object KlibToolLogger : Logger, IrMessageLogger {
    override fun log(message: String) = println(message)
    override fun warning(message: String) = logWarning(message)
    override fun error(message: String) = logWarning(message)

    @Deprecated(Logger.FATAL_DEPRECATION_MESSAGE, ReplaceWith(Logger.FATAL_REPLACEMENT))
    override fun fatal(message: String) = logError(message, withStacktrace = true)

    override fun report(severity: IrMessageLogger.Severity, message: String, location: IrMessageLogger.Location?) {
        when (severity) {
            IrMessageLogger.Severity.INFO -> log(message)
            IrMessageLogger.Severity.WARNING -> warning(message)
            IrMessageLogger.Severity.ERROR -> error(message)
        }
    }
}

val defaultRepository = KFile(DependencyDirectories.localKonanDir.resolve("klib").absolutePath)

open class ModuleDeserializer(val library: ByteArray) {
    protected val moduleHeader: KlibMetadataProtoBuf.Header
        get() = parseModuleHeader(library)

    val moduleName: String
        get() = moduleHeader.moduleName

    val packageFragmentNameList: List<String>
        get() = moduleHeader.packageFragmentNameList

}

private class KlibRepoDeprecationWarning {
    private var alreadyLogged = false

    fun logOnceIfNecessary() {
        if (!alreadyLogged) {
            alreadyLogged = true
            logWarning("Local KLIB repositories to be dropped soon. See https://youtrack.jetbrains.com/issue/KT-61098")
        }
    }
}

class Library(val libraryNameOrPath: String, val requestedRepository: String?) {

    private val klibRepoDeprecationWarning = KlibRepoDeprecationWarning()

    val repository = requestedRepository?.let {
        klibRepoDeprecationWarning.logOnceIfNecessary() // Due to use of "-repository" option.
        KFile(it)
    } ?: defaultRepository

    fun info() {
        val library = libraryInRepoOrCurrentDir(repository, libraryNameOrPath)
        val headerAbiVersion = library.versions.abiVersion
        val headerCompilerVersion = library.versions.compilerVersion
        val headerLibraryVersion = library.versions.libraryVersion
        val headerMetadataVersion = library.versions.metadataVersion
        val moduleName = ModuleDeserializer(library.moduleHeaderData).moduleName

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

    class KlibToolLinker(
        module: ModuleDescriptor, irBuiltIns: IrBuiltIns, symbolTable: SymbolTable
    ) : KotlinIrLinker(module, KlibToolLogger, irBuiltIns, symbolTable, emptyList()) {
        override val fakeOverrideBuilder = IrLinkerFakeOverrideProvider(
            linker = this,
            symbolTable = symbolTable,
            mangler = KonanManglerIr,
            typeSystem = IrTypeSystemContextImpl(builtIns),
            friendModules = emptyMap(),
            partialLinkageSupport = PartialLinkageSupportForLinker.DISABLED,
        )

        override val returnUnboundSymbolsIfSignatureNotFound: Boolean
            get() = true

        override val translationPluginContext: TranslationPluginContext
            get() = TODO("Not needed for ir dumping")

        override fun createModuleDeserializer(moduleDescriptor: ModuleDescriptor, klib: KotlinLibrary?, strategyResolver: (String) -> DeserializationStrategy): IrModuleDeserializer {
            return KlibToolModuleDeserializer(
                    module = moduleDescriptor,
                    klib = klib ?: error("Expecting kotlin library for $moduleDescriptor"),
                    strategyResolver = strategyResolver
            )
        }

        override fun isBuiltInModule(moduleDescriptor: ModuleDescriptor): Boolean {
            return false
        }

        inner class KlibToolModuleDeserializer(
                module: ModuleDescriptor,
                klib: KotlinLibrary,
                strategyResolver: (String) -> DeserializationStrategy
        ) : BasicIrModuleDeserializer(
                this,
                module,
                klib,
                strategyResolver,
                klib.versions.abiVersion ?: KotlinAbiVersion.CURRENT
        )
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    fun dumpIr(output: Appendable, printSignatures: Boolean, signatureVersion: KotlinIrSignatureVersion?) {
        val module = loadModule()
        val library = module.kotlinLibrary
        checkLibraryHasIr(library)

        if (signatureVersion != null && signatureVersion != KotlinIrSignatureVersion.V2) {
            // TODO: support passing any signature version through `DumpIrTreeOptions`, KT-62828
            logWarning("using a non-default signature version in \"dump-ir\" is not supported yet")
        }

        val versionSpec = LanguageVersionSettingsImpl(currentLanguageVersion, currentApiVersion)
        val idSignaturer = KonanIdSignaturer(KonanManglerDesc)
        val symbolTable = SymbolTable(idSignaturer, IrFactoryImpl)
        val typeTranslator = TypeTranslatorImpl(symbolTable, versionSpec, module)
        val irBuiltIns = IrBuiltInsOverDescriptors(module.builtIns, typeTranslator, symbolTable)

        val linker = KlibToolLinker(module, irBuiltIns, symbolTable)
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

    fun contents(output: Appendable, printSignatures: Boolean, signatureVersion: KotlinIrSignatureVersion?) {
        logWarning("\"contents\" has been renamed to \"dump-metadata\". Please, use new command name.")
        dumpMetadata(output, printSignatures, signatureVersion)
    }

    fun dumpMetadata(output: Appendable, printSignatures: Boolean, signatureVersion: KotlinIrSignatureVersion?) {
        val module = loadModule()
        val signatureRenderer = if (printSignatures)
            DefaultKlibSignatureRenderer(signatureVersion, "// Signature: ")
        else
            KlibSignatureRenderer.NO_SIGNATURE
        val printer = DeclarationPrinter(output, DefaultDeclarationHeaderRenderer, signatureRenderer)

        printer.print(module)
    }

    fun signatures(output: Appendable, signatureVersion: KotlinIrSignatureVersion?) {
        logWarning("\"signatures\" has been renamed to \"dump-metadata-signatures\". Please, use new command name.")
        dumpMetadataSignatures(output, signatureVersion)
    }

    fun dumpMetadataSignatures(output: Appendable, signatureVersion: KotlinIrSignatureVersion?) {
        val module = loadModule()
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

    private fun loadModule(): ModuleDescriptor {
        val storageManager = LockBasedStorageManager("klib")
        val library = libraryInRepoOrCurrentDir(repository, libraryNameOrPath)
        val versionSpec = LanguageVersionSettingsImpl(currentLanguageVersion, currentApiVersion)
        val module = KlibFactories.DefaultDeserializedDescriptorFactory.createDescriptorAndNewBuiltIns(library, versionSpec, storageManager, null)

        val defaultModules = mutableListOf<ModuleDescriptorImpl>()
        if (!module.isNativeStdlib()) {
            val resolver = resolverByName(
                emptyList(),
                distributionKlib = Distribution(KotlinNativePaths.homePath.absolutePath).klib,
                skipCurrentDir = true,
                logger = KlibToolLogger
            )
            resolver.defaultLinks(false, true, true).mapTo(defaultModules) {
                KlibFactories.DefaultDeserializedDescriptorFactory.createDescriptor(it, versionSpec, storageManager, module.builtIns, null)
            }
        }

        (defaultModules + module).let { allModules ->
            allModules.forEach { it.setDependencies(allModules) }
        }

        return module
    }
}

val currentLanguageVersion = LanguageVersion.LATEST_STABLE
val currentApiVersion = ApiVersion.LATEST_STABLE

fun libraryInRepo(repository: KFile, name: String) =
        resolverByName(listOf(repository.absolutePath), skipCurrentDir = true, logger = KlibToolLogger).resolve(name)

fun libraryInCurrentDir(name: String) = resolverByName(emptyList(), logger = KlibToolLogger).resolve(name)

fun libraryInRepoOrCurrentDir(repository: KFile, name: String) =
        resolverByName(listOf(repository.absolutePath), logger = KlibToolLogger).resolve(name)

private enum class KnownOption(val option: String) {
    REPOSITORY("-repository"),
    PRINT_SIGNATURES("-print-signatures"),
    SIGNATURE_VERSION("-signature-version");

    companion object {
        fun parseOrNull(option: String): KnownOption? = entries.firstOrNull { it.option == option }
    }
}

fun main(args: Array<String>) {
    val command = Command(args)

    val repository = command.knownOptions[KnownOption.REPOSITORY]?.last()
    val printSignatures = command.knownOptions[KnownOption.PRINT_SIGNATURES]?.last()?.toBoolean() == true

    val signatureVersion = command.parseSignatureVersion()

    val library = Library(command.library, repository)

    when (command.verb) {
        "dump-abi" -> library.dumpAbi(System.out, signatureVersion)
        "dump-ir" -> library.dumpIr(System.out, printSignatures, signatureVersion)
        "dump-ir-signatures" -> library.dumpIrSignatures(System.out, signatureVersion)
        "dump-metadata" -> library.dumpMetadata(System.out, printSignatures, signatureVersion)
        "dump-metadata-signatures" -> library.dumpMetadataSignatures(System.out, signatureVersion)
        "contents" -> library.contents(System.out, printSignatures, signatureVersion)
        "signatures" -> library.signatures(System.out, signatureVersion)
        "info" -> library.info()
        "install" -> library.install()
        "remove" -> library.remove()
        else -> logError("Unknown command: ${command.verb}")
    }
}
