package org.jetbrains.kotlin.site

import junit.framework.TestCase
import java.io.File
import kotlin.util.arrayList

class GenerateSiteTest : TestCase() {

    fun testGenerateSite(): Unit {
        val srcDir = findTemplateDir()
        val generator = SiteGenerator(srcDir, File(srcDir, "../../../target/site"))
        generator.run()
    }

    fun findTemplateDir(): File {
        val path = "src/main/templates"
        for (p in arrayList(".", "apidocs", "library/apidocs")) {
            val sourceDir = File(".", path)
            if (sourceDir.exists()) {
                return sourceDir
            }
        }
        throw IllegalArgumentException("Could not find template directory: $path")
    }
}