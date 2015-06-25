package org.jetbrains.kotlin.doc

import java.io.File
import org.jetbrains.kotlin.doc.model.*
import org.jetbrains.kotlin.doc.templates.*
import org.jetbrains.kotlin.template.TextTemplate

/** Creates JavaDoc style HTML output from the model */
class JavadocStyleHtmlDoclet() : Doclet {

    override fun generate(model: KModel, outputDir: File): Unit {
        fun run(fileName: String, template: TextTemplate): Unit {
            val file = File(outputDir, fileName)
            file.getParentFile()?.mkdirs()

            log("Generating $fileName")
            template.renderTo(file)
        }

        fun generateExtensionFunctions(p: KPackage): Unit {
            val map = inheritedExtensionFunctions(p.functions)
            val pmap = inheritedExtensionProperties(p.properties)
            val classes = hashSetOf<KClass>()
            classes.addAll(map.keySet())
            classes.addAll(pmap.keySet())
            for (c in classes) {
                val functions = map.get(c).orEmpty()
                val properties = pmap.get(c).orEmpty()
                run("${p.nameAsPath}/${c.nameAsPath}-extensions.html",
                ClassExtensionsTemplate(model, p, c, functions, properties))
            }
        }


        println("Generating kdoc to $outputDir excluding packages ${model.config.ignorePackages}")
        run("allclasses-frame.html", AllClassesFrameTemplate(model, " target=\"classFrame\""))
        run("allclasses-noframe.html", AllClassesFrameTemplate(model))
        // run("constant-values.html", ConstantValuesTemplate(model))
        // run("deprecated-list.html", DeprecatedListTemplate(model))
        run("help-doc.html", HelpDocTemplate(model))
        // run("index-all.html", IndexAllTemplate(model))
        run("index.html", IndexTemplate(model))
        run("overview-frame.html", OverviewFrameTemplate(model))
        run("overview-summary.html", OverviewSummaryTemplate(model))
        run("overview-tree.html", OverviewTreeTemplate(model))
        run("package-list", PackageListTemplate(model))
        run("search.xml", SearchXmlTemplate(model))
        // run("serialized-form.html", SerializedFormTemplate(model))
        for (p in model.packages) {
            run("${p.nameAsPath}/package-frame.html", PackageFrameTemplate(model, p))
            run("${p.nameAsPath}/package-summary.html", PackageSummaryTemplate(model, p))
            //run("${p.nameAsPath}/package-tree.html", PackageTreeTemplate(model, p))
            //run("${p.nameAsPath}/package-use.html", PackageUseTemplate(model, p))
            for (c in p.classes) {
                run("${c.nameAsPath}.html", ClassTemplate(model, p, c))
            }

            generateExtensionFunctions(p)
        }
    }

    protected fun log(text: String) {
        println(text)
    }


}
