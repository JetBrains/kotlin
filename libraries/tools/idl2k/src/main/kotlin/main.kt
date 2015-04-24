package org.jetbrains.idl2k

import java.io.File
import java.io.StringReader

fun main(args: Array<String>) {
    val idl = getAllIDLs()
    val defs = parseIDL(StringReader(idl))

    println("IDL dump:")
    idl.lineSequence().forEachIndexed { i, line ->
        println("${i.toString().padStart(4, ' ')}: ${line}")
    }
    println()

    val definitions = mapDefinitions(defs, defs.interfaces.values())

    File("../../../js/js.libraries/src/core/dom3.kt").writer().use { w ->
        w.appendln("/*")
        w.appendln(" * Generated file")
        w.appendln(" * DO NOT EDIT")
        w.appendln(" * ")
        w.appendln(" * See libraries/tools/idl2k for details")
        w.appendln(" */")

        w.appendln()
        w.appendln("package org.w3c.dom3")
        w.appendln()

        w.render(definitions)
    }
}
