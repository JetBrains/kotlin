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
    val dir = File("../../idl")
    dir.mkdirs()

    val urlsPerFiles = urls.groupBy { it.second + ".idl" }

    urlsPerFiles.forEach { e ->
        val fileName = e.key
        val pkg = e.value.first().second

        File(dir, fileName).bufferedWriter().use { w ->
            w.appendln("namespace ${pkg};")
            w.appendln()
            w.appendln()

            e.value.forEach { pair ->
                val (url) = pair
                println("Loading ${url}...")

                w.appendln("// Downloaded from $url")
                if (url.endsWith(".idl")) {
                    w.appendln(URL(url).readText())
                } else {
                    extractIDLText(url, w)
                }
            }

            w.appendln()
        }
    }
}

private fun extractIDLText(url: String, out: Appendable) {
    //    val soup = Jsoup.connect(url).validateTLSCertificates(false).ignoreHttpErrors(true).get()
    val soup = Jsoup.parse(URL(url).readText())
    fun append(it : Element) {
        if (!it.tag().preserveWhitespace()) {
            return append(Element(Tag.valueOf("pre"), it.baseUri()).appendChild(it))
        }

        val text = it.text()
        out.appendln(text)
        if (!text.trimEnd().endsWith(";")) {
            out.appendln(";")
        }
    }

    soup.select("pre.idl").filter {!it.hasClass("extract")}.forEach(::append)
    soup.select("code.idl-code").forEach(::append)
    soup.select("spec-idl").forEach(::append)
}
