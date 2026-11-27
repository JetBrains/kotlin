/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.firapt

import org.jetbrains.kotlin.kapt.base.KaptOptions
import org.jetbrains.kotlin.kapt.base.util.KaptLogger
import org.jetbrains.kotlin.kapt.base.util.info
import java.io.*
import java.net.URI
import java.util.Locale
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic
import javax.tools.FileObject
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.StandardLocation

/**
 * Runs annotation processors over the FIR of the sources and of their dependencies, without stubs and without javac. Java sources of
 * the module and the sources generated in a round are not modeled: later rounds have no root elements.
 */
internal class AptRunner(
    private val options: KaptOptions,
    private val logger: KaptLogger,
    private val env: AptEnv,
) : Closeable {
    private val types = AptTypes(env)
    private val elements = AptElementUtils(env, types)
    private val aptMessager = AptMessager(logger)
    private val aptFiler = AptFiler(options)

    private val processingEnv = object : ProcessingEnvironment {
        override fun getOptions(): Map<String, String> = this@AptRunner.options.processingOptions
        override fun getMessager(): Messager = aptMessager
        override fun getFiler(): Filer = aptFiler
        override fun getElementUtils(): Elements = this@AptRunner.elements
        override fun getTypeUtils(): Types = this@AptRunner.types
        override fun getSourceVersion(): SourceVersion = SourceVersion.latestSupported()
        override fun getLocale(): Locale = Locale.getDefault()
    }

    /** Returns `false` if a processor reported an error. */
    fun run(processors: List<Processor>): Boolean {
        for (processor in processors) processor.init(processingEnv)
        val supported = processors.associateWith { processor -> processor.supportedAnnotationTypes.map { pattern(it) } }
        val called = LinkedHashSet<Processor>()

        var roots: Set<Element> = env.sourceRoots.toSet()
        var round = 1
        while (true) {
            val roundEnv = AptRoundEnvironment(env, elements, roots, processingOver = false, isErrorRaised = { aptMessager.errorRaised })
            val present = roundEnv.presentAnnotations()
            val unclaimed = present.toMutableSet()
            for (processor in processors) {
                val patterns = supported.getValue(processor)
                val matched = unclaimed.filterTo(LinkedHashSet()) { annotation -> patterns.any { it(annotation.qualifiedName.toString()) } }
                val takesAll = patterns.any { it("*") }
                if (processor !in called && matched.isEmpty() && !takesAll) continue
                called += processor
                if (processor.process(matched, roundEnv)) unclaimed -= matched
            }
            if (aptMessager.errorRaised) return false
            val generated = aptFiler.takeGeneratedSources()
            logger.info { "FIR annotation processing: round $round generated ${generated.size} sources" }
            if (generated.isEmpty()) break
            roots = emptySet()
            round++
            if (round > MAX_ROUNDS) break
        }

        val lastRound = AptRoundEnvironment(env, elements, emptySet(), processingOver = true, isErrorRaised = { aptMessager.errorRaised })
        for (processor in called) processor.process(emptySet(), lastRound)
        aptFiler.takeGeneratedSources()
        return !aptMessager.errorRaised
    }

    private fun pattern(supported: String): (String) -> Boolean = when {
        supported == "*" -> { _ -> true }
        supported.endsWith(".*") -> {
            val prefix = supported.removeSuffix("*")
            ({ name -> name.startsWith(prefix) })
        }
        else -> { name -> name == supported }
    }

    override fun close() {}

    private companion object {
        const val MAX_ROUNDS = 100
    }
}

