package org.jetbrains.kotlin.doc

import java.io.File
import org.jetbrains.kotlin.doc.model.KModel

/**
* A simple plugin to KDoc to generate information from the model into a directory
* which can be implemented using any kind of layout or format.
*/
trait Doclet {
    fun generate(model: KModel, outputDir: File): Unit
}
