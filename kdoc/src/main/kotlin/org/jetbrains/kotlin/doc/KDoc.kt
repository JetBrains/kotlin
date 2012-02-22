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
import org.jetbrains.kotlin.model.KMethod
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.kotlin.model.KClass
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.resolve.java.JavaNamespaceDescriptor
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.model.KParameter

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
            val klass = getOrCreateClass(classElement)
            if (klass != null) {
                klass.pkg.local = true
            }
        }
    }

    protected fun containerName(descriptor: DeclarationDescriptor): String {
        val container = descriptor.containingDeclaration
        if (container == null || container is ModuleDescriptor || container is JavaNamespaceDescriptor) {
            return ""
        } else {
            val parent = containerName(container)
            val name = container.getName() ?: ""
            val answer = if (parent.length() > 0) parent + "." + name else name
            return if (answer.startsWith(".")) answer.substring(1) else answer
        }
    }

    protected fun getOrCreateClass(classElement: ClassDescriptor): KClass? {
        //val docComment = getDocCommentFor(classElement.sure()) ?: "";
        val container = classElement.containingDeclaration
        val namespaceName = containerName(classElement)
        val pkg = model.getPackage(namespaceName)
        val name = classElement.getName()
        if (name != null) {
            val klass = pkg.getClass(name)
            klass.initialise {
                klass.description = commentsFor(classElement)
                val descriptors = classElement.getDefaultType().getMemberScope().getAllDescriptors()
                for (descriptor in descriptors) {
                    if (descriptor is CallableDescriptor) {
                        val method = createMethod(descriptor)
                        if (method != null) {
                            klass.methods.add(method)
                        }
                    }
                }
            }
            return klass
        }
        return null
    }

    protected fun createMethod(descriptor: CallableDescriptor): KMethod? {
        val returnType = getType(descriptor.getReturnType())
        if (returnType != null) {
            val method = KMethod(descriptor.getName() ?: "null", returnType)
            method.description = commentsFor(descriptor)
            val params = descriptor.getValueParameters()
            for (param in params) {
                if (param != null) {
                    val p = createParameter(param)
                    if (p != null) {
                        method.parameters.add(p)
                    }
                }
            }
            return method
        }
        return null
    }

    protected fun createParameter(descriptor: ValueParameterDescriptor): KParameter? {
        val returnType = getType(descriptor.getReturnType())
        if (returnType != null) {
            val name = descriptor.getName()
            val answer = KParameter(name, returnType)
            answer.description = commentsFor(descriptor)
            return answer
        }
        return null
    }

    protected fun getType(aType: JetType?): KClass? {
        if (aType != null) {
            val classifierDescriptor = aType.constructor.declarationDescriptor
            if (classifierDescriptor is ClassDescriptor) {
                return getOrCreateClass(classifierDescriptor)
            }
        }
        return null
    }

    override fun generate() {
        if (!model.packages.isEmpty()) {
            val generator = KDocGenerator(model, outputDir)
            generator.execute()
        }
    }

    protected fun commentsFor(descriptor: DeclarationDescriptor): String {
        val psiElement = try {
            context.get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor)
        } catch (e: Throwable) {
            // ignore exceptions on fake descriptors
            null
        }

        // This method is a hack. Doc comments should be easily accessible, but they aren't for now.
        if (psiElement != null) {
            var node = psiElement.getNode()?.getTreePrev()
            while (node != null && (node?.getElementType() == JetTokens.WHITE_SPACE || node?.getElementType() == JetTokens.BLOCK_COMMENT)) {
                node = node?.getTreePrev()
            }
            if (node == null) return ""
            if (node?.getElementType() != JetTokens.DOC_COMMENT) return ""
            return node?.getText() ?: ""
        }
        return ""
    }

}

class KDocGenerator(val model: KModel, val outputDir: File) {

    fun execute(): Unit {
        println("Generating kdoc to $outputDir")
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
