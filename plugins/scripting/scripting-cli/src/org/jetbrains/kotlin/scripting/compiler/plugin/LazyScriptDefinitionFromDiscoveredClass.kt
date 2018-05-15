/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import java.io.File
import java.net.URLClassLoader
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.annotations.KotlinScriptFileExtension
import kotlin.script.experimental.api.*
import kotlin.script.experimental.definitions.ScriptDefinitionFromAnnotatedBaseClass

class LazyScriptDefinitionFromDiscoveredClass(
    classBytes: ByteArray,
    private val className: String,
    private val classpath: List<File>,
    private val messageCollector: MessageCollector
) : KotlinScriptDefinitionAdapterFromNewAPIBase() {
    private val annotationsFromAsm = loadAnnotationsFromClass(classBytes)

    private val classloader by lazy {
        // should use this cl to allow smooth interop with classes explicitly mentioned here, see e.g. scriptDefinition body
        val parentClassloader = LazyScriptDefinitionFromDiscoveredClass::class.java.classLoader
        if (classpath.isEmpty()) parentClassloader
        else URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray(), parentClassloader)
    }

    override val scriptDefinition: ScriptDefinition by lazy {
        messageCollector.report(
            CompilerMessageSeverity.LOGGING,
            "Configure scripting: loading script definition class $className using classpath $classpath\n.  ${Thread.currentThread().stackTrace}"
        )
        try {
            val cls = classloader.loadClass(className).kotlin
            ScriptDefinitionFromAnnotatedBaseClass(
                ScriptingEnvironment(
                    ScriptingEnvironmentProperties.baseClass to cls
                )
            )
        } catch (ex: ClassNotFoundException) {
            messageCollector.report(CompilerMessageSeverity.ERROR, "Cannot find script definition class $className")
            InvalidScriptDefinition
        } catch (ex: Exception) {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Error processing script definition class $className: ${ex.message}"
            )
            InvalidScriptDefinition
        }
    }

    override val scriptFileExtensionWithDot: String by lazy {
        val ext = annotationsFromAsm.find { it.name == KotlinScriptFileExtension::class.simpleName!! }?.args?.first()
                ?: scriptDefinition.properties.let {
                    it.getOrNull(ScriptDefinitionProperties.fileExtension) ?: "kts"
                }
        ".$ext"
    }

    override val name: String by lazy {
        annotationsFromAsm.find { it.name == KotlinScript::class.simpleName!! }?.args?.first()
                ?: super.name
    }
}

object InvalidScriptDefinition : ScriptDefinition {
    override val properties: ScriptDefinitionPropertiesBag = ScriptDefinitionPropertiesBag()
    override val compilationConfigurator: ScriptCompilationConfigurator = object : ScriptCompilationConfigurator {
        override val defaultConfiguration: ScriptCompileConfiguration = ScriptDefinitionPropertiesBag()
    }
    override val evaluator: ScriptEvaluator<*>? = null
}

private class BinAnnData(
    val name: String,
    val args: ArrayList<String> = arrayListOf()
)

private class TemplateAnnotationVisitor(val anns: ArrayList<BinAnnData> = arrayListOf()) : AnnotationVisitor(Opcodes.ASM5) {
    override fun visit(name: String?, value: Any?) {
        anns.last().args.add(value.toString())
    }
}

private class TemplateClassVisitor(val annVisitor: TemplateAnnotationVisitor) : ClassVisitor(Opcodes.ASM5) {
    override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor {
        val shortName = jvmDescToClassId(desc).shortClassName.asString()
        if (shortName.startsWith("KotlinScript")) {
            annVisitor.anns.add(BinAnnData(shortName))
        }
        return annVisitor
    }
}

private fun jvmDescToClassId(desc: String): ClassId {
    assert(desc.startsWith("L") && desc.endsWith(";")) { "Not a JVM descriptor: $desc" }
    val name = desc.substring(1, desc.length - 1)
    val cid = ClassId.topLevel(FqName(name.replace('/', '.')))
    return cid
}

private fun loadAnnotationsFromClass(fileContents: ByteArray): ArrayList<BinAnnData> {

    val visitor =
        TemplateClassVisitor(TemplateAnnotationVisitor())

    ClassReader(fileContents).accept(visitor, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)

    return visitor.annVisitor.anns
}

