/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kaptlite.stubs

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import org.jetbrains.kaptlite.stubs.util.JavaClassName
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.kotlin.kaptlite.kdoc.KDocParsingHelper

class StubGeneratorContext(
    val loader: ClassFileLoader,
    diagnostics: DiagnosticSink,
    private val state: GenerationState,
    private val origins: Map<Any, JvmDeclarationOrigin>
) {
    val checker = StubChecker(state, diagnostics)

    private val javaClassNameMappings: Map<String, JavaClassName> = run {
        val contentRoots = state.configuration[CLIConfigurationKeys.CONTENT_ROOTS] ?: emptyList()
        val javaSourceRoots = contentRoots.filterIsInstance<JavaSourceRoot>()
        if (javaSourceRoots.isEmpty()) {
            return@run emptyMap()
        }

        val psiManager = PsiManager.getInstance(state.project)
        val mappings = HashMap<String, JavaClassName>()

        for (root in javaSourceRoots) {
            root.file.walk().filter { it.isFile && it.extension == "java" }.forEach {
                val vFile = StandardFileSystems.local().findFileByPath(it.absolutePath)
                if (vFile != null) {
                    val javaFile = psiManager.findFile(vFile) as? PsiJavaFile
                    if (javaFile != null) {
                        mappings.putAll(collectNameMappings(javaFile))
                    }
                }
            }
        }

        mappings
    }

    val bindingContext: BindingContext
        get() = state.bindingContext

    val languageVersionSettings: LanguageVersionSettings
        get() = state.languageVersionSettings

    fun getKDocComment(kind: KDocParsingHelper.DeclarationKind, origin: JvmDeclarationOrigin): String? {
        return KDocParsingHelper.getKDocComment(kind, origin, bindingContext)
    }

    fun getClassOrigin(node: ClassNode): JvmDeclarationOrigin {
        return origins[node] ?: JvmDeclarationOrigin.NO_ORIGIN
    }

    fun getMethodOrigin(node: MethodNode): JvmDeclarationOrigin {
        return origins[node] ?: JvmDeclarationOrigin.NO_ORIGIN
    }

    fun getFieldOrigin(node: FieldNode): JvmDeclarationOrigin {
        return origins[node] ?: JvmDeclarationOrigin.NO_ORIGIN
    }

    fun getClassName(internalName: String): JavaClassName {
        if ('$' !in internalName) {
            return getClassNameSimple(internalName)
        }

        javaClassNameMappings[internalName]?.let { return it }
        loader.load(internalName, cache = true)?.let { return getClassName(it) }
        return getClassNameSimple(internalName)
    }

    fun getClassName(classNode: ClassNode): JavaClassName {
        val internalName = classNode.name

        for (innerClass in classNode.innerClasses) {
            if (innerClass.name == internalName) {
                val innerName = innerClass.innerName
                val outerName = innerClass.outerName
                if (innerName == null || outerName == null) {
                    return getClassNameForAnonymousClass(classNode)
                }

                return JavaClassName.Nested(getClassName(outerName), innerName)
            }
        }

        return getClassNameSimple(internalName)
    }

    private fun getClassNameForAnonymousClass(classNode: ClassNode): JavaClassName {
        val superName = classNode.superName ?: JAVA_LANG_OBJECT
        if (superName == JAVA_LANG_OBJECT) {
            val interfaces = classNode.interfaces?.sorted() ?: emptyList()
            val firstInterface = interfaces.firstOrNull()
            if (firstInterface != null) {
                return getClassName(firstInterface)
            }
        }

        return getClassName(superName)
    }

    private fun getClassNameSimple(internalName: String): JavaClassName {
        val packageName = internalName.substringBeforeLast('/', missingDelimiterValue = "").replace('/', '.')
        val simpleName = if (packageName.isEmpty()) internalName else internalName.drop(packageName.length + 1)
        return JavaClassName.TopLevel(packageName, simpleName)
    }

    private fun collectNameMappings(javaFile: PsiJavaFile): Map<String, JavaClassName> {
        val mappings = HashMap<String, JavaClassName>()
        val packageName = javaFile.packageName

        fun process(javaClass: PsiClass) {
            val className = getClassName(packageName, javaClass)
            if (className != null) {
                mappings[className.getInternalName()] = className
            }
        }

        for (javaClass in javaFile.classes) {
            process(javaClass)
            javaClass.allInnerClasses.forEach { process(it) }
        }

        return mappings
    }

    private fun getClassName(packageName: String, javaClass: PsiClass): JavaClassName? {
        val chunks = mutableListOf<String>()

        var current: PsiClass? = javaClass
        while (current != null) {
            val name = current.name ?: return null
            chunks.add(name)
            current = current.containingClass
        }

        val reversed = chunks.asReversed()
        var className: JavaClassName = JavaClassName.TopLevel(packageName, reversed.first())
        for (index in 1 until reversed.size) {
            className = JavaClassName.Nested(className, reversed[index])
        }
        return className
    }
}