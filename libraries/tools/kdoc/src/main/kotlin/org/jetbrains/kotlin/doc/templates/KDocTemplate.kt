package org.jetbrains.kotlin.doc.templates

import kotlin.*
import kotlin.util.*
import org.jetbrains.kotlin.doc.model.KClass
import org.jetbrains.kotlin.doc.model.KAnnotation
import org.jetbrains.kotlin.doc.model.KPackage
import org.jetbrains.kotlin.template.TextTemplate
import org.jetbrains.kotlin.doc.model.KFunction
import java.util.Collection
import org.jetbrains.kotlin.doc.model.KProperty
import org.jetbrains.kotlin.doc.model.KType
import java.util.List

abstract class KDocTemplate() : TextTemplate() {
    open fun rootHref(pkg: KPackage): String {
        return if (!pkg.useExternalLink)
            relativePrefix()
        else
            pkg.model.config.resolveLink(pkg.name)
    }

    open fun href(p: KPackage): String
    = "${rootHref(p)}${p.nameAsPath}/package-summary.html"

    open fun href(c: KClass): String {
        val postfix = if (!c.pkg.useExternalLink) "" else "?is-external=true"
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

    open fun extensionsHref(pkg: KPackage, c: KClass, f: KProperty): String {
        return "${extensionsHref(pkg, c)}#${f.link}"
    }

    open fun sourceHref(klass: KClass): String {
        if (klass.isLinkToSourceRepo()) {
            return klass.sourceLink()
        } else {
            val pkg = klass.pkg
            return if (!pkg.useExternalLink) {
                "${pkg.nameAsRelativePath}src-html/${klass.nameAsPath}.html#line.${klass.sourceLine}"
            } else {
                href(klass)
            }
        }
    }
    open fun sourceHref(f: KFunction): String {
        if (f.isLinkToSourceRepo()) {
            return f.sourceLink()
        } else {
            val owner = f.owner
            return if (owner is KClass) {
                val pkg = owner.pkg
                if (!pkg.useExternalLink) {
                    "${rootHref(pkg)}src-html/${owner.simpleName}.html#line.${f.sourceLine}"
                } else {
                    href(f)
                }
            } else if (owner is KPackage) {
                if (!owner.useExternalLink) {
                    // TODO how to find the function in a package???
                    "${rootHref(owner)}src-html/namespace.html#line.${f.sourceLine}"
                } else {
                    href(owner)
                }
            } else href(f)
        }
    }

    open fun sourceHref(f: KProperty): String {
        if (f.isLinkToSourceRepo()) {
            return f.sourceLink()
        } else {
            val owner = f.owner
            return if (owner is KClass) {
                val pkg = owner.pkg
                if (!pkg.useExternalLink) {
                    "${rootHref(pkg)}src-html/${owner.simpleName}.html#line.${f.sourceLine}"
                } else {
                    href(f)
                }
            } else if (owner is KPackage) {
                if (!owner.useExternalLink) {
                    // TODO how to find the function in a package???
                    "${rootHref(owner)}src-html/namespace.html#line.${f.sourceLine}"
                } else {
                    href(owner)
                }
            } else href(f)
        }
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
                val rest = arguments.subList(0, arguments.size - 1).orEmpty()
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
        val text = arguments.map<KType, String>() { link(it) }.makeString(", ")
        return if (text.length() == 0) empty else "$prefix$text$postfix"
    }

    open fun link(p: KPackage): String {
        return "<A HREF=\"${href(p)}\" title=\"package ${p.name}}\">${p.name}</A>"
    }

    open fun link(a: KAnnotation): String {
        val c = a.klass
        return "<A HREF=\"${href(c)}\" title=\"annotation in ${c.packageName}\">@${c.simpleName}</A>"
    }

    fun searchBox(): String =
"""  <TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1" ALIGN="right">
  <label for="searchBox">Search: </label>
  <input id="searchBox" class="ui-autocomplete-input" autocomplete="off" role="textbox" aria-autocomplete="list" aria-haspopup="true" size="50" data-url="${relativePrefix()}search.xml">
  </TD>"""


    fun stylesheets(): String {
        return """<LINK REL="stylesheet" TYPE="text/css" HREF="${relativePrefix()}stylesheet.css" TITLE="Style">
<LINK REL="stylesheet" TYPE="text/css" HREF="${relativePrefix()}jquery-ui.css" TITLE="Style">
<LINK REL="stylesheet" TYPE="text/css" HREF="${relativePrefix()}kotlin.css" TITLE="Style">
<script type="text/javascript" src="${relativePrefix()}js/jquery.js"></script>
<script type="text/javascript" src="${relativePrefix()}js/jquery-ui.js"></script>
<script type="text/javascript" src="${relativePrefix()}js/apidoc.js"></script>
"""
    }

    protected open fun relativePrefix(): String = ""
}
