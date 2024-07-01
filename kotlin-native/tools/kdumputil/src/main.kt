/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import collections.nextOrNull
import hprof.item
import hprof.normalize
import hprof.readProfile
import hprof.write
import kdump.hprof.toHProfProfile
import kdump.item
import kdump.readDump
import text.prettyPrintln
import java.io.File
import java.io.PushbackInputStream

fun main(args: Array<String>) {
    main(args.iterator())
}

fun main(args: Iterator<String>) {
    when (args.nextOrNull()) {
        "print" ->
            args.nextOrNull()?.let { format ->
                args.nextOrNull()?.let { pathname ->
                    when (format) {
                        "kdump" -> mainPrintKdump(pathname)
                        "hprof" -> mainPrintHprof(pathname)
                        else -> null
                    }
                }
            }

        "convert" ->
            args.nextOrNull()?.let { inFormat ->
                args.nextOrNull()?.let { inPathname ->
                    args.nextOrNull()?.let { outFormat ->
                        args.nextOrNull()?.let { outPathname ->
                            when (inFormat) {
                                "kdump" ->
                                    when (outFormat) {
                                        "hprof" -> mainConvertKdumpHprof(inPathname, outPathname)
                                        else -> null
                                    }

                                else -> null
                            }
                        }
                    }
                }
            }

        else -> null
    } ?: mainUsage()
}

fun mainUsage() {
    println("Usage:")
    println("  kdumputil print kdump <file>")
    println("  kdumputil print hprof <file>")
    println("  kdumputil convert kdump <file> hprof <file>")
}

fun mainPrintKdump(pathname: String) {
    File(pathname).readDump().let { prettyPrintln { item(it) } }
}

fun mainPrintHprof(pathname: String) {
    File(pathname).readProfile().let { prettyPrintln { item(it) } }
}

fun mainConvertKdumpHprof(inPathname: String, outPathname: String) {
    File(inPathname)
            .inputStream()
            .buffered()
            .let { PushbackInputStream(it) }
            .readDump()
            .toHProfProfile()
            .normalize()
            .run { File(outPathname).write(this) }
}
