/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.kapt4

import com.intellij.psi.*
import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.code.TypeTag
import com.sun.tools.javac.parser.Tokens
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.JCTree.*
import kotlinx.kapt.KaptIgnored
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.base.kapt3.KaptFlag
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.kapt3.base.javac.kaptError
import org.jetbrains.kotlin.kapt3.base.javac.reportKaptError
import org.jetbrains.kotlin.kapt3.base.stubs.KaptStubLineInformation
import org.jetbrains.kotlin.kapt3.base.stubs.KotlinPosition
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForFacade
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForNamedClassLike
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isOneSegmentFQN
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression
import org.jetbrains.kotlin.resolve.ArrayFqNames
import org.jetbrains.kotlin.resolve.calls.util.getCalleeExpressionIfAny
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.runUnless
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import java.io.File
import java.lang.annotation.ElementType
import javax.lang.model.element.ElementKind
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.sign

context(Kapt4ContextForStubGeneration)
class Kapt4StubGenerator(private val analysisSession: KtAnalysisSession) {
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

        private val kotlin2JvmTargetMap = mapOf(
            AnnotationTarget.CLASS to ElementType.TYPE,
            AnnotationTarget.ANNOTATION_CLASS to ElementType.ANNOTATION_TYPE,
            AnnotationTarget.CONSTRUCTOR to ElementType.CONSTRUCTOR,
            AnnotationTarget.LOCAL_VARIABLE to ElementType.LOCAL_VARIABLE,
            AnnotationTarget.FUNCTION to ElementType.METHOD,
            AnnotationTarget.PROPERTY_GETTER to ElementType.METHOD,
            AnnotationTarget.PROPERTY_SETTER to ElementType.METHOD,
            AnnotationTarget.FIELD to ElementType.FIELD,
            AnnotationTarget.VALUE_PARAMETER to ElementType.PARAMETER,
            AnnotationTarget.TYPE_PARAMETER to ElementType.TYPE_PARAMETER,
            AnnotationTarget.TYPE to ElementType.TYPE_USE
        )
    }

    private val strictMode = options[KaptFlag.STRICT]
    private val stripMetadata = options[KaptFlag.STRIP_METADATA]
    private val keepKdocComments = options[KaptFlag.KEEP_KDOC_COMMENTS_IN_STUBS]

    private val kdocCommentKeeper = runIf(keepKdocComments) { Kapt4KDocCommentKeeper(analysisSession) }

    fun generateStubs(): Map<KtLightClass, KaptStub?> {
        return classes.associateWith { convertTopLevelClass(it) }
    }

    private fun convertTopLevelClass(lightClass: KtLightClass): KaptStub? {
//        val origin = origins[lightClass]// ?: return null // TODO: handle synthetic declarations from plugins
        val ktFile = origins[lightClass] ?: return null //origin?.element?.containingFile as? KtFile ?: return null
        val lineMappings = Kapt4LineMappingCollector()
        val packageName = (lightClass.parent as? PsiJavaFile)?.packageName ?: TODO()
        val packageClause = runUnless(packageName.isBlank()) { treeMaker.FqName(packageName) }

        val unresolvedQualifiersRecorder = UnresolvedQualifiersRecorder(ktFile)
        val classDeclaration = with(unresolvedQualifiersRecorder) {
            convertClass(lightClass, lineMappings, packageName, true) ?: return null
        }

        classDeclaration.mods.annotations = classDeclaration.mods.annotations

        val classes = JavacList.of<JCTree>(classDeclaration)

        // imports should be collected after class conversion to
        val imports = convertImports(ktFile, unresolvedQualifiersRecorder)

        val nonEmptyImports: JavacList<JCTree> = when {
            imports.size > 0 -> imports
            else -> JavacList.of(treeMaker.Import(treeMaker.FqName("java.lang.System"), false))
        }

        val topLevel = treeMaker.TopLevelJava9Aware(packageClause, nonEmptyImports + classes)
        if (kdocCommentKeeper != null) {
            topLevel.docComments = kdocCommentKeeper.getDocTable(topLevel)
        }
//        TODO
//        KaptJavaFileObject(topLevel, classDeclaration).apply {
//            topLevel.sourcefile = this
//            mutableBindings[clazz.name] = this
//        }
//
//        postProcess(topLevel)

        return KaptStub(topLevel, lineMappings.serialize())
    }

    // TODO: convert to context after fix of KT-54197
    private fun UnresolvedQualifiersRecorder.convertClass(
        lightClass: PsiClass,
        lineMappings: Kapt4LineMappingCollector,
        packageFqName: String,
        isTopLevel: Boolean
    ): JCClassDecl? {
        if (!checkIfValidTypeName(lightClass, lightClass.defaultType)) return null

        val parentClass = lightClass.parent as? PsiClass
        // Java supports only public nested classes inside interfaces and annotations
        if ((parentClass?.isInterface == true || parentClass?.isAnnotationType == true) && !lightClass.isPublic) return null

        val isInnerOrNested = parentClass != null
        val isNested = isInnerOrNested && lightClass.isStatic
        val isInner = isInnerOrNested && !isNested

        val flags = lightClass.accessFlags

        val metadata = runUnless(stripMetadata) {
            when (lightClass) {
                is SymbolLightClassForNamedClassLike -> lightClass.kotlinOrigin?.let { metadataCalculator.calculate(it) }
                is SymbolLightClassForFacade -> {
                    val ktFiles = lightClass.files
                    when (ktFiles.size) {
                        0 -> null
                        1 -> metadataCalculator.calculate(ktFiles.single())
                        else -> metadataCalculator.calculate(ktFiles)
                    }
                }

                else -> null
            }
        }

        val javaRetentionAnnotation = runIf(lightClass.isAnnotationType) {
            lightClass.getAnnotation("kotlin.annotation.Target")?.let { createJavaTargetAnnotation(it) }
        }

        val isEnum = lightClass.isEnum
        val modifiers = convertModifiers(
            lightClass,
            flags.toLong(),
            if (isEnum) ElementKind.ENUM else ElementKind.CLASS,
            packageFqName,
            lightClass.annotations.toList(),
            metadata,
            javaRetentionAnnotation?.let { JavacList.of(javaRetentionAnnotation) } ?: JavacList.nil()
        )

        // TODO: check
        val isDefaultImpls = lightClass.name!!.endsWith("\$DefaultImpls")
                && lightClass.isPublic && lightClass.isFinal && lightClass.isInterface

        // DefaultImpls without any contents don't have INNERCLASS'es inside it (and inside the parent interface)
        if (isDefaultImpls && (isTopLevel || (lightClass.fields.isEmpty() && lightClass.methods.isEmpty()))) {
            return null
        }

        val simpleName = getClassName(lightClass, isDefaultImpls, packageFqName)
        if (!isValidIdentifier(simpleName)) return null

        val classSignature = parseClassSignature(lightClass)

        val enumValues: JavacList<JCTree> = mapJList(lightClass.fields) { field ->
            if (field !is PsiEnumConstant) return@mapJList null
            val constructorArguments = lightClass.constructors.firstOrNull()?.parameters?.mapNotNull { it.type as? PsiType }.orEmpty()
            val args = mapJList(constructorArguments) { convertLiteralExpression(lightClass, getDefaultValue(it)) }

            convertField(
                field, lightClass, lineMappings, packageFqName, treeMaker.NewClass(
                    /* enclosing = */ null,
                    /* typeArgs = */ JavacList.nil(),
                    /* lightClass = */ treeMaker.Ident(treeMaker.name(field.name)),
                    /* args = */ args,
                    /* def = */ null
                )
            )
        }

        val fieldsPositions = mutableMapOf<JCTree, MemberData>()
        val fields = mapJList<PsiField, JCTree>(lightClass.fields) { field ->
            runUnless(field is PsiEnumConstant) { convertField(field, lightClass, lineMappings, packageFqName)?.also {
                    fieldsPositions[it] = MemberData(field.name, field.signature, lineMappings.getPosition(lightClass, field))
                }
            }
        }

        val methodsPositions = mutableMapOf<JCTree, MemberData>()
        val methods = mapJList<PsiMethod, JCTree>(lightClass.methods) { method ->
            if (isEnum && method.isSyntheticStaticEnumMethod()) {
                return@mapJList null
            }

            convertMethod(method, lightClass, lineMappings, packageFqName, isInner)?.also {
                methodsPositions[it] = MemberData(method.name, method.signature, lineMappings.getPosition(lightClass, method))
            }
        }

        val nestedClasses = mapJList(lightClass.innerClasses) { innerClass ->
            convertClass(innerClass, lineMappings, packageFqName, false)
        }

        lineMappings.registerClass(lightClass)

        val classPosition = lineMappings.getPosition(lightClass)
        val sortedFields = JavacList.from(fields.sortedWith(MembersPositionComparator(classPosition, fieldsPositions)))
        val sortedMethods = JavacList.from(methods.sortedWith(MembersPositionComparator(classPosition, methodsPositions)))

        return treeMaker.ClassDef(
            modifiers,
            treeMaker.name(simpleName),
            classSignature.typeParameters,
            classSignature.superClass.takeUnless { classSignature.superClassIsObject || lightClass.isEnum },
            classSignature.interfaces,
            JavacList.from(enumValues + sortedFields + sortedMethods + nestedClasses)
        ).keepKdocCommentsIfNecessary(lightClass)
    }

    private fun PsiMethod.isSyntheticStaticEnumMethod(): Boolean {
        if (!this.isStatic) return false
        return when (name) {
            StandardNames.ENUM_VALUES.asString() -> parameters.isEmpty()
            StandardNames.ENUM_VALUE_OF.asString() -> (parameters.singleOrNull()?.type as? PsiClassType)?.qualifiedName == "java.lang.String"
            else -> false
        }
    }

    private class MemberData(val name: String, val descriptor: String, val position: KotlinPosition?)

    private fun convertImports(file: KtFile, unresolvedQualifiers: UnresolvedQualifiersRecorder): JavacList<JCTree> {
        if (unresolvedQualifiers.isEmpty()) return JavacList.nil()

        val imports = mutableListOf<JCImport>()
        val importedShortNames = mutableSetOf<String>()

        // We prefer ordinary imports over aliased ones.
        val sortedImportDirectives = file.importDirectives.partition { it.aliasName == null }.run { first + second }

        loop@ for (importDirective in sortedImportDirectives) {
            val acceptableByName = when {
                importDirective.isAllUnder -> true
                else -> {
                    val fqName = importDirective.importedFqName ?: continue
                    fqName.asString() in unresolvedQualifiers.qualifiedNames || fqName.shortName().identifier in unresolvedQualifiers.simpleNames
                }
            }

            if (!acceptableByName) continue

            val importedSymbols = with(analysisSession) {
                val importedReference = importDirective.importedReference
                    ?.getCalleeExpressionIfAny()
                    ?.references
                    ?.firstOrNull() as? KtReference
                importedReference?.resolveToSymbols().orEmpty()
            }

            val isAllUnderClassifierImport = importDirective.isAllUnder && importedSymbols.any { it is KtClassOrObjectSymbol }
            val isCallableImport = !importDirective.isAllUnder && importedSymbols.any { it is KtCallableSymbol }
            val isEnumEntryImport = !importDirective.isAllUnder && importedSymbols.any { it is KtEnumEntrySymbol }

            if (isAllUnderClassifierImport || isCallableImport || isEnumEntryImport) continue


            // Qualified name should be valid Java fq-name
            val importedFqName = importDirective.importedFqName?.takeIf { it.pathSegments().size > 1 } ?: continue
            if (!isValidQualifiedName(importedFqName)) continue
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

    // Done
    private fun getClassAccessFlags(lightClass: PsiClass, isNested: Boolean): Int {
        val parentClass = lightClass.parent as? PsiClass

        var access = lightClass.accessFlags
        access = access or when {
            lightClass.isRecord -> Opcodes.ACC_RECORD
            lightClass.isInterface -> Opcodes.ACC_INTERFACE
            lightClass.isEnum -> Opcodes.ACC_ENUM
            else -> 0
        }

        if (parentClass?.isInterface == true) {
            // Classes inside interfaces should always be public and static.
            // See com.sun.tools.javac.comp.Enter.visitClassDef for more information.
            return (access or Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC) and
                    Opcodes.ACC_PRIVATE.inv() and Opcodes.ACC_PROTECTED.inv() // Remove private and protected modifiers
        }

        if (isNested) {
            access = access or Opcodes.ACC_STATIC
        }
        if (lightClass.isAnnotationType) {
            access = access or Opcodes.ACC_ANNOTATION
        }
        return access
    }

    private fun convertMetadataAnnotation(metadata: Metadata): JCAnnotation {
        val argumentsWithNames = mapOf(
            "k" to metadata.kind,
            "mv" to metadata.metadataVersion.toList(),
            "bv" to metadata.bytecodeVersion.toList(),
            "d1" to metadata.data1.toList(),
            "d2" to metadata.data2.toList(),
            "xs" to metadata.extraString,
            "pn" to metadata.packageName,
            "xi" to metadata.extraInt,
        )
        val arguments = argumentsWithNames.map { (name, value) ->
            val jValue = convertLiteralExpression(containingClass = null, value)
            treeMaker.Assign(treeMaker.SimpleName(name), jValue)
        }
        return treeMaker.Annotation(treeMaker.FqName(Metadata::class.java.canonicalName), JavacList.from(arguments))
    }

    // TODO: convert to context after fix of KT-54197
    private fun UnresolvedQualifiersRecorder.convertAnnotation(
        containingClass: PsiClass,
        annotation: PsiAnnotation,
        packageFqName: String? = "",
        filtered: Boolean = true
    ): JCAnnotation? {
        val rawQualifiedName = annotation.qualifiedName ?: return null
        val fqName = treeMaker.getQualifiedName(rawQualifiedName)
        if (filtered) {
            if (BLACKLISTED_ANNOTATIONS.any { fqName.startsWith(it) }) return null
            if (stripMetadata && fqName == KOTLIN_METADATA_ANNOTATION) return null
        }

        val annotationFqName = annotation.resolveAnnotationType()?.defaultType.convertAndRecordErrors(this) {
            val useSimpleName = '.' in fqName && fqName.substringBeforeLast('.', "") == packageFqName

            when {
                useSimpleName -> treeMaker.FqName(fqName.substring(packageFqName!!.length + 1))
                else -> treeMaker.FqName(fqName)
            }
        }

        val values = mapJList<_, JCExpression>(annotation.parameterList.attributes) {
            val name = it.name ?: return@mapJList null
            val value = it.value ?: return@mapJList null
            val expr = convertPsiAnnotationMemberValue(containingClass, value, packageFqName, filtered)
            treeMaker.Assign(treeMaker.SimpleName(name), expr)
        }

        return treeMaker.Annotation(annotationFqName, values)
    }

    // TODO: convert to context after fix of KT-54197
    private fun UnresolvedQualifiersRecorder.convertPsiAnnotationMemberValue(
        containingClass: PsiClass,
        value: PsiAnnotationMemberValue,
        packageFqName: String? = "",
        filtered: Boolean = true
    ): JCExpression {
        return when (value) {
            is PsiArrayInitializerMemberValue -> {
                val arguments = mapJList(value.initializers) {
                    convertPsiAnnotationMemberValue(containingClass, it, packageFqName, filtered)
                }
                treeMaker.NewArray(null, null, arguments)
            }

            is PsiLiteral -> convertLiteralExpression(containingClass = null, value.value)
            is PsiClassObjectAccessExpression -> {
                val type = value.operand.type
                checkIfValidTypeName(containingClass, type)
                treeMaker.Select(treeMaker.SimpleName(type.qualifiedName), treeMaker.name("class"))
            }
            is PsiExpression -> treeMaker.SimpleName(value.text)
            is PsiAnnotation -> convertAnnotation(containingClass, value, packageFqName, filtered) ?: TODO()
            else -> error("Should not be here")
        }
    }

    private fun createJavaTargetAnnotation(retentionAnnotation: PsiAnnotation): JCAnnotation? {
        val kotlinTargetNames = mutableListOf<String>()

        fun collect(value: PsiAnnotationMemberValue?) {
            when (value) {
                is PsiArrayInitializerMemberValue -> value.initializers.forEach { collect(it) }
                is PsiExpression -> kotlinTargetNames += value.text.substringAfterLast(".")
            }
        }

        collect(retentionAnnotation.parameterList.attributes.firstOrNull()?.value)

        val kotlinTargets = kotlinTargetNames.map { AnnotationTarget.valueOf(it) }
        val javaTargets = kotlinTargets.mapNotNull { kotlin2JvmTargetMap[it] }
        if (javaTargets.isEmpty()) return null
        val jArguments = mapJList(javaTargets) { treeMaker.SimpleName("${ElementType::class.java.canonicalName}.${it.name}") }
        return treeMaker.Annotation(
            treeMaker.FqName(java.lang.annotation.Target::class.java.canonicalName),
            JavacList.of(treeMaker.Assign(treeMaker.SimpleName("value"), treeMaker.NewArray(null, null,jArguments)))
        )
    }

    context(UnresolvedQualifiersRecorder)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    private fun convertModifiers(
        containingClass: PsiClass,
        access: Int,
        kind: ElementKind,
        packageFqName: String,
        allAnnotations: List<PsiAnnotation>,
        metadata: Metadata?,
        additionalAnnotations: JavacList<JCAnnotation> = JavacList.nil()
    ): JCModifiers {
        return convertModifiers(containingClass, access.toLong(), kind, packageFqName, allAnnotations, metadata, additionalAnnotations)
    }

    context(UnresolvedQualifiersRecorder)
    @Suppress("IncorrectFormatting") // KTIJ-22227
    private fun convertModifiers(
        containingClass: PsiClass,
        access: Long,
        kind: ElementKind,
        packageFqName: String,
        allAnnotations: List<PsiAnnotation>,
        metadata: Metadata?,
        additionalAnnotations: JavacList<JCAnnotation> = JavacList.nil()
    ): JCModifiers {
        var seenOverride = false
        fun convertAndAdd(list: JavacList<JCAnnotation>, annotation: PsiAnnotation): JavacList<JCAnnotation> {
            if (annotation.hasQualifiedName("java.lang.Override")) {
                if (seenOverride) return list  // KT-34569: skip duplicate @Override annotations
                seenOverride = true
            }
            val annotationTree = convertAnnotation(containingClass, annotation, packageFqName) ?: return list
            return list.prepend(annotationTree)
        }

        var annotations = allAnnotations.fold(JavacList.nil(), ::convertAndAdd)

        if (isDeprecated(access)) {
            val type = treeMaker.RawType(Type.getType(java.lang.Deprecated::class.java))
            annotations = annotations.append(treeMaker.Annotation(type, JavacList.nil()))
        }
        annotations = annotations.prependList(additionalAnnotations)
        if (metadata != null) {
            annotations = annotations.prepend(convertMetadataAnnotation(metadata))
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

    // TODO: convert to context after fix of KT-54197
    private fun UnresolvedQualifiersRecorder.convertField(
        field: PsiField,
        containingClass: PsiClass,
        lineMappings: Kapt4LineMappingCollector,
        packageFqName: String,
        explicitInitializer: JCExpression? = null
    ): JCVariableDecl? {
//        if (field.isSynthetic || isIgnored(field.invisibleAnnotations)) return null // TODO
        // not needed anymore

        val fieldAnnotations = field.annotations.asList()

        if (isIgnored(fieldAnnotations)) return null

//        val fieldAnnotations = when {
//            !isIrBackend && descriptor is PropertyDescriptor -> descriptor.backingField?.annotations
//            else -> descriptor?.annotations
//        } ?: Annotations.EMPTY

        val access = field.accessFlags
        val modifiers = convertModifiers(
            containingClass,
            access, ElementKind.FIELD, packageFqName,
            fieldAnnotations,
            metadata = null
        )

        val name = field.name
        if (!isValidIdentifier(name)) return null

        val type = field.type

        // TODO
//        if (!checkIfValidTypeName(containingClass, type)) {
//            return null
//        }

//        fun typeFromAsm() = signatureParser.parseFieldSignature(field.signature, treeMaker.RawType(type))

        // Enum type must be an identifier (Javac requirement)
        val typeExpression = if (isEnum(access)) {
            treeMaker.SimpleName(treeMaker.getQualifiedName(type).substringAfterLast('.'))
        } else {
            type.convertAndRecordErrors(this)
        }

        lineMappings.registerField(containingClass, field)

        val initializer = explicitInitializer ?: convertPropertyInitializer(containingClass, field)
        return treeMaker.VarDef(modifiers, treeMaker.name(name), typeExpression, initializer).keepKdocCommentsIfNecessary(field)
    }

    private fun convertPropertyInitializer(containingClass: PsiClass, field: PsiField): JCExpression? {
        val origin = field.ktOrigin

        val propertyInitializer = field.initializer

//        if (propertyInitializer is PsiEnumConstant) {
//            if (propertyInitializer != null) {
//                return convertConstantValueArguments(containingClass, value, listOf(propertyInitializer))
//            }
//
//            return convertValueOfPrimitiveTypeOrString(value)
//        }
//
//        val propertyType = (origin?.descriptor as? PropertyDescriptor)?.returnType
//
//        /*
//            Work-around for enum classes in companions.
//            In expressions "Foo.Companion.EnumClass", Java prefers static field over a type name, making the reference invalid.
//        */
//        if (propertyType != null && propertyType.isEnum()) {
//            val enumClass = propertyType.constructor.declarationDescriptor
//            if (enumClass is ClassDescriptor && enumClass.isInsideCompanionObject()) {
//                return null
//            }
//        }
//
//        if (propertyInitializer != null && propertyType != null) {
//            val constValue = getConstantValue(propertyInitializer, propertyType)
//            if (constValue != null) {
//                val asmValue = mapConstantValueToAsmRepresentation(constValue)
//                if (asmValue !== UnknownConstantValue) {
//                    return convertConstantValueArguments(containingClass, asmValue, listOf(propertyInitializer))
//                }
//            }
//        }
//
        if (field.isFinal) {
            val type = field.type
            return if (propertyInitializer is PsiLiteralExpression) {
                val rawValue = propertyInitializer.value
                val rawNumberValue = rawValue as? Number
                val actualValue = when (type) {
                    PsiType.BYTE -> rawNumberValue?.toByte()
                    PsiType.SHORT -> rawNumberValue?.toShort()
                    PsiType.INT -> rawNumberValue?.toInt()
                    PsiType.LONG -> rawNumberValue?.toLong()
                    PsiType.FLOAT -> rawNumberValue?.toFloat()
                    PsiType.DOUBLE -> rawNumberValue?.toDouble()
                    PsiType.CHAR -> rawNumberValue?.toChar()
                    else -> null
                } ?: rawValue
                convertValueOfPrimitiveTypeOrString(actualValue)
            } else {
                convertLiteralExpression(containingClass, getDefaultValue(type))
            }
        }

        return null
    }

    private fun convertLiteralExpression(containingClass: PsiClass?, value: Any?): JCExpression {
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
                    compiler.log.report(kaptError("'${value[1]}' is an invalid Java enum value name"))
                    "InvalidFieldName"
                }

                treeMaker.Select(treeMaker.RawType(enumType), treeMaker.name(valueName))
            }

            is List<*> -> treeMaker.NewArray(null, JavacList.nil(), mapJList(value, ::convertDeeper))

//            is Type -> {
//                checkIfValidTypeName(containingClass, value)
//                treeMaker.Select(treeMaker.Type(value), treeMaker.name("class"))
//            }
//
//            is AnnotationNode -> convertAnnotation(containingClass, value, packageFqName = null, filtered = false)!!
            else -> throw IllegalArgumentException("Illegal literal expression value: $value (${value::class.java.canonicalName})")
        }
    }


    private fun getDefaultValue(type: PsiType): Any? = when (type) {
        PsiType.BYTE -> 0
        PsiType.BOOLEAN -> false
        PsiType.CHAR -> '\u0000'
        PsiType.SHORT -> 0
        PsiType.INT -> 0
        PsiType.LONG -> 0L
        PsiType.FLOAT -> 0.0F
        PsiType.DOUBLE -> 0.0
        else -> null
    }

    private fun convertValueOfPrimitiveTypeOrString(value: Any?): JCExpression? {
        fun specialFpValueNumerator(value: Double): Double = if (value.isNaN()) 0.0 else 1.0 * value.sign
        val convertedValue = when (value) {
            is Char -> treeMaker.Literal(TypeTag.CHAR, value.code)
            is Byte -> treeMaker.TypeCast(treeMaker.TypeIdent(TypeTag.BYTE), treeMaker.Literal(TypeTag.INT, value.toInt()))
            is Short -> treeMaker.TypeCast(treeMaker.TypeIdent(TypeTag.SHORT), treeMaker.Literal(TypeTag.INT, value.toInt()))
            is Boolean, is Int, is Long, is String -> treeMaker.Literal(value)
            is Float -> when {
                value.isFinite() -> treeMaker.Literal(value)
                else -> treeMaker.Binary(
                    Tag.DIV,
                    treeMaker.Literal(specialFpValueNumerator(value.toDouble()).toFloat()),
                    treeMaker.Literal(0.0F)
                )
            }

            is Double -> when {
                value.isFinite() -> treeMaker.Literal(value)
                else -> treeMaker.Binary(Tag.DIV, treeMaker.Literal(specialFpValueNumerator(value)), treeMaker.Literal(0.0))
            }

            else -> null
        }

        return convertedValue
    }

    // TODO: convert to context after fix of KT-54197
    private fun UnresolvedQualifiersRecorder.convertMethod(
        method: PsiMethod,
        containingClass: PsiClass,
        lineMappings: Kapt4LineMappingCollector,
        packageFqName: String,
        isInner: Boolean
    ): JCMethodDecl? {
        if (isIgnored(method.annotations.asList())) return null

        val isAnnotationHolderForProperty =
            method.isSynthetic && method.isStatic && method.name.endsWith(JvmAbi.ANNOTATED_PROPERTY_METHOD_NAME_SUFFIX)

        if (method.isSynthetic && !isAnnotationHolderForProperty) return null

        val isConstructor = method.isConstructor

        val name = method.properName
        if (!isValidIdentifier(name, canBeConstructor = isConstructor)) return null

        val modifiers = convertModifiers(
            containingClass,
            if (containingClass.isEnum && isConstructor)
                (method.accessFlags.toLong() and VISIBILITY_MODIFIERS.inv())
            else
                method.accessFlags.toLong(),
            ElementKind.METHOD,
            packageFqName,
            method.annotations.toList(),
            metadata = null,
        )

        if (containingClass.isInterface && !method.isAbstract && !method.isStatic) {
            modifiers.flags = modifiers.flags or Flags.DEFAULT
        }

        val returnType = method.returnType ?: PsiType.VOID

        val parametersInfo = method.getParametersInfo(containingClass, isInner)

        if (!checkIfValidTypeName(containingClass, returnType)
            || parametersInfo.any { !checkIfValidTypeName(containingClass, it.type) }
        ) {
            return null
        }

        @Suppress("NAME_SHADOWING")
        val jParameters = mapJListIndexed(parametersInfo) { index, info ->
            val lastParameter = index == parametersInfo.lastIndex
            val isArrayType = info.type is PsiArrayType

            val varargs = if (lastParameter && isArrayType && method.hasVarargs) Flags.VARARGS else 0L
            val modifiers = convertModifiers(
                containingClass,
                info.flags or varargs or Flags.PARAMETER,
                ElementKind.PARAMETER,
                packageFqName,
                info.visibleAnnotations + info.invisibleAnnotations, // TODO
                metadata = null
            )

            val name = info.name.takeIf { isValidIdentifier(it) } ?: "p$index"

            val type = info.type.convertAndRecordErrors(this)
            treeMaker.VarDef(modifiers, treeMaker.name(name), type, null)
        }
        val jTypeParameters = mapJList(method.typeParameters) { convertTypeParameter(it) }
        val jExceptionTypes = mapJList(method.throwsTypes) { treeMaker.TypeWithArguments(it as PsiType) }
        val jReturnType = runUnless(isConstructor) {
            returnType.convertAndRecordErrors(this)
        }

        val defaultValue = (method as? PsiAnnotationMethod)?.defaultValue?.let {
            convertPsiAnnotationMemberValue(containingClass, it, packageFqName)
        }

        val body = if (defaultValue != null) {
            null
        } else if (method.isAbstract) {
            null
        } else if (isConstructor && containingClass.isEnum) {
            treeMaker.Block(0, JavacList.nil())
        } else if (isConstructor) {
            val superConstructor = containingClass.superClass?.constructors?.firstOrNull { !it.isPrivate }
            val superClassConstructorCall = if (superConstructor != null) {
                val args = mapJList(superConstructor.parameterList.parameters) { param ->
                    convertLiteralExpression(containingClass, getDefaultValue(param.type))
                }
                val call = treeMaker.Apply(JavacList.nil(), treeMaker.SimpleName("super"), args)
                JavacList.of<JCStatement>(treeMaker.Exec(call))
            } else {
                JavacList.nil()
            }
            treeMaker.Block(0, superClassConstructorCall)
        } else if (returnType == PsiType.VOID) {
            treeMaker.Block(0, JavacList.nil())
        } else {
            val returnStatement = treeMaker.Return(convertLiteralExpression(containingClass, getDefaultValue(returnType)))
            treeMaker.Block(0, JavacList.of(returnStatement))
        }

        lineMappings.registerMethod(containingClass, method)

        return treeMaker.MethodDef(
            modifiers, treeMaker.name(name), jReturnType, jTypeParameters,
            jParameters, jExceptionTypes,
            body, defaultValue
        ).keepSignature(lineMappings, method).keepKdocCommentsIfNecessary(method)
    }

    private fun JCMethodDecl.keepSignature(lineMappings: Kapt4LineMappingCollector, method: PsiMethod): JCMethodDecl {
        lineMappings.registerSignature(this, method)
        return this
    }

    private fun <T : JCTree> T.keepKdocCommentsIfNecessary(element: PsiElement): T {
        kdocCommentKeeper?.saveKDocComment(this, element)
        return this
    }

    private fun isIgnored(annotations: List<PsiAnnotation>?): Boolean {
        val kaptIgnoredAnnotationFqName = KaptIgnored::class.java.canonicalName
        return annotations?.any { it.hasQualifiedName(kaptIgnoredAnnotationFqName) } ?: false
    }

    // TODO: convert to context after fix of KT-54197
    private tailrec fun UnresolvedQualifiersRecorder.checkIfValidTypeName(
        containingClass: PsiClass,
        type: PsiType
    ): Boolean {
        when (type) {
            is PsiArrayType -> return checkIfValidTypeName(containingClass, type.componentType)
            is PsiPrimitiveType -> return true
        }


        if (type.toString() == "PsiType:RootClass") {
            val res = type.resolvedClass
            Unit
        }

        val internalName = type.qualifiedName
        // Ignore type names with Java keywords in it
        if (internalName.split('/', '.').any { it in JAVA_KEYWORDS }) {
            if (strictMode) {
                reportKaptError(
                    "Can't generate a stub for '${internalName}'.",
                    "Type name '${type.qualifiedName}' contains a Java keyword."
                )
            }

            return false
        }

        val clazz = type.resolvedClass ?: return true

        if (doesInnerClassNameConflictWithOuter(clazz)) {
            if (strictMode) {
                reportKaptError(
                    "Can't generate a stub for '${clazz.qualifiedNameWithDollars}'.",
                    "Its name '${clazz.name}' is the same as one of the outer class names.",
                    "Java forbids it. Please change one of the class names."
                )
            }

            return false
        }

        reportIfIllegalTypeUsage(containingClass, type)

        return true
    }

    private fun findContainingClassNode(clazz: PsiClass): PsiClass? {
        return clazz.parent as? PsiClass
    }

    // Java forbids outer and inner class names to be the same. Check if the names are different
    private tailrec fun doesInnerClassNameConflictWithOuter(
        clazz: PsiClass,
        outerClass: PsiClass? = findContainingClassNode(clazz)
    ): Boolean {
        if (outerClass == null) return false
        if (treeMaker.getSimpleName(clazz) == treeMaker.getSimpleName(outerClass)) return true
        // Try to find the containing class for outerClassNode (to check the whole tree recursively)
        val containingClassForOuterClass = findContainingClassNode(outerClass) ?: return false
        return doesInnerClassNameConflictWithOuter(clazz, containingClassForOuterClass)
    }

    // TODO: convert to context after fix of KT-54197
    private fun UnresolvedQualifiersRecorder.reportIfIllegalTypeUsage(
        containingClass: PsiClass,
        type: PsiType
    ) {
        val typeName = type.simpleNameOrNull ?: return
        if (typeName in importsFromRoot) {
            val msg = "${containingClass.qualifiedName}: Can't reference type '${typeName}' from default package in Java stub."
            if (strictMode) reportKaptError(msg)
            else logger.warn(msg)
        }
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun PsiType?.convertAndRecordErrors(
        recorder: UnresolvedQualifiersRecorder, // TODO: convert to context after fix of KT-54197
        ifNonError: () -> JCExpression = { treeMaker.TypeWithArguments(this) }
    ): JCExpression {
        contract {
            callsInPlace(ifNonError, InvocationKind.EXACTLY_ONCE)
        }
        this?.recordErrorTypes(recorder)
        return ifNonError()
    }

    private fun PsiType.recordErrorTypes(
        recorder: UnresolvedQualifiersRecorder  // TODO: convert to context after fix of KT-54197
    ) {
        if (qualifiedNameOrNull == null) {
            recorder.recordUnresolvedQualifier(qualifiedName)
        }
        when (this) {
            is PsiClassType -> typeArguments().forEach { (it as? PsiType)?.recordErrorTypes(recorder) }
            is PsiArrayType -> componentType.recordErrorTypes(recorder)
        }
    }

    // TODO
    private fun getClassName(lightClass: PsiClass, isDefaultImpls: Boolean, packageFqName: String): String {
        return lightClass.name!!
//        return when (descriptor) {
//            is PackageFragmentDescriptor -> {
//                val className = if (packageFqName.isEmpty()) lightClass.name else lightClass.name.drop(packageFqName.length + 1)
//                if (className.isEmpty()) throw IllegalStateException("Invalid package facade class name: ${lightClass.name}")
//                className
//            }
//
//            else -> if (isDefaultImpls) "DefaultImpls" else descriptor.name.asString()
//        }
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

    private class ClassGenericSignature(
        val typeParameters: JavacList<JCTypeParameter>,
        val superClass: JCExpression,
        val interfaces: JavacList<JCExpression>,
        val superClassIsObject: Boolean
    )

    // TODO: convert to context after fix of KT-54197
    private fun UnresolvedQualifiersRecorder.parseClassSignature(psiClass: PsiClass): ClassGenericSignature {
        val superClasses = mutableListOf<JCExpression>()
        val superInterfaces = mutableListOf<JCExpression>()

        val superPsiClasses = psiClass.extendsListTypes.toList()
        val superPsiInterfaces = psiClass.implementsListTypes.toList()

        fun addSuperType(superType: PsiClassType, destination: MutableList<JCExpression>) {
            if (psiClass.isAnnotationType && superType.qualifiedName == "java.lang.annotation.Annotation") return
            destination += superType.convertAndRecordErrors(this)
        }

        var superClassIsObject = false

        superPsiClasses.forEach {
            addSuperType(it, superClasses)
            superClassIsObject = superClassIsObject || it.qualifiedNameOrNull == "java.lang.Object"
        }
        superPsiInterfaces.forEach { addSuperType(it, superInterfaces) }

        val jcTypeParameters = mapJList(psiClass.typeParameters) { convertTypeParameter(it) }
        val jcSuperClass = superClasses.firstOrNull() ?: createJavaLangObjectType().also {
            superClassIsObject = true
        }
        val jcInterfaces = JavacList.from(superInterfaces)
        return ClassGenericSignature(jcTypeParameters, jcSuperClass, jcInterfaces, superClassIsObject)
    }

    private fun createJavaLangObjectType(): JCExpression {
        return treeMaker.FqName("java.lang.Object")
    }

    // TODO: convert to context after fix of KT-54197
    private fun UnresolvedQualifiersRecorder.convertTypeParameter(typeParameter: PsiTypeParameter): JCTypeParameter {
        val classBounds = mutableListOf<JCExpression>()
        val interfaceBounds = mutableListOf<JCExpression>()

        val bounds = typeParameter.bounds
        for (bound in bounds) {
            val boundType = bound as? PsiType ?: continue
            val jBound = boundType.convertAndRecordErrors(this)
            if (boundType.resolvedClass?.isInterface == false) {
                classBounds += jBound
            } else {
                interfaceBounds += jBound
            }
        }
        if (classBounds.isEmpty() && interfaceBounds.isEmpty()) {
            classBounds += createJavaLangObjectType()
        }
        return treeMaker.TypeParameter(treeMaker.name(typeParameter.name!!), JavacList.from(classBounds + interfaceBounds))
    }

    private class UnresolvedQualifiersRecorder(ktFile: KtFile) {
        val importsFromRoot: Set<String> by lazy {
            val importsFromRoot =
                ktFile.importDirectives
                    .filter { !it.isAllUnder }
                    .mapNotNull { im -> im.importPath?.fqName?.takeIf { it.isOneSegmentFQN() } }
            importsFromRoot.mapTo(mutableSetOf()) { it.asString() }
        }

        private val _qualifiedNames = mutableSetOf<String>()
        private val _simpleNames = mutableSetOf<String>()

        val qualifiedNames: Set<String>
            get() = _qualifiedNames
        val simpleNames: Set<String>
            get() = _simpleNames

        fun isEmpty(): Boolean {
            return simpleNames.isEmpty()
        }

        fun recordUnresolvedQualifier(qualifier: String) {
            val separated = qualifier.split(".")
            if (separated.size > 1) {
                _qualifiedNames += qualifier
                _simpleNames += separated.first()
            } else {
                _simpleNames += qualifier
            }
        }
    }
}

