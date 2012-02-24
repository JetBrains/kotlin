package org.jetbrains.kotlin.doc.templates

import org.jetbrains.kotlin.model.KClass
import org.jetbrains.kotlin.model.KAnnotation
import org.jetbrains.kotlin.model.KPackage
import org.jetbrains.kotlin.template.TextTemplate
import org.jetbrains.kotlin.model.KFunction
import java.util.Collection
import org.jetbrains.kotlin.model.KProperty

abstract class KDocTemplate() : TextTemplate() {
    fun rootHref(pkg: KPackage): String {
        return if (pkg.external) {
            // TODO deal with external classes
            ""
        } else relativePrefix()
    }

    fun href(p: KPackage): String
        = "${rootHref(p)}${p.nameAsPath}/package-summary.html"

    fun href(c: KClass): String {
        val postfix = if (c.pkg.external) "?is-external=true" else ""
        return "${rootHref(c.pkg)}${c.nameAsPath}.html$postfix"
    }

    fun href(f: KFunction): String {
        return if (f.owner is KClass) {
            "${href(f.owner)}#${f.link}"
        } else {
            "package-summary.html#${f.link}"
        }
    }

    fun href(f: KProperty): String {
        return if (f.owner is KClass) {
            "${href(f.owner)}#${f.link}"
        } else {
            "package-summary.html#${f.link}"
        }
    }

    fun extensionsHref(pkg: KPackage, c: KClass): String {
        // is inside the pkg so no need to use it...
        return "${c.nameAsPath}-extensions.html"
    }

    fun sourceHref(f: KFunction): String {
        return if (f.owner is KClass) {
            "${rootHref(f.owner.pkg)}src-html/${f.owner.simpleName}.html#line.${f.sourceLine}"
        } else {
            // TODO how to find the function in a package???
            ""
        }
    }

    fun link(c: KClass, fullName: Boolean = false): String {
        // TODO if a class is a generic type parameter, just return the name...
        val prefix = if (c.isAnnotation()) "@" else ""
        val name = if (fullName) c.name else c.simpleName
        return "<A HREF=\"${href(c)}\" title=\"${c.kind} in ${c.packageName}\">$prefix$name</A>"
    }

    fun link(p: KPackage): String {
        return "<A HREF=\"${href(p)}\" title=\"package ${p.name}}\">${p.name}</A>"
    }

    fun link(a: KAnnotation): String {
        val c = a.klass
        return "<A HREF=\"${href(c)}\" title=\"annotation in ${c.packageName}\">@${c.simpleName}</A>"
    }

    protected open fun relativePrefix(): String = ""
}
