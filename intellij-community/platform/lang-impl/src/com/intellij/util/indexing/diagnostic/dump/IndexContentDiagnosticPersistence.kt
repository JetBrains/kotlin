package com.intellij.util.indexing.diagnostic.dump

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.indexing.IndexingBundle
import java.nio.file.Path

object IndexContentDiagnosticPersistence {

  private val jacksonObjectMapper = jacksonObjectMapper()

  fun writeTo(indexContentDiagnostic: IndexContentDiagnostic, destination: Path) {
    jacksonObjectMapper.writerWithDefaultPrettyPrinter().writeValue(destination.toFile(), indexContentDiagnostic)
  }

  fun readFrom(file: Path, indicator: ProgressIndicator): IndexContentDiagnostic {
    indicator.text = IndexingBundle.message("index.content.diagnostic.reading")
    return jacksonObjectMapper.readValue(file.toFile())
  }
}