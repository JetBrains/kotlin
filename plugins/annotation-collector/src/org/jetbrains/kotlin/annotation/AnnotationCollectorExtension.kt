/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.annotation

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.DelegatingClassBuilder
import org.jetbrains.kotlin.codegen.DelegatingClassBuilderFactory
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.*
import java.io.File
import java.io.IOException
import java.io.Writer
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

abstract class AnnotationCollectorExtensionBase(val supportInheritedAnnotations: Boolean) : ClassBuilderInterceptorExtension {

    private object RecordTypes {
        val ANNOTATED_CLASS = "c"
        val ANNOTATED_METHOD = "m"
        val ANNOTATED_FIELD = "f"

        val SHORTENED_ANNOTATION = "a"
        val SHORTENED_PACKAGE_NAME = "p"

        val CLASS_DECLARATION = "d"
    }

    protected abstract val annotationFilterList: List<String>?

    private val shortenedAnnotationCache = ShortenedNameCache(RecordTypes.SHORTENED_ANNOTATION)
    private val shortenedPackageNameCache = ShortenedNameCache(RecordTypes.SHORTENED_PACKAGE_NAME)

    override fun interceptClassBuilderFactory(
            interceptedFactory: ClassBuilderFactory,
            bindingContext: BindingContext,
            diagnostics: DiagnosticSink
    ): ClassBuilderFactory {
        return AnnotationCollectorClassBuilderFactory(interceptedFactory, getWriter(diagnostics), diagnostics)
    }

    protected abstract fun getWriter(diagnostic: DiagnosticSink): Writer
    protected abstract fun closeWriter()

    private inner class AnnotationCollectorClassBuilderFactory(
            delegateFactory: ClassBuilderFactory,
            val writer: Writer,
            val diagnostics: DiagnosticSink
    ) : DelegatingClassBuilderFactory(delegateFactory) {

        override fun newClassBuilder(origin: JvmDeclarationOrigin): DelegatingClassBuilder {
            return AnnotationCollectorClassBuilder(delegate.newClassBuilder(origin), writer, diagnostics)
        }

        override fun close() {
            closeWriter()
            delegate.close()
        }
    }

    private inner class AnnotationCollectorClassBuilder(
            internal val delegateClassBuilder: ClassBuilder,
            val writer: Writer,
            val diagnostics: DiagnosticSink
    ) : DelegatingClassBuilder() {
        private val annotationFilterEnabled: Boolean
        private val annotationFilters: List<Pattern>

        init {
            val nullableAnnotations = annotationFilterList?.map { it.compilePatternOpt() } ?: listOf()
            annotationFilterEnabled = nullableAnnotations.isNotEmpty()
            annotationFilters = nullableAnnotations.filterNotNull()
        }

        private val classVisitor: ClassVisitor by lazy {
            object : ClassVisitor(Opcodes.ASM5, super.getVisitor()) {
                override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                    recordAnnotation(null, RecordTypes.ANNOTATED_CLASS, desc)
                    return super.visitAnnotation(desc, visible)
                }
            }
        }

        private lateinit var currentClassSimpleName: String

        private lateinit var currentPackageName: String

        override fun getVisitor() = classVisitor

        override fun getDelegate() = delegateClassBuilder

        override fun defineClass(
                origin: PsiElement?,
                version: Int,
                access: Int,
                name: String,
                signature: String?,
                superName: String,
                interfaces: Array<out String>
        ) {
            val currentClassSimpleName = name.substringAfterLast('/')
            val currentPackageName = name.substringBeforeLast('/', "").replace('/', '.')

            this.currentClassSimpleName = currentClassSimpleName
            this.currentPackageName = currentPackageName

            if (supportInheritedAnnotations) {
                recordClass(currentPackageName, currentClassSimpleName)
            }

            super.defineClass(origin, version, access, name, signature, superName, interfaces)
        }

