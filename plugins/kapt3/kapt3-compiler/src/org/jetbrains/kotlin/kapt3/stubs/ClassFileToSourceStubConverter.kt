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
import com.sun.tools.javac.parser.Tokens
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.JCTree.*
import com.sun.tools.javac.tree.TreeMaker
import com.sun.tools.javac.tree.TreeScanner
import kotlinx.kapt.KaptIgnored
import org.jetbrains.kotlin.base.kapt3.KaptFlag
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.coroutines.CONTINUATION_PARAMETER_NAME
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.kapt3.KaptContextForStubGeneration
import org.jetbrains.kotlin.kapt3.base.*
import org.jetbrains.kotlin.kapt3.base.javac.kaptError
import org.jetbrains.kotlin.kapt3.base.javac.reportKaptError
import org.jetbrains.kotlin.kapt3.base.stubs.KaptStubLineInformation
import org.jetbrains.kotlin.kapt3.base.stubs.KotlinPosition
import org.jetbrains.kotlin.kapt3.base.util.TopLevelJava9Aware
import org.jetbrains.kotlin.kapt3.javac.KaptJavaFileObject
import org.jetbrains.kotlin.kapt3.javac.KaptTreeMaker
import org.jetbrains.kotlin.kapt3.stubs.ErrorTypeCorrector.TypeKind.*
import org.jetbrains.kotlin.kapt3.util.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.sources.JavaSourceElement
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isOneSegmentFQN
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.ArrayFqNames
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.descriptorUtil.isCompanionObject
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.isEnum
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import java.io.File
import javax.lang.model.element.ElementKind
import kotlin.math.sign
import com.sun.tools.javac.util.List as JavacList

class ClassFileToSourceStubConverter(val kaptContext: KaptContextForStubGeneration, val generateNonExistentClass: Boolean) {
    private companion object {
        private const val VISIBILITY_MODIFIERS = (Opcodes.ACC_PUBLIC or Opcodes.ACC_PRIVATE or Opcodes.ACC_PROTECTED).toLong()
        private const val MODALITY_MODIFIERS = (Opcodes.ACC_FINAL or Opcodes.ACC_ABSTRACT).toLong()

        private const val CLASS_MODIFIERS = VISIBILITY_MODIFIERS or MODALITY_MODIFIERS or
                (Opcodes.ACC_DEPRECATED or Opcodes.ACC_INTERFACE or Opcodes.ACC_ANNOTATION or Opcodes.ACC_ENUM or Opcodes.ACC_STATIC).toLong()

        private const val METHOD_MODIFIERS = VISIBILITY_MODIFIERS or MODALITY_MODIFIERS or
                (Opcodes.ACC_DEPRECATED or Opcodes.ACC_SYNCHRONIZED or Opcodes.ACC_NATIVE or Opcodes.ACC_STATIC or Opcodes.ACC_STRICT).toLong()

        private const val FIELD_MODIFIERS = VISIBILITY_MODIFIERS or MODALITY_MODIFIERS or
                (Opcodes.ACC_VOLATILE or Opcodes.ACC_TRANSIENT or Opcodes.ACC_ENUM or Opcodes.ACC_STATIC).toLong()

        private const val PARAMETER_MODIFIERS = FIELD_MODIFIERS or Flags.PARAMETER or Flags.VARARGS or Opcodes.ACC_FINAL.toLong()

        private val BLACKLISTED_ANNOTATIONS = listOf(
            "java.lang.Deprecated", "kotlin.Deprecated", // Deprecated annotations
            "java.lang.Synthetic",
            "synthetic.kotlin.jvm.GeneratedByJvmOverloads" // kapt3-related annotation for marking JvmOverloads-generated methods
        )

        private val KOTLIN_METADATA_ANNOTATION = Metadata::class.java.name

        private val NON_EXISTENT_CLASS_NAME = FqName("error.NonExistentClass")

        private val JAVA_KEYWORD_FILTER_REGEX = "[a-z]+".toRegex()

        @Suppress("UselessCallOnNotNull") // nullable toString(), KT-27724
        private val JAVA_KEYWORDS = Tokens.TokenKind.values()
            .filter { JAVA_KEYWORD_FILTER_REGEX.matches(it.toString().orEmpty()) }
            .mapTo(hashSetOf(), Any::toString)

        private val KOTLIN_PACKAGE = FqName("kotlin")

        private val ARRAY_OF_FUNCTIONS = (ArrayFqNames.PRIMITIVE_TYPE_TO_ARRAY.values + ArrayFqNames.ARRAY_OF_FUNCTION).toSet()
    }

    private val correctErrorTypes = kaptContext.options[KaptFlag.CORRECT_ERROR_TYPES]
    private val strictMode = kaptContext.options[KaptFlag.STRICT]
    private val stripMetadata = kaptContext.options[KaptFlag.STRIP_METADATA]
    private val keepKdocComments = kaptContext.options[KaptFlag.KEEP_KDOC_COMMENTS_IN_STUBS]

    private val mutableBindings = mutableMapOf<String, KaptJavaFileObject>()

    private val isIrBackend = kaptContext.generationState.isIrBackend

    val bindings: Map<String, KaptJavaFileObject>
        get() = mutableBindings

    private val typeMapper = KaptTypeMapper

    val treeMaker = TreeMaker.instance(kaptContext.context) as KaptTreeMaker

    private val signatureParser = SignatureParser(treeMaker)

    private val kdocCommentKeeper = if (keepKdocComments) KDocCommentKeeper(kaptContext) else null

    private val importsFromRoot by lazy(::collectImportsFromRootPackage)

    private val compiledClassByName = kaptContext.compiledClasses.associateBy { it.name!! }

    private var done = false

    fun convert(): List<KaptStub> {
        if (done) error(ClassFileToSourceStubConverter::class.java.simpleName + " can convert classes only once")
        done = true

        val stubs = kaptContext.compiledClasses.mapNotNullTo(mutableListOf()) { convertTopLevelClass(it) }

        if (generateNonExistentClass) {
            stubs += KaptStub(generateNonExistentClass())
        }

        return stubs
    }

    private fun generateNonExistentClass(): JCCompilationUnit {
        val nonExistentClass = treeMaker.ClassDef(
            treeMaker.Modifiers((Flags.PUBLIC or Flags.FINAL).toLong()),
            treeMaker.name(NON_EXISTENT_CLASS_NAME.shortName().asString()),
            JavacList.nil(),
            null,
            JavacList.nil(),
            JavacList.nil()
        )

        val topLevel = treeMaker.TopLevelJava9Aware(treeMaker.FqName(NON_EXISTENT_CLASS_NAME.parent()), JavacList.of(nonExistentClass))

        topLevel.sourcefile = KaptJavaFileObject(topLevel, nonExistentClass)

        // We basically don't need to add binding for NonExistentClass
        return topLevel
    }

    class KaptStub(val file: JCCompilationUnit, private val kaptMetadata: ByteArray? = null) {
        fun writeMetadataIfNeeded(forSource: File) {
            if (kaptMetadata == null) {
                return
            }

            val metadataFile = File(
                forSource.parentFile,
                forSource.nameWithoutExtension + KaptStubLineInformation.KAPT_METADATA_EXTENSION
            )

            metadataFile.writeBytes(kaptMetadata)
        }
    }

