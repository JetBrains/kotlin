package org.jetbrains.kotlin.doc.templates

import org.jetbrains.kotlin.doc.model.KModel
import org.jetbrains.kotlin.doc.model.KPackage
import org.jetbrains.kotlin.doc.model.filterDuplicateNames
import org.jetbrains.kotlin.doc.model.inheritedExtensionFunctions

class PackageSummaryTemplate(val model: KModel, pkg: KPackage) : PackageTemplateSupport(pkg) {
    override fun render() {
        println("""<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<!--NewPage-->
<HTML>
<HEAD>
$generatedComment
<META http-equiv="Content-Type" content="text/html; charset=UTF-8">
<TITLE>
${pkg.name} (${model.title})
</TITLE>

<META NAME="date" CONTENT="2012-01-09">
<META NAME="date" CONTENT="2012-01-09">
${stylesheets()}

<SCRIPT type="text/javascript">
function windowTitle()
{
    if (location.href.indexOf('is-external=true') == -1) {
        parent.document.title="${pkg.name} (${model.title})";
    }
}
</SCRIPT>
<NOSCRIPT>
</NOSCRIPT>

</HEAD>

<BODY BGCOLOR="white" onload="windowTitle();">
<HR>


<!-- ========= START OF TOP NAVBAR ======= -->
<A NAME="navbar_top"><!-- --></A>
<A HREF="#skip-navbar_top" title="Skip navigation links"></A>
<TABLE BORDER="0" WIDTH="100%" CELLPADDING="1" CELLSPACING="0" SUMMARY="">
<TR>
<TD COLSPAN=2 BGCOLOR="#EEEEFF" CLASS="NavBarCell1">
<A NAME="navbar_top_firstrow"><!-- --></A>
<TABLE BORDER="0" CELLPADDING="0" CELLSPACING="3" SUMMARY="">
  <TR ALIGN="center" VALIGN="top">
<TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <A HREF="${pkg.nameAsRelativePath}overview-summary.html"><FONT CLASS="NavBarFont1"><B>Overview</B></FONT></A>&nbsp;</TD>
<TD BGCOLOR="#FFFFFF" CLASS="NavBarCell1Rev"> &nbsp;<FONT CLASS="NavBarFont1Rev"><B>Package</B></FONT>&nbsp;</TD>
  <TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <FONT CLASS="NavBarFont1">Class</FONT>&nbsp;</TD>
  <TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <A HREF="package-use.html"><FONT CLASS="NavBarFont1"><B>Use</B></FONT></A>&nbsp;</TD>
  <TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <A HREF="package-tree.html"><FONT CLASS="NavBarFont1"><B>Tree</B></FONT></A>&nbsp;</TD>
<TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <A HREF="${pkg.nameAsRelativePath}deprecated-list.html"><FONT CLASS="NavBarFont1"><B>Deprecated</B></FONT></A>&nbsp;</TD>
<TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <A HREF="${pkg.nameAsRelativePath}index-all.html"><FONT CLASS="NavBarFont1"><B>Index</B></FONT></A>&nbsp;</TD>
<TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <A HREF="${pkg.nameAsRelativePath}help-doc.html"><FONT CLASS="NavBarFont1"><B>Help</B></FONT></A>&nbsp;</TD>
${searchBox()}
  </TR>
</TABLE>
</TD>
<TD ALIGN="right" VALIGN="top" ROWSPAN=3><EM>
</EM>
</TD>
</TR>

<TR>""")

        printNextPrevPackages()
        println("""<TD BGCOLOR="white" CLASS="NavBarCell2"><FONT SIZE="-2">
  <A HREF="${relativePrefix()}index.html" target="_top"><B>FRAMES</B></A>  &nbsp;
&nbsp;<A HREF="package-summary.html" target="_top"><B>NO FRAMES</B></A>  &nbsp;
&nbsp;<SCRIPT type="text/javascript">
  <!--
  if(window==top) {
    document.writeln('<A HREF="${relativePrefix()}allclasses-noframe.html"><B>All Classes</B></A>');
  }
  //-->
</SCRIPT>
<NOSCRIPT>
  <A HREF="${relativePrefix()}allclasses-noframe.html"><B>All Classes</B></A>
</NOSCRIPT>


</FONT></TD>
</TR>
</TABLE>
<A NAME="skip-navbar_top"></A>
<!-- ========= END OF TOP NAVBAR ========= -->

<HR>
<FONT SIZE="-1">""")

        for (a in pkg.annotations) {
            val url = a.url
            if (url != null) {
                println("""<A HREF="$url?is-external=true" title="class or interface in ${a.packageName}">@${a.simpleName}</A>""")
            }
        }
        println("""</FONT><H2>
Package ${pkg.name}
</H2>
${pkg.description(this)}
<P>
<B>See:</B>
<BR>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<A HREF="#package_description"><B>Description</B></A>
<P>
""")

        printClasses("trait", "Traits")
        printClasses("class", "Classes")
        printClasses("enum", "Enums")
        printClasses("annotation", "Annotations")
        printClasses("exception", "Exceptions")

        printPropertySummary(pkg.packageProperties())
        printFunctionSummary(pkg.packageFunctions())

        printExtensionFunctions()

        println("""<A NAME="package_description"><!-- --></A><H2>
Package ${pkg.name} Description
</H2>

<P>
${pkg.detailedDescription(this)}

<h2>Contents</h2>

""")

        for ((group, list) in pkg.groupClassMap()) {
            println(""" <h3>$group</h3>

 <ul>""")
            for (c in list) {
                println(""" <li><A HREF="${pkg.nameAsRelativePath}${c.nameAsPath}.html" title="class in ${pkg.name}"><CODE>${c.simpleName}</CODE></A>""")
            }
            println("""
 </ul>""")
        }
        println("""<P>""")

        printFunctionDetail(pkg.packageFunctions())

        println("""

<P>
<DL>
</DL>
<HR>


<!-- ======= START OF BOTTOM NAVBAR ====== -->
<A NAME="navbar_bottom"><!-- --></A>
<A HREF="#skip-navbar_bottom" title="Skip navigation links"></A>
<TABLE BORDER="0" WIDTH="100%" CELLPADDING="1" CELLSPACING="0" SUMMARY="">
<TR>
<TD COLSPAN=2 BGCOLOR="#EEEEFF" CLASS="NavBarCell1">
<A NAME="navbar_bottom_firstrow"><!-- --></A>
<TABLE BORDER="0" CELLPADDING="0" CELLSPACING="3" SUMMARY="">
  <TR ALIGN="center" VALIGN="top">
  <TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <A HREF="${pkg.nameAsRelativePath}overview-summary.html"><FONT CLASS="NavBarFont1"><B>Overview</B></FONT></A>&nbsp;</TD>
  <TD BGCOLOR="#FFFFFF" CLASS="NavBarCell1Rev"> &nbsp;<FONT CLASS="NavBarFont1Rev"><B>Package</B></FONT>&nbsp;</TD>
  <TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <FONT CLASS="NavBarFont1">Class</FONT>&nbsp;</TD>
  <TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <A HREF="package-use.html"><FONT CLASS="NavBarFont1"><B>Use</B></FONT></A>&nbsp;</TD>
  <TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <A HREF="package-tree.html"><FONT CLASS="NavBarFont1"><B>Tree</B></FONT></A>&nbsp;</TD>
  <TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <A HREF="${pkg.nameAsRelativePath}deprecated-list.html"><FONT CLASS="NavBarFont1"><B>Deprecated</B></FONT></A>&nbsp;</TD>
  <TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <A HREF="${pkg.nameAsRelativePath}index-all.html"><FONT CLASS="NavBarFont1"><B>Index</B></FONT></A>&nbsp;</TD>
  <TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <A HREF="${pkg.nameAsRelativePath}help-doc.html"><FONT CLASS="NavBarFont1"><B>Help</B></FONT></A>&nbsp;</TD>
  </TR>
</TABLE>
</TD>
<TD ALIGN="right" VALIGN="top" ROWSPAN=3><EM>
</EM>
</TD>
</TR>

<TR>""")

        printNextPrevPackages()

        println("""<TD BGCOLOR="white" CLASS="NavBarCell2"><FONT SIZE="-2">
  <A HREF="${relativePrefix()}index.html" target="_top"><B>FRAMES</B></A>  &nbsp;
&nbsp;<A HREF="package-summary.html" target="_top"><B>NO FRAMES</B></A>  &nbsp;
&nbsp;<SCRIPT type="text/javascript">
  <!--
  if(window==top) {
    document.writeln('<A HREF="${pkg.nameAsRelativePath}allclasses-noframe.html"><B>All Classes</B></A>');
  }
  //-->
</SCRIPT>
<NOSCRIPT>
  <A HREF="${pkg.nameAsRelativePath}allclasses-noframe.html"><B>All Classes</B></A>
</NOSCRIPT>


</FONT></TD>
</TR>
</TABLE>
<A NAME="skip-navbar_bottom"></A>
<!-- ======== END OF BOTTOM NAVBAR ======= -->

<HR>
Copyright &#169; 2010-2012. All Rights Reserved.
</BODY>
</HTML>""")
    }


