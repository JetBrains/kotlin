package org.jetbrains.idl2k

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Tag
import java.net.URL

val urls = listOf(
        "https://raw.githubusercontent.com/whatwg/html-mirror/master/source",
        "https://html.spec.whatwg.org/",
        "https://raw.githubusercontent.com/whatwg/dom/master/dom.html",
        "http://www.w3.org/TR/uievents/",
        "https://dvcs.w3.org/hg/editing/raw-file/tip/editing.html",
        "https://raw.githubusercontent.com/whatwg/xhr/master/Overview.src.html",
        "https://raw.githubusercontent.com/w3c/FileAPI/gh-pages/index.html",
        "https://raw.githubusercontent.com/whatwg/notifications/master/notifications.html",
        "https://raw.githubusercontent.com/whatwg/fullscreen/master/Overview.src.html",
        "http://www.w3.org/TR/DOM-Parsing/",
        "http://slightlyoff.github.io/ServiceWorker/spec/service_worker/index.html",
        "https://raw.githubusercontent.com/whatwg/fetch/master/Overview.src.html",
        "http://www.w3.org/TR/vibration/",
        "http://dev.w3.org/csswg/cssom/",
        "https://www.khronos.org/registry/webgl/specs/latest/1.0/webgl.idl"
)

private fun extractIDLText(url: String, out: StringBuilder) {
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

fun getAllIDLs(): String =
    StringBuilder {
        urls.forEach {
            if (it.endsWith(".idl")) {
                appendln(URL(it).readText())
            } else {
                extractIDLText(it, this)
            }
        }
    }.toString()

