package org.jetbrains.kotlin.doc

import std.*
import std.util.*

import org.jetbrains.kotlin.doc.templates.*
import org.jetbrains.kotlin.template.TextTemplate
import org.jetbrains.kotlin.model.*

import java.io.File
import java.util.List
import java.util.HashSet
import java.util.Collection

import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.BindingContext.*
import org.jetbrains.jet.compiler.CompilerPlugin
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.resolve.java.JavaNamespaceDescriptor
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.jet.util.slicedmap.WritableSlice
import org.jetbrains.jet.lang.resolve.BindingContextUtils


class KDoc(val outputDir: File) : CompilerPlugin {
    val model = KModel()
    var context: BindingContext? = null

    override fun processFiles(context: BindingContext?, sources: List<JetFile?>?) {
        $context = context
        if (context != null && sources != null) {
            val allNamespaces = HashSet<NamespaceDescriptor>()
            for (source in sources) {
                if (source != null) {
                    // We retrieve a descriptor by a PSI element from the context
                    val namespaceDescriptor = BindingContextUtils.namespaceDescriptor(context, source)
                    if (namespaceDescriptor != null) {
                        allNamespaces.add(namespaceDescriptor);
                    }
                }
            }
            val allClasses = HashSet<KClass>()
            for (namespace in allNamespaces) {
                model.getPackage(namespace)
                for (descriptor in namespace.getMemberScope().getAllDescriptors()) {
                    if (descriptor is ClassDescriptor) {
                        val klass = model.getClass(descriptor)
                        if (klass != null) {
                            allClasses.add(klass)
                        }
                    } else if (descriptor is NamespaceDescriptor) {
                        model.getPackage(descriptor)
                    }
                }
            }

            // lets add the functions...
            for (klass in allClasses) {
                addFunctions(klass, klass.functions, klass.descriptor.getDefaultType().getMemberScope())
            }
            for (pkg in model.allPackages) {
                for (descriptor in pkg.descriptor.getMemberScope().getAllDescriptors()) {
                    if (descriptor is CallableDescriptor) {
                        val function = createFunction(pkg, descriptor)
                        if (function != null && function.extensionClass == null) {
                            pkg.functions.add(function)
                            pkg.local = true
                        }
                    }
                }
            }
            generate();
        }
    }

    fun generate(): Unit {
        val generator = KDocGenerator(model, outputDir)
        generator.execute()
    }


    protected fun addFunctions(owner: KClassOrPackage, list: Collection<KFunction>, scope: JetScope): Unit {
        val descriptors = scope.getAllDescriptors()
        for (descriptor in descriptors) {
            if (descriptor is CallableDescriptor) {
                val function = createFunction(owner, descriptor)
                if (function != null) {
                    list.add(function)
                }
            }
        }
    }

    protected fun createFunction(owner: KClassOrPackage, descriptor: CallableDescriptor): KFunction? {
        val returnType = model.getClass(descriptor.getReturnType())
        if (returnType != null) {
            val function = KFunction(owner, descriptor.getName() ?: "null", returnType)
            function.description = commentsFor(descriptor)
            val params = descriptor.getValueParameters()
            for (param in params) {
                if (param != null) {
                    val p = createParameter(param)
                    if (p != null) {
                        function.parameters.add(p)
                    }
                }
            }
            val receiver = descriptor.getReceiverParameter()
            if (receiver is ExtensionReceiver) {
                function.extensionClass = model.getClass(receiver.getType())
            }
            return function
        }
        return null
    }

    protected fun createParameter(descriptor: ValueParameterDescriptor): KParameter? {
        val returnType = model.getClass(descriptor.getReturnType())
        if (returnType != null) {
            val name = descriptor.getName()
            val answer = KParameter(name, returnType)
            answer.description = commentsFor(descriptor)
            return answer
        }
        return null
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