internal class AptRoundEnvironment(
    private val env: AptEnv,
    private val elements: AptElementUtils,
    private val roots: Set<Element>,
    private val processingOver: Boolean,
    private val isErrorRaised: () -> Boolean,
) : RoundEnvironment {
    /** Annotation type to the root elements, and the elements inside them, that it annotates. */
    private val index: Map<TypeElement, Set<Element>> by lazy {
        val result = LinkedHashMap<TypeElement, MutableSet<Element>>()
        fun add(element: Element) {
            for (mirror in element.annotationMirrors) {
                val type = mirror.annotationType.asElement() as TypeElement
                result.getOrPut(type) { LinkedHashSet() } += element
            }
        }
        fun scan(element: Element) {
            add(element)
            when (element) {
                is TypeElement -> {
                    element.typeParameters.forEach(::add)
                    element.enclosedElements.forEach(::scan)
                }
                is ExecutableElement -> {
                    element.typeParameters.forEach(::add)
                    element.parameters.forEach(::add)
                }
            }
        }
        roots.forEach(::scan)
        addInherited(result)
        result
    }

    private fun addInherited(result: MutableMap<TypeElement, MutableSet<Element>>) {
        val inherited = result.keys.filter { annotation ->
            annotation.annotationMirrors.any {
                (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("java.lang.annotation.Inherited")
            }
        }
        if (inherited.isEmpty()) return
        for (root in roots.filterIsInstance<TypeElement>()) {
            for (mirror in elements.getAllAnnotationMirrors(root)) {
                val type = mirror.annotationType.asElement() as TypeElement
                if (type in inherited) result.getOrPut(type) { LinkedHashSet() } += root
            }
        }
    }

    fun presentAnnotations(): Set<TypeElement> = index.keys

    override fun processingOver(): Boolean = processingOver
    override fun errorRaised(): Boolean = isErrorRaised()
    override fun getRootElements(): Set<Element> = roots
    override fun getElementsAnnotatedWith(a: TypeElement): Set<Element> = index[a].orEmpty()

    override fun getElementsAnnotatedWith(a: Class<out Annotation>): Set<Element> {
        val type = elements.getTypeElement(a.canonicalName) ?: return emptySet()
        return getElementsAnnotatedWith(type)
    }
}

internal class AptMessager(private val logger: KaptLogger) : Messager {
    var errorRaised = false
        private set

    override fun printMessage(kind: Diagnostic.Kind, msg: CharSequence): Unit = printMessage(kind, msg, null, null, null)
    override fun printMessage(kind: Diagnostic.Kind, msg: CharSequence, e: Element?): Unit = printMessage(kind, msg, e, null, null)
    override fun printMessage(kind: Diagnostic.Kind, msg: CharSequence, e: Element?, a: AnnotationMirror?): Unit =
        printMessage(kind, msg, e, a, null)

    override fun printMessage(kind: Diagnostic.Kind, msg: CharSequence, e: Element?, a: AnnotationMirror?, v: AnnotationValue?) {
        val text = if (e != null) "$e: $msg" else msg.toString()
        when (kind) {
            Diagnostic.Kind.ERROR -> {
                errorRaised = true
                logger.error(text)
            }
            Diagnostic.Kind.WARNING, Diagnostic.Kind.MANDATORY_WARNING -> logger.warn(text)
            else -> logger.info(text)
        }
    }
}

internal class AptFiler(private val options: KaptOptions) : Filer {
    private val created = HashSet<String>()
    private val generatedSources = mutableListOf<File>()

    fun takeGeneratedSources(): List<File> = generatedSources.toList().also { generatedSources.clear() }

    override fun createSourceFile(name: CharSequence, vararg originatingElements: Element?): JavaFileObject {
        val typeName = name.toString().substringAfter('/')
        val file = File(options.sourcesOutputDir, typeName.replace('.', '/') + ".java")
        register(file)
        generatedSources += file
        return AptFileObject(file, JavaFileObject.Kind.SOURCE)
    }

    override fun createClassFile(name: CharSequence, vararg originatingElements: Element?): JavaFileObject {
        val typeName = name.toString().substringAfter('/')
        val file = File(options.classesOutputDir, typeName.replace('.', '/') + ".class")
        register(file)
        return AptFileObject(file, JavaFileObject.Kind.CLASS)
    }

    override fun createResource(
        location: JavaFileManager.Location, moduleAndPkg: CharSequence, relativeName: CharSequence, vararg originatingElements: Element?,
    ): FileObject {
        val file = resourceFile(location, moduleAndPkg, relativeName)
        register(file)
        return AptFileObject(file, JavaFileObject.Kind.OTHER)
    }

    override fun getResource(location: JavaFileManager.Location, moduleAndPkg: CharSequence, relativeName: CharSequence): FileObject {
        val file = resourceFile(location, moduleAndPkg, relativeName)
        if (!file.isFile) throw FileNotFoundException(file.path)
        return AptFileObject(file, JavaFileObject.Kind.OTHER)
    }

    private fun resourceFile(location: JavaFileManager.Location, moduleAndPkg: CharSequence, relativeName: CharSequence): File {
        val root = when (location) {
            StandardLocation.SOURCE_OUTPUT -> options.sourcesOutputDir
            StandardLocation.CLASS_OUTPUT -> options.classesOutputDir
            else -> options.getKotlinGeneratedSourcesDirectory()?.takeIf { location.name == "KOTLIN_GENERATED" }
                ?: throw IllegalArgumentException("Unsupported location: $location")
        }
        val pkg = moduleAndPkg.toString().substringAfter('/').replace('.', '/')
        return if (pkg.isEmpty()) File(root, relativeName.toString()) else File(File(root, pkg), relativeName.toString())
    }

    private fun register(file: File) {
        if (!created.add(file.canonicalPath)) throw FilerException("Attempt to reopen a file for path ${file.path}")
    }
}

private class AptFileObject(private val file: File, kind: JavaFileObject.Kind) : SimpleJavaFileObject(file.toURI(), kind) {
    override fun toUri(): URI = file.toURI()
    override fun getName(): String = file.path

    override fun openOutputStream(): OutputStream {
        file.parentFile.mkdirs()
        return BufferedOutputStream(FileOutputStream(file))
    }

    override fun openWriter(): Writer = OutputStreamWriter(openOutputStream(), Charsets.UTF_8)
    override fun openInputStream(): InputStream = BufferedInputStream(FileInputStream(file))
    override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence = file.readText()
    override fun getLastModified(): Long = file.lastModified()
    override fun delete(): Boolean = file.delete()
}