    private fun convertTopLevelClass(clazz: ClassNode): KaptStub? {
        val origin = kaptContext.origins[clazz] ?: return null
        val ktFile = origin.element?.containingFile as? KtFile ?: return null
        val descriptor = origin.descriptor ?: return null

        // Nested classes will be processed during the outer classes conversion
        if ((descriptor as? ClassDescriptor)?.isNested == true) return null

        val lineMappings = KaptLineMappingCollector(kaptContext)

        val packageName = ktFile.packageFqName.asString()
        val packageClause = if (packageName.isEmpty()) null else treeMaker.FqName(packageName)

        val classDeclaration = convertClass(clazz, lineMappings, packageName, true) ?: return null

        classDeclaration.mods.annotations = classDeclaration.mods.annotations

        val imports = if (correctErrorTypes) convertImports(ktFile, classDeclaration) else JavacList.nil()

        val nonEmptyImports: JavacList<JCTree> = when {
            imports.size > 0 -> imports
            else -> JavacList.of(treeMaker.Import(treeMaker.FqName("java.lang.System"), false))
        }

        val classes = JavacList.of<JCTree>(classDeclaration)

        val topLevel = treeMaker.TopLevelJava9Aware(packageClause, nonEmptyImports + classes)
        if (kdocCommentKeeper != null) {
            topLevel.docComments = kdocCommentKeeper.getDocTable(topLevel)
        }

        KaptJavaFileObject(topLevel, classDeclaration).apply {
            topLevel.sourcefile = this
            mutableBindings[clazz.name] = this
        }

        postProcess(topLevel)

        return KaptStub(topLevel, lineMappings.serialize())
    }

    private fun postProcess(topLevel: JCCompilationUnit) {
        topLevel.accept(object : TreeScanner() {
            override fun visitClassDef(clazz: JCClassDecl) {
                // Delete enums inside enum values
                if (clazz.isEnum()) {
                    for (child in clazz.defs) {
                        if (child is JCVariableDecl) {
                            deleteAllEnumsInside(child)
                        }
                    }
                }

                super.visitClassDef(clazz)
            }

            private fun JCClassDecl.isEnum() = mods.flags and Opcodes.ACC_ENUM.toLong() != 0L

            private fun deleteAllEnumsInside(def: JCTree) {
                def.accept(object : TreeScanner() {
                    override fun visitClassDef(clazz: JCClassDecl) {
                        clazz.defs = mapJList(clazz.defs) { child ->
                            if (child is JCClassDecl && child.isEnum()) null else child
                        }

                        super.visitClassDef(clazz)
                    }
                })
            }
        })
    }

    private fun convertImports(file: KtFile, classDeclaration: JCClassDecl): JavacList<JCTree> {
        val imports = mutableListOf<JCImport>()
        val importedShortNames = mutableSetOf<String>()

        // We prefer ordinary imports over aliased ones.
        val sortedImportDirectives = file.importDirectives.partition { it.aliasName == null }.run { first + second }

        loop@ for (importDirective in sortedImportDirectives) {
            // Qualified name should be valid Java fq-name
            val importedFqName = importDirective.importedFqName?.takeIf { it.pathSegments().size > 1 } ?: continue
            if (!isValidQualifiedName(importedFqName)) continue

            val shortName = importedFqName.shortName()
            if (shortName.asString() == classDeclaration.simpleName.toString()) continue

            val importedReference = /* resolveImportReference */ run {
                val referenceExpression = getReferenceExpression(importDirective.importedReference) ?: return@run null

                val bindingContext = kaptContext.bindingContext
                bindingContext[BindingContext.REFERENCE_TARGET, referenceExpression]?.let { return@run it }

                val allTargets = bindingContext[BindingContext.AMBIGUOUS_REFERENCE_TARGET, referenceExpression] ?: return@run null
                allTargets.find { it is CallableDescriptor }?.let { return@run it }

                return@run allTargets.firstOrNull()
            }

            val isCallableImport = importedReference is CallableDescriptor
            val isEnumEntry = (importedReference as? ClassDescriptor)?.kind == ClassKind.ENUM_ENTRY
            val isAllUnderClassifierImport = importDirective.isAllUnder && importedReference is ClassifierDescriptor

            if (isCallableImport || isEnumEntry || isAllUnderClassifierImport) {
                continue@loop
            }

            val importedExpr = treeMaker.FqName(importedFqName.asString())

            imports += if (importDirective.isAllUnder) {
                treeMaker.Import(treeMaker.Select(importedExpr, treeMaker.nameTable.names.asterisk), false)
            } else {
                if (!importedShortNames.add(importedFqName.shortName().asString())) {
                    continue
                }

                treeMaker.Import(importedExpr, false)
            }
        }

        return JavacList.from(imports)
    }

