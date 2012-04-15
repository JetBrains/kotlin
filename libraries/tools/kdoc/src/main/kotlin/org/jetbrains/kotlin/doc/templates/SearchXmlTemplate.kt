package org.jetbrains.kotlin.doc.templates

import kotlin.*
import org.jetbrains.kotlin.template.*
import kotlin.io.*
import kotlin.util.*
import java.util.*
import org.jetbrains.kotlin.doc.model.KModel

class SearchXmlTemplate(val model: KModel) : KDocTemplate() {
    override fun render() {
        println("""<?xml version="1.0" encoding="UTF-8"?>
<searches>""")
        for (c in model.classes) {
            println("""<search>
  <href>${c.nameAsPath}.html</href>
  <name>${c.name}</name>
  <kind>${c.kind}</kind>
</search>""")
        }
        println("""</searches>""")
    }
}
