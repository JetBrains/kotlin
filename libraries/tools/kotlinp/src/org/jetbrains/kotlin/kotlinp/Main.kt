/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp

import java.io.File
import java.io.IOException
import kotlin.system.exitProcess

object Main {
    private fun run(args: Array<String>) {
        val paths = arrayListOf<String>()
        var verbose = false

        var i = 0
        while (true) {
            val arg = args.getOrNull(i++) ?: break

            if (arg == "-help" || arg == "-h") {
                printUsageAndExit()
            } else if (arg == "-verbose") {
                verbose = true
            } else if (arg == "-version") {
                printVersionAndExit()
            } else if (arg.startsWith("-")) {
                throw KotlinpException("unsupported argument: $arg")
            } else {
                paths.add(arg)
            }
        }

        val kotlinp = Kotlinp(KotlinpSettings(verbose))

        for (path in paths) {
            val file = File(path)
            if (!file.exists()) throw KotlinpException("file does not exist: $path")

            val text = try {
                when (file.extension) {
                    "class" -> kotlinp.renderClassFile(kotlinp.readClassFile(file))
                    "kotlin_module" -> kotlinp.renderModuleFile(kotlinp.readModuleFile(file))
                    else -> throw KotlinpException("only .class and .kotlin_module files are supported")
                }
            } catch (e: IOException) {
                throw KotlinpException("I/O operation failed: ${e.message}")
            }

            print(text)
        }

        if (paths.isEmpty()) {
            throw KotlinpException("no files specified")
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            run(args)
        } catch (e: KotlinpException) {
            System.err.println("error: " + e.message)
            exitProcess(1)
        }
    }

    private fun printUsageAndExit() {
        println(
            """kotlinp: print Kotlin declarations in the given class file.

Usage: kotlinp <options> <classes>
where possible options include:
  -verbose                   Display information in more detail, minimizing ambiguities but worsening readability
  -version                   Display Kotlin version
  -help (-h)                 Print a synopsis of options
"""
        )
        exitProcess(0)
    }

    private fun printVersionAndExit() {
        // TODO: get version from manifest
        val version = "@snapshot@"

        println("Kotlin version " + version + " (JRE " + System.getProperty("java.runtime.version") + ")")
        exitProcess(0)
    }
}
