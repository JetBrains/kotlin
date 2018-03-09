/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp

import java.io.File
import java.io.IOException

object Main {
    private fun run(args: Array<String>) {
        val paths = arrayListOf<String>()

        var i = 0
        while (true) {
            val arg = args.getOrNull(i++) ?: break

            if ("-help" == arg || "-h" == arg) {
                printUsageAndExit()
            } else if ("-version" == arg) {
                printVersionAndExit()
            } else if (arg.startsWith("-")) {
                throw KotlinpException("unsupported argument: $arg")
            } else {
                paths.add(arg)
            }
        }

        for (path in paths) {
            if (!path.endsWith(".class")) throw KotlinpException("only .class files are supported")

            val file = File(path)
            if (!file.exists()) throw KotlinpException("file does not exist: $path")

            try {
                print(Kotlinp.renderClassFile(Kotlinp.readClassFile(file)))
            } catch (e: IOException) {
                throw KotlinpException("I/O operation failed: ${e.message}")
            }
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
            System.exit(1)
        }
    }

    private fun printUsageAndExit() {
        println(
            """kotlinp: print Kotlin declarations in the given class file.

Usage: kotlinp <options> <classes>
where possible options include:
  -version                   Display Kotlin version
  -help (-h)                 Print a synopsis of options
"""
        )
        System.exit(0)
    }

    private fun printVersionAndExit() {
        // TODO: get version from manifest
        val version = "@snapshot@"

        println("Kotlin version " + version + " (JRE " + System.getProperty("java.runtime.version") + ")")
        System.exit(0)
    }
}