    /**
     * Returns false for the inner classes or if the origin for the class was not found.
     */
    private fun convertClass(
        clazz: ClassNode,
        lineMappings: KaptLineMappingCollector,
        packageFqName: String,
        isTopLevel: Boolean
    ): JCClassDecl? {
        if (isSynthetic(clazz.access)) return null
        if (!checkIfValidTypeName(clazz, Type.getObjectType(clazz.name))) return null

        val descriptor = kaptContext.origins[clazz]?.descriptor ?: return null
        val isNested = (descriptor as? ClassDescriptor)?.isNested ?: false
        val isInner = isNested && (descriptor as? ClassDescriptor)?.isInner ?: false

        val flags = getClassAccessFlags(clazz, descriptor, isInner, isNested)

        val isEnum = clazz.isEnum()
        val isAnnotation = clazz.isAnnotation()

        val modifiers = convertModifiers(
            clazz,
            flags,
            if (isEnum) ElementKind.ENUM else ElementKind.CLASS,
            packageFqName, clazz.visibleAnnotations, clazz.invisibleAnnotations, descriptor.annotations
        )

        val isDefaultImpls = clazz.name.endsWith("${descriptor.name.asString()}\$DefaultImpls")
                && isPublic(clazz.access) && isFinal(clazz.access)
                && descriptor is ClassDescriptor
                && descriptor.kind == ClassKind.INTERFACE

        // DefaultImpls without any contents don't have INNERCLASS'es inside it (and inside the parent interface)
        if (isDefaultImpls && (isTopLevel || (clazz.fields.isNullOrEmpty() && clazz.methods.isNullOrEmpty()))) {
            return null
        }

        val simpleName = getClassName(clazz, descriptor, isDefaultImpls, packageFqName)
        if (!isValidIdentifier(simpleName)) return null

        val interfaces = mapJList(clazz.interfaces) {
            if (isAnnotation && it == "java/lang/annotation/Annotation") return@mapJList null
            treeMaker.FqName(treeMaker.getQualifiedName(it))
        }

        val superClass = treeMaker.FqName(treeMaker.getQualifiedName(clazz.superName))

        val genericType = signatureParser.parseClassSignature(clazz.signature, superClass, interfaces)

        class EnumValueData(val field: FieldNode, val innerClass: InnerClassNode?, val correspondingClass: ClassNode?)

        val enumValuesData = clazz.fields.filter { it.isEnumValue() }.map { field ->
            var foundInnerClass: InnerClassNode? = null
            var correspondingClass: ClassNode? = null

            for (innerClass in clazz.innerClasses) {
                // Class should have the same name as enum value
                if (innerClass.innerName != field.name) continue
                val classNode = compiledClassByName[innerClass.name] ?: continue

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

            val args = mapJList(constructorArguments.drop(2)) { convertLiteralExpression(clazz, getDefaultValue(it)) }

            val def = data.correspondingClass?.let { convertClass(it, lineMappings, packageFqName, false) }

            convertField(
                data.field, clazz, lineMappings, packageFqName, treeMaker.NewClass(
                    /* enclosing = */ null,
                    /* typeArgs = */ JavacList.nil(),
                    /* clazz = */ treeMaker.Ident(treeMaker.name(data.field.name)),
                    /* args = */ args,
                    /* def = */ def
                )
            )
        }

        val fieldsPositions = mutableMapOf<JCTree, MemberData>()
        val fields = mapJList<FieldNode, JCTree>(clazz.fields) { fieldNode ->
            if (fieldNode.isEnumValue()) {
                null
            } else {
                convertField(fieldNode, clazz, lineMappings, packageFqName)?.also {
                    fieldsPositions[it] = MemberData(fieldNode.name, fieldNode.desc, lineMappings.getPosition(clazz, fieldNode))
                }
            }
        }

        val methodsPositions = mutableMapOf<JCTree, MemberData>()
        val methods = mapJList<MethodNode, JCTree>(clazz.methods) { methodNode ->
            if (isEnum) {
                if (methodNode.name == "values" && methodNode.desc == "()[L${clazz.name};") return@mapJList null
                if (methodNode.name == "valueOf" && methodNode.desc == "(Ljava/lang/String;)L${clazz.name};") return@mapJList null
            }

            convertMethod(methodNode, clazz, lineMappings, packageFqName, isInner)?.also {
                methodsPositions[it] = MemberData(methodNode.name, methodNode.desc, lineMappings.getPosition(clazz, methodNode))
            }
        }

        val nestedClasses = mapJList<InnerClassNode, JCTree>(clazz.innerClasses) { innerClass ->
            if (enumValuesData.any { it.innerClass == innerClass }) return@mapJList null
            if (innerClass.outerName != clazz.name) return@mapJList null
            val innerClassNode = compiledClassByName[innerClass.name] ?: return@mapJList null
            convertClass(innerClassNode, lineMappings, packageFqName, false)
        }

        lineMappings.registerClass(clazz)

        val superTypes = calculateSuperTypes(clazz, genericType)

        val classPosition = lineMappings.getPosition(clazz)
        val sortedFields = JavacList.from(fields.sortedWith(MembersPositionComparator(classPosition, fieldsPositions)))
        val sortedMethods = JavacList.from(methods.sortedWith(MembersPositionComparator(classPosition, methodsPositions)))

        return treeMaker.ClassDef(
            modifiers,
            treeMaker.name(simpleName),
            genericType.typeParameters,
            superTypes.superClass,
            superTypes.interfaces,
            enumValues + sortedFields + sortedMethods + nestedClasses
        ).keepKdocCommentsIfNecessary(clazz)
    }

    private class MemberData(val name: String, val descriptor: String, val position: KotlinPosition?)

    /**
     * Sort class members. If the source file for the class is unknown, just sort using name and descriptor. Otherwise:
     * - all members in the same source file as the class come first (members may come from other source files)
     * - members from the class are sorted using their position in the source file
     * - members from other source files are sorted using their name and descriptor
     *
     * More details: Class methods and fields are currently sorted at serialization (see DescriptorSerializer.sort) and at deserialization
     * (see DeserializedMemberScope.OptimizedImplementation#addMembers). Therefore, the contents of the generated stub files are sorted in
     * incremental builds but not in clean builds.
     * The consequence is that the contents of the generated stub files may not be consistent across a clean build and an incremental
     * build, making the build non-deterministic and dependent tasks run unnecessarily (see KT-40882).
     */
    private class MembersPositionComparator(val classSource: KotlinPosition?, val memberData: Map<JCTree, MemberData>) :
        Comparator<JCTree> {
        override fun compare(o1: JCTree, o2: JCTree): Int {
            val data1 = memberData.getValue(o1)
            val data2 = memberData.getValue(o2)
            classSource ?: return compareDescriptors(data1, data2)

            val position1 = data1.position
            val position2 = data2.position

            return if (position1 != null && position1.path == classSource.path) {
                if (position2 != null && position2.path == classSource.path) {
                    val positionCompare = position1.pos.compareTo(position2.pos)
                    if (positionCompare != 0) positionCompare
                    else compareDescriptors(data1, data2)
                } else {
                    -1
                }
            } else if (position2 != null && position2.path == classSource.path) {
                1
            } else {
                compareDescriptors(data1, data2)
            }
        }

        private fun compareDescriptors(m1: MemberData, m2: MemberData): Int {
            val nameComparison = m1.name.compareTo(m2.name)
            if (nameComparison != 0) return nameComparison
            return m1.descriptor.compareTo(m2.descriptor)
        }
    }

    private class ClassSupertypes(val superClass: JCExpression?, val interfaces: JavacList<JCExpression>)

    private fun calculateSuperTypes(clazz: ClassNode, genericType: SignatureParser.ClassGenericSignature): ClassSupertypes {
        val hasSuperClass = clazz.superName != "java/lang/Object" && !clazz.isEnum()

        val defaultSuperTypes = ClassSupertypes(
            if (hasSuperClass) genericType.superClass else null,
            genericType.interfaces
        )

        if (!correctErrorTypes) {
            return defaultSuperTypes
        }

        val declaration = kaptContext.origins[clazz]?.element as? KtClassOrObject ?: return defaultSuperTypes
        val declarationDescriptor = kaptContext.bindingContext[BindingContext.CLASS, declaration] ?: return defaultSuperTypes

        if (typeMapper.mapType(declarationDescriptor.defaultType) != Type.getObjectType(clazz.name)) {
            return defaultSuperTypes
        }

        val (superClass, superInterfaces) = partitionSuperTypes(declaration) ?: return defaultSuperTypes

        val sameSuperClassCount = (superClass == null) == (defaultSuperTypes.superClass == null)
        val sameSuperInterfaceCount = superInterfaces.size == defaultSuperTypes.interfaces.size

        if (sameSuperClassCount && sameSuperInterfaceCount) {
            return defaultSuperTypes
        }

        class SuperTypeCalculationFailure : RuntimeException()

        fun nonErrorType(ref: () -> KtTypeReference?): JCExpression {
            assert(correctErrorTypes)

            return getNonErrorType<JCExpression>(
                ErrorUtils.createErrorType(ErrorTypeKind.ERROR_SUPER_TYPE),
                ErrorTypeCorrector.TypeKind.SUPER_TYPE,
                ref
            ) { throw SuperTypeCalculationFailure() }
        }

        return try {
            ClassSupertypes(
                superClass?.let { nonErrorType { it } },
                mapJList(superInterfaces) { nonErrorType { it } }
            )
        } catch (e: SuperTypeCalculationFailure) {
            defaultSuperTypes
        }
    }

    private fun partitionSuperTypes(declaration: KtClassOrObject): Pair<KtTypeReference?, List<KtTypeReference>>? {
        val superTypeEntries = declaration.superTypeListEntries
            .takeIf { it.isNotEmpty() }
            ?: return Pair(null, emptyList())

        val classEntries = mutableListOf<KtSuperTypeListEntry>()
        val interfaceEntries = mutableListOf<KtSuperTypeListEntry>()
        val otherEntries = mutableListOf<KtSuperTypeListEntry>()

        for (entry in superTypeEntries) {
            val type = kaptContext.bindingContext[BindingContext.TYPE, entry.typeReference]
            val classDescriptor = type?.constructor?.declarationDescriptor as? ClassDescriptor

            if (type != null && !type.isError && classDescriptor != null) {
                val container = if (classDescriptor.kind == ClassKind.INTERFACE) interfaceEntries else classEntries
                container += entry
                continue
            }

            if (entry is KtSuperTypeCallEntry) {
                classEntries += entry
                continue
            }

            otherEntries += entry
        }

        for (entry in otherEntries) {
            if (classEntries.isEmpty()) {
                if (declaration is KtClass && !declaration.isInterface() && declaration.hasOnlySecondaryConstructors()) {
                    classEntries += entry
                    continue
                }
            }

            interfaceEntries += entry
        }

        if (classEntries.size > 1) {
            // Error in user code, several entries were resolved to classes
            return null
        }

        return Pair(classEntries.firstOrNull()?.typeReference, interfaceEntries.mapNotNull { it.typeReference })
    }

    private fun KtClass.hasOnlySecondaryConstructors(): Boolean {
        return primaryConstructor == null && secondaryConstructors.isNotEmpty()
    }

    private tailrec fun checkIfValidTypeName(containingClass: ClassNode, type: Type): Boolean {
        if (type.sort == Type.ARRAY) {
            return checkIfValidTypeName(containingClass, type.elementType)
        }

        if (type.sort != Type.OBJECT) return true

        val internalName = type.internalName
        // Ignore type names with Java keywords in it
        if (internalName.split('/', '.').any { it in JAVA_KEYWORDS }) {
            if (strictMode) {
                kaptContext.reportKaptError(
                    "Can't generate a stub for '${containingClass.className}'.",
                    "Type name '${type.className}' contains a Java keyword."
                )
            }

            return false
        }

        val clazz = compiledClassByName[internalName] ?: return true

        if (doesInnerClassNameConflictWithOuter(clazz)) {
            if (strictMode) {
                kaptContext.reportKaptError(
                    "Can't generate a stub for '${containingClass.className}'.",
                    "Its name '${clazz.simpleName}' is the same as one of the outer class names.",
                    "Java forbids it. Please change one of the class names."
                )
            }

            return false
        }

        reportIfIllegalTypeUsage(containingClass, type)

        return true
    }

    private fun findContainingClassNode(clazz: ClassNode): ClassNode? {
        val innerClassForOuter = clazz.innerClasses.firstOrNull { it.name == clazz.name } ?: return null
        return compiledClassByName[innerClassForOuter.outerName]
    }

    // Java forbids outer and inner class names to be the same. Check if the names are different
    private tailrec fun doesInnerClassNameConflictWithOuter(
        clazz: ClassNode,
        outerClass: ClassNode? = findContainingClassNode(clazz)
    ): Boolean {
        if (outerClass == null) return false
        if (treeMaker.getSimpleName(clazz) == treeMaker.getSimpleName(outerClass)) return true
        // Try to find the containing class for outerClassNode (to check the whole tree recursively)
        val containingClassForOuterClass = findContainingClassNode(outerClass) ?: return false
        return doesInnerClassNameConflictWithOuter(clazz, containingClassForOuterClass)
    }

    private fun getClassAccessFlags(clazz: ClassNode, descriptor: DeclarationDescriptor, isInner: Boolean, isNested: Boolean): Int {
        if ((descriptor.containingDeclaration as? ClassDescriptor)?.kind == ClassKind.INTERFACE) {
            // Classes inside interfaces should always be public and static.
            // See com.sun.tools.javac.comp.Enter.visitClassDef for more information.
            return (clazz.access or Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC) and
                    Opcodes.ACC_PRIVATE.inv() and Opcodes.ACC_PROTECTED.inv() // Remove private and protected modifiers
        }
        var access = clazz.access
        if ((descriptor as? ClassDescriptor)?.kind == ClassKind.ENUM_CLASS) {
            // Enums are final in the bytecode, but "final enum" is not allowed in Java.
            access = access and Opcodes.ACC_FINAL.inv()
        }
        if (!isInner && isNested) {
            access = access or Opcodes.ACC_STATIC
        }
        return access
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
        containingClass: ClassNode,
        lineMappings: KaptLineMappingCollector,
        packageFqName: String,
        explicitInitializer: JCExpression? = null
    ): JCVariableDecl? {
        if (isSynthetic(field.access) || isIgnored(field.invisibleAnnotations)) return null
        // not needed anymore
        val origin = kaptContext.origins[field]
        val descriptor = origin?.descriptor

        val fieldAnnotations = when {
            !isIrBackend && descriptor is PropertyDescriptor -> descriptor.backingField?.annotations
            else -> descriptor?.annotations
        } ?: Annotations.EMPTY

        val modifiers = convertModifiers(
            containingClass,
            field.access, ElementKind.FIELD, packageFqName,
            field.visibleAnnotations, field.invisibleAnnotations, fieldAnnotations
        )

        val name = field.name
        if (!isValidIdentifier(name)) return null

        val type = getFieldType(field, origin)

        if (!checkIfValidTypeName(containingClass, type)) {
            return null
        }

        fun typeFromAsm() = signatureParser.parseFieldSignature(field.signature, treeMaker.Type(type))

        // Enum type must be an identifier (Javac requirement)
        val typeExpression = if (isEnum(field.access)) {
            treeMaker.SimpleName(treeMaker.getQualifiedName(type).substringAfterLast('.'))
        } else if (descriptor is PropertyDescriptor && descriptor.isDelegated) {
            getNonErrorType(
                (origin.element as? KtProperty)?.delegateExpression?.getType(kaptContext.bindingContext),
                RETURN_TYPE,
                ktTypeProvider = { null },
                ifNonError = ::typeFromAsm
            )
        } else {
            getNonErrorType(
                (descriptor as? CallableDescriptor)?.returnType,
                RETURN_TYPE,
                ktTypeProvider = {
                    val fieldOrigin = (kaptContext.origins[field]?.element as? KtCallableDeclaration)
                        ?.takeIf { it !is KtFunction }

                    fieldOrigin?.typeReference
                },
                ifNonError = ::typeFromAsm
            )
        }

        lineMappings.registerField(containingClass, field)

        val initializer = explicitInitializer ?: convertPropertyInitializer(containingClass, field)
        return treeMaker.VarDef(modifiers, treeMaker.name(name), typeExpression, initializer).keepKdocCommentsIfNecessary(field)
    }

    private fun convertPropertyInitializer(containingClass: ClassNode, field: FieldNode): JCExpression? {
        val value = field.value

        val origin = kaptContext.origins[field]

        val propertyInitializer = when (val declaration = origin?.element) {
            is KtProperty -> declaration.initializer
            is KtParameter -> if (kaptContext.options[KaptFlag.DUMP_DEFAULT_PARAMETER_VALUES]) declaration.defaultValue else null
            else -> null
        }

        if (value != null) {
            if (propertyInitializer != null) {
                return convertConstantValueArguments(containingClass, value, listOf(propertyInitializer))
            }

            return convertValueOfPrimitiveTypeOrString(value)
        }

        val propertyType = (origin?.descriptor as? PropertyDescriptor)?.returnType

        /*
            Work-around for enum classes in companions.
            In expressions "Foo.Companion.EnumClass", Java prefers static field over a type name, making the reference invalid.
        */
        if (propertyType != null && propertyType.isEnum()) {
            val enumClass = propertyType.constructor.declarationDescriptor
            if (enumClass is ClassDescriptor && enumClass.isInsideCompanionObject()) {
                return null
            }
        }

        if (propertyInitializer != null && propertyType != null) {
            val constValue = getConstantValue(propertyInitializer, propertyType)
            if (constValue != null) {
                val asmValue = mapConstantValueToAsmRepresentation(constValue)
                if (asmValue !== UnknownConstantValue) {
                    return convertConstantValueArguments(containingClass, asmValue, listOf(propertyInitializer))
                }
            }
        }

        if (isFinal(field.access)) {
            val type = Type.getType(field.desc)
            return convertLiteralExpression(containingClass, getDefaultValue(type))
        }

        return null
    }

    private fun DeclarationDescriptor.isInsideCompanionObject(): Boolean {
        val parent = containingDeclaration ?: return false
        if (parent.isCompanionObject()) {
            return true
        }

        return parent.isInsideCompanionObject()
    }

    private object UnknownConstantValue

    private fun getConstantValue(expression: KtExpression, expectedType: KotlinType): ConstantValue<*>? {
        val moduleDescriptor = kaptContext.generationState.module
        val languageVersionSettings = kaptContext.generationState.languageVersionSettings
        val evaluator = ConstantExpressionEvaluator(moduleDescriptor, languageVersionSettings, kaptContext.project)
        val trace = DelegatingBindingTrace(kaptContext.bindingContext, "Kapt")
        val const = evaluator.evaluateExpression(expression, trace, expectedType)
        if (const == null || const.isError || !const.canBeUsedInAnnotations || const.usesNonConstValAsConstant) {
            return null
        }
        return const.toConstantValue(expectedType)
    }

    private fun mapConstantValueToAsmRepresentation(value: ConstantValue<*>): Any? {
        return when (value) {
            is ByteValue -> value.value
            is CharValue -> value.value
            is IntValue -> value.value
            is LongValue -> value.value
            is ShortValue -> value.value
            is UByteValue -> value.value
            is UShortValue -> value.value
            is UIntValue -> value.value
            is ULongValue -> value.value
            is AnnotationValue -> {
                val annotationDescriptor = value.value
                val annotationNode = AnnotationNode(typeMapper.mapType(annotationDescriptor.type).descriptor)
                val values = ArrayList<Any?>(annotationDescriptor.allValueArguments.size * 2)
                for ((name, arg) in annotationDescriptor.allValueArguments) {
                    val mapped = mapConstantValueToAsmRepresentation(arg)
                    if (mapped === UnknownConstantValue) {
                        return UnknownConstantValue
                    }

                    values += name.asString()
                    values += mapped
                }
                annotationNode.values = values
                return annotationNode
            }
            is ArrayValue -> {
                val children = value.value
                val result = ArrayList<Any?>(children.size)
                for (child in children) {
                    val mapped = mapConstantValueToAsmRepresentation(child)
                    if (mapped === UnknownConstantValue) {
                        return UnknownConstantValue
                    }
                    result += mapped
                }
                return result
            }
            is BooleanValue -> value.value
            is DoubleValue -> value.value
            is EnumValue -> {
                val (classId, name) = value.value
                val enumType = AsmUtil.asmTypeByClassId(classId)
                return arrayOf(enumType.descriptor, name.asString())
            }
            is FloatValue -> value.value
            is StringValue -> value.value
            is NullValue -> null
            else -> {
                // KClassValue is intentionally omitted as incompatible with Java
                UnknownConstantValue
            }
        }
    }

    private fun convertMethod(
        method: MethodNode,
        containingClass: ClassNode,
        lineMappings: KaptLineMappingCollector,
        packageFqName: String,
        isInner: Boolean
    ): JCMethodDecl? {
        if (isIgnored(method.invisibleAnnotations)) return null
        val descriptor = kaptContext.origins[method]?.descriptor as? CallableDescriptor ?: return null

        val isAnnotationHolderForProperty =
            isSynthetic(method.access) && isStatic(method.access) && method.name.endsWith(JvmAbi.ANNOTATED_PROPERTY_METHOD_NAME_SUFFIX)

        if (isSynthetic(method.access) && !isAnnotationHolderForProperty) return null

        val isOverridden = descriptor.overriddenDescriptors.isNotEmpty()
        val visibleAnnotations = if (isOverridden) {
            (method.visibleAnnotations ?: emptyList()) + AnnotationNode(Type.getType(Override::class.java).descriptor)
        } else {
            method.visibleAnnotations
        }

        val isConstructor = method.name == "<init>"

        val name = method.name
        if (!isValidIdentifier(name, canBeConstructor = isConstructor)) return null

        val modifiers = convertModifiers(
            containingClass,
            if (containingClass.isEnum() && isConstructor)
                (method.access.toLong() and VISIBILITY_MODIFIERS.inv())
            else
                method.access.toLong(),
            ElementKind.METHOD, packageFqName, visibleAnnotations, method.invisibleAnnotations, descriptor.annotations
        )

        if (containingClass.isInterface() && !method.isAbstract() && !method.isStatic()) {
            modifiers.flags = modifiers.flags or Flags.DEFAULT
        }

        val asmReturnType = Type.getReturnType(method.desc)
        val jcReturnType = if (isConstructor) null else treeMaker.Type(asmReturnType)

        val parametersInfo = method.getParametersInfo(containingClass, isInner, descriptor)

        if (!checkIfValidTypeName(containingClass, asmReturnType)
            || parametersInfo.any { !checkIfValidTypeName(containingClass, it.type) }
        ) {
            return null
        }

        @Suppress("NAME_SHADOWING")
        val parameters = mapJListIndexed(parametersInfo) { index, info ->
            val lastParameter = index == parametersInfo.lastIndex
            val isArrayType = info.type.sort == Type.ARRAY

            val varargs = if (lastParameter && isArrayType && method.isVarargs()) Flags.VARARGS else 0L
            val modifiers = convertModifiers(
                containingClass,
                info.flags or varargs or Flags.PARAMETER,
                ElementKind.PARAMETER,
                packageFqName,
                info.visibleAnnotations,
                info.invisibleAnnotations,
                Annotations.EMPTY /* TODO */
            )

            val name = info.name.takeIf { isValidIdentifier(it) } ?: ("p" + index + "_" + info.name.hashCode().ushr(1))
            val type = treeMaker.Type(info.type)
            treeMaker.VarDef(modifiers, treeMaker.name(name), type, null)
        }

        val exceptionTypes = mapJList(method.exceptions) { treeMaker.FqName(it) }

        val valueParametersFromDescriptor = descriptor.valueParameters
        val (genericSignature, returnType) =
            extractMethodSignatureTypes(descriptor, exceptionTypes, jcReturnType, method, parameters, valueParametersFromDescriptor)

        val defaultValue = method.annotationDefault?.let { convertLiteralExpression(containingClass, it) }

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
            val superClassConstructor = superClass.constructors.firstOrNull {
                it.visibility.isVisible(null, it, declaration, useSpecialRulesForPrivateSealedConstructors = true)
            }

            val superClassConstructorCall = if (superClassConstructor != null) {
                val args = mapJList(superClassConstructor.valueParameters) { param ->
                    convertLiteralExpression(containingClass, getDefaultValue(typeMapper.mapType(param.type)))
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
            val returnStatement = treeMaker.Return(convertLiteralExpression(containingClass, getDefaultValue(asmReturnType)))
            treeMaker.Block(0, JavacList.of(returnStatement))
        }

        lineMappings.registerMethod(containingClass, method)

        return treeMaker.MethodDef(
            modifiers, treeMaker.name(name), returnType, genericSignature.typeParameters,
            genericSignature.parameterTypes, genericSignature.exceptionTypes,
            body, defaultValue
        ).keepSignature(lineMappings, method).keepKdocCommentsIfNecessary(method)
    }

    private fun isIgnored(annotations: List<AnnotationNode>?): Boolean {
        val kaptIgnoredAnnotationFqName = KaptIgnored::class.java.name
        return annotations?.any { Type.getType(it.desc).className == kaptIgnoredAnnotationFqName } ?: false
    }

    private fun extractMethodSignatureTypes(
        descriptor: CallableDescriptor,
        exceptionTypes: JavacList<JCExpression>,
        jcReturnType: JCExpression?,
        method: MethodNode,
        parameters: JavacList<JCVariableDecl>,
        valueParametersFromDescriptor: List<ValueParameterDescriptor>
    ): Pair<SignatureParser.MethodGenericSignature, JCExpression?> {
        val genericSignature = signatureParser.parseMethodSignature(
            method.signature, parameters, exceptionTypes, jcReturnType,
            nonErrorParameterTypeProvider = { index, lazyType ->
                if (descriptor is PropertySetterDescriptor && valueParametersFromDescriptor.size == 1 && index == 0) {
                    getNonErrorType(descriptor.correspondingProperty.returnType, METHOD_PARAMETER_TYPE,
                                    ktTypeProvider = {
                                        val setterOrigin = (kaptContext.origins[method]?.element as? KtCallableDeclaration)
                                            ?.takeIf { it !is KtFunction }

                                        setterOrigin?.typeReference
                                    },
                                    ifNonError = { lazyType() })
                } else if (descriptor is FunctionDescriptor && valueParametersFromDescriptor.size == parameters.size) {
                    val parameterDescriptor = valueParametersFromDescriptor[index]
                    val sourceElement = kaptContext.origins[method]?.element as? KtFunction

                    getNonErrorType(
                        parameterDescriptor.type, METHOD_PARAMETER_TYPE,
                        ktTypeProvider = {
                            if (sourceElement == null) return@getNonErrorType null

                            if (sourceElement.hasDeclaredReturnType() && isContinuationParameter(parameterDescriptor)) {
                                val continuationTypeFqName = StandardNames.CONTINUATION_INTERFACE_FQ_NAME
                                val functionReturnType = sourceElement.typeReference!!.text
                                KtPsiFactory(kaptContext.project).createType("$continuationTypeFqName<$functionReturnType>")
                            } else {
                                sourceElement.valueParameters.getOrNull(index)?.typeReference
                            }
                        },
                        ifNonError = { lazyType() })
                } else {
                    lazyType()
                }
            })

        val returnType = getNonErrorType(
            descriptor.returnType, RETURN_TYPE,
            ktTypeProvider = {
                when (val element = kaptContext.origins[method]?.element) {
                    is KtFunction -> element.typeReference
                    is KtProperty -> if (descriptor is PropertyGetterDescriptor) element.typeReference else null
                    is KtPropertyAccessor -> if (descriptor is PropertyGetterDescriptor) element.property.typeReference else null
                    is KtParameter -> if (descriptor is PropertyGetterDescriptor) element.typeReference else null
                    else -> null
                }
            },
            ifNonError = { genericSignature.returnType }
        )

        return Pair(genericSignature, returnType)
    }

    private fun isContinuationParameter(descriptor: ValueParameterDescriptor): Boolean {
        val containingCallable = descriptor.containingDeclaration

        return containingCallable.valueParameters.lastOrNull() == descriptor
                && descriptor.name == CONTINUATION_PARAMETER_NAME
                && descriptor.source == SourceElement.NO_SOURCE
                && descriptor.type.constructor.declarationDescriptor?.fqNameSafe == StandardNames.CONTINUATION_INTERFACE_FQ_NAME
    }

    private fun <T : JCExpression?> getNonErrorType(
        type: KotlinType?,
        kind: ErrorTypeCorrector.TypeKind,
        ktTypeProvider: () -> KtTypeReference?,
        ifNonError: () -> T
    ): T {
        if (!correctErrorTypes) {
            return ifNonError()
        }

        if (type?.containsErrorTypes() == true) {
            val typeFromSource = ktTypeProvider()?.typeElement
            val ktFile = typeFromSource?.containingKtFile
            if (ktFile != null) {
                @Suppress("UNCHECKED_CAST")
                return ErrorTypeCorrector(this, kind, ktFile).convert(typeFromSource, emptyMap()) as T
            }
        }

        val nonErrorType = ifNonError()

        if (nonErrorType is JCFieldAccess) {
            val qualifier = nonErrorType.selected
            if (nonErrorType.name.toString() == NON_EXISTENT_CLASS_NAME.shortName().asString()
                && qualifier is JCIdent
                && qualifier.name.toString() == NON_EXISTENT_CLASS_NAME.parent().asString()
            ) {
                @Suppress("UNCHECKED_CAST")
                return treeMaker.FqName("java.lang.Object") as T
            }
        }

        return nonErrorType
    }

    private fun isValidQualifiedName(name: FqName) = name.pathSegments().all { isValidIdentifier(it.asString()) }

    private fun isValidIdentifier(name: String, canBeConstructor: Boolean = false): Boolean {
        if (canBeConstructor && name == "<init>") {
            return true
        }

        if (name in JAVA_KEYWORDS) return false

        if (name.isEmpty()
            || !Character.isJavaIdentifierStart(name[0])
            || name.drop(1).any { !Character.isJavaIdentifierPart(it) }
        ) {
            return false
        }

        return true
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun convertModifiers(
        containingClass: ClassNode,
        access: Int,
        kind: ElementKind,
        packageFqName: String,
        visibleAnnotations: List<AnnotationNode>?,
        invisibleAnnotations: List<AnnotationNode>?,
        descriptorAnnotations: Annotations
    ): JCModifiers = convertModifiers(
        containingClass,
        access.toLong(),
        kind,
        packageFqName,
        visibleAnnotations,
        invisibleAnnotations,
        descriptorAnnotations
    )

    private fun convertModifiers(
        containingClass: ClassNode,
        access: Long,
        kind: ElementKind,
        packageFqName: String,
        visibleAnnotations: List<AnnotationNode>?,
        invisibleAnnotations: List<AnnotationNode>?,
        descriptorAnnotations: Annotations
    ): JCModifiers {
        var seenOverride = false
        val seenAnnotations = mutableSetOf<AnnotationDescriptor>()
        fun convertAndAdd(list: JavacList<JCAnnotation>, annotation: AnnotationNode): JavacList<JCAnnotation> {
            if (annotation.desc == "Ljava/lang/Override;") {
                if (seenOverride) return list  // KT-34569: skip duplicate @Override annotations
                seenOverride = true
            }
            // Missing annotation classes can match against multiple annotation descriptors
            val annotationDescriptor = descriptorAnnotations.firstOrNull {
                it !in seenAnnotations && checkIfAnnotationValueMatches(annotation, AnnotationValue(it))
            }?.also {
                seenAnnotations += it
            }
            val annotationTree = convertAnnotation(containingClass, annotation, packageFqName, annotationDescriptor) ?: return list
            return list.prepend(annotationTree)
        }

        var annotations = visibleAnnotations?.fold(JavacList.nil<JCAnnotation>(), ::convertAndAdd) ?: JavacList.nil()
        annotations = invisibleAnnotations?.fold(annotations, ::convertAndAdd) ?: annotations

        if (isDeprecated(access)) {
            val type = treeMaker.Type(Type.getType(java.lang.Deprecated::class.java))
            annotations = annotations.append(treeMaker.Annotation(type, JavacList.nil()))
        }

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

    private fun convertAnnotation(
        containingClass: ClassNode,
        annotation: AnnotationNode,
        packageFqName: String? = "",
        annotationDescriptor: AnnotationDescriptor? = null,
        filtered: Boolean = true
    ): JCAnnotation? {
        val annotationType = Type.getType(annotation.desc)
        val fqName = treeMaker.getQualifiedName(annotationType)

        if (filtered) {
            if (BLACKLISTED_ANNOTATIONS.any { fqName.startsWith(it) }) return null
            if (stripMetadata && fqName == KOTLIN_METADATA_ANNOTATION) return null
        }

        val ktAnnotation = annotationDescriptor?.source?.getPsi() as? KtAnnotationEntry
        val annotationFqName = getNonErrorType(
            annotationDescriptor?.type,
            ANNOTATION,
            { ktAnnotation?.typeReference },
            {
                val useSimpleName = '.' in fqName && fqName.substringBeforeLast('.', "") == packageFqName

                when {
                    useSimpleName -> treeMaker.FqName(fqName.substring(packageFqName!!.length + 1))
                    else -> treeMaker.Type(annotationType)
                }
            }
        )

        val argMapping = ktAnnotation?.calleeExpression
            ?.getResolvedCall(kaptContext.bindingContext)?.valueArguments
            ?.mapKeys { it.key.name.asString() }
            ?: emptyMap()

        val constantValues = pairedListToMap(annotation.values)

        val values = if (argMapping.isNotEmpty()) {
            argMapping.mapNotNull { (parameterName, arg) ->
                if (arg is DefaultValueArgument) return@mapNotNull null
                convertAnnotationArgumentWithName(containingClass, constantValues[parameterName], arg, parameterName)
            }
        } else {
            constantValues.mapNotNull { (parameterName, arg) ->
                convertAnnotationArgumentWithName(containingClass, arg, null, parameterName)
            }
        }

        return treeMaker.Annotation(annotationFqName, JavacList.from(values))
    }

    private fun convertAnnotationArgumentWithName(
        containingClass: ClassNode,
        constantValue: Any?,
        value: ResolvedValueArgument?,
        name: String
    ): JCExpression? {
        if (!isValidIdentifier(name)) return null
        val args = value?.arguments?.mapNotNull { it.getArgumentExpression() } ?: emptyList()
        val expr = convertConstantValueArguments(containingClass, constantValue, args) ?: return null
        return treeMaker.Assign(treeMaker.SimpleName(name), expr)
    }

    private fun convertConstantValueArguments(containingClass: ClassNode, constantValue: Any?, args: List<KtExpression>): JCExpression? {
        val singleArg = args.singleOrNull()

        fun tryParseTypeExpression(expression: KtExpression?): JCExpression? {
            if (expression is KtReferenceExpression) {
                val descriptor = kaptContext.bindingContext[BindingContext.REFERENCE_TARGET, expression]
                if (descriptor is ClassDescriptor) {
                    return treeMaker.FqName(descriptor.fqNameSafe)
                } else if (descriptor is TypeAliasDescriptor) {
                    descriptor.classDescriptor?.fqNameSafe?.let { return treeMaker.FqName(it) }
                }
            }

            return when (expression) {
                is KtSimpleNameExpression -> treeMaker.SimpleName(expression.getReferencedName())
                is KtDotQualifiedExpression -> {
                    val selector = expression.selectorExpression as? KtSimpleNameExpression ?: return null
                    val receiver = tryParseTypeExpression(expression.receiverExpression) ?: return null
                    return treeMaker.Select(receiver, treeMaker.name(selector.getReferencedName()))
                }
                else -> null
            }
        }

        fun tryParseTypeLiteralExpression(expression: KtExpression?): JCExpression? {
            val literalExpression = expression as? KtClassLiteralExpression ?: return null
            val typeExpression = tryParseTypeExpression(literalExpression.receiverExpression) ?: return null
            return treeMaker.Select(typeExpression, treeMaker.name("class"))
        }

        fun unwrapArgumentExpression(): List<KtExpression?>? =
            when (singleArg) {
                is KtCallExpression -> {
                    val resultingDescriptor = singleArg.getResolvedCall(kaptContext.bindingContext)?.resultingDescriptor

                    if (resultingDescriptor is FunctionDescriptor && isArrayOfFunction(resultingDescriptor))
                        singleArg.valueArguments.map { it.getArgumentExpression() }
                    else
                        null
                }
                is KtCollectionLiteralExpression -> singleArg.getInnerExpressions()
                is KtDotQualifiedExpression -> listOf(singleArg)
                null -> args
                else -> null
            }


        if (constantValue.isOfPrimitiveType()) {
            // Do not inline primitive constants
            tryParseReferenceToIntConstant(singleArg)?.let { return it }
        } else if (constantValue is List<*> &&
            constantValue.isNotEmpty() &&
            args.isNotEmpty() &&
            constantValue.all { it.isOfPrimitiveType() }
        ) {
            unwrapArgumentExpression()?.let { argumentExpressions ->
                val parsed = argumentExpressions.mapNotNull(::tryParseReferenceToIntConstant).toJavacList()
                if (parsed.size == argumentExpressions.size) {
                    return treeMaker.NewArray(null, null, parsed)
                }
            }
        }

        // Unresolved class literal
        if (constantValue == null && singleArg is KtClassLiteralExpression) {
            tryParseTypeLiteralExpression(singleArg)?.let { return it }
        }

        // Some of class literals in vararg list are unresolved
        if (args.isNotEmpty() && args[0] is KtClassLiteralExpression && constantValue is List<*> && args.size != constantValue.size) {
            val literalExpressions = mapJList(args, ::tryParseTypeLiteralExpression)
            if (literalExpressions.size == args.size) {
                return treeMaker.NewArray(null, null, literalExpressions)
            }
        }

        // Probably arrayOf(SomeUnresolvedType::class, ...)
        if (constantValue is List<*>) {
            val callArgs = unwrapArgumentExpression()
            // So we make sure something is absent in the constant value
            if (callArgs != null && callArgs.size > constantValue.size) {
                val literalExpressions = mapJList(callArgs, ::tryParseTypeLiteralExpression)
                if (literalExpressions.size == callArgs.size) {
                    return treeMaker.NewArray(null, null, literalExpressions)
                }
            }
        }

        return convertLiteralExpression(containingClass, constantValue)
    }

    private fun tryParseReferenceToIntConstant(expression: KtExpression?): JCExpression? {
        val bindingContext = kaptContext.bindingContext

        val expressionToResolve = when (expression) {
            is KtDotQualifiedExpression -> expression.selectorExpression
            else -> expression
        }

        val resolvedCall = expressionToResolve.getResolvedCall(bindingContext) ?: return null
        // Disable inlining only for Java statics
        val resultingDescriptor = resolvedCall.resultingDescriptor.takeIf { it.source is JavaSourceElement } ?: return null
        val fqName = resultingDescriptor.fqNameOrNull()?.takeIf { isValidQualifiedName(it) } ?: return null
        return treeMaker.FqName(fqName)
    }

    private fun convertValueOfPrimitiveTypeOrString(value: Any?): JCExpression? {
        fun specialFpValueNumerator(value: Double): Double = if (value.isNaN()) 0.0 else 1.0 * value.sign
        return when (value) {
            is Char -> treeMaker.Literal(TypeTag.CHAR, value.code)
            is Byte -> treeMaker.TypeCast(treeMaker.TypeIdent(TypeTag.BYTE), treeMaker.Literal(TypeTag.INT, value.toInt()))
            is Short -> treeMaker.TypeCast(treeMaker.TypeIdent(TypeTag.SHORT), treeMaker.Literal(TypeTag.INT, value.toInt()))
            is Boolean, is Int, is Long, is String -> treeMaker.Literal(value)
            is Float ->
                when {
                    value.isFinite() -> treeMaker.Literal(value)
                    else -> treeMaker.Binary(Tag.DIV, treeMaker.Literal(specialFpValueNumerator(value.toDouble()).toFloat()), treeMaker.Literal(0.0F))
                }
            is Double ->
                when {
                    value.isFinite() -> treeMaker.Literal(value)
                    else -> treeMaker.Binary(Tag.DIV, treeMaker.Literal(specialFpValueNumerator(value)), treeMaker.Literal(0.0))
                }
            else -> null
        }
    }

    private fun checkIfAnnotationValueMatches(asm: Any?, desc: ConstantValue<*>): Boolean {
        return when (asm) {
            null -> desc.value == null
            is Char -> desc is CharValue && desc.value == asm
            is Byte -> desc is ByteValue && desc.value == asm
            is Short -> desc is ShortValue && desc.value == asm
            is Boolean -> desc is BooleanValue && desc.value == asm
            is Int -> desc is IntValue && desc.value == asm
            is Long -> desc is LongValue && desc.value == asm
            is Float -> desc is FloatValue && desc.value == asm
            is Double -> desc is DoubleValue && desc.value == asm
            is String -> desc is StringValue && desc.value == asm
            is ByteArray -> desc is ArrayValue && desc.value.size == asm.size
            is BooleanArray -> desc is ArrayValue && desc.value.size == asm.size
            is CharArray -> desc is ArrayValue && desc.value.size == asm.size
            is ShortArray -> desc is ArrayValue && desc.value.size == asm.size
            is IntArray -> desc is ArrayValue && desc.value.size == asm.size
            is LongArray -> desc is ArrayValue && desc.value.size == asm.size
            is FloatArray -> desc is ArrayValue && desc.value.size == asm.size
            is DoubleArray -> desc is ArrayValue && desc.value.size == asm.size
            is Array<*> -> { // Two-element String array for enumerations ([desc, fieldName])
                assert(asm.size == 2)
                val valueName = (asm[1] as String).takeIf { isValidIdentifier(it) } ?: return false
                // It's not that easy to check types here because of fqName/internalName differences.
                // But enums can't extend other enums, so this should be enough.
                desc is EnumValue && desc.enumEntryName.asString() == valueName
            }
            is List<*> -> {
                desc is ArrayValue
                        && asm.size == desc.value.size
                        && asm.zip(desc.value).all { (eAsm, eDesc) -> checkIfAnnotationValueMatches(eAsm, eDesc) }
            }
            is Type -> desc is KClassValue && typeMapper.mapType(desc.getArgumentType(kaptContext.generationState.module)) == asm
            is AnnotationNode -> {
                val annotationDescriptor = (desc as? AnnotationValue)?.value ?: return false
                if (typeMapper.mapType(annotationDescriptor.type).descriptor != asm.desc) return false
                val asmAnnotationArgs = pairedListToMap(asm.values)
                if (annotationDescriptor.allValueArguments.size != asmAnnotationArgs.size) return false

                for ((descName, descValue) in annotationDescriptor.allValueArguments) {
                    val asmValue = asmAnnotationArgs[descName.asString()] ?: return false
                    if (!checkIfAnnotationValueMatches(asmValue, descValue)) return false
                }

                true
            }
            else -> false
        }
    }

    private fun convertLiteralExpression(containingClass: ClassNode, value: Any?): JCExpression {
        fun convertDeeper(value: Any?) = convertLiteralExpression(containingClass, value)

        convertValueOfPrimitiveTypeOrString(value)?.let { return it }

        return when (value) {
            null -> treeMaker.Literal(TypeTag.BOT, null)

            is ByteArray -> treeMaker.NewArray(null, JavacList.nil(), mapJList(value.asIterable(), ::convertDeeper))
            is BooleanArray -> treeMaker.NewArray(null, JavacList.nil(), mapJList(value.asIterable(), ::convertDeeper))
            is CharArray -> treeMaker.NewArray(null, JavacList.nil(), mapJList(value.asIterable(), ::convertDeeper))
            is ShortArray -> treeMaker.NewArray(null, JavacList.nil(), mapJList(value.asIterable(), ::convertDeeper))
            is IntArray -> treeMaker.NewArray(null, JavacList.nil(), mapJList(value.asIterable(), ::convertDeeper))
            is LongArray -> treeMaker.NewArray(null, JavacList.nil(), mapJList(value.asIterable(), ::convertDeeper))
            is FloatArray -> treeMaker.NewArray(null, JavacList.nil(), mapJList(value.asIterable(), ::convertDeeper))
            is DoubleArray -> treeMaker.NewArray(null, JavacList.nil(), mapJList(value.asIterable(), ::convertDeeper))
            is Array<*> -> { // Two-element String array for enumerations ([desc, fieldName])
                assert(value.size == 2)
                val enumType = Type.getType(value[0] as String)
                val valueName = (value[1] as String).takeIf { isValidIdentifier(it) } ?: run {
                    kaptContext.compiler.log.report(kaptContext.kaptError("'${value[1]}' is an invalid Java enum value name"))
                    "InvalidFieldName"
                }

                treeMaker.Select(treeMaker.Type(enumType), treeMaker.name(valueName))
            }
            is List<*> -> treeMaker.NewArray(null, JavacList.nil(), mapJList(value, ::convertDeeper))

            is Type -> {
                checkIfValidTypeName(containingClass, value)
                treeMaker.Select(treeMaker.Type(value), treeMaker.name("class"))
            }
            is AnnotationNode -> convertAnnotation(containingClass, value, packageFqName = null, filtered = false)!!
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

    private fun <T : JCTree> T.keepKdocCommentsIfNecessary(node: Any): T {
        kdocCommentKeeper?.saveKDocComment(this, node)
        return this
    }

    private fun JCMethodDecl.keepSignature(lineMappings: KaptLineMappingCollector, node: MethodNode): JCMethodDecl {
        lineMappings.registerSignature(this, node)
        return this
    }

    private fun getFieldType(field: FieldNode, origin: JvmDeclarationOrigin?): Type {
        val fieldType = Type.getType(field.desc)
        return when (val declaration = origin?.element) {
            is KtProperty -> {
                //replace anonymous type in delegate (if any)
                val delegateType = kaptContext.bindingContext[BindingContext.EXPRESSION_TYPE_INFO, declaration.delegateExpression]?.type
                delegateType?.let {
                    val replaced = replaceAnonymousTypeWithSuperType(it)
                    //not changed => not anonymous type => use type from field
                    if (replaced != it) replaced else null
                }?.let(::convertKotlinType) ?: fieldType
            }
            else -> fieldType
        }
    }

    private fun convertKotlinType(type: KotlinType): Type = typeMapper.mapType(type, TypeMappingMode.GENERIC_ARGUMENT)

    private fun getFileForClass(c: ClassNode): KtFile? = kaptContext.origins[c]?.element?.containingFile as? KtFile

    private fun reportIfIllegalTypeUsage(containingClass: ClassNode, type: Type) {
        val file = getFileForClass(containingClass)
        importsFromRoot[file]?.let { importsFromRoot ->
            val typeName = type.className
            if (importsFromRoot.contains(typeName)) {
                val msg = "${containingClass.className}: Can't reference type '${typeName}' from default package in Java stub."
                if (strictMode) kaptContext.reportKaptError(msg)
                else kaptContext.logger.warn(msg)
            }
        }
    }

    private fun collectImportsFromRootPackage(): Map<KtFile, Set<String>> =
        kaptContext.compiledClasses.mapNotNull(::getFileForClass).distinct().map { file ->
            val importsFromRoot =
                file.importDirectives
                    .filter { !it.isAllUnder }
                    .mapNotNull { im -> im.importPath?.fqName?.takeIf { it.isOneSegmentFQN() } }
            file to importsFromRoot.mapTo(mutableSetOf()) { it.asString() }
        }.toMap()

    private fun isArrayOfFunction(d: FunctionDescriptor): Boolean {
        val name = d.fqNameSafe
        return name.parent() == KOTLIN_PACKAGE && ARRAY_OF_FUNCTIONS.contains(name.shortName())
    }

}

private fun Any?.isOfPrimitiveType(): Boolean = when (this) {
    is Boolean, is Byte, is Int, is Long, is Short, is Char, is Float, is Double -> true
    else -> false
}

private val ClassDescriptor.isNested: Boolean
    get() = containingDeclaration is ClassDescriptor

internal tailrec fun getReferenceExpression(expression: KtExpression?): KtReferenceExpression? = when (expression) {
    is KtReferenceExpression -> expression
    is KtQualifiedExpression -> getReferenceExpression(expression.selectorExpression)
    else -> null
}
