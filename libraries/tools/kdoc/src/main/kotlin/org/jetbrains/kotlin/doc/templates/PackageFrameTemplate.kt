package org.jetbrains.kotlin.doc.templates

import org.jetbrains.kotlin.doc.model.KModel
import org.jetbrains.kotlin.doc.model.KPackage
import org.jetbrains.kotlin.doc.model.extensionFunctions
import org.jetbrains.kotlin.doc.model.filterDuplicateNames

class PackageFrameTemplate(val model: KModel, p: KPackage) : PackageTemplateSupport(p) {
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
${stylesheets()}

</HEAD>

<BODY BGCOLOR="white">
<FONT size="+1" CLASS="FrameTitleFont">
<A HREF="${pkg.nameAsRelativePath}${pkg.nameAsPath}/package-summary.html" target="classFrame">${pkg.name}</A></FONT>""")

        printClasses("trait", "Traits")
        printClasses("class", "Classes")
        printClasses("enum", "Enums")
        printClasses("annotation", "Annotations")
        printClasses("exception", "Exceptions")
        printPackageProperties()
        printFunctions()
        printExtensionFunctions()

        println("""</BODY>
</HTML>""")
    }

    protected fun printClasses(kind: String, description: String): Unit {
        val classes = pkg.classes.filter{ it.kind == kind }
        if (! classes.isEmpty()) {
            println("""<TABLE BORDER="0" WIDTH="100%" SUMMARY="">
<TR>
<TD NOWRAP><FONT size="+1" CLASS="FrameHeadingFont">""")
            print(description)
            println("""</FONT>&nbsp;
<FONT CLASS="FrameItemFont">
<BR>""")
            for (c in classes) {
                val formatted = if (kind == "interface") "<I>${c.simpleName}</I>" else c.simpleName
                println("""<A HREF="${c.simpleName}.html" title="$kind in ${pkg.name}" target="classFrame">$formatted</A>
<BR>""")
            }
            println("""</TR>
</TABLE>""")
        }
    }

    protected fun printFunctions(): Unit {
        val functions = filterDuplicateNames(pkg.packageFunctions())
        if (! functions.isEmpty()) {
            println("""<TABLE BORDER="0" WIDTH="100%" SUMMARY="">
<TR>
<TD NOWRAP><FONT size="+1" CLASS="FrameHeadingFont">Functions</FONT>&nbsp;
<FONT CLASS="FrameItemFont">
<BR>""")

            var lastName = ""
            for (c in functions) {
                // only show the first function with a given name
                if (c.name != lastName) {
                    println("""<A HREF="${href(c)}" title="function in ${pkg.name}" target="classFrame"><I>${c.name}</I></A>
    <BR>""")
                }
                lastName = c.name
            }
            println("""</TR>
</TABLE>""")
        }
    }

    protected fun printExtensionFunctions(): Unit {
        val map = extensionFunctions(pkg.functions)
        if (! map.isEmpty()) {
            println("""<TABLE BORDER="0" WIDTH="100%" SUMMARY="">
<TR>
<TD NOWRAP><FONT size="+1" CLASS="FrameHeadingFont">Extensions</FONT>&nbsp;
<FONT CLASS="FrameItemFont">
<BR>""")
            for (e in map) {
                val c = e.key
                if (c != null) {
                    println("""<A HREF="${extensionsHref(pkg, c)}" title="extensions functions on class ${c.name} from ${pkg.name}" target="classFrame"><I>${c.name}</I></A>
<BR>""")
                }
            }
            println("""</TR>
</TABLE>""")
        }
    }
    protected fun printPackageProperties(): Unit {
        val list = pkg.packageProperties()
        if (list.isNotEmpty()) {
            println("""<TABLE BORDER="0" WIDTH="100%" SUMMARY="">
<TR>
<TD NOWRAP><FONT size="+1" CLASS="FrameHeadingFont">Properties</FONT>&nbsp;
<FONT CLASS="FrameItemFont">
<BR>""")
            for (c in list) {
                if (c != null) {
                    println("""<A HREF="${href(pkg, c)}" title="property from ${pkg.name}" target="classFrame"><I>${c.name}</I></A>
<BR>""")
                }
            }
            println("""</TR>
</TABLE>""")
        }
    }
}
