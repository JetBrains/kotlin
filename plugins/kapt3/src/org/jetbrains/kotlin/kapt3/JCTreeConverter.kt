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

package org.jetbrains.kotlin.kapt3

import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.JCTree.*
import com.sun.tools.javac.tree.TreeMaker
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Name
import com.sun.tools.javac.util.Names
import com.sun.tools.javac.util.SharedNameTable
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import javax.lang.model.element.ElementKind
import com.sun.tools.javac.util.List as JavacList

class JCTreeConverter(context: Context, val classes: List<ClassNode>, val origins: Map<Any, JvmDeclarationOrigin>) {
    private companion object {
        private val VISIBILITY_MODIFIERS = Opcodes.ACC_PUBLIC or Opcodes.ACC_PRIVATE or Opcodes.ACC_PROTECTED
        private val MODALITY_MODIFIERS = Opcodes.ACC_FINAL or Opcodes.ACC_ABSTRACT

        private val CLASS_MODIFIERS = VISIBILITY_MODIFIERS or MODALITY_MODIFIERS or Opcodes.ACC_DEPRECATED

        private val METHOD_MODIFIERS = VISIBILITY_MODIFIERS or MODALITY_MODIFIERS or
                Opcodes.ACC_SYNCHRONIZED or Opcodes.ACC_STATIC or Opcodes.ACC_NATIVE or Opcodes.ACC_DEPRECATED

        private val FIELD_MODIFIERS = VISIBILITY_MODIFIERS or MODALITY_MODIFIERS or
                Opcodes.ACC_STATIC or Opcodes.ACC_VOLATILE or Opcodes.ACC_TRANSIENT

        private val BLACKLISTED_ANNOTATATIONS = listOf(
                "java.lang.Deprecated", "kotlin.Deprecated", // Deprecated annotations
                "java.lang.annotation.", // Java annotations
                "org.jetbrains.annotations.", // Nullable/NotNull, ReadOnly, Mutable
                "kotlin.jvm.", "kotlin.Metadata" // Kotlin annotations from runtime
        )
    }

    private val treeMaker = TreeMaker.instance(context)
    private val nameTable = SharedNameTable(Names.instance(context))

    private var done = false

    fun convert(): JavacList<JCCompilationUnit> {
        if (done) error(JCTreeConverter::class.java.simpleName + " can convert classes only once")
        done = true
        return mapValues(classes) { convertTopLevelClass(it) }
    }

    private fun convertTopLevelClass(clazz: ClassNode): JCCompilationUnit? {
        val origin = origins[clazz] ?: return null
        val ktFile = origin.element?.containingFile as? KtFile ?: return null
        val descriptor = origin.descriptor as? ClassDescriptor ?: return null

        // Nested classes will be processed during the outer classes conversion
        if (descriptor.containingDeclaration is ClassDescriptor) return null

        val classDeclaration = convert(clazz)

        val packageAnnotations = JavacList.nil<JCAnnotation>()
        val packageName = ktFile.packageFqName.asString()
        val packageClause = if (packageName.isEmpty()) null else convertFqName(packageName)

        val imports = JavacList.nil<JCTree>()
        val classes = JavacList.of<JCTree>(classDeclaration)

        return treeMaker.TopLevel(packageAnnotations, packageClause, imports + classes)
    }

    /**
     * Returns false for the inner classe or if the origin for the class was not found.
     */
    private fun convert(clazz: ClassNode): JCClassDecl? {
        if (isSynthetic(clazz.access)) return null

        val descriptor = origins[clazz]?.descriptor as? ClassDescriptor ?: return null
        val modifiers = convertModifiers(clazz.access, ElementKind.CLASS, clazz.visibleAnnotations, clazz.invisibleAnnotations)
        val simpleName = name(descriptor.name.asString())
        val typeParams = JavacList.nil<JCTypeParameter>()
        val extending = if (clazz.superName == "java/lang/Object") null else convertFqName(clazz.superName)
        val implementing = mapValues(clazz.interfaces) { convertFqName(it) }

        val fields = mapValues<FieldNode, JCTree>(clazz.fields) { convertField(it) }
        val methods = mapValues<MethodNode, JCTree>(clazz.methods) { convertMethod(it, simpleName) }
        val nestedClasses = mapValues<InnerClassNode, JCTree>(clazz.innerClasses) { innerClass ->
            if (innerClass.outerName != clazz.name) return@mapValues null
            val innerClassNode = classes.firstOrNull { it.name == innerClass.name } ?: return@mapValues null
            convert(innerClassNode)
        }

        return treeMaker.ClassDef(modifiers, simpleName, typeParams, extending, implementing, fields + methods + nestedClasses)
    }

