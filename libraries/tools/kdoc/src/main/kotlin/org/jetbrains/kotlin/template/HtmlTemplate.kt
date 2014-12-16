package org.jetbrains.kotlin.template

abstract class HtmlTemplate() : TextTemplate() {

    fun tag(
            tagName: String,
            style: String? = null,
            className: String? = null,
            attributes: List<Pair<String, String>> = listOf(),
            content: () -> Unit) {
        val allAttributes = arrayListOf<Pair<String, String>>()
        if (style != null)
            allAttributes.add(Pair("style", style))
        if (className != null)
            allAttributes.add(Pair("class", className))

        allAttributes.addAll(attributes)

        print(
                if (allAttributes.isEmpty()) {
                    "<$tagName>"
                }
                else {
                    // TODO: escape values
                    "<$tagName ${allAttributes.map { t -> "${t.first}='${t.second.escapeHtml()}'" }.makeString(" ")}>"
                }
        )
        content()
        print("</$tagName>")
    }

    fun text(text: String) {
        print(text.escapeHtml())
    }

    fun tag(tagName: String, content: String) =
    tag(tagName) {
        text(content)
    }

    fun html(content: () -> Unit) {
        println("<!DOCTYPE html>")
        tag(tagName = "html", content = content)
    }

    fun head(content: () -> Unit) =
        tag(tagName = "head", content = content)

    fun linkCssStylesheet(href: String) =
        tag(
            tagName = "link",
            attributes = listOf(Pair("rel", "stylesheet"), Pair("type", "text/css"), Pair("href", href))) {}

    fun body(style: String? = null, className: String? = null, content: () -> Unit) =
        tag(tagName = "body", style = style, className = className, content = content)

    fun table(style: String? = null, className: String? = null, content: () -> Unit) =
        tag(tagName = "table", style = style, className = className, content = content)

    fun tr(style: String? = null, className: String? = null, content: () -> Unit) =
        tag(tagName = "tr", style = style, className = className, content = content)

    fun td(style: String? = null, className: String? = null, content: () -> Unit) =
        tag(tagName = "td", style = style, className = className, content = content)

    fun title(title: String) =
        tag("title", title)

    fun div(style: String? = null, className: String? = null, content: () -> Unit) =
        tag(tagName = "div", style = style, className = className, content = content)

    fun span(style: String? = null, className: String? = null, content: () -> Unit) =
        tag(tagName = "span", style = style, className = className, content = content)

    fun a(href: String? = null, name: String? = null, content: () -> Unit) {
        val attributes = arrayListOf<Pair<String, String>>()
        if (href != null)
            attributes.add(Pair("href", href))
        if (name != null)
            attributes.add(Pair("name", name))
        tag(tagName = "a", attributes = attributes, content = content)
    }
}

