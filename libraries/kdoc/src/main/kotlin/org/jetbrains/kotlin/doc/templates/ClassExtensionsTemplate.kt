package org.jetbrains.kotlin.doc.templates

import kotlin.*
import org.jetbrains.kotlin.template.*
import kotlin.io.*
import kotlin.util.*
import java.util.*
import org.jetbrains.kotlin.doc.model.KModel
import org.jetbrains.kotlin.doc.model.KPackage
import org.jetbrains.kotlin.doc.model.KClass
import org.jetbrains.kotlin.doc.model.KFunction
import org.jetbrains.kotlin.doc.model.KAnnotation
import org.jetbrains.kotlin.doc.model.KProperty

class ClassExtensionsTemplate(m: KModel, p: KPackage, k: KClass,
        val functions: Collection<KFunction>, val properties: Collection<KProperty>) : ClassTemplate(m, p, k) {

    protected override fun relativePrefix(): String = "${pkg.nameAsRelativePath}${klass.pkg.nameAsRelativePath}"

    override fun pageTitle(): String = "${klass.name} Extensions fom ${pkg.name} (${model.title})"

    override fun printBody(): Unit {
        println("""<HR>
<!-- ======== START OF CLASS EXTENSIONS DATA ======== -->
<H2>
<FONT SIZE="-1">
${pkg.name}</FONT>
<BR>
Extensions on ${klass.name}</H2>
<DL>
<DT>
extension functions on class <A HREF="${sourceHref(klass)}"><B>${klass.name}</B></A><DT>
from package ${link(pkg)}
</DL>
</PRE>

<P>""")
        printPropertySummary(properties)
        printFunctionSummary(functions)
        printFunctionDetail(functions)
        println("""<!-- ========= END OF CLASS EXTENSIONS DATA ========= -->
<HR>
""")
    }

    override fun href(f: KFunction): String = if (f.extensionClass == klass) "#${f.link}" else super.href(f)

}