        override fun newField(
                origin: JvmDeclarationOrigin,
                access: Int,
                name: String,
                desc: String,
                signature: String?,
                value: Any?
        ): FieldVisitor {
            return object : FieldVisitor(Opcodes.ASM5, super.newField(origin, access, name, desc, signature, value)) {
                override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                    recordAnnotation(name, RecordTypes.ANNOTATED_FIELD, desc)
                    return super.visitAnnotation(desc, visible)
                }
            }
        }

        override fun newMethod(
                origin: JvmDeclarationOrigin,
                access: Int,
                name: String,
                desc: String,
                signature: String?,
                exceptions: Array<out String>?
        ): MethodVisitor {
            return object : MethodVisitor(Opcodes.ASM5, super.newMethod(origin, access, name, desc, signature, exceptions)) {
                override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                    recordAnnotation(name, RecordTypes.ANNOTATED_METHOD, desc)
                    return super.visitAnnotation(desc, visible)
                }
            }
        }

        private fun isAnnotationHandled(annotationFqName: String): Boolean {
            return if (annotationFilterEnabled)
                annotationFilters.any { it.matcher(annotationFqName).matches() }
            else annotationFqName != JvmAnnotationNames.METADATA_FQ_NAME.asString()
        }

        private fun recordClass(packageName: String, className: String) {
            if (!isValidName(packageName) || !isValidName(className)) return

            val packageNameId = if (!packageName.isEmpty())
                shortenedPackageNameCache.save(packageName, writer)
            else null

            val outputClassName = getOutputClassName(packageNameId, className)
            writer.write("${RecordTypes.CLASS_DECLARATION} $outputClassName\n")
        }

        private fun recordAnnotation(name: String?, type: String, annotationDesc: String) {
            val annotationFqName = Type.getType(annotationDesc).className
            if (!isAnnotationHandled(annotationFqName) || !isValidName(annotationFqName)) return

            if (name != null && !isValidName(name)) return

            try {
                val packageName = this.currentPackageName
                if (!isValidName(packageName)) return

                val className = this.currentClassSimpleName
                if (!isValidName(className)) return

                val annotationId = shortenedAnnotationCache.save(annotationFqName, writer)

                val packageNameId = if (!packageName.isEmpty())
                    shortenedPackageNameCache.save(packageName, writer)
                else null

                val outputClassName = getOutputClassName(packageNameId, className)
                val elementName = if (name != null) " $name" else ""

                writer.write("$type $annotationId $outputClassName$elementName\n")
            }
            catch (e: IOException) {
                throw e
            }
        }

        private fun isValidName(name: String): Boolean {
            return ' ' !in name
        }

        private fun getOutputClassName(packageNameId: String?, className: String): String {
            return if (packageNameId == null) className else "$packageNameId/$className"
        }

        private fun String.compilePatternOpt(): Pattern? {
            return try {
                Pattern.compile(this)
            }
            catch (e: PatternSyntaxException) {
                null
            }
        }
    }

    private class ShortenedNameCache(val type: String) {
        private val internalCache = hashMapOf<String, String>()
        private var counter: Int = 0

        fun save(name: String, writer: Writer): String {
            return internalCache.getOrPut(name) {
                val resultId = counter.toString()
                writer.write("$type $name $resultId\n")
                counter += 1
                resultId
            }
        }
    }
}

class AnnotationCollectorExtension(
        override val annotationFilterList: List<String>? = null,
        private val outputFilename: String? = null,
        supportInheritedAnnotations: Boolean
) : AnnotationCollectorExtensionBase(supportInheritedAnnotations) {

    private var writerInternal: Writer? = null

    override fun closeWriter() {
        writerInternal?.close()
    }

    override fun getWriter(diagnostic: DiagnosticSink): Writer {
        return writerInternal ?: try {
            with (File(outputFilename)) {
                val parent = parentFile
                if (!parent.exists()) parent.mkdirs()
                writerInternal = bufferedWriter()
                writerInternal!!
            }
        }
        catch (e: IOException) {
            throw e
        }
    }
}