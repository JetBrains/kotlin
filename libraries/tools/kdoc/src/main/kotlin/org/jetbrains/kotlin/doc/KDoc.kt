package org.jetbrains.kotlin.doc

import java.io.File
import org.jetbrains.kotlin.doc.highlighter2.Html2CompilerPlugin
import org.jetbrains.kotlin.doc.model.*

/** Generates the Kotlin Documentation for the model */
class KDoc(arguments: KDocArguments) : KModelCompilerPlugin(arguments) {

    protected override fun processModel(model: KModel) {
        val outputDir = File(arguments.apply().docOutputDir)

        // TODO allow this to be configured; maybe we use configuration on the KotlinModule
        // to define what doclets to use?
        val generator = JavadocStyleHtmlDoclet()
        generator.generate(model, outputDir)

        val srcGenerator = Html2CompilerPlugin(arguments)
        srcGenerator.generate(model, outputDir)
    }
}
