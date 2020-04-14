// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.createDirectories
import com.intellij.util.io.delete
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.asSequence

object IndexDiagnosticDumper {

  private val LOG = Logger.getInstance(IndexDiagnosticDumper::class.java)

  private val jacksonMapper by lazy {
    jacksonObjectMapper().registerKotlinModule().writerWithDefaultPrettyPrinter()
  }

  @Synchronized
  fun dumpProjectIndexingHistoryToLogSubdirectory(projectIndexingHistory: ProjectIndexingHistory) {
    val logPath = PathManager.getLogPath()
    try {
      val indexDiagnosticDirectory = Paths.get(logPath).resolve("index-diagnostic")
      indexDiagnosticDirectory.createDirectories()

      val fileNamePrefix = "diagnostic-"

      val diagnosticJson = indexDiagnosticDirectory.resolve("$fileNamePrefix${System.currentTimeMillis()}.json")

      val jsonIndexDiagnostic = JsonIndexDiagnostic.generateForHistory(projectIndexingHistory)
      jacksonMapper.writeValue(diagnosticJson.toFile(), jsonIndexDiagnostic)

      val limitOfHistories = 20
      val survivedHistories = Files.list(indexDiagnosticDirectory)
        .asSequence()
        .filter { it.fileName.toString().startsWith(fileNamePrefix) && it.fileName.toString().endsWith(".json") }
        .sortedByDescending { file ->
          val timeStamp = file.fileName.toString().substringAfter(fileNamePrefix).substringBefore(".json").toLongOrNull()
          timeStamp ?: 0L
        }
        .take(limitOfHistories)
        .toSet()

      val toBeRemovedFiles = Files.list(indexDiagnosticDirectory).asSequence().filterNot { it in survivedHistories }
      toBeRemovedFiles.forEach { it.delete() }
    }
    catch (e: Exception) {
      LOG.warn("Failed to dump index diagnostic", e)
    }
  }

}