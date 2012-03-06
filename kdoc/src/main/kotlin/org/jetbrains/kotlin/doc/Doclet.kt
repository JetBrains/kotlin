package org.jetbrains.kotlin.doc

import org.jetbrains.kotlin.doc.model.KModel
import java.io.File

/**
 * A simple plugin to KDoc to generate information from the model into a directory
 * which can be implemented using any kind of layout or format.
 */
trait Doclet {
    fun generate(model: KModel, outputDir: File): Unit
}
