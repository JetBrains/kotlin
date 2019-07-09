/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.idl2k.dl

import org.jetbrains.idl2k.urls
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Tag
import java.io.File
import java.net.URL

fun main(args: Array<String>) {
    val dir = File("../../stdlib/js/idl")
    dir.mkdirs()

    var packageFilter: String? = null
    val argsIterator = args.iterator()
    while (argsIterator.hasNext()) {
        val arg = argsIterator.next()

        when (arg) {
            "--pkg" -> if (argsIterator.hasNext()) packageFilter = argsIterator.next() else throw IllegalArgumentException("argument $arg requires argument")
            else -> throw IllegalArgumentException("Argument $arg is unknown")
        }
    }

    val urlsPerFiles = urls.filter { packageFilter == null || it.second == packageFilter }.groupBy { it.second + ".idl" }

    urlsPerFiles.forEach { e ->
        val fileName = e.key
        val pkg = e.value.first().second

        File(dir, fileName).bufferedWriter().use { w ->
            w.appendln("namespace $pkg;")
            w.appendln()
            w.appendln()

            e.value.forEach { (url) ->
                println("Loading $url...")

                w.appendln("// Downloaded from $url")
                val content = fetch(url)

                if (content != null) {
                    if (url.endsWith(".idl")) {
                        w.appendln(content)
                    } else {
                        extractIDLText(content, w)
                    }
                }
            }

            w.appendln()
        }
    }
}

private fun fetch(url: String): String? {
    try {
        return URL(url).readText()
    } catch (e: Exception) {
        println("failed to download ${url}, if it's not a local problem, revisit the list of downloaded entities")
        e.printStackTrace()
        return null
    }
}

private fun Appendable.append(element: Element) {
    val text = element.text()
    appendln(text)
    if (!text.trimEnd().endsWith(";")) {
        appendln(";")
    }
}


private fun List<Element>.attachTo(out: Appendable) = map { element ->
    if (!element.tag().preserveWhitespace()) {
        Element(Tag.valueOf("pre"), element.baseUri()).appendChild(element)
    } else element
}.forEach { out.append(it) }


private fun extractIDLText(rawContent: String, out: Appendable) {
    val soup = Jsoup.parse(rawContent)

    soup.select(".dfn-panel").remove()

    soup.select("pre.idl").filter {!it.hasClass("extract")}.attachTo(out)
    soup.select("code.idl-code").attachTo(out)
    soup.select("spec-idl").attachTo(out)
}