    private fun convertField(field: FieldNode): JCVariableDecl? {
        if (isSynthetic(field.access)) return null

        val modifiers = convertModifiers(field.access, ElementKind.FIELD, field.visibleAnnotations, field.invisibleAnnotations)
        val name = name(field.name)
        val type = convertFqName(Type.getType(field.desc).className)
        val value = field.value
        val initializer = when (value) {
            is Byte, is Boolean, is Char, is Short, is Int, is Long, is Float, is Double, is String -> treeMaker.Literal(value)
            else -> null
        }
        return treeMaker.VarDef(modifiers, name, type, initializer)
    }

    private fun convertMethod(method: MethodNode, containingClassSimpleName: Name): JCMethodDecl? {
        if (isSynthetic(method.access)) return null

        val modifiers = convertModifiers(method.access, ElementKind.METHOD, method.visibleAnnotations, method.invisibleAnnotations)
        val isConstructor = method.name == "<init>"
        val name = if (isConstructor) containingClassSimpleName else name(method.name)
        val typeParameters = JavacList.nil<JCTypeParameter>()
        val receiverParameter = null

        val returnType = Type.getReturnType(method.desc)
        val returnTypeExpr = convertFqName(returnType.className)

        val parametersInfo = method.getParametersInfo()
        val parameters = mapValues(parametersInfo) { info ->
            val modifiers = convertModifiers(info.access, ElementKind.PARAMETER, info.visibleAnnotations, info.invisibleAnnotations)
            val name = name(info.name)
            val type = convertFqName(info.type)
            treeMaker.VarDef(modifiers, name, type, null)
        }

        val thrown = mapValues(method.exceptions) { convertFqName(it) }

        val defaultValue = method.annotationDefault?.let { convertLiteralExpression(it) }

        val body = if (defaultValue != null) {
            null
        } else if (isConstructor || returnType == Type.VOID_TYPE) {
            treeMaker.Block(0, JavacList.nil())
        } else {
            val returnStatement = treeMaker.Return(convertLiteralExpression(getDefaultValue(returnType)))
            treeMaker.Block(0, JavacList.of(returnStatement))
        }

        return treeMaker.MethodDef(modifiers, name, returnTypeExpr,
                                   typeParameters, receiverParameter, parameters, thrown, body, defaultValue)
    }

    private fun convertModifiers(
            access: Int,
            kind: ElementKind,
            visibleAnnotations: List<AnnotationNode>?,
            invisibleAnnotations: List<AnnotationNode>?
    ): JCModifiers {
        var annotations = visibleAnnotations?.fold(JavacList.nil<JCAnnotation>()) { list, anno ->
            convertAnnotation(anno)?.let { list.prepend(it) } ?: list
        } ?: JavacList.nil()
        annotations = invisibleAnnotations?.fold(annotations) { list, anno ->
            convertAnnotation(anno)?.let { list.prepend(it) } ?: list
        } ?: annotations

        val flags = when (kind) {
            ElementKind.CLASS -> access and CLASS_MODIFIERS
            ElementKind.METHOD -> access and METHOD_MODIFIERS
            ElementKind.FIELD -> access and FIELD_MODIFIERS
            ElementKind.PARAMETER -> access and Opcodes.ACC_FINAL
            else -> throw IllegalArgumentException("Invalid element kind: $kind")
        }
        return treeMaker.Modifiers(flags.toLong(), annotations)
    }

    private fun convertAnnotation(annotation: AnnotationNode): JCAnnotation? {
        val fqName = Type.getType(annotation.desc).className
        if (BLACKLISTED_ANNOTATATIONS.any { fqName.startsWith(it) }) return null

        val name = convertFqName(fqName)
        val values = mapPairedValues<JCExpression>(annotation.values) { key, value ->
            treeMaker.Assign(convertSimpleName(key), convertLiteralExpression(value))
        }
        return treeMaker.Annotation(name, values)
    }

