package org.jetbrains.kotlin.gradle.tasks

import java.io.File

class KaptOptions {
    var supportInheritedAnnotations: Boolean = false
    var classFileStubsDir: File? = null
    var annotationsFile: File? = null
}