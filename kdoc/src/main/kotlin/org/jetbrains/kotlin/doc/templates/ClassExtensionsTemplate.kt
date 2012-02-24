package org.jetbrains.kotlin.doc.templates

import std.*
import org.jetbrains.kotlin.template.*
import std.io.*
import std.util.*
import java.util.*
import org.jetbrains.kotlin.model.KModel
import org.jetbrains.kotlin.model.KPackage
import org.jetbrains.kotlin.model.KClass
import org.jetbrains.kotlin.model.KFunction
import org.jetbrains.kotlin.model.KAnnotation

class ClassExtensionsTemplate(m: KModel, p: KPackage, k: KClass, var functions: Collection<KFunction>) : ClassTemplate(m, p, k) {

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
extension functions on class <A HREF="${pkg.nameAsRelativePath}src-html/${klass.nameAsPath}.html#line.${klass.sourceLine}"><B>${klass.name}</B></A><DT>
from package ${link(pkg)}
</DL>
</PRE>

<P>""")
        printFunctionSummary(functions)
        printFunctionDetail(functions)
        println("""<!-- ========= END OF CLASS EXTENSIONS DATA ========= -->
<HR>
""")
    }

    override fun href(f: KFunction): String = "#${f.link}"

}
