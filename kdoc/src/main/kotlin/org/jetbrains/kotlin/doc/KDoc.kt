package org.jetbrains.kotlin.doc

import std.template.*

import org.jetbrains.kotlin.doc.templates.*
import org.jetbrains.kotlin.model.KModel

import java.io.File

class KDocProcessor(val model: KModel, val outputDir: File) {

    fun execute(): Unit {
        run("index.html", IndexTemplate(model))
    }

    protected fun run(fileName: String, template: TextTemplate): Unit {
        val file = File(outputDir, fileName)
        file.getParentFile()?.mkdirs()

        log("Generating $fileName")
        template.renderTo(file)
    }

    protected fun log(text: String) {
        println(text)
    }

}
