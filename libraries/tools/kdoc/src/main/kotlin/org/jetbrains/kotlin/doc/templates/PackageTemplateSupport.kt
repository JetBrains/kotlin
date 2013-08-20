package org.jetbrains.kotlin.doc.templates

import java.util.*
import org.jetbrains.kotlin.doc.model.KAnnotation
import org.jetbrains.kotlin.doc.model.KFunction
import org.jetbrains.kotlin.doc.model.KPackage
import org.jetbrains.kotlin.doc.model.KProperty

abstract class PackageTemplateSupport(open val pkg: KPackage) : KDocTemplate() {

    protected override fun relativePrefix(): String = pkg.nameAsRelativePath

    val funKeyword = keyword("fun")
    val valKeyword = keyword("val")
    val varKeyword = keyword("var")

    protected fun keyword(name: String): String = "<B>$name</B>"

    fun printFunctionSummary(functions: Collection<KFunction>): Unit {
        if (functions.isNotEmpty()) {
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
            print("$funKeyword ")
            printTypeParameters(function)
            printReceiverType(function, "<BR>")
            println("""</CODE></FONT></TD>
</TR>
</TABLE>""")
        } else {
            print(funKeyword)
            printReceiverType(function)
        }
        // print receiver type
        println("""</CODE></FONT></TD>""")
        print("""<TD><CODE><B><A HREF="${href(function)}">${function.name}</A></B>""")
        printParameters(function)
        print(": ")
        print(link(function.returnType))
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


    fun printReceiverType(function: KFunction, prefix: String = " ", postfix: String = "", none: String = ""): Unit {
        val receiverType = function.receiverType
        if (receiverType != null) {
            print(prefix)
            print(link(receiverType))
            print(postfix)
        } else {
            print(none)
        }
    }

    fun printFunctionDetail(functions: Collection<KFunction>): Unit {
        if (functions.isNotEmpty()) {
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
        println("""<div class="doc-member function">""")
        println("""<div class="source-detail"><a href="${sourceHref(function)}"${function.sourceTargetAttribute()}>source</a></div>""")

        println("""<A NAME="${function.name}{${function.parameterTypeText}}"><!-- --></A><A NAME="${function.link}"><!-- --></A><H3>""")
        println("""${function.name}</H3>""")
        println("""<PRE>""")
        println("""<FONT SIZE="-1">""")
        printAnnotations(function.annotations)
        print("""</FONT>${function.modifiers.makeString(" ")} $funKeyword""")

        printTypeParameters(function, " ")
        printReceiverType(function, " ", ".", " ")
        print("""<B>${function.name}</B>""")
        printParameters(function)
        print(": ")
        print(link(function.returnType))

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
        println("""</div>""")
    }

    fun printPropertySummary(properties: Collection<KProperty>): Unit {
        if (properties.isNotEmpty()) {
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
        print(if (property.isVar()) varKeyword else valKeyword)
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
        println("""</CODE></FONT></TD>""")
        print("""<TD>
<div class="doc-member property">
<div class="source-detail"><a HREF="${sourceHref(property)}"${property.sourceTargetAttribute()}>source</a></div>
        <CODE><B>${property.name}</B>: """)
        print(link(property.returnType))
        //printParameters(property)
        println("""</CODE>""")
        println("""""")
        println("""<BR>""")
        println("""&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${deprecated}&nbsp;${property.detailedDescription(this)}
</div></TD>""")
        println("""</TR>""")
    }

    fun printTypeParameters(method: KFunction, separator: String = ""): Unit {
        val typeParameters = method.typeParameters
        if (!typeParameters.isEmpty()) {
            print(separator)
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
                p.varArgType()!!
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
}
