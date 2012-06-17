package org.jetbrains.kotlin.doc.highlighter2

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

fun File.write(callback: (FileOutputStream) -> Unit) =
    FileOutputStream(this).use<FileOutputStream, Unit>(callback)
