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
                        val klass = getOrCreateClass(descriptor)
                        if (klass != null) {
                            allClasses.add(klass)
                        }
                    } else if (descriptor is NamespaceDescriptor) {
                        model.getPackage(descriptor)
                    }
                }
                //addNamespace(namespace)
            }
            /*
                        for (namespace in allNamespaces) {
                            for (descriptor in namespace.getMemberScope().getAllDescriptors()) {
                                if (descriptor is CallableDescriptor) {
                                    val pkg = getPackage(namespace)
                                    val function = createFunction(pkg, descriptor)
                                    if (function != null && function.extensionClass == null) {
                                        pkg.functions.add(function)
                                        pkg.local = true
                                    }
                                }
                            }
                            //addNamespace(namespace)
                        }
            */

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


    protected fun getOrCreateClass(classElement: ClassDescriptor): KClass? {
        //val docComment = getDocCommentFor(classElement.sure()) ?: "";
        val name = classElement.getName()
        val container = classElement.containingDeclaration
        if (name != null && container is NamespaceDescriptor) {
            val pkg = model.getPackage(container)
            //val namespaceName = containerName(classElement)
            val klass = KClass(pkg, classElement, name)
            pkg.classMap.put(name, klass)
            if (pkg.description.length() == 0) {
                pkg.description = commentsFor(container)
            }
            pkg.local = true
            klass.description = commentsFor(classElement)
            val superTypes = classElement.getTypeConstructor().getSupertypes()
            for (st in superTypes) {
                val sc = getType(st)
                if (sc != null) {
                    klass.baseClasses.add(sc)
                }
            }
            //addFunctions(klass, klass.functions, classElement.getDefaultType().getMemberScope())
            return klass
        } else {
            println("No package found for $container and class $name")
            return null
        }
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
        val returnType = getType(descriptor.getReturnType())
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
                function.extensionClass = getType(receiver.getType())
            }
            return function
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

    protected fun commentsFor(descriptor: DeclarationDescriptor): String {
        /*
        val psiElement = try {
            BindingContextUtils.descriptorToDeclaration(context.sure(), descriptor)
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
        */
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
