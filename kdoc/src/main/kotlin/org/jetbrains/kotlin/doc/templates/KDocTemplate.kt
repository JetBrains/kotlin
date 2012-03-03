package org.jetbrains.kotlin.doc.templates

import std.*
import std.util.*
import org.jetbrains.kotlin.model.KClass
import org.jetbrains.kotlin.model.KAnnotation
import org.jetbrains.kotlin.model.KPackage
import org.jetbrains.kotlin.template.TextTemplate
import org.jetbrains.kotlin.model.KFunction
import java.util.Collection
import org.jetbrains.kotlin.model.KProperty
import org.jetbrains.kotlin.model.KType
import java.util.List

abstract class KDocTemplate() : TextTemplate() {
    open fun rootHref(pkg: KPackage): String {
        return if (pkg.external) {
            // TODO deal with external classes
            ""
        } else relativePrefix()
    }

    open fun href(p: KPackage): String
        = "${rootHref(p)}${p.nameAsPath}/package-summary.html"

    open fun href(c: KClass): String {
        val postfix = if (c.pkg.external) "?is-external=true" else ""
        return "${rootHref(c.pkg)}${c.nameAsPath}.html$postfix"
    }

    open fun href(f: KFunction): String {
        return if (f.owner is KClass) {
            "${href(f.owner)}#${f.link}"
        } else {
            "package-summary.html#${f.link}"
        }
    }

    open fun href(f: KProperty): String {
        return if (f.owner is KClass) {
            "${href(f.owner)}#${f.link}"
        } else {
            "package-summary.html#${f.link}"
        }
    }

    open fun extensionsHref(pkg: KPackage, c: KClass): String {
        // is inside the pkg so no need to use it...
        return "${c.nameAsPath}-extensions.html"
    }

    open fun extensionsHref(pkg: KPackage, c: KClass, f: KFunction): String {
        return "${extensionsHref(pkg, c)}#${f.link}"
    }

    open fun sourceHref(f: KFunction): String {
        val owner = f.owner
        return if (owner is KClass) {
            "${rootHref(owner.pkg)}src-html/${owner.simpleName}.html#line.${f.sourceLine}"
        } else if (owner is KPackage) {
            // TODO how to find the function in a package???
            "${rootHref(owner)}src-html/namespace.html#line.${f.sourceLine}"
        } else href(f)
    }

    open fun link(c: KClass, fullName: Boolean = false): String {
        val prefix = if (c.isAnnotation()) "@" else ""
        val name = if (fullName) c.name else c.simpleName
        return "<A HREF=\"${href(c)}\" title=\"${c.kind} in ${c.packageName}\">$prefix$name</A>"
    }

    open fun link(t: KType, fullName: Boolean = false): String {
        val c = t.klass
        val arguments = t.arguments
        return if (c != null) {
            val prefix = if (c.isAnnotation()) "@" else ""
            val cname = c.name
            if ((cname.startsWith("jet.Function") || cname.startsWith("jet.ExtensionFunction")) && arguments.notEmpty()) {
                val rt = arguments.last()
                // TODO use drop()
                val rest = arguments.subList(0, arguments.size - 1).notNull()
                "${typeArguments(rest, "(", ")", "()")}&nbsp;<A HREF=\"${href(c)}\" title=\"${c.kind} in ${c.packageName}\">-&gt;</a>&nbsp;${link(rt)}"
            } else if (cname.startsWith("jet.Tuple")) {
                if (arguments.isEmpty()) {
                    "Unit"
                } else {
                    "<A HREF=\"${href(c)}\" title=\"${c.kind} in ${c.packageName}\">#</a>${typeArguments(arguments, "(", ")", "()")}"
                }
            } else {
                val name = if (fullName) cname else c.simpleName
                "<A HREF=\"${href(c)}\" title=\"${c.kind} in ${c.packageName}\">$prefix$name</A>${typeArguments(arguments)}"
            }
        } else {
            "${t.name}${typeArguments(arguments)}"
        }
    }

    open fun typeArguments(arguments: List<KType>, val prefix: String = "&lt;", val postfix: String = "&gt;", val empty: String = ""): String {
        val text = arguments.map<KType, String>() { link(it) }.join(", ")
        return if (text.length() == 0) empty else "$prefix$text$postfix"
    }

    open fun link(p: KPackage): String {
        return "<A HREF=\"${href(p)}\" title=\"package ${p.name}}\">${p.name}</A>"
    }

    open fun link(a: KAnnotation): String {
        val c = a.klass
        return "<A HREF=\"${href(c)}\" title=\"annotation in ${c.packageName}\">@${c.simpleName}</A>"
    }

    protected open fun relativePrefix(): String = ""
}
