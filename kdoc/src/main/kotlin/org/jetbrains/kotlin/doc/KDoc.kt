package org.jetbrains.kotlin.doc

import org.jetbrains.kotlin.doc.templates.*
import org.jetbrains.kotlin.template.TextTemplate
import org.jetbrains.kotlin.model.KModel

import java.io.File
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.compiler.CompilerPlugin
import java.util.List
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor
import java.util.HashSet
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.descriptors.ClassDescriptor

class KDoc(val outputDir: File) : KDocSupport() {
    val model = KModel()

    /** TODO compile errors so lets use Java for this bit for now...
    override fun processFiles(context: BindingContext, files: List<JetFile?>?) {
        println("==== KDoc running! Generating to $outputDir")
        if (context != null) {
            val namespaces = HashSet<NamespaceDescriptor>()
            if (files != null) {
                for (source in files) {
                    if (source != null) {
                        val namespaceDescriptor = context.get(BindingContext.NAMESPACE, source)
                        if (namespaceDescriptor != null) {
                            namespaces.add(namespaceDescriptor)
                        }
                    }
                }
            }
        }
    }
    */

    override fun addClass(namespace: NamespaceDescriptor?, classElement: ClassDescriptor?) {
        if (namespace != null && classElement != null) {
            //val docComment = getDocCommentFor(classElement.sure()) ?: "";
            val name = classElement.getName()
            val namespaceName = namespace.getName() ?: ""
            val pkg = model.getPackage(namespaceName)
            if (name != null) {
                println("Found namespace ${namespaceName} class: ${name}")
                pkg.getClass(name)
            }
        }
    }

    override fun generate() {
        /*
                for (classElement in allClasses) {
                    if (classElement != null) {
                    }
                }
        */
        if (!model.packages.isEmpty()) {
            val generator = KDocGenerator(model, outputDir)
            generator.execute()
        }
    }

    protected fun getDocCommentFor(psiElement: PsiElement): String? {
        // This method is a hack. Doc comments should be easily accessible, but they aren't for now.
        var node = psiElement.getNode()?.getTreePrev()
        while (node != null && (node?.getElementType() == JetTokens.WHITE_SPACE || node?.getElementType() == JetTokens.BLOCK_COMMENT)) {
            node = node?.getTreePrev();
        }
        if (node == null) return null;
        if (node?.getElementType() != JetTokens.DOC_COMMENT) return null;
        return node?.getText();
    }

}

class KDocGenerator(val model: KModel, val outputDir: File) {

    fun execute(): Unit {
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
        // run("serialized-form.html", SerializedFormTemplate(model))
        /**
        TODO
        */
        for (p in model.packages) {
            run("${p.nameAsPath}/package-frame.html", PackageFrameTemplate(model, p))
            run("${p.nameAsPath}/package-summary.html", PackageSummaryTemplate(model, p))
            //run("${p.nameAsPath}/package-tree.html", PackageTreeTemplate(model, p))
            //run("${p.nameAsPath}/package-use.html", PackageUseTemplate(model, p))
            for (c in p.classes) {
                run("${c.nameAsPath}.html", ClassTemplate(model, p, c))
            }
        }
    }

    protected fun run(fileName: String, template: TextTemplate): Unit {
        val file = File(outputDir, fileName)
        file.getParentFile()?.mkdirs()

        log("Generating $fileName")
        template.renderTo(file)
    }

    protected fun log(text: String) {
        println(text)
    }

}
