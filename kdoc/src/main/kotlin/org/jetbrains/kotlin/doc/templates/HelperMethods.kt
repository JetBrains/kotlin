package org.jetbrains.kotlin.doc.templates

import org.jetbrains.kotlin.model.KClass
import org.jetbrains.kotlin.model.KAnnotation
import org.jetbrains.kotlin.model.KPackage
import org.jetbrains.kotlin.template.TextTemplate

abstract class KDocTemplate() : TextTemplate() {
    fun href(pkg: KPackage): String {
        return if (pkg.external) {
            // TODO deal with external classes
            ""
        } else relativePrefix()
    }

    fun href(c: KClass): String {
        val postfix = if (c.pkg.external) "?is-external=true" else ""
        return "${href(c.pkg)}${c.nameAsPath}.html$postfix"
    }

    fun link(c: KClass, fullName: Boolean = false): String {
        // TODO if a class is a generic type parameter, just return the name...
        val prefix = if (c.isAnnotation()) "@" else ""
        val name = if (fullName) c.name else c.simpleName
        return "<A HREF=\"${href(c)}\" title=\"${c.kind} in ${c.packageName}\">$prefix$name</A>"
    }

    fun link(a: KAnnotation): String {
        val c = a.klass
        return "<A HREF=\"${href(c)}\" title=\"annotation in ${c.packageName}\">@${c.simpleName}</A>"
    }

    protected open fun relativePrefix(): String = ""
}

abstract class PackageTemplateSupport(private val _pkg: KPackage) : KDocTemplate() {

    // TODO this verbosity is to work around this bug: http://youtrack.jetbrains.com/issue/KT-1398
    public val pkg: KPackage
        get() = _pkg

    protected override fun relativePrefix(): String = pkg.nameAsRelativePath

}