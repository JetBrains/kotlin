package org.jetbrains.kotlin.doc.templates

import org.jetbrains.kotlin.doc.model.KModel

class OverviewFrameTemplate(val model: KModel) : KDocTemplate() {
    override fun render() {
        println("""<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<!--NewPage-->
<HTML>
<HEAD>
$generatedComment
<META http-equiv="Content-Type" content="text/html; charset=UTF-8">
<TITLE>
Overview List (${model.title})
</TITLE>

<META NAME="date" CONTENT="2012-01-09">
<LINK REL ="stylesheet" TYPE="text/css" HREF="stylesheet.css" TITLE="Style">
</HEAD>

<BODY BGCOLOR="white">

<TABLE BORDER="0" WIDTH="100%" SUMMARY="">
<TR>
<TH ALIGN="left" NOWRAP><FONT size="+1" CLASS="FrameTitleFont">
<B></B></FONT></TH>
</TR>
</TABLE>

<TABLE BORDER="0" WIDTH="100%" SUMMARY="">
<TR>
<TD NOWRAP><FONT CLASS="FrameItemFont"><A HREF="allclasses-frame.html" target="packageFrame">All Classes</A></FONT>
<P>
<FONT size="+1" CLASS="FrameHeadingFont">
Packages</FONT>
<BR>""")
        for (p in model.packages) {
            println("""<FONT CLASS="FrameItemFont"><A HREF="${p.nameAsPath}/package-frame.html" target="packageFrame">${p.name}</A></FONT>
<BR>""")
        }
        println("""</TD>
</TR>
</TABLE>

<P>
&nbsp;
</BODY>
</HTML>
""")
    }
}
