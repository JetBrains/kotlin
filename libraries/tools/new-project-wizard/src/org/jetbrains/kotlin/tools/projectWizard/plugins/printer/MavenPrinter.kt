package org.jetbrains.kotlin.tools.projectWizard.plugins.printer

import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.render

class
MavenPrinter(override val indent: Int = 4) : BuildFilePrinter() {
    fun node(
        name: String,
        attributes: List<Pair<String, String>> = emptyList(),
        singleLine: Boolean = false,
        body: () -> Unit
    ) {
        indent()
        +"<$name"
        if (attributes.isNotEmpty()) {
            +" "
            attributes.list(separator = { +" " }) { (attribute, value) ->
                +attribute
                +"= \""
                +value
                +"\""
            }
        }
        +">"
        if (singleLine) body()
        else {
            indented {
                nl()
                body()
            }
        }
        if (!singleLine) indent()
        +"</$name>"
        nl()
    }

    // We already have line break after node
    override fun <T : BuildSystemIR> List<T>.listNl() {
        list(separator = { +"" }) { it.render(this) }
    }

    val String.quotified
        get() = "\"$this\""

    fun singleLineNode(
        name: String,
        attributes: List<Pair<String, String>> = emptyList(),
        body: () -> Unit
    ) = node(name, attributes, singleLine = true, body = body)

    fun pom(body: () -> Unit) {
        +"""
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xmlns="http://maven.apache.org/POM/4.0.0"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
              """.trimIndent()
        indented {
            nl()
            singleLineNode("modelVersion") { +"4.0.0" }
            nl()
            body()
        }
        +"</project>"
    }
}