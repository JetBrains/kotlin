/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.klib

import kotlin.system.exitProcess
import java.util.Properties
// TODO: Extract these as a shared jar?
import org.jetbrains.kotlin.backend.konan.library.impl.LibraryReaderImpl
import org.jetbrains.kotlin.backend.konan.library.KonanLibrarySearchPathResolver
import org.jetbrains.kotlin.backend.konan.util.File
import org.jetbrains.kotlin.backend.konan.util.copyTo
import org.jetbrains.kotlin.konan.target.TargetManager

fun printUsage() {
    println("Usage: klib <command> <library> <options>")
    println("where the commands are:")
    println("\tinfo\tgeneral information about the library")
    println("\tinstall\tinstall the library to the local repository")
    println("\tcontents\tlist contents of the library")
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

// TODO: Get rid of the hardcoded path.
val defaultRepository = File(File.userHome, ".konan")

class Library(val name: String, val requestedRepository: String?, val target: String) {

    val repository = requestedRepository?.File() ?: defaultRepository
    fun info() {
        val library = libraryInRepoOrCurrentDir(repository, name)
        val header = Properties()
        val manifestFile = library.manifestFile
        manifestFile.bufferedReader().use { reader ->
            header.load(reader)
        }
        val headerAbiVersion = library.abiVersion
        val moduleName = ModuleDeserializer(library.moduleHeaderData).moduleName

        println("")
        println("Resolved to: ${library.libDir.absolutePath}")
        println("Module name: $moduleName")
        println("ABI version: $headerAbiVersion")
        val targets = library.targetsDir.listFiles.map{it.name}.joinToString(", ")
        print("Available targets: $targets\n")
    }

    fun install() {
        remove(true) 

        val klibFile = libraryInCurrentDir(name).klibFile
        val newLocation = File(repository, "klib")
        newLocation.mkdirs()
        val newFile = File(newLocation, klibFile.name)
        klibFile.copyTo(newFile)
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
        library?.libDir?.deleteRecursively()
        library?.klibFile?.delete()
    }

    fun contents() {
        val library = libraryInRepoOrCurrentDir(repository, name)
        val printer = PrettyPrinter(
            library.moduleHeaderData, {name -> library.packageMetadata(name)})
        
        printer.packageFragmentNameList.forEach{ 
            printer.printPackageFragment(it)
        }
    }
}

// TODO: need to do something here.
val currentAbiVersion = 1

val File.konanLibrary 
    get() = LibraryReaderImpl(this, currentAbiVersion, null)

fun libraryInRepo(repository: File, name: String): LibraryReaderImpl {
    val resolver = KonanLibrarySearchPathResolver(listOf(repository.absolutePath), null, null, skipCurrentDir = true)
    return resolver.resolve(name).konanLibrary
}

fun libraryInCurrentDir(name: String): LibraryReaderImpl {
    val resolver = KonanLibrarySearchPathResolver(emptyList(), null, null)
    return resolver.resolve(name).konanLibrary
}

fun libraryInRepoOrCurrentDir(repository: File, name: String): LibraryReaderImpl {
    val resolver = KonanLibrarySearchPathResolver(listOf(repository.absolutePath), null, null)
    return resolver.resolve(name).konanLibrary
}


fun main(args: Array<String>) {
    val command = Command(args)

    val targetManager = TargetManager(command.options["target"]?.last())
    val target = targetManager.targetName

    val repository = command.options["-repository"]?.last()

    val library = Library(command.library, repository, target)

    warn("IMPORTANT: the library format is unstable now. It can change with any new git commit without warning!")

    when (command.verb) {
        "contents"  -> library.contents()
        "info"      -> library.info()
        "install"   -> library.install()
        "remove"    -> library.remove()
        else        -> error("Unknown command ${command.verb}.")
    }
}

