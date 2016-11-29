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

package org.jetbrains.kotlin.kapt3.stubs

import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.code.TypeTag
import com.sun.tools.javac.file.JavacFileManager
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.JCTree.*
import com.sun.tools.javac.tree.TreeMaker
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.kapt3.*
import org.jetbrains.kotlin.kapt3.javac.KaptTreeMaker
import org.jetbrains.kotlin.kapt3.javac.KaptJavaFileObject
import org.jetbrains.kotlin.kapt3.util.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import javax.lang.model.element.ElementKind
import javax.tools.JavaFileManager
import com.sun.tools.javac.util.List as JavacList

class ClassFileToSourceStubConverter(
        val kaptContext: KaptContext,
        val typeMapper: KotlinTypeMapper,
        val generateNonExistentClass: Boolean
) {
    private companion object {
        private val VISIBILITY_MODIFIERS = (Opcodes.ACC_PUBLIC or Opcodes.ACC_PRIVATE or Opcodes.ACC_PROTECTED).toLong()
        private val MODALITY_MODIFIERS = (Opcodes.ACC_FINAL or Opcodes.ACC_ABSTRACT).toLong()

        private val CLASS_MODIFIERS = VISIBILITY_MODIFIERS or MODALITY_MODIFIERS or
                (Opcodes.ACC_DEPRECATED or Opcodes.ACC_INTERFACE or Opcodes.ACC_ANNOTATION or Opcodes.ACC_ENUM or Opcodes.ACC_STATIC).toLong()

        private val METHOD_MODIFIERS = VISIBILITY_MODIFIERS or MODALITY_MODIFIERS or
                (Opcodes.ACC_DEPRECATED or Opcodes.ACC_SYNCHRONIZED or Opcodes.ACC_NATIVE or Opcodes.ACC_STATIC or Opcodes.ACC_STRICT).toLong()

        private val FIELD_MODIFIERS = VISIBILITY_MODIFIERS or MODALITY_MODIFIERS or
                (Opcodes.ACC_VOLATILE or Opcodes.ACC_TRANSIENT or Opcodes.ACC_ENUM or Opcodes.ACC_STATIC).toLong()

        private val PARAMETER_MODIFIERS = FIELD_MODIFIERS or Flags.PARAMETER or Flags.VARARGS or Opcodes.ACC_FINAL.toLong()

        private val BLACKLISTED_ANNOTATIONS = listOf(
                "java.lang.Deprecated", "kotlin.Deprecated", // Deprecated annotations
                "java.lang.Synthetic",
                "synthetic.kotlin.jvm.GeneratedByJvmOverloads", // kapt3-related annotation for marking JvmOverloads-generated methods
                "java.lang.annotation.", // Java annotations
                "org.jetbrains.annotations.", // Nullable/NotNull, ReadOnly, Mutable
                "kotlin.jvm.", "kotlin.Metadata" // Kotlin annotations from runtime
        )
    }

    private val fileManager = kaptContext.context.get(JavaFileManager::class.java) as JavacFileManager
    private val treeMaker = TreeMaker.instance(kaptContext.context) as KaptTreeMaker
    private val signatureParser = SignatureParser(treeMaker)

    private var done = false

    fun convert(): JavacList<JCCompilationUnit> {
        if (done) error(ClassFileToSourceStubConverter::class.java.simpleName + " can convert classes only once")
        done = true

        var stubs = mapJList(kaptContext.compiledClasses) { convertTopLevelClass(it) }

        if (generateNonExistentClass) {
            stubs = stubs.append(generateNonExistentClass())
        }

        return stubs
    }

    private fun generateNonExistentClass(): JCCompilationUnit {
        val nonExistentClass = treeMaker.ClassDef(
                treeMaker.Modifiers((Flags.PUBLIC or Flags.FINAL).toLong()),
                treeMaker.name("NonExistentClass"),
                JavacList.nil(),
                null,
                JavacList.nil(),
                JavacList.nil())

        val topLevel = treeMaker.TopLevel(JavacList.nil(), treeMaker.SimpleName("error"), JavacList.of(nonExistentClass))
        topLevel.sourcefile = KaptJavaFileObject(topLevel, nonExistentClass, fileManager)
        return topLevel
    }

    private fun convertTopLevelClass(clazz: ClassNode): JCCompilationUnit? {
        val origin = kaptContext.origins[clazz] ?: return null
        val ktFile = origin.element?.containingFile as? KtFile ?: return null
        val descriptor = origin.descriptor as? DeclarationDescriptor ?: return null

        // Nested classes will be processed during the outer classes conversion
        if ((descriptor as? ClassDescriptor)?.isNested ?: false) return null

        val packageAnnotations = JavacList.nil<JCAnnotation>()
        val packageName = ktFile.packageFqName.asString()
        val packageClause = if (packageName.isEmpty()) null else treeMaker.FqName(packageName)

        val classDeclaration = convertClass(clazz, packageName, true) ?: return null

        val imports = JavacList.nil<JCTree>()
        val classes = JavacList.of<JCTree>(classDeclaration)

        val topLevel = treeMaker.TopLevel(packageAnnotations, packageClause, imports + classes)
        topLevel.sourcefile = KaptJavaFileObject(topLevel, classDeclaration, fileManager)
        return topLevel
    }

    /**
     * Returns false for the inner classes or if the origin for the class was not found.
     */
    private fun convertClass(clazz: ClassNode, packageFqName: String, isTopLevel: Boolean): JCClassDecl? {
        if (isSynthetic(clazz.access)) return null

        val descriptor = kaptContext.origins[clazz]?.descriptor as? DeclarationDescriptor ?: return null
        val isNested = (descriptor as? ClassDescriptor)?.isNested ?: false
        val isInner = isNested && (descriptor as? ClassDescriptor)?.isInner ?: false

        val modifiers = convertModifiers(
                if (!isInner && isNested) (clazz.access or Opcodes.ACC_STATIC) else clazz.access,
                ElementKind.CLASS, packageFqName, clazz.visibleAnnotations, clazz.invisibleAnnotations)

        val isEnum = clazz.isEnum()
        val isAnnotation = clazz.isAnnotation()

        val isDefaultImpls = clazz.name.endsWith("${descriptor.name.asString()}/DefaultImpls")
                             && isPublic(clazz.access) && isFinal(clazz.access)
                             && descriptor is ClassDescriptor
                             && descriptor.kind == ClassKind.INTERFACE

        // DefaultImpls without any contents don't have INNERCLASS'es inside it (and inside the parent interface)
        if (isDefaultImpls && (isTopLevel || (clazz.fields.isNullOrEmpty() && clazz.methods.isNullOrEmpty()))) {
            return null
        }

        val simpleName = when (descriptor) {
            is PackageFragmentDescriptor -> {
                val className = if (packageFqName.isEmpty()) clazz.name else clazz.name.drop(packageFqName.length + 1)
                if (className.isEmpty()) throw IllegalStateException("Invalid package facade class name: ${clazz.name}")
                className
            }
            else -> if (isDefaultImpls) "DefaultImpls" else descriptor.name.asString()
        }

        val interfaces = mapJList(clazz.interfaces) {
            if (isAnnotation && it == "java/lang/annotation/Annotation") return@mapJList null
            treeMaker.FqName(it)
        }

        val superClass = treeMaker.FqName(clazz.superName)

        val hasSuperClass = clazz.superName != "java/lang/Object" && !isEnum
        val genericType = signatureParser.parseClassSignature(clazz.signature, superClass, interfaces)

        val fields = mapJList<FieldNode, JCTree>(clazz.fields) { convertField(it, packageFqName) }
        val methods = mapJList<MethodNode, JCTree>(clazz.methods) {
            if (isEnum) {
                if (it.name == "values" && it.desc == "()[L${clazz.name};") return@mapJList null
                if (it.name == "valueOf" && it.desc == "(Ljava/lang/String;)L${clazz.name};") return@mapJList null
            }

            convertMethod(it, clazz, packageFqName)
        }
        val nestedClasses = mapJList<InnerClassNode, JCTree>(clazz.innerClasses) { innerClass ->
            if (innerClass.outerName != clazz.name) return@mapJList null
            val innerClassNode = kaptContext.compiledClasses.firstOrNull { it.name == innerClass.name } ?: return@mapJList null
            convertClass(innerClassNode, packageFqName, false)
        }

        return treeMaker.ClassDef(
                modifiers,
                treeMaker.name(simpleName),
                genericType.typeParameters,
                if (hasSuperClass) genericType.superClass else null,
                genericType.interfaces,
                fields + methods + nestedClasses)
    }

    private fun convertField(field: FieldNode, packageFqName: String): JCVariableDecl? {
        if (isSynthetic(field.access)) return null
        val descriptor = kaptContext.origins[field]?.descriptor

        val modifiers = convertModifiers(field.access, ElementKind.FIELD, packageFqName, field.visibleAnnotations, field.invisibleAnnotations)
        val name = treeMaker.name(field.name)
        val type = Type.getType(field.desc)

        // Enum type must be an identifier (Javac requirement)
        val typeExpression = if (isEnum(field.access))
            treeMaker.SimpleName(type.className.substringAfterLast('.'))
        else
            getNotAnonymousType(descriptor) { signatureParser.parseFieldSignature(field.signature, treeMaker.Type(type)) }

        val value = field.value

        val initializer = convertValueOfPrimitiveTypeOrString(value)
                          ?: if (isFinal(field.access)) convertLiteralExpression(getDefaultValue(type)) else null

        return treeMaker.VarDef(modifiers, name, typeExpression, initializer)
    }

    private fun convertMethod(method: MethodNode, containingClass: ClassNode, packageFqName: String): JCMethodDecl? {
        if (isSynthetic(method.access)) return null
        val descriptor = kaptContext.origins[method]?.descriptor as? CallableDescriptor ?: return null

        val isOverridden = descriptor.overriddenDescriptors.isNotEmpty()
        val visibleAnnotations = if (isOverridden) {
            (method.visibleAnnotations ?: emptyList()) + AnnotationNode(Type.getType(Override::class.java).descriptor)
        } else {
            method.visibleAnnotations
        }

        val isConstructor = method.name == "<init>"
        val name = treeMaker.name(method.name)

        val modifiers = convertModifiers(
                if (containingClass.isEnum() && isConstructor)
                    (method.access.toLong() and VISIBILITY_MODIFIERS.inv())
                else
                    method.access.toLong(),
                ElementKind.METHOD, packageFqName, visibleAnnotations, method.invisibleAnnotations)

        val asmReturnType = Type.getReturnType(method.desc)
        val jcReturnType = if (isConstructor) null else treeMaker.Type(asmReturnType)

        val parametersInfo = method.getParametersInfo(containingClass)
        @Suppress("NAME_SHADOWING")
        val parameters = mapJListIndexed(parametersInfo) { index, info ->
            val lastParameter = index == parametersInfo.lastIndex
            val isArrayType = info.type.sort == Type.ARRAY

            val varargs = if (lastParameter && isArrayType && method.isVarargs()) Flags.VARARGS else 0L
            val modifiers = convertModifiers(
                    info.flags or varargs or Flags.PARAMETER,
                    ElementKind.PARAMETER,
                    packageFqName,
                    info.visibleAnnotations,
                    info.invisibleAnnotations)

            val name = treeMaker.name(info.name)
            val type = treeMaker.Type(info.type)
            treeMaker.VarDef(modifiers, name, type, null)
        }

        val exceptionTypes = mapJList(method.exceptions) { treeMaker.FqName(it) }

        val genericSignature = signatureParser.parseMethodSignature(method.signature, parameters, exceptionTypes, jcReturnType)
        val returnType = getNotAnonymousType(descriptor) { genericSignature.returnType }

        val defaultValue = method.annotationDefault?.let { convertLiteralExpression(it) }

        val body = if (defaultValue != null) {
            null
        } else if (isAbstract(method.access)) {
            null
        } else if (isConstructor && containingClass.isEnum()) {
            treeMaker.Block(0, JavacList.nil())
        } else if (isConstructor) {
            // We already checked it in convertClass()
            val declaration = kaptContext.origins[containingClass]?.descriptor as ClassDescriptor
            val superClass = declaration.getSuperClassOrAny()
            val superClassConstructor = superClass.constructors.firstOrNull { it.visibility.isVisible(null, it, declaration) }

            val superClassConstructorCall = if (superClassConstructor != null) {
                val args = mapJList(superClassConstructor.valueParameters) { param ->
                    convertLiteralExpression(getDefaultValue(typeMapper.mapType(param.type)))
                }
                val call = treeMaker.Apply(JavacList.nil(), treeMaker.SimpleName("super"), args)
                JavacList.of<JCStatement>(treeMaker.Exec(call))
            } else {
                JavacList.nil<JCStatement>()
            }

            treeMaker.Block(0, superClassConstructorCall)
        } else if (asmReturnType == Type.VOID_TYPE) {
            treeMaker.Block(0, JavacList.nil())
        } else {
            val returnStatement = treeMaker.Return(convertLiteralExpression(getDefaultValue(asmReturnType)))
            treeMaker.Block(0, JavacList.of(returnStatement))
        }

        return treeMaker.MethodDef(
                modifiers, name, returnType, genericSignature.typeParameters,
                genericSignature.parameterTypes, genericSignature.exceptionTypes,
                body, defaultValue)
    }

    private inline fun <T : JCExpression?> getNotAnonymousType(descriptor: DeclarationDescriptor?, f: () -> T): T {
        if (descriptor is CallableDescriptor) {
            val returnTypeDescriptor = descriptor.returnType?.constructor?.declarationDescriptor
            if (returnTypeDescriptor is ClassDescriptor && DescriptorUtils.isAnonymousObject(returnTypeDescriptor)) {
                @Suppress("UNCHECKED_CAST")
                return getMostSuitableSuperTypeForAnonymousType(returnTypeDescriptor) as T
            }
        }

        return f()
    }

    private fun getMostSuitableSuperTypeForAnonymousType(typeDescriptor: ClassDescriptor): JCExpression {
        val superClass = typeDescriptor.getSuperClassNotAny()
        if (superClass != null) {
            return treeMaker.Type(typeMapper.mapType(superClass))
        } else {
            val sortedSuperTypes = typeDescriptor.typeConstructor.supertypes
                    .sortedBy { it.constructor.declarationDescriptor?.name?.asString() ?: "" }

            for (superType in sortedSuperTypes) {
                if (superType.isAnyOrNullableAny()) continue
                return treeMaker.Type(typeMapper.mapType(superType))
            }
        }

        return treeMaker.FqName("java.lang.Object")
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun convertModifiers(
            access: Int,
            kind: ElementKind,
            packageFqName: String,
            visibleAnnotations: List<AnnotationNode>?,
            invisibleAnnotations: List<AnnotationNode>?
    ): JCModifiers = convertModifiers(access.toLong(), kind, packageFqName, visibleAnnotations, invisibleAnnotations)

    private fun convertModifiers(
            access: Long,
            kind: ElementKind,
            packageFqName: String,
            visibleAnnotations: List<AnnotationNode>?,
            invisibleAnnotations: List<AnnotationNode>?
    ): JCModifiers {
        var annotations = visibleAnnotations?.fold(JavacList.nil<JCAnnotation>()) { list, anno ->
            convertAnnotation(anno, packageFqName)?.let { list.prepend(it) } ?: list
        } ?: JavacList.nil()
        annotations = invisibleAnnotations?.fold(annotations) { list, anno ->
            convertAnnotation(anno, packageFqName)?.let { list.prepend(it) } ?: list
        } ?: annotations

        val flags = when (kind) {
            ElementKind.CLASS -> access and CLASS_MODIFIERS
            ElementKind.METHOD -> access and METHOD_MODIFIERS
            ElementKind.FIELD -> access and FIELD_MODIFIERS
            ElementKind.PARAMETER -> access and PARAMETER_MODIFIERS
            else -> throw IllegalArgumentException("Invalid element kind: $kind")
        }
        return treeMaker.Modifiers(flags, annotations)
    }

    private fun convertAnnotation(annotation: AnnotationNode, packageFqName: String? = "", filtered: Boolean = true): JCAnnotation? {
        val annotationType = Type.getType(annotation.desc)
        val fqName = annotationType.className

        if (filtered) {
            if (BLACKLISTED_ANNOTATIONS.any { fqName.startsWith(it) }) return null
        }

        val useSimpleName = '.' in fqName && fqName.substringBeforeLast('.', "") == packageFqName
        val name = if (useSimpleName) treeMaker.FqName(fqName.substring(packageFqName!!.length + 1)) else treeMaker.Type(annotationType)
        val values = mapPairedValuesJList<JCExpression>(annotation.values) { key, value ->
            treeMaker.Assign(treeMaker.SimpleName(key), convertLiteralExpression(value))
        }
        return treeMaker.Annotation(name, values)
    }

    private fun convertValueOfPrimitiveTypeOrString(value: Any?): JCExpression? {
        return when (value) {
            is Char -> treeMaker.Literal(TypeTag.CHAR, value.toInt())
            is Byte -> treeMaker.TypeCast(treeMaker.TypeIdent(TypeTag.BYTE), treeMaker.Literal(TypeTag.INT, value.toInt()))
            is Short -> treeMaker.TypeCast(treeMaker.TypeIdent(TypeTag.SHORT), treeMaker.Literal(TypeTag.INT, value.toInt()))
            is Boolean, is Int, is Long, is Float, is Double, is String -> treeMaker.Literal(value)
            else -> null
        }
    }

    private fun convertLiteralExpression(value: Any?): JCExpression {
        convertValueOfPrimitiveTypeOrString(value)?.let { return it }

        return when (value) {
            null -> treeMaker.Literal(TypeTag.BOT, null)

            is ByteArray -> treeMaker.NewArray(null, JavacList.nil(), mapJList(value.asIterable()) { convertLiteralExpression(it) })
            is BooleanArray -> treeMaker.NewArray(null, JavacList.nil(), mapJList(value.asIterable()) { convertLiteralExpression(it) })
            is CharArray -> treeMaker.NewArray(null, JavacList.nil(), mapJList(value.asIterable()) { convertLiteralExpression(it) })
            is ShortArray -> treeMaker.NewArray(null, JavacList.nil(), mapJList(value.asIterable()) { convertLiteralExpression(it) })
            is IntArray -> treeMaker.NewArray(null, JavacList.nil(), mapJList(value.asIterable()) { convertLiteralExpression(it) })
            is LongArray -> treeMaker.NewArray(null, JavacList.nil(), mapJList(value.asIterable()) { convertLiteralExpression(it) })
            is FloatArray -> treeMaker.NewArray(null, JavacList.nil(), mapJList(value.asIterable()) { convertLiteralExpression(it) })
            is DoubleArray -> treeMaker.NewArray(null, JavacList.nil(), mapJList(value.asIterable()) { convertLiteralExpression(it) })
            is Array<*> -> { // Two-element String array for enumerations ([desc, fieldName])
                assert(value.size == 2)
                val enumType = Type.getType(value[0] as String)
                val valueName = value[1] as String
                treeMaker.Select(treeMaker.Type(enumType), treeMaker.name(valueName))
            }
            is List<*> -> treeMaker.NewArray(null, JavacList.nil(), mapJList(value) { convertLiteralExpression(it) })

            is Type -> treeMaker.Select(treeMaker.Type(value), treeMaker.name("class"))
            is AnnotationNode -> convertAnnotation(value, packageFqName = null, filtered = false)!!
            else -> throw IllegalArgumentException("Illegal literal expression value: $value (${value.javaClass.canonicalName})")
        }
    }

    private fun getDefaultValue(type: Type): Any? = when (type) {
        Type.BYTE_TYPE -> 0
        Type.BOOLEAN_TYPE -> false
        Type.CHAR_TYPE -> '\u0000'
        Type.SHORT_TYPE -> 0
        Type.INT_TYPE -> 0
        Type.LONG_TYPE -> 0L
        Type.FLOAT_TYPE -> 0.0F
        Type.DOUBLE_TYPE -> 0.0
        else -> null
    }
}

private val ClassDescriptor.isNested: Boolean
    get() = containingDeclaration is ClassDescriptor