    protected fun printClasses(kind: String, description: String): Unit {
        val classes = pkg.classes.filter{ it.kind == kind }
        if (! classes.isEmpty()) {
            print("""<TABLE BORDER="1" WIDTH="100%" CELLPADDING="3" CELLSPACING="0" SUMMARY="">
<TR BGCOLOR="#CCCCFF" CLASS="TableHeadingColor">
<TH ALIGN="left" COLSPAN="2"><FONT SIZE="+2">
<B>$description Summary</B></FONT></TH>
</TR>""")

            for (c in classes) {
                println("""<TR BGCOLOR="white" CLASS="TableRowColor">
<TD WIDTH="15%"><B><A HREF="${pkg.nameAsRelativePath}${c.nameAsPath}.html" title="$kind in ${pkg.name}">${c.simpleName}</A></B></TD>
<TD>${c.description(this)}</TD>
</TR>""")
            }
            println("""</TABLE>
&nbsp;

<P>
""")
        }
    }

    // TODO delete
    protected fun printFunctions(): Unit {
        val functions = pkg.functions.filter{ it.extensionClass == null }
        if (! functions.isEmpty()) {
            print("""<TABLE BORDER="1" WIDTH="100%" CELLPADDING="3" CELLSPACING="0" SUMMARY="">
<TR BGCOLOR="#CCCCFF" CLASS="TableHeadingColor">
<TH ALIGN="left" COLSPAN="2"><FONT SIZE="+2">
<B>Functions Summary</B></FONT></TH>
</TR>""")

            for (f in functions) {
                println("""<TR BGCOLOR="white" CLASS="TableRowColor">
<TD WIDTH="15%"><B><A HREF="${href(f)}" title="function in ${pkg.name}">${f.name}</A></B></TD>
<TD>${f.description(this)}</TD>
</TR>""")
            }
            println("""</TABLE>
&nbsp;

<P>
""")
        }
    }

