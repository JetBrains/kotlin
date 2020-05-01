package com.intellij.util.indexing.diagnostic.dump.paths

data class IndexedFilePath(
  val originalFileSystemId: Int,
  val fileType: String?, //TODO: make not null after new installer.
  val originalFileUrl: String,
  val portableFilePath: PortableFilePath,
  val filePropertyPusherValues: Map<String /* Pusher presentable name */, String /* Presentable file immediate pushed value */>? //TODO: make not null after new installer.
) {
  override fun toString(): String = buildString {
    appendln("File URL = $originalFileUrl")
    appendln("File ID = $originalFileSystemId")
    appendln("File type = $fileType")
    appendln("Portable path = ${portableFilePath.presentablePath}")
    append("File property pusher values: ")
    if (filePropertyPusherValues != null && filePropertyPusherValues.isNotEmpty()) {
      appendln()
      for ((key, value) in filePropertyPusherValues) {
        appendln("  $key -> $value")
      }
    } else {
      appendln("<empty>")
    }
  }
}