/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.kapt4

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.intellij.psi.*
import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.code.TypeTag
import com.sun.tools.javac.parser.Tokens
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.JCTree.*
import kotlinx.kapt.KaptIgnored
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.base.kapt3.KaptFlag
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.kapt3.base.javac.kaptError
import org.jetbrains.kotlin.kapt3.base.javac.reportKaptError
import org.jetbrains.kotlin.kapt3.base.stubs.KaptStubLineInformation
import org.jetbrains.kotlin.kapt3.base.util.TopLevelJava9Aware
import org.jetbrains.kotlin.kapt3.stubs.MemberData
import org.jetbrains.kotlin.kapt3.stubs.MembersPositionComparator
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForNamedClassLike
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.*
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.isOneSegmentFQN
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.util.getCalleeExpressionIfAny
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.runUnless
import org.jetbrains.kotlin.utils.toMetadataVersion
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import java.io.File
import javax.lang.model.element.ElementKind
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.sign

context(Kapt4ContextForStubGeneration)
internal class Kapt4StubGenerator {
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
            "java.lang.Synthetic",
            "synthetic.kotlin.jvm.GeneratedByJvmOverloads" // kapt3-related annotation for marking JvmOverloads-generated methods
        )

        private val KOTLIN_METADATA_ANNOTATION = Metadata::class.java.name

        private val JAVA_KEYWORD_FILTER_REGEX = "[a-z]+".toRegex()

        @Suppress("UselessCallOnNotNull") // nullable toString(), KT-27724
        private val JAVA_KEYWORDS = Tokens.TokenKind.values()
            .filter { JAVA_KEYWORD_FILTER_REGEX.matches(it.toString().orEmpty()) }
            .mapTo(hashSetOf(), Any::toString)
    }

    private val strictMode = options[KaptFlag.STRICT]
    private val stripMetadata = options[KaptFlag.STRIP_METADATA]
    private val keepKdocComments = options[KaptFlag.KEEP_KDOC_COMMENTS_IN_STUBS]
    private val dumpDefaultParameterValues = options[KaptFlag.DUMP_DEFAULT_PARAMETER_VALUES]

    private val kdocCommentKeeper = runIf(keepKdocComments) { Kapt4KDocCommentKeeper(this@Kapt4ContextForStubGeneration) }

    internal fun generateStubs(): Map<KtLightClass, KaptStub?> {
        return classes.associateWith { convertTopLevelClass(it) }
    }

    private fun convertTopLevelClass(lightClass: KtLightClass): KaptStub? {
        val ktFiles = when(lightClass) {
            is KtLightClassForFacade -> lightClass.files
            else -> listOfNotNull(lightClass.kotlinOrigin?.containingKtFile)
        }
        val lineMappings = Kapt4LineMappingCollector()
        val packageName = (lightClass.parent as? PsiJavaFile)?.packageName ?: return null
        val packageClause = runUnless(packageName.isBlank()) { treeMaker.FqName(packageName) }

        val unresolvedQualifiersRecorder = UnresolvedQualifiersRecorder(ktFiles)
        val classDeclaration = with(unresolvedQualifiersRecorder) {
            convertClass(lightClass, lineMappings, packageName) ?: return null
        }

        val classes = JavacList.of<JCTree>(classDeclaration)

        // imports should be collected after class conversion to
        val imports = ktFiles.fold(JavacList.nil<JCTree>()) { acc, file ->
            acc.appendList(convertImports(file, unresolvedQualifiersRecorder))
        }

        val topLevel = treeMaker.TopLevelJava9Aware(packageClause, imports + classes)
        if (kdocCommentKeeper != null) {
            topLevel.docComments = kdocCommentKeeper.getDocTable(topLevel)
        }

        return KaptStub(topLevel, lineMappings.serialize())
    }

    context(UnresolvedQualifiersRecorder)
    private fun convertClass(
        lightClass: PsiClass,
        lineMappings: Kapt4LineMappingCollector,
        packageFqName: String
    ): JCClassDecl? {
        if (!checkIfValidTypeName(lightClass, lightClass.defaultType)) return null

        val parentClass = lightClass.parent as? PsiClass

        val flags = if ((parentClass?.isInterface == true || parentClass?.isAnnotationType == true) && !lightClass.isPublic)
            (lightClass.accessFlags and Flags.PRIVATE.toLong().inv()) else lightClass.accessFlags

        val metadata = calculateMetadata(lightClass)

        val isEnum = lightClass.isEnum
        val modifiers = convertModifiers(
            lightClass,
            flags,
            if (isEnum) ElementKind.ENUM else ElementKind.CLASS,
            packageFqName,
            lightClass.annotations.toList(),
            metadata,
        )

        val simpleName = lightClass.name!!
        if (!isValidIdentifier(simpleName)) return null

        val classSignature = parseClassSignature(lightClass)

        val enumValues: JavacList<JCTree> = mapJList(lightClass.fields) { field ->
            if (field !is PsiEnumConstant) return@mapJList null
            val constructorArguments = lightClass.constructors.firstOrNull()?.parameters?.mapNotNull { it.type as? PsiType }.orEmpty()
            val args = mapJList(constructorArguments) { convertLiteralExpression(getDefaultValue(it)) }

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

            convertMethod(method, lightClass, lineMappings, packageFqName)?.also {
                methodsPositions[it] = MemberData(method.name, method.signature, lineMappings.getPosition(lightClass, method))
            }
        }

        val nestedClasses = mapJList(lightClass.innerClasses) { innerClass ->
            convertClass(innerClass, lineMappings, packageFqName)
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

    private fun convertImports(file: KtFile, unresolvedQualifiers: UnresolvedQualifiersRecorder): JavacList<JCTree> {
        if (unresolvedQualifiers.isEmpty()) return JavacList.nil()

        val imports = mutableListOf<JCImport>()
        val importedShortNames = mutableSetOf<String>()

        // We prefer ordinary imports over aliased ones.
        val sortedImportDirectives = file.importDirectives.partition { it.aliasName == null }.run { first + second }

        loop@ for (importDirective in sortedImportDirectives) {
            val acceptableByName = when {
                importDirective.isAllUnder -> unresolvedQualifiers.simpleNames.isNotEmpty()
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

    private fun convertMetadataAnnotation(metadata: Metadata): JCAnnotation {
        val argumentsWithNames = mutableMapOf<String, Any>()
        if (metadata.kind != 1) argumentsWithNames[KIND_FIELD_NAME] = metadata.kind
        argumentsWithNames[METADATA_VERSION_FIELD_NAME] = metadata.metadataVersion.toList()
        if (metadata.data1.isNotEmpty()) argumentsWithNames[METADATA_DATA_FIELD_NAME] = metadata.data1.toList()
        if (metadata.data2.isNotEmpty()) argumentsWithNames[METADATA_STRINGS_FIELD_NAME] = metadata.data2.toList()
        if (metadata.extraString.isNotEmpty()) argumentsWithNames[METADATA_EXTRA_STRING_FIELD_NAME] = metadata.extraString
        if (metadata.packageName.isNotEmpty()) argumentsWithNames[METADATA_PACKAGE_NAME_FIELD_NAME] = metadata.packageName
        if (metadata.extraInt != 0) argumentsWithNames[METADATA_EXTRA_INT_FIELD_NAME] = metadata.extraInt

        val arguments = argumentsWithNames.map { (name, value) ->
            val jValue = convertLiteralExpression(value)
            treeMaker.Assign(treeMaker.SimpleName(name), jValue)
        }
        return treeMaker.Annotation(treeMaker.FqName(Metadata::class.java.canonicalName), JavacList.from(arguments))
    }

    context(UnresolvedQualifiersRecorder)
    private fun convertAnnotation(
        containingClass: PsiClass,
        annotation: PsiAnnotation,
        packageFqName: String
    ): JCAnnotation? {
        val rawQualifiedName = annotation.qualifiedName ?: return null
        val fqName = treeMaker.getQualifiedName(rawQualifiedName)

        if (BLACKLISTED_ANNOTATIONS.any { fqName.startsWith(it) }) return null
        if (stripMetadata && fqName == KOTLIN_METADATA_ANNOTATION) return null


        val annotationFqName = annotation.resolveAnnotationType()?.defaultType.convertAndRecordErrors {
            val useSimpleName = '.' in fqName && fqName.substringBeforeLast('.', "") == packageFqName

            when {
                useSimpleName -> treeMaker.FqName(fqName.substring(packageFqName.length + 1))
                else -> treeMaker.FqName(fqName)
            }
        }

        val values = mapJList<_, JCExpression>(annotation.parameterList.attributes) {
            val name = it.name?.takeIf { name -> isValidIdentifier(name) } ?: return@mapJList null
            val value = it.value
            val expr = if (value == null) {
                ((it as? KtLightElementBase)?.kotlinOrigin as? KtDotQualifiedExpression)?.let { convertDotQualifiedExpression(it) }
                    ?: return@mapJList null
            } else {
                convertPsiAnnotationMemberValue(containingClass, value, packageFqName)
            }
            treeMaker.Assign(treeMaker.SimpleName(name), expr)
        }

        return treeMaker.Annotation(annotationFqName, values)
    }

    private fun convertDotQualifiedExpression(dotQualifiedExpression: KtDotQualifiedExpression): JCExpression? {
        val qualifier = dotQualifiedExpression.lastChild as? KtNameReferenceExpression ?: return null
        val name = qualifier.text.takeIf { isValidIdentifier(it) } ?: "InvalidFieldName"
        val lhs = when(val left = dotQualifiedExpression.firstChild) {
            is KtNameReferenceExpression -> treeMaker.SimpleName(left.getReferencedName())
            is KtDotQualifiedExpression -> convertDotQualifiedExpression(left) ?: return null
            else -> return null
        }
        return treeMaker.Select(lhs, treeMaker.name(name))
    }

    context(UnresolvedQualifiersRecorder)
    private fun convertPsiAnnotationMemberValue(
        containingClass: PsiClass,
        value: PsiAnnotationMemberValue,
        packageFqName: String,
    ): JCExpression? {
        return when (value) {
            is PsiArrayInitializerMemberValue -> {
                val arguments = mapJList(value.initializers) {
                    convertPsiAnnotationMemberValue(containingClass, it, packageFqName)
                }
                treeMaker.NewArray(null, null, arguments)
            }

            is PsiLiteral -> convertLiteralExpression(value.value)
            is PsiClassObjectAccessExpression -> {
                val type = value.operand.type
                checkIfValidTypeName(containingClass, type)
                treeMaker.Select(treeMaker.SimpleName(type.qualifiedName), treeMaker.name("class"))
            }
            is PsiAnnotation -> convertAnnotation(containingClass, value, packageFqName)
            else -> treeMaker.SimpleName(value.text)
        }
    }

    context(UnresolvedQualifiersRecorder)
    private fun convertModifiers(
        containingClass: PsiClass,
        access: Long,
        kind: ElementKind,
        packageFqName: String,
        allAnnotations: List<PsiAnnotation>,
        metadata: Metadata?,
        excludeNullabilityAnnotations: Boolean = false,
    ): JCModifiers {
        var seenDeprecated = false
        fun convertAndAdd(list: JavacList<JCAnnotation>, annotation: PsiAnnotation): JavacList<JCAnnotation> {
            seenDeprecated = seenDeprecated or annotation.hasQualifiedName("java.lang.Deprecated")
            if (excludeNullabilityAnnotations &&
                (annotation.hasQualifiedName("org.jetbrains.annotations.NotNull") || annotation.hasQualifiedName("org.jetbrains.annotations.Nullable"))
            ) return list
            val annotationTree = convertAnnotation(containingClass, annotation, packageFqName) ?: return list
            return list.prepend(annotationTree)
        }

        var annotations = allAnnotations.reversed().fold(JavacList.nil(), ::convertAndAdd)

        if (!seenDeprecated && isDeprecated(access)) {
            val type = treeMaker.RawType(Type.getType(java.lang.Deprecated::class.java))
            annotations = annotations.append(treeMaker.Annotation(type, JavacList.nil()))
        }
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

    class KaptStub(val file: JCCompilationUnit, private val kaptMetadata: ByteArray) {
        fun writeMetadataIfNeeded(forSource: File): File {
            val metadataFile = File(
                forSource.parentFile,
                forSource.nameWithoutExtension + KaptStubLineInformation.KAPT_METADATA_EXTENSION
            )

            metadataFile.writeBytes(kaptMetadata)
            return metadataFile
        }
    }

    context(UnresolvedQualifiersRecorder)
    private fun convertField(
        field: PsiField,
        containingClass: PsiClass,
        lineMappings: Kapt4LineMappingCollector,
        packageFqName: String,
        explicitInitializer: JCExpression? = null
    ): JCVariableDecl? {
        val fieldAnnotations = field.annotations.asList()

        if (isIgnored(fieldAnnotations)) return null

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

        if (!checkIfValidTypeName(containingClass, type)) return null

        // Enum type must be an identifier (Javac requirement)
        val typeExpression = if (isEnum(access)) {
            treeMaker.SimpleName(treeMaker.getQualifiedName(type as PsiClassType).substringAfterLast('.'))
        } else {
            type.convertAndRecordErrors()
        }

        lineMappings.registerField(containingClass, field)
        val skip = field.navigationElement is KtParameter && !dumpDefaultParameterValues
        val initializer =
            explicitInitializer ?: convertPropertyInitializer(if (skip) null else field.initializer, field.type, field.isFinal)
        return treeMaker.VarDef(modifiers, treeMaker.name(name), typeExpression, initializer).keepKdocCommentsIfNecessary(field)
    }

    private fun convertPropertyInitializer(propertyInitializer: PsiExpression?, type: PsiType, usedDefault: Boolean): JCExpression? {
        if (propertyInitializer != null || usedDefault) {
            return when (propertyInitializer) {
                is PsiLiteralExpression -> {
                    val rawValue = propertyInitializer.value
                    val rawNumberValue = rawValue as? Number
                    val actualValue = when (type) {
                        PsiType.BYTE -> rawNumberValue?.toByte()
                        PsiType.SHORT -> rawNumberValue?.toShort()
                        PsiType.INT -> rawNumberValue?.toInt()
                        PsiType.LONG -> rawNumberValue?.toLong()
                        PsiType.FLOAT -> rawNumberValue?.toFloat()
                        PsiType.DOUBLE -> rawNumberValue?.toDouble()
                        else -> null
                    } ?: rawValue
                    convertValueOfPrimitiveTypeOrString(actualValue)
                }
                is PsiPrefixExpression -> {
                    assert(propertyInitializer.operationSign.tokenType == JavaTokenType.MINUS)
                    val operand = convertPropertyInitializer(propertyInitializer.operand, type, usedDefault)
                    if (operand.toString().startsWith("-")) operand // overflow
                    else treeMaker.Unary(Tag.NEG, operand)
                }
                is PsiBinaryExpression -> {
                    assert(propertyInitializer.operationSign.tokenType == JavaTokenType.DIV)
                    treeMaker.Binary(
                        Tag.DIV,
                        convertPropertyInitializer(propertyInitializer.lOperand, type, false),
                        convertPropertyInitializer(propertyInitializer.rOperand, type, false)
                    )
                }
                is PsiReferenceExpression ->
                    when (val resolved = propertyInitializer.resolve()) {
                        is PsiEnumConstant ->
                            treeMaker.FqName(resolved.containingClass!!.qualifiedName + "." + resolved.name)
                        else -> null
                    }
                is PsiArrayInitializerExpression ->
                    treeMaker.NewArray(
                        null, JavacList.nil(),
                        mapJList(propertyInitializer.initializers) { convertPropertyInitializer(it, type.deepComponentType, false) }
                    )
                else -> convertLiteralExpression(getDefaultValue(type))
            }
        }

        return null
    }

    private fun convertLiteralExpression(value: Any?): JCExpression {
        fun convertDeeper(value: Any?) = convertLiteralExpression(value)

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
            null -> treeMaker.Literal(TypeTag.BOT, null)
            else -> null
        }

        return convertedValue
    }

    context(UnresolvedQualifiersRecorder)
    private fun convertMethod(
        method: PsiMethod,
        containingClass: PsiClass,
        lineMappings: Kapt4LineMappingCollector,
        packageFqName: String,
    ): JCMethodDecl? {
        if (isIgnored(method.annotations.asList())) return null

        val isConstructor = method.isConstructor

        val name = method.name
        if (!isConstructor && !isValidIdentifier(name)) return null
        val returnType = method.returnType ?: PsiType.VOID
        val modifiers = convertModifiers(
            containingClass,
            if (containingClass.isEnum && isConstructor)
                (method.accessFlags and VISIBILITY_MODIFIERS.inv())
            else
                method.accessFlags,
            ElementKind.METHOD,
            packageFqName,
            method.annotations.toList(),
            metadata = null,
            excludeNullabilityAnnotations = returnType == PsiType.VOID
        )

        if (method.hasModifierProperty(PsiModifier.DEFAULT)) {
            modifiers.flags = modifiers.flags or Flags.DEFAULT
        }

        val parametersInfo = method.getParametersInfo()

        if (!checkIfValidTypeName(containingClass, returnType)
            || parametersInfo.any { !checkIfValidTypeName(containingClass, it.type) }
        ) {
            return null
        }

        @Suppress("NAME_SHADOWING")
        val jParameters = mapJListIndexed(parametersInfo) { index, info ->
            val lastParameter = index == parametersInfo.lastIndex
            val isArrayType = info.type is PsiArrayType

            val varargs = if (lastParameter && isArrayType && method.isVarArgs) Flags.VARARGS else 0L
            val modifiers = convertModifiers(
                containingClass,
                Flags.PARAMETER or varargs, // Kapt never marked method parameters as "final"
                ElementKind.PARAMETER,
                packageFqName,
                info.annotations,
                metadata = null
            )

            val defaultName = info.name
            val name = when {
                isValidIdentifier(defaultName) -> defaultName
                defaultName == SpecialNames.IMPLICIT_SET_PARAMETER.asString() -> "p0"
                else -> "p${index}_${info.name.hashCode().ushr(1)}"
            }
            val type = info.type.convertAndRecordErrors()
            treeMaker.VarDef(modifiers, treeMaker.name(name), type, null)
        }
        val jTypeParameters = mapJList(method.typeParameters) { convertTypeParameter(it) }
        val jExceptionTypes = mapJList(method.throwsTypes) { treeMaker.TypeWithArguments(it as PsiType) }
        val jReturnType = runUnless(isConstructor) {
            returnType.convertAndRecordErrors()
        }

        val defaultValue = (method as? PsiAnnotationMethod)?.defaultValue?.let {
            convertPsiAnnotationMemberValue(containingClass, it, packageFqName)
        }

        val body = if (defaultValue != null) {
            null
        } else if (method.isAbstract or (modifiers.flags and Flags.ABSTRACT.toLong() != 0L)) {
            null
        } else if (isConstructor && containingClass.isEnum) {
            treeMaker.Block(0, JavacList.nil())
        } else if (isConstructor) {
            val superConstructor = containingClass.superClass?.constructors?.firstOrNull { !it.isPrivate }
            val superClassConstructorCall = if (superConstructor != null) {
                val args = mapJList(superConstructor.parameterList.parameters) { param ->
                    convertLiteralExpression(getDefaultValue(param.type))
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
            val returnStatement = treeMaker.Return(convertLiteralExpression(getDefaultValue(returnType)))
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

    context(UnresolvedQualifiersRecorder)
    private fun checkIfValidTypeName(
        containingClass: PsiClass,
        type: PsiType
    ): Boolean {
        when (type) {
            is PsiArrayType -> return checkIfValidTypeName(containingClass, type.componentType)
            is PsiPrimitiveType -> return true
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

    context(UnresolvedQualifiersRecorder)
    private fun reportIfIllegalTypeUsage(
        containingClass: PsiClass,
        type: PsiType
    ) {
        val typeName = type.simpleNameOrNull ?: return
        if (typeName !in reportedTypes && typeName in importsFromRoot) {
            reportedTypes += typeName
            val msg = "${containingClass.qualifiedName}: Can't reference type '${typeName}' from default package in Java stub."
            if (strictMode) reportKaptError(msg)
            else logger.warn(msg)
        }
    }

    context(UnresolvedQualifiersRecorder)
    @OptIn(ExperimentalContracts::class)
    private inline fun PsiType?.convertAndRecordErrors(
        ifNonError: () -> JCExpression = { treeMaker.TypeWithArguments(this!!) }
    ): JCExpression {
        contract {
            callsInPlace(ifNonError, InvocationKind.EXACTLY_ONCE)
        }
        this?.recordErrorTypes()
        return ifNonError()
    }

    context(UnresolvedQualifiersRecorder)
    private fun PsiType.recordErrorTypes() {
        if (this is PsiEllipsisType) {
            this.componentType.recordErrorTypes()
            return
        }
        if (qualifiedNameOrNull == null) {
            recordUnresolvedQualifier(qualifiedName)
        }
        when (this) {
            is PsiClassType -> typeArguments().forEach { (it as? PsiType)?.recordErrorTypes() }
            is PsiArrayType -> componentType.recordErrorTypes()
        }
    }

    private fun isValidQualifiedName(name: FqName) = name.pathSegments().all { isValidIdentifier(it.asString()) }

    private fun isValidIdentifier(name: String): Boolean {
        if (name in JAVA_KEYWORDS) return false

        return !(name.isEmpty()
                || !Character.isJavaIdentifierStart(name[0])
                || name.drop(1).any { !Character.isJavaIdentifierPart(it) })
    }

    private class ClassGenericSignature(
        val typeParameters: JavacList<JCTypeParameter>,
        val superClass: JCExpression,
        val interfaces: JavacList<JCExpression>,
        val superClassIsObject: Boolean
    )

    context(UnresolvedQualifiersRecorder)
    private fun parseClassSignature(psiClass: PsiClass): ClassGenericSignature {
        val superClasses = mutableListOf<JCExpression>()
        val superInterfaces = mutableListOf<JCExpression>()

        val superPsiClasses = psiClass.extendsListTypes.toList()
        val superPsiInterfaces = psiClass.implementsListTypes.toList()

        fun addSuperType(superType: PsiClassType, destination: MutableList<JCExpression>) {
            if (psiClass.isAnnotationType && superType.qualifiedName == "java.lang.annotation.Annotation") return
            destination += superType.convertAndRecordErrors()
        }

        var superClassIsObject = false

        superPsiClasses.forEach {
            addSuperType(it, superClasses)
            superClassIsObject = superClassIsObject || it.qualifiedNameOrNull == "java.lang.Object"
        }
        for (superInterface in superPsiInterfaces) {
            if (superInterface.qualifiedName.startsWith("kotlin.collections.")) continue
            addSuperType(superInterface, superInterfaces)
        }

        val jcTypeParameters = mapJList(psiClass.typeParameters) { convertTypeParameter(it) }
        val jcSuperClass = superClasses.firstOrNull().takeUnless { psiClass.isInterface } ?: createJavaLangObjectType().also {
            superClassIsObject = true
        }
        val jcInterfaces = JavacList.from(if (psiClass.isInterface) superClasses else superInterfaces)
        return ClassGenericSignature(jcTypeParameters, jcSuperClass, jcInterfaces, superClassIsObject)
    }

    private fun createJavaLangObjectType(): JCExpression {
        return treeMaker.FqName("java.lang.Object")
    }

    context(UnresolvedQualifiersRecorder)
    private fun convertTypeParameter(typeParameter: PsiTypeParameter): JCTypeParameter {
        val classBounds = mutableListOf<JCExpression>()
        val interfaceBounds = mutableListOf<JCExpression>()

        val bounds = typeParameter.bounds
        for (bound in bounds) {
            val boundType = bound as? PsiType ?: continue
            val jBound = boundType.convertAndRecordErrors()
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

    private class UnresolvedQualifiersRecorder(ktFiles: Iterable<KtFile>) {
        val importsFromRoot: Set<String> by lazy {
            val importsFromRoot =
                ktFiles
                    .flatMap { it.importDirectives }
                    .filter { !it.isAllUnder }
                    .mapNotNull { im -> im.importPath?.fqName?.takeIf { it.isOneSegmentFQN() } }
            importsFromRoot.mapTo(mutableSetOf()) { it.asString() }
        }

        private val _qualifiedNames = mutableSetOf<String>()
        private val _simpleNames = mutableSetOf<String>()
        val reportedTypes = mutableSetOf<String>()

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

    private fun calculateMetadata(lightClass: PsiClass): Metadata? {
        if (stripMetadata) return null

        return with(analysisSession) {
            when (lightClass) {
                is KtLightClassForFacade ->
                    if (lightClass.multiFileClass)
                        lightClass.qualifiedName?.let { createMultifileClassMetadata(lightClass, it) }
                    else
                        lightClass.files.singleOrNull()?.calculateMetadata(elementMapping(lightClass))
                is SymbolLightClassForNamedClassLike ->
                    lightClass.kotlinOrigin?.calculateMetadata(elementMapping(lightClass))
                else -> null
            }
        }
    }

    private fun createMultifileClassMetadata(lightClass: KtLightClassForFacade, qualifiedName: String): Metadata =
        Metadata(
            kind = KotlinClassHeader.Kind.MULTIFILE_CLASS.id,
            metadataVersion = LanguageVersion.KOTLIN_2_0.toMetadataVersion().toArray(),
            data1 = lightClass.files.map {
                JvmFileClassUtil.manglePartName(qualifiedName.replace('.', '/'), it.name)
            }.toTypedArray(),
            extraInt = METADATA_JVM_IR_FLAG or METADATA_FIR_FLAG or METADATA_JVM_IR_STABLE_ABI_FLAG
        )

    private fun elementMapping(lightClass: PsiClass): Multimap<KtElement, PsiElement> =
        HashMultimap.create<KtElement, PsiElement>().apply {
            (lightClass.methods.asSequence() + lightClass.fields.asSequence() + lightClass.constructors.asSequence()).forEach {
                put((it as KtLightElement<*, *>).kotlinOrigin, it)
            }
        }
}
