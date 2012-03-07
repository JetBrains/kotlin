package org.jetbrains.kotlin.site

import kotlin.io.*

import java.io.File
import org.pegdown.Extensions
import org.pegdown.PegDownProcessor
import org.pegdown.LinkRenderer

class SiteGenerator(val sourceDir: File, val outputDir: File) : Runnable {
    public var markdownProcessor: PegDownProcessor = PegDownProcessor(Extensions.ALL)
    public var linkRendered: LinkRenderer = LinkRenderer()

    override fun run() {
        println("Generating the site to $outputDir")

        sourceDir.recurse {
            if (it.isFile()) {
                var relativePath = sourceDir.relativePath(it)
                println("Processing ${relativePath}")
                var text = it.readText()
                if (it.extension == "md") {
                    text = markdownProcessor.markdownToHtml(text, linkRendered) ?: ""
                    relativePath = relativePath.trimTrailing(it.extension) + "html"
                }
                text = layout(relativePath, it, text)
                val outFile = File(outputDir, relativePath)
                outFile.directory.mkdirs()
                outFile.writeText(text)
            }
        }
    }

    /**
     * Applies a layout to the given file
     */
    fun layout(uri: String, file: File, text: String): String {
        return """<html>
<body>
$text
</body>
</html>
"""
    }
}