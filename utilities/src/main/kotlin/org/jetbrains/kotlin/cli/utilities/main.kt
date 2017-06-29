package org.jetbrains.kotlin.cli.utilities

import org.jetbrains.kotlin.backend.konan.util.File
import org.jetbrains.kotlin.cli.bc.main as konancMain
import org.jetbrains.kotlin.native.interop.gen.jvm.main as cinteropMain
import org.jetbrains.kotlin.cli.klib.main as klibMain

fun invokeCinterop(args: Array<String>) {
    var outputFileName = "nativelib"
    var target = "host"
    for (i in args.indices) {
        if (args[i].startsWith("-o")) 
            outputFileName = args.getOrElse(i+1){outputFileName}
        if (args[i] == "-target") 
            target = args.getOrElse(i+1){target}
    }

    val buildDir = File("$outputFileName-build")
    val generatedDir = File(buildDir, "kotlin")
    val nativesDir = File(buildDir, "natives")
    val cstubsName ="cstubs"
    val manifest = File(buildDir, "manifest.properties")

    val additionalArgs = listOf<String>(
        "-generated", generatedDir.path, 
        "-natives", nativesDir.path, 
        "-cstubsname", cstubsName,
        "-manifest", manifest.path,
        "-flavor", "native")

    val cinteropArgs = (additionalArgs + args.toList()).toTypedArray()
    cinteropMain(cinteropArgs)

    val konancArgs = arrayOf(
        generatedDir.path, 
        "-produce", "library", 
        "-nativelibrary", File(nativesDir, "$cstubsName.bc").path,
        "-o", outputFileName,
        "-target", target,
        "-manifest", manifest.path
     )
    konancMain(konancArgs)
}

fun main(args: Array<String>) {
    val utilityName = args[0]
    val utilityArgs = args.drop(1).toTypedArray()
    when (utilityName) {
        "konanc" ->
            konancMain(utilityArgs)
        "cinterop" ->
            invokeCinterop(utilityArgs)
        "klib" ->
            klibMain(utilityArgs)
        else ->
            error("Unexpected utility name")
    }
}

