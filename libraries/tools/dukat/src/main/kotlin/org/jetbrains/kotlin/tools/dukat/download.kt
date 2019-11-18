/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.dukat

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
            w.appendln("package $pkg;")
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

private val urls = listOf(
    "https://raw.githubusercontent.com/whatwg/html-mirror/master/source" to "org.w3c.dom",
    "https://html.spec.whatwg.org/" to "org.w3c.dom",
    "https://raw.githubusercontent.com/whatwg/dom/master/dom.html" to "org.w3c.dom",
    "https://dvcs.w3.org/hg/editing/raw-file/tip/editing.html" to "org.w3c.dom",
    "https://www.w3.org/TR/animation-timing/" to "org.w3c.dom",
    "https://www.w3.org/TR/geometry-1/" to "org.w3c.dom",
    "https://www.w3.org/TR/cssom-view/" to "org.w3c.dom",
    "https://www.w3.org/TR/touch-events/" to "org.w3c.dom",
    "https://www.w3.org/TR/uievents/" to "org.w3c.dom.events",
    "https://www.w3.org/TR/pointerevents/" to "org.w3c.dom.pointerevents",

    "https://drafts.csswg.org/cssom/" to "org.w3c.dom.css",
    "https://www.w3.org/TR/css-masking-1/" to "org.w3c.css.masking",

    "https://w3c.github.io/mediacapture-main/" to "org.w3c.dom.mediacapture",
    "https://www.w3.org/TR/DOM-Parsing/" to "org.w3c.dom.parsing",
    "https://w3c.github.io/clipboard-apis" to "org.w3c.dom.clipboard",
    "https://raw.githubusercontent.com/whatwg/url/master/url.html" to "org.w3c.dom.url",

    "https://www.w3.org/TR/SVG2/single-page.html" to "org.w3c.dom.svg",
    "https://www.khronos.org/registry/webgl/specs/latest/1.0/webgl.idl" to "org.khronos.webgl",
    "https://www.khronos.org/registry/typedarray/specs/latest/typedarray.idl" to "org.khronos.webgl",

    "https://raw.githubusercontent.com/whatwg/xhr/master/Overview.src.html" to "org.w3c.xhr",
    "https://raw.githubusercontent.com/whatwg/fetch/master/Overview.src.html" to "org.w3c.fetch",
    "https://raw.githubusercontent.com/w3c/FileAPI/gh-pages/index.html" to "org.w3c.files",

    "https://raw.githubusercontent.com/whatwg/notifications/master/notifications.html" to "org.w3c.notifications",
    "https://raw.githubusercontent.com/whatwg/fullscreen/master/fullscreen.html" to "org.w3c.fullscreen",
    "https://www.w3.org/TR/vibration/" to "org.w3c.vibration",

    "https://www.w3.org/TR/hr-time/" to "org.w3c.performance",
    "https://www.w3.org/TR/2012/REC-navigation-timing-20121217/" to "org.w3c.performance",

    "https://w3c.github.io/ServiceWorker/" to "org.w3c.workers"
)
