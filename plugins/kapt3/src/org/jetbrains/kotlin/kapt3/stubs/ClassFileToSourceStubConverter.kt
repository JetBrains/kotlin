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
import com.sun.tools.javac.parser.Tokens
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.JCTree.*
import com.sun.tools.javac.tree.TreeMaker
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.kapt3.*
import org.jetbrains.kotlin.kapt3.javac.KaptTreeMaker
import org.jetbrains.kotlin.kapt3.javac.KaptJavaFileObject
import org.jetbrains.kotlin.kapt3.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import javax.lang.model.element.ElementKind
import javax.tools.JavaFileManager
import com.sun.tools.javac.util.List as JavacList

class ClassFileToSourceStubConverter(
        val kaptContext: KaptContext<GenerationState>,
        val generateNonExistentClass: Boolean,
        val correctErrorTypes: Boolean
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
                "kotlin.jvm." // Kotlin annotations from runtime
        )

        private val JAVA_KEYWORD_FILTER_REGEX = "[a-z]+".toRegex()

        private val JAVA_KEYWORDS = Tokens.TokenKind.values()
                .filter { JAVA_KEYWORD_FILTER_REGEX.matches(it.toString().orEmpty()) }
                .mapTo(hashSetOf(), Any::toString)
    }

    private val _bindings = mutableMapOf<String, KaptJavaFileObject>()

    val bindings: Map<String, KaptJavaFileObject>
        get() = _bindings

    private val fileManager = kaptContext.context.get(JavaFileManager::class.java) as JavacFileManager

    val treeMaker = TreeMaker.instance(kaptContext.context) as KaptTreeMaker

    private val signatureParser = SignatureParser(treeMaker)

    private val anonymousTypeHandler = AnonymousTypeHandler(this)

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

        // We basically don't need to add binding for NonExistentClass
        return topLevel
    }

    private fun convertTopLevelClass(clazz: ClassNode): JCCompilationUnit? {
        val origin = kaptContext.origins[clazz] ?: return null
        val ktFile = origin.element?.containingFile as? KtFile ?: return null
        val descriptor = origin.descriptor ?: return null

        // Nested classes will be processed during the outer classes conversion
        if ((descriptor as? ClassDescriptor)?.isNested ?: false) return null

        val packageAnnotations = JavacList.nil<JCAnnotation>()
        val packageName = ktFile.packageFqName.asString()
        val packageClause = if (packageName.isEmpty()) null else treeMaker.FqName(packageName)

        val classDeclaration = convertClass(clazz, packageName, true) ?: return null

        val imports = if (correctErrorTypes) convertImports(ktFile, classDeclaration) else JavacList.nil()
        val classes = JavacList.of<JCTree>(classDeclaration)

        val topLevel = treeMaker.TopLevel(packageAnnotations, packageClause, imports + classes)

        KaptJavaFileObject(topLevel, classDeclaration, fileManager).apply {
            topLevel.sourcefile = this
            _bindings[clazz.name] = this
        }

        return topLevel
    }

    private fun convertImports(file: KtFile, classDeclaration: JCClassDecl): JavacList<JCTree> {
        val imports = mutableListOf<JCTree>()

        for (importDirective in file.importDirectives) {
            // Qualified name should be valid Java fq-name
            val importedFqName = importDirective.importedFqName ?: continue
            if (!isValidQualifiedName(importedFqName)) continue

            val shortName = importedFqName.shortName()
            if (shortName.asString() == classDeclaration.simpleName.toString()) continue

            // If alias is specified, it also should be valid Java name
            val aliasName = importDirective.aliasName
            if (aliasName != null /*TODO support aliases */ /*&& getValidIdentifierName(aliasName) == null*/) continue

            val importedReference = getReferenceExpression(importDirective.importedReference)
                    ?.let { kaptContext.bindingContext[BindingContext.REFERENCE_TARGET, it] }

            if (importedReference is CallableDescriptor) continue

            val importedExpr = treeMaker.FqName(importedFqName.asString())

            imports += if (importDirective.isAllUnder) {
                treeMaker.Import(treeMaker.Select(importedExpr, treeMaker.nameTable.names.asterisk), false)
            } else {
                treeMaker.Import(importedExpr, false)
            }
        }

        return JavacList.from(imports)
    }

    private tailrec fun getReferenceExpression(expression: KtExpression?): KtReferenceExpression? = when (expression) {
        is KtReferenceExpression -> expression
        is KtQualifiedExpression -> getReferenceExpression(expression.selectorExpression)
        else -> null
    }

    /**
     * Returns false for the inner classes or if the origin for the class was not found.
     */
    private fun convertClass(clazz: ClassNode, packageFqName: String, isTopLevel: Boolean): JCClassDecl? {
        if (isSynthetic(clazz.access)) return null
        if (checkIfShouldBeIgnored(Type.getObjectType(clazz.name))) return null

        val descriptor = kaptContext.origins[clazz]?.descriptor ?: return null
        val isNested = (descriptor as? ClassDescriptor)?.isNested ?: false
        val isInner = isNested && (descriptor as? ClassDescriptor)?.isInner ?: false

        val flags = getClassAccessFlags(clazz, descriptor, isInner, isNested)

        val isEnum = clazz.isEnum()
        val isAnnotation = clazz.isAnnotation()

        val modifiers = convertModifiers(flags,
                if (isEnum) ElementKind.ENUM else ElementKind.CLASS,
                packageFqName, clazz.visibleAnnotations, clazz.invisibleAnnotations)

        val isDefaultImpls = clazz.name.endsWith("${descriptor.name.asString()}\$DefaultImpls")
                             && isPublic(clazz.access) && isFinal(clazz.access)
                             && descriptor is ClassDescriptor
                             && descriptor.kind == ClassKind.INTERFACE

        // DefaultImpls without any contents don't have INNERCLASS'es inside it (and inside the parent interface)
        if (isDefaultImpls && (isTopLevel || (clazz.fields.isNullOrEmpty() && clazz.methods.isNullOrEmpty()))) {
            return null
        }

        val simpleName = getValidIdentifierName(getClassName(clazz, descriptor, isDefaultImpls, packageFqName)) ?: return null

        val interfaces = mapJList(clazz.interfaces) {
            if (isAnnotation && it == "java/lang/annotation/Annotation") return@mapJList null
            treeMaker.FqName(treeMaker.getQualifiedName(it))
        }

        val superClass = treeMaker.FqName(treeMaker.getQualifiedName(clazz.superName))

        val hasSuperClass = clazz.superName != "java/lang/Object" && !isEnum
        val genericType = signatureParser.parseClassSignature(clazz.signature, superClass, interfaces)

        class EnumValueData(val field: FieldNode, val innerClass: InnerClassNode?, val correspondingClass: ClassNode?)

        val enumValuesData = clazz.fields.filter { it.isEnumValue() }.map { field ->
            var foundInnerClass: InnerClassNode? = null
            var correspondingClass: ClassNode? = null

            for (innerClass in clazz.innerClasses) {
                // Class should have the same name as enum value
                if (innerClass.innerName != field.name) continue
                val classNode = kaptContext.compiledClasses.firstOrNull { it.name == innerClass.name } ?: continue

                // Super class name of the class should be our enum class
                if (classNode.superName != clazz.name) continue

                correspondingClass = classNode
                foundInnerClass = innerClass
                break
            }

            EnumValueData(field, foundInnerClass, correspondingClass)
        }

        val enumValues: JavacList<JCTree> = mapJList(enumValuesData) { data ->
            val constructorArguments = Type.getArgumentTypes(clazz.methods.firstOrNull {
                it.name == "<init>" && Type.getArgumentsAndReturnSizes(it.desc).shr(2) >= 2
            }?.desc ?: "()Z")

            val args = mapJList(constructorArguments.drop(2)) { convertLiteralExpression(getDefaultValue(it)) }
            
            val def = data.correspondingClass?.let { convertClass(it, packageFqName, false) }

            convertField(data.field, packageFqName, treeMaker.NewClass(
                    /* enclosing = */ null,
                    /* typeArgs = */ JavacList.nil(),
                    /* clazz = */ treeMaker.Ident(treeMaker.name(data.field.name)),
                    /* args = */ args,
                    /* def = */ def))
        }

        val fields = mapJList<FieldNode, JCTree>(clazz.fields) {
            if (it.isEnumValue()) null else convertField(it, packageFqName)
        }

        val methods = mapJList<MethodNode, JCTree>(clazz.methods) {
            if (isEnum) {
                if (it.name == "values" && it.desc == "()[L${clazz.name};") return@mapJList null
                if (it.name == "valueOf" && it.desc == "(Ljava/lang/String;)L${clazz.name};") return@mapJList null
            }

            convertMethod(it, clazz, packageFqName)
        }

        val nestedClasses = mapJList<InnerClassNode, JCTree>(clazz.innerClasses) { innerClass ->
            if (enumValuesData.any { it.innerClass == innerClass }) return@mapJList null
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
                enumValues + fields + methods + nestedClasses)
    }

    private tailrec fun checkIfShouldBeIgnored(type: Type): Boolean {
        if (type.sort == Type.ARRAY) {
            return checkIfShouldBeIgnored(type.elementType)
        }

        if (type.sort != Type.OBJECT) return false

        val internalName = type.internalName
        // Ignore type names with Java keywords in it
        if (internalName.split('/', '.').any { it in JAVA_KEYWORDS }) {
            return true
        }

        val clazz = kaptContext.compiledClasses.firstOrNull { it.name == internalName } ?: return false
        return checkIfInnerClassNameConflictsWithOuter(clazz)
    }

    private fun findContainingClassNode(clazz: ClassNode): ClassNode? {
        val innerClassForOuter = clazz.innerClasses.firstOrNull { it.name == clazz.name } ?: return null
        return kaptContext.compiledClasses.firstOrNull { it.name == innerClassForOuter.outerName }
    }

    // Java forbids outer and inner class names to be the same. Check if the names are different
    private tailrec fun checkIfInnerClassNameConflictsWithOuter(
            clazz: ClassNode,
            outerClass: ClassNode? = findContainingClassNode(clazz)
    ): Boolean {
        if (outerClass == null) return false
        if (treeMaker.getSimpleName(clazz) == treeMaker.getSimpleName(outerClass)) return true
        // Try to find the containing class for outerClassNode (to check the whole tree recursively)
        val containingClassForOuterClass = findContainingClassNode(outerClass) ?: return false
        return checkIfInnerClassNameConflictsWithOuter(clazz, containingClassForOuterClass)
    }

    private fun getClassAccessFlags(clazz: ClassNode, descriptor: DeclarationDescriptor, isInner: Boolean, isNested: Boolean) = when {
        (descriptor.containingDeclaration as? ClassDescriptor)?.kind == ClassKind.INTERFACE -> {
            // Classes inside interfaces should always be public and static.
            // See com.sun.tools.javac.comp.Enter.visitClassDef for more information.
            (clazz.access or Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC) and
                    Opcodes.ACC_PRIVATE.inv() and Opcodes.ACC_PROTECTED.inv() // Remove private and protected modifiers
        }
        !isInner && isNested -> clazz.access or Opcodes.ACC_STATIC
        else -> clazz.access
    }

    private fun getClassName(clazz: ClassNode, descriptor: DeclarationDescriptor, isDefaultImpls: Boolean, packageFqName: String): String {
        return when (descriptor) {
            is PackageFragmentDescriptor -> {
                val className = if (packageFqName.isEmpty()) clazz.name else clazz.name.drop(packageFqName.length + 1)
                if (className.isEmpty()) throw IllegalStateException("Invalid package facade class name: ${clazz.name}")
                className
            }
            else -> if (isDefaultImpls) "DefaultImpls" else descriptor.name.asString()
        }
    }

    private fun convertField(
            field: FieldNode,
            packageFqName: String,
            explicitInitializer: JCExpression? = null
    ): JCVariableDecl? {
        if (isSynthetic(field.access)) return null
        // not needed anymore
        val origin = kaptContext.origins[field]
        val descriptor = origin?.descriptor

        val modifiers = convertModifiers(field.access, ElementKind.FIELD, packageFqName, field.visibleAnnotations, field.invisibleAnnotations)
        val name = getValidIdentifierName(field.name) ?: return null
        val type = Type.getType(field.desc)

        if (checkIfShouldBeIgnored(type)) {
            return null
        }

        // Enum type must be an identifier (Javac requirement)
        val typeExpression = if (isEnum(field.access))
            treeMaker.SimpleName(treeMaker.getQualifiedName(type).substringAfterLast('.'))
        else
            anonymousTypeHandler.getNonAnonymousType(descriptor) {
                getNonErrorType((descriptor as? CallableDescriptor)?.returnType,
                                ktTypeProvider = { (kaptContext.origins[field]?.element as? KtVariableDeclaration)?.typeReference },
                                ifNonError = { signatureParser.parseFieldSignature(field.signature, treeMaker.Type(type)) })
            }

        val value = field.value

        val initializer = explicitInitializer
                          ?: convertValueOfPrimitiveTypeOrString(value)
                          ?: if (isFinal(field.access)) convertLiteralExpression(getDefaultValue(type)) else null

        return treeMaker.VarDef(modifiers, treeMaker.name(name), typeExpression, initializer)
    }

    private fun convertMethod(method: MethodNode, containingClass: ClassNode, packageFqName: String): JCMethodDecl? {
        val descriptor = kaptContext.origins[method]?.descriptor as? CallableDescriptor ?: return null

        val isAnnotationHolderForProperty = descriptor is PropertyDescriptor && isSynthetic(method.access)
                                            && isStatic(method.access) && method.name.endsWith("\$annotations")

        if (isSynthetic(method.access) && !isAnnotationHolderForProperty) return null

        val isOverridden = descriptor.overriddenDescriptors.isNotEmpty()
        val visibleAnnotations = if (isOverridden) {
            (method.visibleAnnotations ?: emptyList()) + AnnotationNode(Type.getType(Override::class.java).descriptor)
        } else {
            method.visibleAnnotations
        }

        val isConstructor = method.name == "<init>"
        val name = getValidIdentifierName(method.name, canBeConstructor = true) ?: return null

        val modifiers = convertModifiers(
                if (containingClass.isEnum() && isConstructor)
                    (method.access.toLong() and VISIBILITY_MODIFIERS.inv())
                else
                    method.access.toLong(),
                ElementKind.METHOD, packageFqName, visibleAnnotations, method.invisibleAnnotations)

        val asmReturnType = Type.getReturnType(method.desc)
        val jcReturnType = if (isConstructor) null else treeMaker.Type(asmReturnType)

        val parametersInfo = method.getParametersInfo(containingClass)

        if (checkIfShouldBeIgnored(asmReturnType) || parametersInfo.any { checkIfShouldBeIgnored(it.type) }) {
            return null
        }

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

            val name = treeMaker.name(getValidIdentifierName(info.name) ?: "p${index}_" + info.name.hashCode().ushr(1))
            val type = treeMaker.Type(info.type)
            treeMaker.VarDef(modifiers, name, type, null)
        }

        val exceptionTypes = mapJList(method.exceptions) { treeMaker.FqName(it) }

        val valueParametersFromDescriptor = descriptor.valueParameters
        val (genericSignature, returnType) = extractMethodSignatureTypes(
                descriptor, exceptionTypes, jcReturnType, method, parameters, valueParametersFromDescriptor)

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
                    convertLiteralExpression(getDefaultValue(kaptContext.generationState.typeMapper.mapType(param.type)))
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
                modifiers, treeMaker.name(name), returnType, genericSignature.typeParameters,
                genericSignature.parameterTypes, genericSignature.exceptionTypes,
                body, defaultValue)
    }

    private fun extractMethodSignatureTypes(
            descriptor: CallableDescriptor,
            exceptionTypes: JavacList<JCExpression>,
            jcReturnType: JCExpression?, method: MethodNode,
            parameters: JavacList<JCVariableDecl>,
            valueParametersFromDescriptor: List<ValueParameterDescriptor>
    ): Pair<SignatureParser.MethodGenericSignature, JCExpression?> {
        val genericSignature = signatureParser.parseMethodSignature(
                method.signature, parameters, exceptionTypes, jcReturnType,
                nonErrorParameterTypeProvider = { index, lazyType ->
                    if (descriptor is PropertySetterDescriptor && valueParametersFromDescriptor.size == 1 && index == 0) {
                        getNonErrorType(descriptor.correspondingProperty.returnType,
                                        ktTypeProvider = { (kaptContext.origins[method]?.element as? KtVariableDeclaration)?.typeReference },
                                        ifNonError = { lazyType() })
                    }
                    else if (descriptor is FunctionDescriptor && valueParametersFromDescriptor.size == parameters.size) {
                        getNonErrorType(valueParametersFromDescriptor[index].type,
                                        ktTypeProvider = {
                                            (kaptContext.origins[method]?.element as? KtFunction)?.valueParameters?.get(index)?.typeReference
                                        },
                                        ifNonError = { lazyType() })
                    }
                    else {
                        lazyType()
                    }
                })

        val returnType = anonymousTypeHandler.getNonAnonymousType(descriptor) {
            getNonErrorType(descriptor.returnType,
                            ktTypeProvider = {
                                val element = kaptContext.origins[method]?.element
                                when (element) {
                                    is KtFunction -> element.typeReference
                                    is KtProperty -> if (method.name.startsWith("get")) element.typeReference else null
                                    else -> null
                                }
                            },
                            ifNonError = { genericSignature.returnType })
        }

        return Pair(genericSignature, returnType)
    }

    private inline fun <T : JCExpression?> getNonErrorType(
            type: KotlinType?,
            ktTypeProvider: () -> KtTypeReference?,
            ifNonError: () -> T
    ): T {
        if (!correctErrorTypes) {
            return ifNonError()
        }

        if (type?.containsErrorTypes() ?: false) {
            val ktType = ktTypeProvider()
            if (ktType != null) {
                @Suppress("UNCHECKED_CAST")
                return convertKtType(ktType, this) as T
            }
        }

        return ifNonError()
    }

    private fun isValidQualifiedName(name: FqName): Boolean {
        return name.pathSegments().all { getValidIdentifierName(it.asString(), false) != null }
    }

    fun getValidIdentifierName(name: String, canBeConstructor: Boolean = false): String? {
        if (canBeConstructor && name == "<init>") {
            return name
        }

        if (name in JAVA_KEYWORDS) return null

        if (name.isEmpty()
            || !Character.isJavaIdentifierStart(name[0])
            || name.drop(1).any { !Character.isJavaIdentifierPart(it) }
        ) {
            return null
        }

        return name
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
            ElementKind.ENUM -> access and CLASS_MODIFIERS and Opcodes.ACC_ABSTRACT.inv().toLong()
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
        val fqName = treeMaker.getQualifiedName(annotationType)

        if (filtered) {
            if (BLACKLISTED_ANNOTATIONS.any { fqName.startsWith(it) }) return null
        }

        val useSimpleName = '.' in fqName && fqName.substringBeforeLast('.', "") == packageFqName
        val name = if (useSimpleName) treeMaker.FqName(fqName.substring(packageFqName!!.length + 1)) else treeMaker.Type(annotationType)
        val values = mapPairedValuesJList<JCExpression>(annotation.values) { key, value ->
            val name = getValidIdentifierName(key) ?: return@mapPairedValuesJList null
            treeMaker.Assign(treeMaker.SimpleName(name), convertLiteralExpression(value))
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
                val valueName = getValidIdentifierName(value[1] as String) ?: "InvalidFieldName"
                treeMaker.Select(treeMaker.Type(enumType), treeMaker.name(valueName))
            }
            is List<*> -> treeMaker.NewArray(null, JavacList.nil(), mapJList(value) { convertLiteralExpression(it) })

            is Type -> treeMaker.Select(treeMaker.Type(value), treeMaker.name("class"))
            is AnnotationNode -> convertAnnotation(value, packageFqName = null, filtered = false)!!
            else -> throw IllegalArgumentException("Illegal literal expression value: $value (${value::class.java.canonicalName})")
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