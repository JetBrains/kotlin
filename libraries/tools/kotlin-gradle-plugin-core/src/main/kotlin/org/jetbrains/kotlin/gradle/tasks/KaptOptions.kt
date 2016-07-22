package org.jetbrains.kotlin.gradle.tasks

import java.io.File

class KaptOptions {
    var supportInheritedAnnotations: Boolean = false
    var generateStubs: Boolean = false
    var annotationsFile: File? = null
}