    protected fun printExtensionFunctions(): Unit {
        val map = inheritedExtensionFunctions(pkg.functions)
        if (! map.isEmpty()) {
            print("""<TABLE BORDER="1" WIDTH="100%" CELLPADDING="3" CELLSPACING="0" SUMMARY="">
<TR BGCOLOR="#CCCCFF" CLASS="TableHeadingColor">
<TH ALIGN="left" COLSPAN="2"><FONT SIZE="+2">
<B>Extensions Summary</B></FONT></TH>
</TR>""")

            for ((c, list) in map) {
                println("""<TR BGCOLOR="white" CLASS="TableRowColor">
<TD WIDTH="15%"><B><A HREF="${extensionsHref(pkg, c)}" title="extensions on ${pkg.name}">${c.name}</A></B></TD>
<TD>""")
                for (f in filterDuplicateNames(list)) {
                    println("""<A HREF="${extensionsHref(pkg, c, f)}">${f.name}<A> """)
                }
                println("""</TD>
</TR>""")
            }
            println("""</TABLE>
&nbsp;

<P>
""")
        }
    }


    protected fun printNextPrevPackages(): Unit {
        println("""<TD BGCOLOR="white" CLASS="NavBarCell2"><FONT SIZE="-2">""")
        val prev = model.previous(pkg)
        if (prev != null) {
            println("""&nbsp;<A HREF="${prev.nameAsRelativePath}${prev.nameAsPath}/package-summary.html"><B>PREV PACKAGE</B></A>&nbsp;""")
        } else {
            println("""&nbsp;PREV PACKAGE&nbsp;""" )
        }
        val next = model.next(pkg)
        if (next != null) {
            println("""&nbsp;<A HREF="${next.nameAsRelativePath}${next.nameAsPath}/package-summary.html"><B>NEXT PACKAGE</B></A>""")
        } else {
            println("""&nbsp;NEXT PACKAGE""" )
        }
        println("""</FONT></TD>""")

    }
}
