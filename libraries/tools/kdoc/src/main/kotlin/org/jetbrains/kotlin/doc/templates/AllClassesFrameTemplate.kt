package org.jetbrains.kotlin.doc.templates

import org.jetbrains.kotlin.doc.model.KModel

class AllClassesFrameTemplate(val model: KModel, val classAttributes: String = "") : KDocTemplate() {
    override fun render() {
        println("""<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<!--NewPage-->
<HTML>
<HEAD>
$generatedComment
<META http-equiv="Content-Type"
        content="text/html; charset=UTF-8">
<TITLE>
All Classes (${model.title})</TITLE>

<META NAME="date" CONTENT="2012-01-09">
<LINK REL ="stylesheet" TYPE="text/css" HREF="stylesheet.css" TITLE="Style">
</HEAD>

<BODY BGCOLOR="white">
<FONT size="+1" CLASS="FrameHeadingFont">
<B>All Classes</B></FONT>
<BR>

<TABLE BORDER="0" WIDTH="100%" SUMMARY="">
<TR>
<TD NOWRAP><FONT CLASS="FrameItemFont">""")
        for (c in model.classes) {
            println("""<A HREF="${c.nameAsPath}.html" title="class in ${c.packageName}"$classAttributes>${c.simpleName}</A>
<BR>""")
        }
        println("""</FONT></TD>
</TR>
</TABLE>
</BODY>
</HTML>
""")
    }
}
