package org.jetbrains.kotlin.doc.templates

import std.*
import std.io.*
import std.util.*
import java.util.*
import org.jetbrains.kotlin.template.*
import org.jetbrains.kotlin.model.KModel
import org.jetbrains.kotlin.model.KPackage
import org.jetbrains.kotlin.model.KClass
import org.jetbrains.kotlin.model.KFunction
import org.jetbrains.kotlin.model.KAnnotation


abstract class PackageTemplateSupport(open val pkg: KPackage) : KDocTemplate() {

    protected override fun relativePrefix(): String = pkg.nameAsRelativePath


    fun printFunctionSummary(functions: Collection<KFunction>): Unit {
        if (functions.notEmpty()) {
            println("""<!-- ========== METHOD SUMMARY =========== -->

<A NAME="method_summary"><!-- --></A>
<TABLE BORDER="1" WIDTH="100%" CELLPADDING="3" CELLSPACING="0" SUMMARY="">
<TR BGCOLOR="#CCCCFF" CLASS="TableHeadingColor">
<TH ALIGN="left" COLSPAN="2"><FONT SIZE="+2">
<B>Method Summary</B></FONT></TH>
</TR>""")

            for (f in functions) {
                printFunctionSummary(f)
            }
            println("""</TABLE>
&nbsp;
<P>
""")
        }
    }

    fun printFunctionDetail(functions: Collection<KFunction>): Unit {
        if (functions.notEmpty()) {
            println("""

            <!-- ============ METHOD DETAIL ========== -->

            <A NAME="method_detail"><!-- --></A>
            <TABLE BORDER="1" WIDTH="100%" CELLPADDING="3" CELLSPACING="0" SUMMARY="">
            <TR BGCOLOR="#CCCCFF" CLASS="TableHeadingColor">
            <TH ALIGN="left" COLSPAN="1"><FONT SIZE="+2">
            <B>Method Detail</B></FONT></TH>
            </TR>
            </TABLE>
            """)

            for (f in functions) {
                printFunctionDetail(f)
            }
        }
    }

    fun printFunctionSummary(method: KFunction): Unit {
        val deprecated = if (method.deprecated) "<B>Deprecated.</B>" else ""
        print("""<TR BGCOLOR="white" CLASS="TableRowColor">
<TD ALIGN="right" VALIGN="top" WIDTH="1%"><FONT SIZE="-1">
<CODE>""")
        if (!method.typeParameters.isEmpty()) {
            println("""<TABLE BORDER="0" CELLPADDING="0" CELLSPACING="0" SUMMARY="">
            <TR ALIGN="right" VALIGN="">
            <TD NOWRAP><FONT SIZE="-1">
            <CODE>""")
            printTypeParameters(method)
            println("<BR>")
            print(link(method.returnType))
            println("""</CODE></FONT></TD>
</TR>
</TABLE>""")
        } else {
            print(link(method.returnType))
        }
        println("</CODE></FONT></TD>")
        print("<TD><CODE><B><A HREF=\"${href(method)}\">${method.name}</A></B>")
        printParameters(method)
        println("</CODE>")
        println("")
        println("<BR>")
        println("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${deprecated}&nbsp;${method.detailedDescription}</TD>")
        println("</TR>")
    }

    fun printFunctionDetail(method: KFunction): Unit {
        println("<A NAME=\"${method.name}{${method.parameterTypeText}})\"><!-- --></A><A NAME=\"${method.name}(${method.typeParametersText})\"><!-- --></A><H3>")
        println("${method.name}</H3>")
        println("<PRE>")
        println("<FONT SIZE=\"-1\">")
        printAnnotations(method.annotations)
        print("</FONT>${method.modifiers.join(" ")} ")

        printTypeParameters(method)
        print(link(method.returnType))
        print(" <A HREF=\"${sourceHref(method)}\"><B>${method.name}</B></A>")
        printParameters(method)
        val exlist = method.exceptions
        var first = true
        if (!exlist.isEmpty()) {
            println("                                throws ");
            for (ex in exlist) {
                if (first) first = false else print(", ")
                print(link(ex))
            }
        }
        println("</PRE>")

        /* TODO
        println("""<DL>
<DD><B>Deprecated.</B>&nbsp;TODO text
<P>
<DD><b>Deprecated.</b>
<P>
<DD>
<DL>
<DT><B>Throws:</B>
<DD><CODE>${link(ex}</CODE><DT><B>Since:</B></DT>
<DD>${since}</DD>
</DL>
</DD>
</DL>
*/
        println("<HR>")
    }

    fun printTypeParameters(method: KFunction): Unit {
        val typeParameters = method.typeParameters
        if (!typeParameters.isEmpty()) {
            print("&lt")
            var separator = ""
            for (t in typeParameters) {
                print(separator)
                separator = ", "
                print(t.name)
                val elist = t.extends
                if (!elist.isEmpty()) {
                    print(" extends ")
                    var esep = ""
                    for (e in elist) {
                        print(esep)
                        esep = " & "
                        print(link(e))
                    }
                }
            }
            print("&gt")
        }
    }

    fun printParameters(method: KFunction): Unit {
        print("(")
        var first = true
        for (p in method.parameters) {
            if (first) first = false else print(", ")
            print("${p.name}:&nbsp;")
            print(link(p.klass))
        }
        print(")")
    }

    fun printAnnotations(annotations: Collection<KAnnotation>): Unit {
        for (a in annotations) {
            println(link(a))
        }
    }
}