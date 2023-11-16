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
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.resolverByName
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.util.DependencyDirectories
import org.jetbrains.kotlin.konan.util.KonanHomeProvider
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.kotlinLibrary
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.util.Logger
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import kotlin.system.exitProcess

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
               dump-ir                   Dump the intermediate representation (IR) of all declarations in the library. The output of this
                                           command is intended to be used for debugging purposes only.
               dump-ir-signatures        Dump IR signatures of all public declarations in the library and all public declarations consumed
                                           by this library (as two separate lists). This command relies purely on the data in IR.
               dump-metadata-signatures  Dump IR signatures of all public declarations in the library. Note, that this command renders
                                           the signatures based on the library metadata. This is different from "dump-ir-signatures",
                                           which renders signatures based on the IR. On practice, in most cases there is no difference
                                           between output of these two commands. However, if IR transforming compiler plugins
                                           (such as Compose) were used during compilation of the library, there would be different
                                           signatures for patched declarations.
               signatures                [DEPRECATED] Renamed to "dump-metadata-signatures". Please, use new command name.
               dump-metadata             Dump the metadata of all public declarations in the library in the form of Kotlin-alike code.
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

private fun parseArgs(args: Array<String>): Map<String, List<String>> {
    val commandLine = mutableMapOf<String, MutableList<String>>()
    for (index in args.indices step 2) {
        val key = args[index]
        if (key[0] != '-') {
            throw IllegalArgumentException("Expected a flag with initial dash: $key")
        }
        if (index + 1 == args.size) {
            throw IllegalArgumentException("Expected an value after $key")
        }
        val value = listOf(args[index + 1])
        commandLine[key]?.addAll(value) ?: commandLine.put(key, value.toMutableList())
    }
    return commandLine
}


class Command(args: Array<String>) {
    init {
        if (args.size < 2) {
            printUsage()
            exitProcess(0)
        }
    }

    val verb = args[0]
    val library = args[1]
    val options: Map<String, List<String>> = parseArgs(args.drop(2).toTypedArray())
}

private fun Command.parseSignatureVersion(): KotlinIrSignatureVersion? {
    val rawSignatureVersion = options["-signature-version"]?.last() ?: return null
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
    override fun warning(message: String) = logWarning(message)
    override fun error(message: String) = logWarning(message)
    override fun fatal(message: String) = logError(message, withStacktrace = true)
    override fun log(message: String) = println(message)
    override fun report(severity: IrMessageLogger.Severity, message: String, location: IrMessageLogger.Location?) {
        when (severity) {
            IrMessageLogger.Severity.INFO -> log(message)
            IrMessageLogger.Severity.WARNING -> warning(message)
            IrMessageLogger.Severity.ERROR -> error(message)
        }
    }
}

val defaultRepository = File(DependencyDirectories.localKonanDir.resolve("klib").absolutePath)

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
        File(it)
    } ?: defaultRepository

    fun info() {
        val library = libraryInRepoOrCurrentDir(repository, libraryNameOrPath)
        val headerAbiVersion = library.versions.abiVersion
        val headerCompilerVersion = library.versions.compilerVersion
        val headerLibraryVersion = library.versions.libraryVersion
        val headerMetadataVersion = library.versions.metadataVersion
        val moduleName = ModuleDeserializer(library.moduleHeaderData).moduleName

        println("")
        println("Resolved to: ${library.libraryName.File().absolutePath}")
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

        val libraryTrueName = File(libraryNameOrPath).name.removeSuffixIfPresent(KLIB_FILE_EXTENSION_WITH_DOT)
        val library = libraryInCurrentDir(libraryNameOrPath)

        val installLibDir = File(repository, libraryTrueName)

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
                    distributionKlib = Distribution(KonanHomeProvider.determineKonanHome()).klib,
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

fun libraryInRepo(repository: File, name: String) =
        resolverByName(listOf(repository.absolutePath), skipCurrentDir = true, logger = KlibToolLogger).resolve(name)

fun libraryInCurrentDir(name: String) = resolverByName(emptyList(), logger = KlibToolLogger).resolve(name)

fun libraryInRepoOrCurrentDir(repository: File, name: String) =
        resolverByName(listOf(repository.absolutePath), logger = KlibToolLogger).resolve(name)

fun main(args: Array<String>) {
    val command = Command(args)

    val repository = command.options["-repository"]?.last()
    val printSignatures = command.options["-print-signatures"]?.last()?.toBoolean() == true

    val signatureVersion = command.parseSignatureVersion()

    val library = Library(command.library, repository)

    when (command.verb) {
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
