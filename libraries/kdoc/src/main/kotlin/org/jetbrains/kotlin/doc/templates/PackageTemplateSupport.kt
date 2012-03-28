package org.jetbrains.kotlin.doc.templates

import kotlin.*
import kotlin.io.*
import kotlin.util.*
import java.util.*
import org.jetbrains.kotlin.template.*
import org.jetbrains.kotlin.doc.model.KModel
import org.jetbrains.kotlin.doc.model.KPackage
import org.jetbrains.kotlin.doc.model.KClass
import org.jetbrains.kotlin.doc.model.KFunction
import org.jetbrains.kotlin.doc.model.KAnnotation
import org.jetbrains.kotlin.doc.model.KProperty
import org.jetbrains.kotlin.doc.model.KTypeParameter


abstract class PackageTemplateSupport(open val pkg: KPackage) : KDocTemplate() {

    protected override fun relativePrefix(): String = pkg.nameAsRelativePath


    fun printFunctionSummary(functions: Collection<KFunction>): Unit {
        if (functions.notEmpty()) {
            println("""<!-- ========== FUNCTION SUMMARY =========== -->

<A NAME="method_summary"><!-- --></A>
<TABLE BORDER="1" WIDTH="100%" CELLPADDING="3" CELLSPACING="0" SUMMARY="">
<TR BGCOLOR="#CCCCFF" CLASS="TableHeadingColor">
<TH ALIGN="left" COLSPAN="2"><FONT SIZE="+2">
<B>Function Summary</B></FONT></TH>
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

    fun printFunctionSummary(function: KFunction): Unit {
        val deprecated = if (function.deprecated) "<B>Deprecated.</B>" else ""
        print("""<TR BGCOLOR="white" CLASS="TableRowColor">
<TD ALIGN="right" VALIGN="top" WIDTH="1%"><FONT SIZE="-1">
<CODE>""")
        if (!function.typeParameters.isEmpty()) {
            println("""<TABLE BORDER="0" CELLPADDING="0" CELLSPACING="0" SUMMARY="">
            <TR ALIGN="right" VALIGN="">
            <TD NOWRAP><FONT SIZE="-1">
            <CODE>""")
            printTypeParameters(function)
            println("""<BR>""")
            print(link(function.returnType))
            println("""</CODE></FONT></TD>
</TR>
</TABLE>""")
        } else {
            print(link(function.returnType))
        }
        println("""</CODE></FONT></TD>""")
        print("""<TD><CODE><B><A HREF="${href(function)}">${function.name}</A></B>""")
        printParameters(function)
        println("""</CODE>""")
        println("")
        if (true) {
            println("""<BR>""")
            println("""${function.description(this)}""")
        } else {
            println("""<BR>""")
            println("""&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${deprecated}&nbsp;${function.detailedDescription(this)}</TD>""")
        }
        println("""</TD>""")
        println("""</TR>""")
    }

    fun printFunctionDetail(functions: Collection<KFunction>): Unit {
        if (functions.notEmpty()) {
            println("""

            <!-- ============ FUNCTION DETAIL ========== -->

            <A NAME="method_detail"><!-- --></A>
            <TABLE BORDER="1" WIDTH="100%" CELLPADDING="3" CELLSPACING="0" SUMMARY="">
            <TR BGCOLOR="#CCCCFF" CLASS="TableHeadingColor">
            <TH ALIGN="left" COLSPAN="1"><FONT SIZE="+2">
            <B>Function Detail</B></FONT></TH>
            </TR>
            </TABLE>
            """)

            for (f in functions) {
                printFunctionDetail(f)
            }
        }
    }

    fun printFunctionDetail(function: KFunction): Unit {
        println("""<A NAME="${function.name}{${function.parameterTypeText}}"><!-- --></A><A NAME="${function.link}"><!-- --></A><H3>""")
        println("""${function.name}</H3>""")
        println("""<PRE>""")
        println("""<FONT SIZE="-1">""")
        printAnnotations(function.annotations)
        print("""</FONT>${function.modifiers.join(" ")} """)

        printTypeParameters(function)
        print(link(function.returnType))

        print(""" <A HREF="${sourceHref(function)}"><B>${function.name}</B></A>""")
        printParameters(function)
        val exlist = function.exceptions
        var first = true
        if (!exlist.isEmpty()) {
            println("""                                throws """);
            for (ex in exlist) {
                if (first) first = false else print(", ")
                print(link(ex))
            }
        }
        println("""</PRE>""")

        println(function.detailedDescription(this))
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
        println("""<HR>""")
    }

    fun printPropertySummary(properties: Collection<KProperty>): Unit {
        if (properties.notEmpty()) {
            println("""<!-- ========== PROPERTY SUMMARY =========== -->

<A NAME="method_summary"><!-- --></A>
<TABLE BORDER="1" WIDTH="100%" CELLPADDING="3" CELLSPACING="0" SUMMARY="">
<TR BGCOLOR="#CCCCFF" CLASS="TableHeadingColor">
<TH ALIGN="left" COLSPAN="2"><FONT SIZE="+2">
<B>Property Summary</B></FONT></TH>
</TR>""")

            for (f in properties) {
                printPropertySummary(f)
            }
            println("""</TABLE>
&nbsp;
<P>
""")
        }
    }

    fun printPropertySummary(property: KProperty): Unit {
        val deprecated = if (property.deprecated) "<B>Deprecated.</B>" else ""
        print("""<TR BGCOLOR="white" CLASS="TableRowColor">
<TD ALIGN="right" VALIGN="top" WIDTH="1%"><FONT SIZE="-1">
<CODE>""")
        /*
        if (!property.typeParameters.isEmpty()) {
            println("""<TABLE BORDER="0" CELLPADDING="0" CELLSPACING="0" SUMMARY="">
            <TR ALIGN="right" VALIGN="">
            <TD NOWRAP><FONT SIZE="-1">
            <CODE>""")
            printTypeParameters(property)
            println("""<BR>""")
            print(link(property.returnType))
            println("""</CODE></FONT></TD>
</TR>
</TABLE>""")
        } else {
            print(link(property.returnType))
        }
        */
        print(link(property.returnType))
        println("""</CODE></FONT></TD>""")
        print("""<TD><CODE><B><A HREF="${href(property)}">${property.name}</A></B>""")
        //printParameters(property)
        println("""</CODE>""")
        println("""""")
        println("""<BR>""")
        println("""&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${deprecated}&nbsp;${property.detailedDescription(this)}</TD>""")
        println("""</TR>""")
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
            print("&gt;&nbsp;")
        }
    }

    fun printParameters(method: KFunction): Unit {
        print("(")
        var first = true
        var defaultValue = false
        for (p in method.parameters) {
            if (!p.hasDefaultValue() && defaultValue) {
                print("]")
                defaultValue = false
            }
            if (first) first = false else print(", ")
            if (p.hasDefaultValue() && !defaultValue) {
                print(" [")
                defaultValue = true
            }
            val pType = if (p.isVarArg()) {
                print("vararg ")
                p.varArgType().sure()
            } else {
                p.aType
            }
            print("${p.name}:&nbsp;")
            print(link(pType))
        }
        if (defaultValue) {
            print("]")
        }
        print(")")
    }


    fun printAnnotations(annotations: Collection<KAnnotation>): Unit {
        for (a in annotations) {
            println(link(a))
        }
    }

    fun stylesheets(): String {
        return """<LINK REL="stylesheet" TYPE="text/css" HREF="${relativePrefix()}stylesheet.css" TITLE="Style">
        <LINK REL="stylesheet" TYPE="text/css" HREF="${relativePrefix()}kotlin.css" TITLE="Style">"""
    }
}