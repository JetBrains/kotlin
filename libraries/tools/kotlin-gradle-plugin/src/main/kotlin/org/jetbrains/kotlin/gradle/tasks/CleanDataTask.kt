/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import java.time.ZoneId

open class CleanDataTask : DefaultTask() {

    @Input
    lateinit var installationDirPath: String

    private val accessFileSuffix = "-access"

    @Input
    var ttl: Long = 30

    @Suppress("unused")
    @TaskAction
    fun exec() {
        val expirationDate = LocalDate.now().minusDays(ttl)
        val dir = File(installationDirPath)

        fun modificationDate(file: File): LocalDate {
            val lastModifiedTime = Files.getLastModifiedTime(file.toPath())
            return lastModifiedTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        }

        fun getAllLinkedFileNames(accessFileName: String): List<String> {
            return listOf(accessFileName, accessFileName.removeSuffix(accessFileSuffix))
        }

        val filesInDirByName = dir.listFiles()?.map { it.name to it }?.toMap()

        filesInDirByName
            ?.filter { (fileName, file) ->
                file.isFile && fileName.endsWith(accessFileSuffix)
                        && modificationDate(file).isBefore(expirationDate)
            }
            ?.flatMap { (filename, _) -> getAllLinkedFileNames(filename) }
            ?.forEach { fileName -> filesInDirByName[fileName]?.deleteRecursively()!! }

    }

    companion object {
        const val NAME: String = "KotlinClean"
    }

}