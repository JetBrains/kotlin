package org.jetbrains.kotlin.template

import java.util.List


abstract class HtmlTemplate() : TextTemplate() {

    fun tag(
            tagName: String,
            style: String? = null,
            className: String? = null,
            attributes: List<#(String, String)> = arrayList(),
            content: () -> Unit) {
        val allAttributesBuilder = listBuilder<#(String, String)>()
        if (style != null)
            allAttributesBuilder.add(#("style", style))
        if (className != null)
            allAttributesBuilder.add(#("class", className))

        // TODO: add addAll to ListBuilder
        for (attribute in attributes)
            allAttributesBuilder.add(attribute)

        val allAttributes = allAttributesBuilder.build()

        print(
                if (allAttributes.isEmpty()) {
                    "<$tagName>"
                }
                else {
                    // TODO: escape values
                    "<$tagName ${allAttributes.map { t -> "${t._1}='${t._2.escapeHtml()}'" }.makeString(" ")}>"
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
            attributes = arrayList(#("rel", "stylesheet"), #("type", "text/css"), #("href", href))) {}

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
        val attributes = listBuilder<#(String, String)>()
        if (href != null)
            attributes.add(#("href", href))
        if (name != null)
            attributes.add(#("name", name))
        tag(tagName = "a", attributes = attributes.build(), content = content)
    }
}

