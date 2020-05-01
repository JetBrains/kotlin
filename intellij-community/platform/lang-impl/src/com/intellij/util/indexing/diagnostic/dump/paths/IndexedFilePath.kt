package com.intellij.util.indexing.diagnostic.dump.paths

data class IndexedFilePath(
  val originalFileSystemId: Int,
  val fileType: String?, //TODO: make not null after new installer.
  val originalFileUrl: String,
  val portableFilePath: PortableFilePath
) {
  override fun toString(): String = buildString {
    appendln("File URL = $originalFileUrl")
    appendln("File ID = $originalFileSystemId")
    appendln("File type = $fileType")
    appendln("Portable path = ${portableFilePath.presentablePath}")
  }
}