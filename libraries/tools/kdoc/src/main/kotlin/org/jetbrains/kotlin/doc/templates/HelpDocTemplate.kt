package org.jetbrains.kotlin.doc.templates

import org.jetbrains.kotlin.doc.model.KModel

class HelpDocTemplate(val model: KModel) : KDocTemplate() {
    override fun render() {
        println("""<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Frameset//EN" "http://www.w3.org/TR/html4/frameset.dtd">
<!--NewPage-->
<HTML>
<HEAD>
$generatedComment
<META http-equiv="Content-Type" content="text/html; charset=UTF-8">
<TITLE>
API Help (${model.title})
</TITLE>

<META NAME="date" CONTENT="2012-01-09">

<LINK REL ="stylesheet" TYPE="text/css" HREF="stylesheet.css" TITLE="Style">

<SCRIPT type="text/javascript">
function windowTitle()
{
    if (location.href.indexOf('is-external=true') == -1) {
        parent.document.title="API Help (${model.title})";
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
  <TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <A HREF="overview-summary.html"><FONT CLASS="NavBarFont1"><B>Overview</B></FONT></A>&nbsp;</TD>
  <TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <FONT CLASS="NavBarFont1">Package</FONT>&nbsp;</TD>
  <TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <FONT CLASS="NavBarFont1">Class</FONT>&nbsp;</TD>
  <TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <FONT CLASS="NavBarFont1">Use</FONT>&nbsp;</TD>
  <TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <A HREF="overview-tree.html"><FONT CLASS="NavBarFont1"><B>Tree</B></FONT></A>&nbsp;</TD>
  <TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <A HREF="deprecated-list.html"><FONT CLASS="NavBarFont1"><B>Deprecated</B></FONT></A>&nbsp;</TD>
  <TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <A HREF="index-all.html"><FONT CLASS="NavBarFont1"><B>Index</B></FONT></A>&nbsp;</TD>
  <TD BGCOLOR="#FFFFFF" CLASS="NavBarCell1Rev"> &nbsp;<FONT CLASS="NavBarFont1Rev"><B>Help</B></FONT>&nbsp;</TD>
  </TR>
</TABLE>
</TD>
<TD ALIGN="right" VALIGN="top" ROWSPAN=3><EM>
</EM>
</TD>
</TR>

<TR>
<TD BGCOLOR="white" CLASS="NavBarCell2"><FONT SIZE="-2">
&nbsp;PREV&nbsp;
&nbsp;NEXT</FONT></TD>
<TD BGCOLOR="white" CLASS="NavBarCell2"><FONT SIZE="-2">
  <A HREF="index.html?help-doc.html" target="_top"><B>FRAMES</B></A>  &nbsp;
&nbsp;<A HREF="help-doc.html" target="_top"><B>NO FRAMES</B></A>  &nbsp;
&nbsp;<SCRIPT type="text/javascript">
  <!--
  if(window==top) {
    document.writeln('<A HREF="allclasses-noframe.html"><B>All Classes</B></A>');
  }
  //-->
</SCRIPT>
<NOSCRIPT>
  <A HREF="allclasses-noframe.html"><B>All Classes</B></A>
</NOSCRIPT>


</FONT></TD>
</TR>
</TABLE>
<A NAME="skip-navbar_top"></A>
<!-- ========= END OF TOP NAVBAR ========= -->

<HR>
<CENTER>
<H1>
How This API Document Is Organized</H1>
</CENTER>
This API (Application Programming Interface) document has pages corresponding to the items in the navigation bar, described as follows.<H3>
Overview</H3>
<BLOCKQUOTE>

<P>
The <A HREF="overview-summary.html">Overview</A> page is the front page of this API document and provides a list of all packages with a summary for each.  This page can also contain an overall description of the set of packages.</BLOCKQUOTE>
<H3>
Package</H3>
<BLOCKQUOTE>

<P>
Each package has a page that contains a list of its classes and interfaces, with a summary for each. This page can contain four categories:<UL>
<LI>Interfaces (italic)<LI>Classes<LI>Enums<LI>Exceptions<LI>Errors<LI>Annotation Types</UL>
</BLOCKQUOTE>
<H3>
Class/Interface</H3>
<BLOCKQUOTE>

<P>
Each class, interface, nested class and nested interface has its own separate page. Each of these pages has three sections consisting of a class/interface description, summary tables, and detailed member descriptions:<UL>
<LI>Class inheritance diagram<LI>Direct Subclasses<LI>All Known Subinterfaces<LI>All Known Implementing Classes<LI>Class/interface declaration<LI>Class/interface description
<P>
<LI>Nested Class Summary<LI>Field Summary<LI>Constructor Summary<LI>Method Summary
<P>
<LI>Field Detail<LI>Constructor Detail<LI>Method Detail</UL>
Each summary entry contains the first sentence from the detailed description for that item. The summary entries are alphabetical, while the detailed descriptions are in the order they appear in the source code. This preserves the logical groupings established by the programmer.</BLOCKQUOTE>
</BLOCKQUOTE>
<H3>
Annotation Type</H3>
<BLOCKQUOTE>

<P>
Each annotation type has its own separate page with the following sections:<UL>
<LI>Annotation Type declaration<LI>Annotation Type description<LI>Required Element Summary<LI>Optional Element Summary<LI>Element Detail</UL>
</BLOCKQUOTE>
</BLOCKQUOTE>
<H3>
Enum</H3>
<BLOCKQUOTE>

<P>
Each enum has its own separate page with the following sections:<UL>
<LI>Enum declaration<LI>Enum description<LI>Enum Constant Summary<LI>Enum Constant Detail</UL>
</BLOCKQUOTE>
<H3>
Use</H3>
<BLOCKQUOTE>
Each documented package, class and interface has its own Use page.  This page describes what packages, classes, methods, constructors and fields use any part of the given class or package. Given a class or interface A, its Use page includes subclasses of A, fields declared as A, methods that return A, and methods and constructors with parameters of type A.  You can access this page by first going to the package, class or interface, then clicking on the "Use" link in the navigation bar.</BLOCKQUOTE>
<H3>
Tree (Class Hierarchy)</H3>
<BLOCKQUOTE>
There is a <A HREF="overview-tree.html">Class Hierarchy</A> page for all packages, plus a hierarchy for each package. Each hierarchy page contains a list of classes and a list of interfaces. The classes are organized by inheritance structure starting with <code>java.lang.Object</code>. The interfaces do not inherit from <code>java.lang.Object</code>.<UL>
<LI>When viewing the Overview page, clicking on "Tree" displays the hierarchy for all packages.<LI>When viewing a particular package, class or interface page, clicking "Tree" displays the hierarchy for only that package.</UL>
</BLOCKQUOTE>
<H3>
Deprecated API</H3>
<BLOCKQUOTE>
The <A HREF="deprecated-list.html">Deprecated API</A> page lists all of the API that have been deprecated. A deprecated API is not recommended for use, generally due to improvements, and a replacement API is usually given. Deprecated APIs may be removed in future implementations.</BLOCKQUOTE>
<H3>
Index</H3>
<BLOCKQUOTE>
The <A HREF="index-all.html">Index</A> contains an alphabetic list of all classes, interfaces, constructors, methods, and fields.</BLOCKQUOTE>
<H3>
Prev/Next</H3>
These links take you to the next or previous class, interface, package, or related page.<H3>
Frames/No Frames</H3>
These links show and hide the HTML frames.  All pages are available with or without frames.
<P>
<H3>
Serialized Form</H3>
Each serializable or externalizable class has a description of its serialization fields and methods. This information is of interest to re-implementors, not to developers using the API. While there is no link in the navigation bar, you can get to this information by going to any serialized class and clicking "Serialized Form" in the "See also" section of the class description.
<P>
<H3>
Constant Field Values</H3>
The <a href="constant-values.html">Constant Field Values</a> page lists the static final fields and their values.
<P>
<FONT SIZE="-1">
<EM>
This help file applies to API documentation generated using the standard doclet.</EM>
</FONT>
<BR>
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
  <TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <A HREF="overview-summary.html"><FONT CLASS="NavBarFont1"><B>Overview</B></FONT></A>&nbsp;</TD>
  <TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <FONT CLASS="NavBarFont1">Package</FONT>&nbsp;</TD>
  <TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <FONT CLASS="NavBarFont1">Class</FONT>&nbsp;</TD>
  <TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <FONT CLASS="NavBarFont1">Use</FONT>&nbsp;</TD>
  <TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <A HREF="overview-tree.html"><FONT CLASS="NavBarFont1"><B>Tree</B></FONT></A>&nbsp;</TD>
  <TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <A HREF="deprecated-list.html"><FONT CLASS="NavBarFont1"><B>Deprecated</B></FONT></A>&nbsp;</TD>
  <TD BGCOLOR="#EEEEFF" CLASS="NavBarCell1">    <A HREF="index-all.html"><FONT CLASS="NavBarFont1"><B>Index</B></FONT></A>&nbsp;</TD>
  <TD BGCOLOR="#FFFFFF" CLASS="NavBarCell1Rev"> &nbsp;<FONT CLASS="NavBarFont1Rev"><B>Help</B></FONT>&nbsp;</TD>
  </TR>
</TABLE>
</TD>
<TD ALIGN="right" VALIGN="top" ROWSPAN=3><EM>
</EM>
</TD>
</TR>

<TR>
<TD BGCOLOR="white" CLASS="NavBarCell2"><FONT SIZE="-2">
&nbsp;PREV&nbsp;
&nbsp;NEXT</FONT></TD>
<TD BGCOLOR="white" CLASS="NavBarCell2"><FONT SIZE="-2">
  <A HREF="index.html?help-doc.html" target="_top"><B>FRAMES</B></A>  &nbsp;
&nbsp;<A HREF="help-doc.html" target="_top"><B>NO FRAMES</B></A>  &nbsp;
&nbsp;<SCRIPT type="text/javascript">
  <!--
  if(window==top) {
    document.writeln('<A HREF="allclasses-noframe.html"><B>All Classes</B></A>');
  }
  //-->
</SCRIPT>
<NOSCRIPT>
  <A HREF="allclasses-noframe.html"><B>All Classes</B></A>
</NOSCRIPT>


</FONT></TD>
</TR>
</TABLE>
<A NAME="skip-navbar_bottom"></A>
<!-- ======== END OF BOTTOM NAVBAR ======= -->

<HR>
Copyright &#169; 2012. All Rights Reserved.
</BODY>
</HTML>""")
    }
}
