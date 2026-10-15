package kdump

import java.io.File
import java.io.PushbackInputStream

fun File.readDump() = inputStream().buffered().run { PushbackInputStream(this) }.readDump()