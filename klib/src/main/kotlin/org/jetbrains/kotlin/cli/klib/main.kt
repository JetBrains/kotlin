package org.jetbrains.kotlin.cli.klib

import kotlin.system.exitProcess
import java.util.Properties
// TODO: Extract these as a shared jar?
import org.jetbrains.kotlin.backend.konan.library.SplitLibraryReader
import org.jetbrains.kotlin.backend.konan.util.File
import org.jetbrains.kotlin.backend.konan.util.copyTo
import org.jetbrains.kotlin.backend.konan.TargetManager

fun printUsage() {
    println("Usage: klib <command> <library> <options>")
    println("where the commands are:")
    println("\tinfo\tgeneral information about the library")
    println("\tinstall\tinstall the library to the local repository")
    println("\tlist\tcontents of the library")
    println("\tremove\tremove the library from the local repository")
    println("and the options are:")
    println("\t-repository <path>\twork with the specified repository")
    println("\t-target <name>\tinspect specifics of the given target")
}

private fun parseArgs(args: Array<String>): Map<String, List<String>> {
    val commandLine = mutableMapOf<String, MutableList<String>>()
    for (index in 0..args.size - 1 step 2) {
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


class Command(args: Array<String>){
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

fun error(text: String) {
    println("error: $text")
    exitProcess(1)
}

class Library(val name: String, val repository: String, val target: String) {

    val file = File(name)
    val repositoryFile = File(repository)

    // TODO: need to do something here.
    val currentAbiVersion = 1

    val splitLibrary = SplitLibraryReader(file, currentAbiVersion, target)
    val manifestFile = splitLibrary.manifestFile

    fun info() {
        val header = Properties()
        manifestFile.bufferedReader().use { reader ->
            header.load(reader)
        }
        val headerAbiVersion = header.getProperty("abi_version")!!
        val moduleName = header.getProperty("module_name")!!
        println("Module name: $moduleName")
        println("ABI version: $headerAbiVersion")
        val targets = splitLibrary.targetsDir.listFiles.map{it.name}.joinToString(", ")
        print("Available targets: $targets\n")
    }

    fun install() {
        remove()
        val baseName = splitLibrary.klibFile.name
        val newKlibName = File(repositoryFile, baseName)
        splitLibrary.klibFile.copyTo(newKlibName)
    }

    fun remove() {
        repositoryFile.mkdirs()
        val baseName = splitLibrary.klibFile.name
        val newDirName = File(repositoryFile, splitLibrary.libDir.name)
        val newKlibName = File(repositoryFile, baseName)
        newKlibName.deleteRecursively()
        newDirName.deleteRecursively()
    }

    fun list() {
        // TODO: implement printing deserialized module protobuf.
        println("Module listing not implemented yet.")
    }
}

fun main(args: Array<String>) {
    val command = Command(args)

    val targetManager = TargetManager(command.options["target"]?.last())
    val target = targetManager.currentName

    val repository = command.options["repository"]?.last()
    val repositoryList = repository ?.let { listOf(it) } ?: emptyList()

    val userHome = File(System.getProperty("user.home")).absolutePath
    val userKonan = File(userHome, ".konan")
    val userRepo = File(userKonan, "klib")

    val library = Library(command.library, repository ?: userRepo.path, target)

    warn("IMPORTANT: the library format is unstable now. It can change with any new git commit without warning!")

    when (command.verb) {
        "info"      -> library.info()
        "install"   -> library.install()
        "list"      -> library.list()
        "remove"    -> library.remove()
        else        -> error("Unknown command ${command.verb}.")
    }
}

