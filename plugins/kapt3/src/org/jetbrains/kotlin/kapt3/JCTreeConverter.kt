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

import com.sun.tools.javac.code.TypeTag
import com.sun.tools.javac.file.JavacFileManager
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.JCTree.*
import com.sun.tools.javac.tree.TreeMaker
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Names
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import java.util.*
import javax.lang.model.element.ElementKind
import javax.tools.JavaFileManager
import com.sun.tools.javac.util.List as JavacList

class JCTreeConverter(
        context: Context,
        val typeMapper: KotlinTypeMapper,
        val classes: List<ClassNode>,
        val origins: Map<Any, JvmDeclarationOrigin>
) {
    private companion object {
        private val VISIBILITY_MODIFIERS = Opcodes.ACC_PUBLIC or Opcodes.ACC_PRIVATE or Opcodes.ACC_PROTECTED
        private val MODALITY_MODIFIERS = Opcodes.ACC_FINAL or Opcodes.ACC_ABSTRACT

        private val CLASS_MODIFIERS = VISIBILITY_MODIFIERS or MODALITY_MODIFIERS or
                Opcodes.ACC_DEPRECATED or Opcodes.ACC_INTERFACE or Opcodes.ACC_ANNOTATION or Opcodes.ACC_ENUM or Opcodes.ACC_STATIC

        private val METHOD_MODIFIERS = VISIBILITY_MODIFIERS or MODALITY_MODIFIERS or
                Opcodes.ACC_SYNCHRONIZED or Opcodes.ACC_STATIC or Opcodes.ACC_NATIVE or Opcodes.ACC_DEPRECATED

        private val FIELD_MODIFIERS = VISIBILITY_MODIFIERS or MODALITY_MODIFIERS or
                Opcodes.ACC_STATIC or Opcodes.ACC_VOLATILE or Opcodes.ACC_TRANSIENT or Opcodes.ACC_ENUM

        private val BLACKLISTED_ANNOTATATIONS = listOf(
                "java.lang.Deprecated", "kotlin.Deprecated", // Deprecated annotations
                "java.lang.Synthetic", //
                "java.lang.annotation.", // Java annotations
                "org.jetbrains.annotations.", // Nullable/NotNull, ReadOnly, Mutable
                "kotlin.jvm.", "kotlin.Metadata" // Kotlin annotations from runtime
        )
    }

    private val fileManager = context.get(JavaFileManager::class.java) as JavacFileManager
    private val treeMaker = TreeMaker.instance(context)
    private val nameTable = Names.instance(context).table

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
        if (descriptor.isNested) return null

        val classDeclaration = convertClass(clazz, true) ?: return null

        val packageAnnotations = JavacList.nil<JCAnnotation>()
        val packageName = ktFile.packageFqName.asString()
        val packageClause = if (packageName.isEmpty()) null else convertFqName(packageName)

        val imports = JavacList.nil<JCTree>()
        val classes = JavacList.of<JCTree>(classDeclaration)

        val topLevel = treeMaker.TopLevel(packageAnnotations, packageClause, imports + classes)
        val javaFileObject = SyntheticJavaFileObject(topLevel, classDeclaration, System.currentTimeMillis(), fileManager)
        topLevel.sourcefile = javaFileObject
        return topLevel
    }

    /**
     * Returns false for the inner classe or if the origin for the class was not found.
     */
    private fun convertClass(clazz: ClassNode, isTopLevel: Boolean): JCClassDecl? {
        if (isSynthetic(clazz.access)) return null

        val descriptor = origins[clazz]?.descriptor as? ClassDescriptor ?: return null
        val modifiers = convertModifiers(
                if (!descriptor.isInner && descriptor.isNested) (clazz.access or Opcodes.ACC_STATIC) else clazz.access,
                ElementKind.CLASS, clazz.visibleAnnotations, clazz.invisibleAnnotations)

        val isEnum = clazz.isEnum()
        val isAnnotation = clazz.isAnnotation()

        val isDefaultImpls = clazz.name.endsWith("${descriptor.name.asString()}/DefaultImpls")
                             && isPublic(clazz.access) && isFinal(clazz.access)
                             && descriptor.kind == ClassKind.INTERFACE

        // DefaultImpls without any contents don't have INNERCLASS'es inside it (and inside the parent interface)
        if (isDefaultImpls && (isTopLevel || (clazz.fields.isNullOrEmpty() && clazz.methods.isNullOrEmpty()))) {
            return null
        }

        val simpleName = name(if (isDefaultImpls) "DefaultImpls" else descriptor.name.asString())

        val typeParams = JavacList.nil<JCTypeParameter>()
        val extending = if (clazz.superName == "java/lang/Object" || isEnum) null else convertFqName(clazz.superName)

        val implementing = mapValues(clazz.interfaces) {
            if (isAnnotation && it == "java/lang/annotation/Annotation") return@mapValues null
            convertFqName(it)
        }

        val fields = mapValues<FieldNode, JCTree>(clazz.fields) { convertField(it) }
        val methods = mapValues<MethodNode, JCTree>(clazz.methods) {
            if (isEnum) {
                if (it.name == "values" && it.desc == "()[L${clazz.name};") return@mapValues null
                if (it.name == "valueOf" && it.desc == "(Ljava/lang/String;)L${clazz.name};") return@mapValues null
            }

            convertMethod(it, clazz)
        }
        val nestedClasses = mapValues<InnerClassNode, JCTree>(clazz.innerClasses) { innerClass ->
            if (innerClass.outerName != clazz.name) return@mapValues null
            val innerClassNode = classes.firstOrNull { it.name == innerClass.name } ?: return@mapValues null
            convertClass(innerClassNode, false)
        }

        return treeMaker.ClassDef(modifiers, simpleName, typeParams, extending, implementing, fields + methods + nestedClasses)
    }

    private fun convertField(field: FieldNode): JCVariableDecl? {
        if (isSynthetic(field.access)) return null

        val modifiers = convertModifiers(field.access, ElementKind.FIELD, field.visibleAnnotations, field.invisibleAnnotations)
        val name = name(field.name)
        val type = Type.getType(field.desc)

        // Enum type must be an identifier (Javac requirement)
        val typeExpression = if (isEnum(field.access)) {
            convertSimpleName(type.className.substringAfterLast('.'))
        } else {
            convertType(type)
        }

        val value = field.value

        val initializer = when (value) {
            is Char -> treeMaker.Literal(TypeTag.CHAR, value.toInt())
            is Byte, is Boolean, is Short, is Int, is Long, is Float, is Double, is String -> treeMaker.Literal(value)
            else -> if (isFinal(field.access)) convertLiteralExpression(getDefaultValue(type)) else null
        }

        return treeMaker.VarDef(modifiers, name, typeExpression, initializer)
    }

    private fun convertMethod(method: MethodNode, containingClass: ClassNode): JCMethodDecl? {
        if (isSynthetic(method.access)) return null
        val descriptor = origins[method]?.descriptor as? CallableDescriptor ?: return null

        val isOverridden = descriptor.overriddenDescriptors.isNotEmpty()
        val visibleAnnotations = if (isOverridden) {
            (method.visibleAnnotations ?: emptyList()) + AnnotationNode(Type.getType(Override::class.java).descriptor)
        } else {
            method.visibleAnnotations
        }

        val modifiers = convertModifiers(
                if (containingClass.isEnum()) (method.access and VISIBILITY_MODIFIERS.inv()) else method.access,
                ElementKind.METHOD, visibleAnnotations, method.invisibleAnnotations)

        val isConstructor = method.name == "<init>"
        val name = name(method.name)
        val typeParameters = JavacList.nil<JCTypeParameter>()
        val receiverParameter = null

        val returnType = Type.getReturnType(method.desc)
        val returnTypeExpr = if (isConstructor) null else convertType(returnType)

        val parametersInfo = method.getParametersInfo(containingClass)
        @Suppress("NAME_SHADOWING")
        val parameters = mapValues(parametersInfo) { info ->
            val modifiers = convertModifiers(info.access, ElementKind.PARAMETER, info.visibleAnnotations, info.invisibleAnnotations)
            val name = name(info.name)
            val type = convertType(info.type)
            treeMaker.VarDef(modifiers, name, type, null)
        }

        val thrown = mapValues(method.exceptions) { convertFqName(it) }

        val defaultValue = method.annotationDefault?.let { convertLiteralExpression(it) }

        val body = if (defaultValue != null) {
            null
        } else if (isAbstract(method.access)) {
            null
        } else if (isConstructor && containingClass.isEnum()) {
            treeMaker.Block(0, JavacList.nil())
        } else if (isConstructor) {
            // We already checked it in convertClass()
            val declaration = origins[containingClass]?.descriptor as ClassDescriptor
            val superClass = declaration.getSuperClassOrAny()
            val superClassConstructor = superClass.constructors.firstOrNull { it.visibility.isVisible(null, it, declaration) }

            val superClassConstructorCall = if (superClassConstructor != null) {
                val args = mapValues(superClassConstructor.valueParameters) { param ->
                    convertLiteralExpression(getDefaultValue(typeMapper.mapType(param.type)))
                }
                val call = treeMaker.Apply(JavacList.nil(), convertSimpleName("super"), args)
                JavacList.of<JCStatement>(treeMaker.Exec(call))
            } else {
                JavacList.nil<JCStatement>()
            }

            treeMaker.Block(0, superClassConstructorCall)
        } else if (returnType == Type.VOID_TYPE) {
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
        val annotationType = Type.getType(annotation.desc)
        val fqName = annotationType.className
        if (BLACKLISTED_ANNOTATATIONS.any { fqName.startsWith(it) }) return null

        val name = convertType(annotationType)
        val values = mapPairedValues<JCExpression>(annotation.values) { key, value ->
            treeMaker.Assign(convertSimpleName(key), convertLiteralExpression(value))
        }
        return treeMaker.Annotation(name, values)
    }

    private fun convertLiteralExpression(value: Any?): JCExpression {
        return when (value) {
            null -> treeMaker.Literal(TypeTag.BOT, null)
            is Char -> treeMaker.Literal(TypeTag.CHAR, value.toInt())
            is Byte, is Boolean, is Short, is Int, is Long, is Float, is Double, is String -> treeMaker.Literal(value)

            is ByteArray -> treeMaker.NewArray(null, JavacList.nil(), mapValues(value.asIterable()) { convertLiteralExpression(it) })
            is BooleanArray -> treeMaker.NewArray(null, JavacList.nil(), mapValues(value.asIterable()) { convertLiteralExpression(it) })
            is CharArray -> treeMaker.NewArray(null, JavacList.nil(), mapValues(value.asIterable()) { convertLiteralExpression(it) })
            is ShortArray -> treeMaker.NewArray(null, JavacList.nil(), mapValues(value.asIterable()) { convertLiteralExpression(it) })
            is IntArray -> treeMaker.NewArray(null, JavacList.nil(), mapValues(value.asIterable()) { convertLiteralExpression(it) })
            is LongArray -> treeMaker.NewArray(null, JavacList.nil(), mapValues(value.asIterable()) { convertLiteralExpression(it) })
            is FloatArray -> treeMaker.NewArray(null, JavacList.nil(), mapValues(value.asIterable()) { convertLiteralExpression(it) })
            is DoubleArray -> treeMaker.NewArray(null, JavacList.nil(), mapValues(value.asIterable()) { convertLiteralExpression(it) })
            is Array<*> -> { // Two-element String array for enumerations ([desc, fieldName])
                assert(value.size == 2)
                val enumType = Type.getType(value[0] as String)
                val valueName = value[1] as String
                treeMaker.Select(convertType(enumType), name(valueName))
            }
            is List<*> -> treeMaker.NewArray(null, JavacList.nil(), mapValues(value) { convertLiteralExpression(it) })

            is Type -> treeMaker.Select(convertType(value), name("class"))
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

    private fun convertType(type: Type): JCExpression {
        convertBuiltinType(type)?.let { return it }
        if (type.sort == Type.ARRAY) {
            return treeMaker.TypeArray(convertType(type.elementType))
        }
        return convertFqName(type.className)
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

    private fun convertBuiltinType(type: Type): JCExpression? {
        val typeTag = when (type) {
            Type.BYTE_TYPE -> TypeTag.BYTE
            Type.BOOLEAN_TYPE -> TypeTag.BOOLEAN
            Type.CHAR_TYPE -> TypeTag.CHAR
            Type.SHORT_TYPE -> TypeTag.SHORT
            Type.INT_TYPE -> TypeTag.INT
            Type.LONG_TYPE -> TypeTag.LONG
            Type.FLOAT_TYPE -> TypeTag.FLOAT
            Type.DOUBLE_TYPE -> TypeTag.DOUBLE
            Type.VOID_TYPE -> TypeTag.VOID
            else -> null
        } ?: return null
        return treeMaker.TypeIdent(typeTag)
    }

    private fun convertSimpleName(name: String): JCExpression = treeMaker.Ident(name(name))

    private fun name(name: String) = nameTable.fromString(name)
}

private class ParameterInfo(
        val access: Int,
        val name: String,
        val type: Type,
        val visibleAnnotations: List<AnnotationNode>?,
        val invisibleAnnotations: List<AnnotationNode>?)

private fun MethodNode.getParametersInfo(containingClass: ClassNode): List<ParameterInfo> {
    val localVariables = this.localVariables ?: emptyList()
    val parameters = this.parameters ?: emptyList()
    val isStatic = isStatic(access)

    // First and second parameters in enum constructors are synthetic, we should ignore them
    val isEnumConstructor = (name == "<init>") && containingClass.isEnum()
    val startParameterIndex = if (isEnumConstructor) 2 else 0

    val parameterTypes = Type.getArgumentTypes(desc)

    val parameterInfos = ArrayList<ParameterInfo>(parameterTypes.size - startParameterIndex)
    for (index in startParameterIndex..parameterTypes.lastIndex) {
        val type = parameterTypes[index]
        var name = parameters.getOrNull(index - startParameterIndex)?.name
                   ?: localVariables.getOrNull(index + (if (isStatic) 0 else 1))?.name
                   ?: "p${index - startParameterIndex}"

        // Property setters has bad parameter names
        if (name.startsWith("<") && name.endsWith(">")) {
            name = "p${index - startParameterIndex}"
        }

        val visibleAnnotations = visibleParameterAnnotations?.get(index)
        val invisibleAnnotations = invisibleParameterAnnotations?.get(index)
        parameterInfos += ParameterInfo(0, name, type, visibleAnnotations, invisibleAnnotations)
    }
    return parameterInfos
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

private val ClassDescriptor.isNested: Boolean
    get() = containingDeclaration is ClassDescriptor

private fun isEnum(access: Int) = (access and Opcodes.ACC_ENUM) > 0
private fun isPublic(access: Int) = (access and Opcodes.ACC_PUBLIC) > 0
private fun isSynthetic(access: Int) = (access and Opcodes.ACC_SYNTHETIC) > 0
private fun isFinal(access: Int) = (access and Opcodes.ACC_FINAL) > 0
private fun isStatic(access: Int) = (access and Opcodes.ACC_STATIC) > 0
private fun isAbstract(access: Int) = (access and Opcodes.ACC_ABSTRACT) > 0
private fun ClassNode.isEnum() = (access and Opcodes.ACC_ENUM) > 0
private fun ClassNode.isAnnotation() = (access and Opcodes.ACC_ANNOTATION) > 0

private fun <T> List<T>.isNullOrEmpty() = this == null || this.isEmpty()