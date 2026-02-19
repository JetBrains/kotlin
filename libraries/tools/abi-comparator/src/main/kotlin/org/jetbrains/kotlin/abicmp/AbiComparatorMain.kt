/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp

import org.jetbrains.kotlin.abicmp.defects.Location
import org.jetbrains.kotlin.abicmp.reports.ClassReport
import org.jetbrains.kotlin.abicmp.reports.ModuleMetadataReport
import org.jetbrains.kotlin.abicmp.tasks.CheckerConfigurationBuilder
import org.jetbrains.kotlin.abicmp.tasks.DirTask
import org.jetbrains.kotlin.abicmp.tasks.JarTask
import org.jetbrains.kotlin.abicmp.tasks.checkerConfiguration
import java.io.File
import java.io.IOException
import java.util.jar.JarFile
import kotlin.system.exitProcess

fun usage() {
    println(
        """
        Usage:
        java -jar abi-comparator.jar jar <jarPath1> <jarPath2> <reportFilePath.html>
        OR
        java -jar abi-comparator.jar dir <inputDir1> <inputDir2> <reportDir>
        """.trimIndent()
    )
    exitProcess(1)
}

fun runJarComparator(jar1: File, jar2: File, report: File) {
    when {
        !jar1.isFile -> {
            println("ERROR: jar1 doesn't exist: `${jar1.absolutePath}`")
            usage()
        }
        !jar2.isFile -> {
            println("ERROR: jar2 doesn't exist: `${jar2.absolutePath}`")
            usage()
        }
    }

    val task = JarTask(
        "MainReportHeader",
        JarFile(jar1),
        JarFile(jar2),
        "ReportHeader1",
        "ReportHeader2",
        report,
        checkerConfiguration {}
    )

    println("Starting")
    task.run()
    println("Finished")
}

fun runDirComparator(dir1: File, dir2: File, reportDir: File) {
    when {
        !dir1.isDirectory -> {
            println("ERROR: input directory #1 does not exist `${dir1.absolutePath}`")
            usage()
        }
        !dir2.isDirectory -> {
            println("ERROR: input directory #2 does not exist `${dir2.absolutePath}`")
            usage()
        }
        !reportDir.isDirectory -> {
            println("ERROR: report directory does not exist `${reportDir.absolutePath}`")
            usage()
        }
    }
    val task = DirTask(
        dir1,
        dir2,
        "id1",
        "id2",
        "ReportHeader1",
        "ReportHeader2",
        reportDir,
        checkerConfiguration {}
    )
    println("Starting")
    task.run()
    println("Finished")

}

class AbiComparatorMain {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            if (args.size < 4) {
                println("Not enough arguments")
                usage()
            }
            when (val mode = args[0]) {
                "jar" -> runJarComparator(File(args[1]), File(args[2]), File(args[3]))
                "dir" -> runDirComparator(File(args[1]), File(args[2]), File(args[3]))
                else -> {
                    println("Unknown option $mode")
                    usage()
                }
            }
        }
    }
}