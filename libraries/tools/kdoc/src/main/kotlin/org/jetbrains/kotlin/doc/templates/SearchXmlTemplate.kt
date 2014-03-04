package org.jetbrains.kotlin.doc.templates

import java.util.*
import org.jetbrains.kotlin.doc.model.*

class SearchXmlTemplate(val model: KModel): KDocTemplate() {

    class Search(val name: String, val href: String, val kind: String) {
        override fun toString() = "Search($name, $href, $kind)"
    }

    override fun render() {
        val map = TreeMap<String, Search>()

        fun add(name: String, href: String, kind: String): Unit {
            val search = Search(name, href, kind)
            if (!map.containsKey(name)) {
                map.put(name, search)
            }
        }

        fun add(owner: KClass, c: KFunction, href: String): Unit {
            add("${c.name}() [${owner.name}]", href, "fun")
        }

        fun add(owner: KClass, c: KProperty, href: String): Unit {
            add("${c.name} [${owner.name}]", href, c.kind())
        }

        for (c in model.classes) {
            add("${c.simpleName} [${c.pkg.name}]", "${c.nameAsPath}.html", c.kind)

            c.functions.forEach{ add(c, it, href(it)) }
            c.properties.forEach{ add(c, it, href(it)) }
        }

        for (p in model.packages) {
            val map = inheritedExtensionFunctions(p.functions)
            val pmap = inheritedExtensionProperties(p.properties)
            val classes = hashSet<KClass>()
            classes.addAll(map.keySet())
            classes.addAll(pmap.keySet())
            for (c in classes) {
                if (c != null) {
                    val functions = map.get(c).orEmpty()
                    val properties = pmap.get(c).orEmpty()

                    functions.forEach{ add(c, it, p.nameAsPath + "/" + extensionsHref(p, c, it)) }
                    functions.forEach{ add(c, it, p.nameAsPath + "/" + extensionsHref(p, c, it)) }
                }
            }
        }

        println("""<?xml version="1.0" encoding="UTF-8"?>
<searches>""")
        for (s in map.values()!!) {
            println("""<search>
  <href>${s.href}</href>
  <name>${s.name}</name>
  <kind>${s.kind}</kind>
</search>""")
        }
        println("""</searches>""")
    }

}
