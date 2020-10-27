/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.cli.klib

// TODO: Extract `library` package as a shared jar?
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION_WITH_DOT
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.konan.util.DependencyProcessor
import org.jetbrains.kotlin.library.unpackZippedKonanLibraryTo
import org.jetbrains.kotlin.konan.util.KlibMetadataFactories
import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.deserialization.PlatformDependentTypeTransformer
import org.jetbrains.kotlin.util.Logger
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.resolverByName
import org.jetbrains.kotlin.konan.util.KonanHomeProvider
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import kotlin.system.exitProcess

internal val KlibFactories = KlibMetadataFactories(::KonanBuiltIns, DynamicTypeDeserializer, PlatformDependentTypeTransformer.None)

fun printUsage() {
    println("Usage: klib <command> <library> <options>")
    println("where the commands are:")
    println("\tinfo\tgeneral information about the library")
    println("\tinstall\tinstall the library to the local repository")
    println("\tcontents\tlist contents of the library")
    println("\tsignatures\tlist of ID signatures in the library")
    println("\tremove\tremove the library from the local repository")
    println("and the options are:")
    println("\t-repository <path>\twork with the specified repository")
    println("\t-target <name>\tinspect specifics of the given target")
    println("\t-print-signatures [true|false]\tprint ID signature for every declaration (only for \"contents\" command)")
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

object KlibToolLogger : Logger {
    override fun warning(message: String) = org.jetbrains.kotlin.cli.klib.warn(message)
    override fun error(message: String) = org.jetbrains.kotlin.cli.klib.warn(message)
    override fun fatal(message: String) = org.jetbrains.kotlin.cli.klib.error(message)
    override fun log(message: String) = println(message)
}

val defaultRepository = File(DependencyProcessor.localKonanDir.resolve("klib").absolutePath)

open class ModuleDeserializer(val library: ByteArray) {
    protected val moduleHeader: KlibMetadataProtoBuf.Header
        get() = parseModuleHeader(library)

    val moduleName: String
        get() = moduleHeader.moduleName

    val packageFragmentNameList: List<String>
        get() = moduleHeader.packageFragmentNameList

}

class Library(val name: String, val requestedRepository: String?, val target: String) {

    val repository = requestedRepository?.File() ?: defaultRepository
    fun info() {
        val library = libraryInRepoOrCurrentDir(repository, name)
        val headerAbiVersion = library.versions.abiVersion
        val headerCompilerVersion = library.versions.compilerVersion
        val headerLibraryVersion = library.versions.libraryVersion
        val headerMetadataVersion = library.versions.metadataVersion
        val headerIrVersion = library.versions.irVersion
        val moduleName = ModuleDeserializer(library.moduleHeaderData).moduleName

        println("")
        println("Resolved to: ${library.libraryName.File().absolutePath}")
        println("Module name: $moduleName")
        println("ABI version: $headerAbiVersion")
        println("Compiler version: ${headerCompilerVersion}")
        println("Library version: $headerLibraryVersion")
        println("Metadata version: $headerMetadataVersion")
        println("IR version: $headerIrVersion")

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

        Library(File(name).name.removeSuffix(KLIB_FILE_EXTENSION_WITH_DOT), requestedRepository, target).remove(true)

        val library = libraryInCurrentDir(name)
        val newLibDir = File(repository, library.libraryFile.name.removeSuffix(KLIB_FILE_EXTENSION_WITH_DOT))

        library.libraryFile.unpackZippedKonanLibraryTo(newLibDir)
    }

    fun remove(blind: Boolean = false) {
        if (!repository.exists) error("Repository does not exist: $repository")

        val library = try {
            val library = libraryInRepo(repository, name)
            if (blind) warn("Removing The previously installed $name from $repository.")
            library

        } catch (e: Throwable) {
            if (!blind) println(e.message)
            null

        }
        library?.libraryFile?.deleteRecursively()
    }

    fun contents(output: Appendable, printSignatures: Boolean) {
        val module = loadModule()
        val signatureRenderer = if (printSignatures) DefaultIdSignatureRenderer("// ID signature: ") else IdSignatureRenderer.NO_SIGNATURE
        val printer = DeclarationPrinter(output, DefaultDeclarationHeaderRenderer, signatureRenderer)

        printer.print(module)
    }

    fun signatures(output: Appendable) {
        val module = loadModule()
        val printer = SignaturePrinter(output, DefaultIdSignatureRenderer())

        printer.print(module)
    }

    private fun loadModule(): ModuleDescriptor {
        val storageManager = LockBasedStorageManager("klib")
        val library = libraryInRepoOrCurrentDir(repository, name)
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
        "contents" -> library.contents(System.out, printSignatures)
        "signatures" -> library.signatures(System.out)
        "info" -> library.info()
        "install" -> library.install()
        "remove" -> library.remove()
        else -> error("Unknown command ${command.verb}.")
    }
}