    private fun convertLiteralExpression(value: Any?): JCExpression {
        return when (value) {
            null -> convertSimpleName("null")
            is Byte, is Boolean, is Char, is Short, is Int, is Long, is Float, is Double, is String -> treeMaker.Literal(value)

            is ByteArray -> treeMaker.NewArray(null, JavacList.nil(), mapValues(value.asIterable()) { convertLiteralExpression(it) })
            is BooleanArray -> treeMaker.NewArray(null, JavacList.nil(), mapValues(value.asIterable()) { convertLiteralExpression(it) })
            is CharArray -> treeMaker.NewArray(null, JavacList.nil(), mapValues(value.asIterable()) { convertLiteralExpression(it) })
            is ShortArray -> treeMaker.NewArray(null, JavacList.nil(), mapValues(value.asIterable()) { convertLiteralExpression(it) })
            is IntArray -> treeMaker.NewArray(null, JavacList.nil(), mapValues(value.asIterable()) { convertLiteralExpression(it) })
            is LongArray -> treeMaker.NewArray(null, JavacList.nil(), mapValues(value.asIterable()) { convertLiteralExpression(it) })
            is FloatArray -> treeMaker.NewArray(null, JavacList.nil(), mapValues(value.asIterable()) { convertLiteralExpression(it) })
            is DoubleArray -> treeMaker.NewArray(null, JavacList.nil(), mapValues(value.asIterable()) { convertLiteralExpression(it) })
            is Array<*> -> treeMaker.NewArray(null, JavacList.nil(), mapValues(value.asIterable()) { convertLiteralExpression(it) })
            is List<*> -> treeMaker.NewArray(null, JavacList.nil(), mapValues(value) { convertLiteralExpression(it) })

            is Type -> treeMaker.Select(convertFqName(value.className), name("class"))
            is AnnotationNode -> convertAnnotation(value) ?: error("Annotation is filtered out")
            else -> throw IllegalArgumentException("Illegal literal expression value: $value (${value.javaClass.canonicalName})")
        }
    }

    private fun getDefaultValue(type: Type): Any? = when (type) {
        Type.BYTE_TYPE -> 0.toByte()
        Type.BOOLEAN_TYPE -> false
        Type.CHAR_TYPE -> '\u0000'
        Type.SHORT_TYPE -> 0.toShort()
        Type.INT_TYPE -> 0
        Type.LONG_TYPE -> 0L
        Type.FLOAT_TYPE -> 0.0F
        Type.DOUBLE_TYPE -> 0.0
        else -> null
    }

    private fun convertFqName(internalOrFqName: String): JCExpression {
        val path = internalOrFqName.replace('/', '.').split('.')
        assert(path.isNotEmpty())
        if (path.size == 1) return convertSimpleName(path.single())

        var expr = treeMaker.Select(convertSimpleName(path[0]), name(path[1]))
        for (index in 2..path.lastIndex) {
            expr = treeMaker.Select(expr, name(path[index]))
        }
        return expr
    }

    private fun convertSimpleName(name: String): JCIdent = treeMaker.Ident(name(name))

    private fun name(name: String) = nameTable.fromString(name)
}

private class ParameterInfo(
        val access: Int,
        val name: String,
        val type: String,
        val visibleAnnotations: List<AnnotationNode>?,
        val invisibleAnnotations: List<AnnotationNode>?)

private fun MethodNode.getParametersInfo(): List<ParameterInfo> {
    val localVariables = this.localVariables ?: emptyList()
    val parameters = this.parameters ?: emptyList()
    val isStatic = (access and Opcodes.ACC_STATIC) > 0
    val parameterTypes = Type.getArgumentTypes(desc)
    return parameterTypes.mapIndexed { index, type ->
        val name = parameters.getOrNull(index)?.name ?: localVariables.getOrNull(index + (if (isStatic) 0 else 1))?.name ?: "p$index"
        val visibleAnnotations = visibleParameterAnnotations?.get(index)
        val invisibleAnnotations = invisibleParameterAnnotations?.get(index)
        ParameterInfo(0, name, type.className, visibleAnnotations, invisibleAnnotations)
    }
}

private inline fun <T, R> mapValues(values: Iterable<T>?, f: (T) -> R?): JavacList<R> {
    if (values == null) return JavacList.nil()

    var result = JavacList.nil<R>()
    for (item in values) {
        f(item)?.let { result = result.prepend(it) }
    }
    return result.reverse()
}

private inline fun <T> mapPairedValues(valuePairs: List<Any>?, f: (String, Any) -> T?): JavacList<T> {
    if (valuePairs == null || valuePairs.isEmpty()) return JavacList.nil()

    val size = valuePairs.size
    var result = JavacList.nil<T>()
    assert(size % 2 == 0)
    var index = 0
    while (index < size) {
        val key = valuePairs[index] as String
        val value = valuePairs[index + 1]
        f(key, value)?.let { result = result.prepend(it) }
        index += 2
    }
    return result.reverse()
}

private operator fun <T : Any> JavacList<T>.plus(other: JavacList<T>): JavacList<T> {
    return this.appendList(other)
}

private fun isSynthetic(access: Int) = (access and Opcodes.ACC_SYNTHETIC) > 0