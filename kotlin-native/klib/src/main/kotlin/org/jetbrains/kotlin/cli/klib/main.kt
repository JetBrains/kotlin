/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.cli.klib

// TODO: Extract `library` package as a shared jar?
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideBuilder
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
import org.jetbrains.kotlin.konan.target.PlatformManager
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

internal val KlibFactories = KlibMetadataFactories(::KonanBuiltIns, DynamicTypeDeserializer)

fun printUsage() {
    println("Usage: klib <command> <library> <options>")
    println("where the commands are:")
    println("\tinfo\tgeneral information about the library")
    println("\tinstall\tinstall the library to the local repository")
    println("\tdump-ir\tprint out the intermediate representation (IR) for the library (to be used for debugging purposes only)")
    println("\tcontents\tlist contents of the library")
    println("\tsignatures\tlist of ID signatures in the library")
    println("\tremove\tremove the library from the local repository")
    println("and the options are:")
    println("\t-repository <path>\twork with the specified repository")
    println("\t-target <name>\tinspect specifics of the given target")
    println("\t-print-signatures [true|false]\tprint ID signature for every declaration (only for \"contents\" and \"dump-ir\" commands)")
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
    val options = parseArgs(args.drop(2).toTypedArray())
}

fun warn(text: String) {
    println("warning: $text")
}

fun error(text: String): Nothing {
    kotlin.error("error: $text")
}

object KlibToolLogger : Logger, IrMessageLogger {
    override fun warning(message: String) = warn(message)
    override fun error(message: String) = warn(message)
    override fun fatal(message: String) = org.jetbrains.kotlin.cli.klib.error(message)
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

class Library(val libraryNameOrPath: String, val requestedRepository: String?, val target: String) {

    val repository = requestedRepository?.File() ?: defaultRepository
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
        println("Compiler version: ${headerCompilerVersion}")
        println("Library version: $headerLibraryVersion")
        println("Metadata version: $headerMetadataVersion")

        if (library is KonanLibrary) {
            val targets = library.targetList.joinToString(", ")
            print("Available targets: $targets\n")
        }
    }

    fun install() {
        if (!repository.exists) {
            warn("Repository does not exist: $repository. Creating.")
            repository.mkdirs()
        }

        val libraryTrueName = File(libraryNameOrPath).name.removeSuffixIfPresent(KLIB_FILE_EXTENSION_WITH_DOT)
        val library = libraryInCurrentDir(libraryNameOrPath)

        val installLibDir = File(repository, libraryTrueName)

        if (installLibDir.exists) installLibDir.deleteRecursively()

        library.libraryFile.unpackZippedKonanLibraryTo(installLibDir)
    }

    fun remove(blind: Boolean = false) {
        if (!repository.exists) error("Repository does not exist: $repository")

        val library = try {
            val library = libraryInRepo(repository, libraryNameOrPath)
            if (blind) warn("Removing The previously installed $libraryNameOrPath from $repository.")
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
        override val fakeOverrideBuilder = FakeOverrideBuilder(
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
            return KlibToolModuleDeserializer(moduleDescriptor, klib ?: error("Expecting kotlin library for $moduleDescriptor"), strategyResolver)
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
    fun ir(output: Appendable, printSignatures: Boolean) {
        val module = loadModule()
        if (module.kotlinLibrary.isInterop) error("Deserializing IR from IR-less libraries is not supported yet")
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
        val irFragment = linker.deserializeFullModule(module, module.kotlinLibrary)
        linker.resolveModuleDeserializer(module, null).init()
        linker.modulesWithReachableTopLevels.forEach(IrModuleDeserializer::deserializeReachableDeclarations)
        output.append(irFragment.dump(DumpIrTreeOptions(printSignatures = printSignatures)))
    }

    fun contents(output: Appendable, printSignatures: Boolean) {
        val module = loadModule()
        val signatureRenderer = if (printSignatures) DefaultKlibSignatureRenderer("// ID signature: ") else KlibSignatureRenderer.NO_SIGNATURE
        val printer = DeclarationPrinter(output, DefaultDeclarationHeaderRenderer, signatureRenderer)

        printer.print(module)
    }

    fun signatures(output: Appendable) {
        val module = loadModule()
        val printer = SignaturePrinter(output, DefaultKlibSignatureRenderer())

        printer.print(module)
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

    val targetManager = PlatformManager(KonanHomeProvider.determineKonanHome())
            .targetManager(command.options["-target"]?.last())
    val target = targetManager.targetName

    val repository = command.options["-repository"]?.last()
    val printSignatures = command.options["-print-signatures"]?.last()?.toBoolean() == true

    val library = Library(command.library, repository, target)

    when (command.verb) {
        "dump-ir" -> library.ir(System.out, printSignatures)
        "contents" -> library.contents(System.out, printSignatures)
        "signatures" -> library.signatures(System.out)
        "info" -> library.info()
        "install" -> library.install()
        "remove" -> library.remove()
        else -> error("Unknown command ${command.verb}.")
    }
}
