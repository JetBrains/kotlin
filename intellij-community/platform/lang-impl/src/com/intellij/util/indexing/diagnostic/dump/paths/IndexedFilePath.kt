package com.intellij.util.indexing.diagnostic.dump.paths

data class IndexedFilePath(
  val originalFileSystemId: Int,
  val fileType: String,
  val fileSize: Long?, //TODO: make not null after new installer.
  val originalFileUrl: String,
  val portableFilePath: PortableFilePath,
  val filePropertyPusherValues: Map<String /* Pusher presentable name */, String /* Presentable file immediate pushed value */>
) {
  override fun toString(): String = buildString {
    appendln("File URL = $originalFileUrl")
    appendln("File ID = $originalFileSystemId")
    if (fileSize != null) {
      appendln("File size = $fileSize")
    }
    appendln("File type = $fileType")
    appendln("Portable path = ${portableFilePath.presentablePath}")
    append("File property pusher values: ")
    if (filePropertyPusherValues.isNotEmpty()) {
      appendln()
      for ((key, value) in filePropertyPusherValues) {
        appendln("  $key -> $value")
      }
    } else {
      appendln("<empty>")
    }
  }
}