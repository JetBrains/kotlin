/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.kapt4

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.intellij.lang.ASTNode
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.elements.*
import org.jetbrains.kotlin.asJava.findFacadeClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.kapt3.base.KaptFlag
import org.jetbrains.kotlin.kapt3.base.stubs.KaptStubLineInformation
import org.jetbrains.kotlin.kapt3.stubs.MemberData
import org.jetbrains.kotlin.kapt3.stubs.extractComment
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForNamedClassLike
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.*
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parameterIndex
import org.jetbrains.kotlin.resolve.calls.util.getCalleeExpressionIfAny
import org.jetbrains.kotlin.utils.toMetadataVersion
import org.jetbrains.kotlin.kapt3.base.KaptOptions
import org.jetbrains.kotlin.kapt3.stubs.MembersPositionComparator
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.utils.Printer
import java.io.File

internal fun generateStubs(
    files: List<KtFile>,
    options: KaptOptions,
    onError: (messages: String) -> Unit,
    analysisSession: KtAnalysisSession,
    overriddenMetadataVersion: BinaryVersion? = null,
    metadataRenderer: (Printer.(Metadata) -> Unit)? = null
): Map<KtLightClass, KaptStub?> =
    StubGenerator(files, options, onError, analysisSession, metadataRenderer, overriddenMetadataVersion).generateStubs()

class KaptStub(val source: String, val kaptMetadata: ByteArray) {
    fun writeMetadata(forSource: File) {
        File(forSource.parentFile, forSource.nameWithoutExtension + KaptStubLineInformation.KAPT_METADATA_EXTENSION)
            .apply { writeBytes(kaptMetadata) }
    }
}

private class StubGenerator(
    private val files: List<KtFile>,
    options: KaptOptions,
    private val onError: (String) -> Unit,
    private val analysisSession: KtAnalysisSession,
    private val metadataRenderer: (Printer.(Metadata) -> Unit)? = null,
    private val overriddenMetadataVersion: BinaryVersion? = null,
) {
    private val strictMode = options[KaptFlag.STRICT]
    private val stripMetadata = options[KaptFlag.STRIP_METADATA]
    private val keepKdocComments = options[KaptFlag.KEEP_KDOC_COMMENTS_IN_STUBS]
    private val dumpDefaultParameterValues = options[KaptFlag.DUMP_DEFAULT_PARAMETER_VALUES]


    fun generateStubs(): Map<KtLightClass, KaptStub?> =
        buildSet {
            files.flatMapTo(this) { file ->
                file.children.filterIsInstance<KtClassOrObject>().mapNotNull {
                    it.toLightClass()
                }
            }
            files.mapNotNullTo(this) { ktFile -> ktFile.findFacadeClass() }
        }.associateWith {
            FileGenerator(it).generateStub()
        }


    private inner class FileGenerator(private val topLevelClass: KtLightClass) {
        private val packageName = (topLevelClass.parent as PsiJavaFile).packageName
        private val lineMappings = Kapt4LineMappingCollector()
        private val ktFiles = when (topLevelClass) {
            is KtLightClassForFacade -> topLevelClass.files
            else -> listOfNotNull(topLevelClass.kotlinOrigin?.containingKtFile)
        }
        private val importsFromRoot: Set<String> by lazy {
            ktFiles.flatMap { it.importDirectives }
                .filter { !it.isAllUnder }
                .mapNotNull { im -> im.importPath?.fqName?.takeIf { it.isOneSegmentFQN() }?.asString() }
                .toSet()
        }

        private val unresolvedQualifiedNames = mutableSetOf<String>()
        private val unresolvedSimpleNames = mutableSetOf<String>()
        private val reportedTypes = mutableSetOf<String>()

        private fun recordUnresolvedQualifier(qualifier: String) {
            val separated = qualifier.split(".")
            if (separated.size > 1) {
                unresolvedQualifiedNames += qualifier
                unresolvedSimpleNames += separated.first()
            } else {
                unresolvedSimpleNames += qualifier
            }
        }

        fun generateStub(): KaptStub? {
            val ktFiles = when (topLevelClass) {
                is KtLightClassForFacade -> topLevelClass.files
                else -> listOfNotNull(topLevelClass.kotlinOrigin?.containingKtFile)
            }

            val classBody = with(ClassGenerator(topLevelClass)) {
                printToString { printClass() }
            }

            if (classBody.isEmpty()) return null

            val stub = printToString {
                if (packageName.isNotEmpty()) {
                    printWithNoIndent("package ", packageName, ";\n\n")
                }
                ktFiles.forEach { printImports(it) }
                printWithNoIndent(classBody)
            }

            return KaptStub(stub, lineMappings.serialize())
        }

        private fun Printer.printImports(file: KtFile) {
            if (unresolvedSimpleNames.isEmpty()) return

            val importedShortNames = mutableSetOf<String>()

            // We prefer ordinary imports over aliased ones.
            val sortedImportDirectives = file.importDirectives.partition { it.aliasName == null }.run { first + second }

            for (importDirective in sortedImportDirectives) {
                val acceptableByName = when {
                    importDirective.isAllUnder -> unresolvedSimpleNames.isNotEmpty()
                    else -> {
                        val fqName = importDirective.importedFqName ?: continue
                        fqName.asString() in unresolvedQualifiedNames || fqName.shortName().identifier in unresolvedSimpleNames
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
                printWithNoIndent("import ")
                when {
                    importDirective.isAllUnder -> printWithNoIndent(importedFqName.asString(), ".*")
                    importedShortNames.add(importedFqName.shortName().asString()) -> printWithNoIndent(importedFqName)
                }
                printlnWithNoIndent(";")
            }
        }

        private inner class ClassGenerator(private val psiClass: PsiClass) {
            fun Printer.printClass() {
                val simpleName = psiClass.name ?: return
                if (!isValidIdentifier(simpleName)) return
                if (!checkIfValidTypeName(psiClass.defaultType)) return
                lineMappings.registerClass(psiClass)

                printComment(psiClass)

                val classWord = when {
                    psiClass.isAnnotationType -> "@interface"
                    psiClass.isInterface -> "interface"
                    psiClass.isEnum -> "enum"
                    else -> "class"
                }
                calculateMetadata(psiClass)?.let { printMetadata(it) }
                printIndent()
                printModifiers(psiClass)
                printWithNoIndent(classWord, " ", simpleName)
                printTypeParams(psiClass.typeParameters)

                psiClass.extendsList
                    ?.referencedTypes
                    ?.asList()
                    ?.let { if (!psiClass.isInterface) it.take(1) else it }
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { superClasses ->
                        printWithNoIndent(" extends ")
                        superClasses.forEachIndexed { index, type ->
                            if (index > 0) printWithNoIndent(", ")
                            printType(type)
                        }
                    }

                psiClass.implementsList
                    ?.referencedTypes
                    ?.filterNot { it.canonicalText.startsWith("kotlin.collections.") }
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { interfaces ->
                        printWithNoIndent(" implements ")
                        interfaces.forEachIndexed { index, type ->
                            if (index > 0) printWithNoIndent(", ")
                            printType(type)
                        }
                    }
                printlnWithNoIndent("{")
                pushIndent()

                if (psiClass.isEnum) {
                    val values = psiClass.fields
                        .filterIsInstance<PsiEnumConstant>()
                        .filter { isValidIdentifier(it.name) }
                    values.forEachIndexed { index, value ->
                        value.annotations.forEach {
                            printAnnotation(it, true)
                        }
                        print(value.name)
                        if (index < values.size - 1) printlnWithNoIndent(",")
                    }
                    printlnWithNoIndent(";")
                    printlnWithNoIndent()
                }

                val classPosition = lineMappings.getPosition(psiClass)

                val fieldsPositions = psiClass.fields
                    .filterNot { it is PsiEnumConstant }
                    .onEach { lineMappings.registerField(psiClass, it) }
                    .associateWith { MemberData(it.name, it.signature, lineMappings.getPosition(psiClass, it)) }

                fieldsPositions.keys.sortedWith(MembersPositionComparator(classPosition, fieldsPositions)).forEach {
                    printField(it)
                }

                val methodsPositions = psiClass.methods
                    .filterNot {
                        it.isConstructor && psiClass is PsiEnumConstantInitializer
                                || psiClass.isEnum && it.isSyntheticStaticEnumMethod()
                                || it.hasAnnotation("kotlinx.kapt.KaptIgnored")
                    }
                    .onEach { lineMappings.registerMethod(psiClass, it) }
                    .associateWith { MemberData(it.name, it.signature, lineMappings.getPosition(psiClass, it)) }

                if (fieldsPositions.isNotEmpty() && methodsPositions.isNotEmpty()) printlnWithNoIndent()
                methodsPositions.keys.sortedWith(MembersPositionComparator(classPosition, methodsPositions))
                    .forEach {
                        lineMappings.registerSignature(javacSignature(it), it)
                        printMethod(it)
                    }

                if (psiClass.innerClasses.isNotEmpty() && (fieldsPositions.isNotEmpty() || methodsPositions.isNotEmpty())) println()
                psiClass.innerClasses.forEach {
                    with(ClassGenerator(it)) { printClass() }
                }
                popIndent()
                println("}")
            }

            private fun Printer.printComment(element: PsiElement) {
                if (!keepKdocComments) return
                getKDocComment(element)?.let { comment ->
                    println("/**")
                    comment.split("\n").forEach {
                        println(" * ", it)
                    }
                    println("*/")
                }
            }

            private fun Printer.printField(psiVariable: PsiVariable) {
                if (!isValidIdentifier(psiVariable.name!!)) return
                if ((psiVariable is PsiField) && (psiVariable.containingClass != null) && !checkIfValidTypeName(psiVariable.type)) return

                printComment(psiVariable)
                printModifiers(psiVariable)
                printType(psiVariable.type)
                printWithNoIndent(" ", psiVariable.name)
                if (psiVariable.hasInitializer() && (dumpDefaultParameterValues || psiVariable.navigationElement !is KtParameter)) {
                    printWithNoIndent(" = ", psiVariable.initializer?.text)
                } else if (psiVariable.isFinal) {
                    printWithNoIndent(" = ", defaultValue(psiVariable.type))
                }
                printlnWithNoIndent(";")
                printlnWithNoIndent()
            }

            private fun Printer.printMethod(method: PsiMethod) {
                if (!isValidIdentifier(method.name)) return

                if (method.returnType?.let { checkIfValidTypeName(it) } == false
                    || method.parameterList.parameters.any { !checkIfValidTypeName(it.type) }
                ) return

                printComment(method)
                printModifiers(method)
                printTypeParams(method.typeParameters)

                method.returnType?.let {
                    printType(it)
                    printWithNoIndent(" ")
                }
                printWithNoIndent(method.name, "(")
                method.parameterList.parameters.filter { isValidIdentifier(paramName(it)) }.forEachIndexed { index, param ->
                    if (index > 0) printWithNoIndent(", ")
                    printModifiers(param)
                    printType(param.type)
                    printWithNoIndent(" ", paramName(param))
                }
                printWithNoIndent(")")
                (method as? PsiAnnotationMethod)?.defaultValue?.let {
                    printWithNoIndent(" default ")
                    printAnnotationMemberValue(it)
                }

                method.throwsList.referencedTypes.takeIf { it.isNotEmpty() }?.let { thrownTypes ->
                    printWithNoIndent(" throws ")
                    thrownTypes.forEachIndexed { index, typ ->
                        if (index > 0) printWithNoIndent(", ")
                        printType(typ)
                    }
                }

                if (method.isAbstract) {
                    printlnWithNoIndent(";")
                } else {
                    printlnWithNoIndent(" {")
                    pushIndent()

                    if (method.isConstructor && !psiClass.isEnum) {
                        val superConstructor = method.containingClass?.superClass?.constructors?.firstOrNull { !it.isPrivate }
                        if (superConstructor != null) {
                            print("super(")
                            val args = superConstructor.parameterList.parameters.map { defaultValue(it.type) }
                            args.forEachIndexed { index, arg ->
                                if (index > 0) printWithNoIndent(", ")
                                printWithNoIndent(arg)
                            }
                            printlnWithNoIndent(");")
                        }
                    } else if (method.returnType != null && method.returnType != PsiType.VOID) {
                        println("return ", defaultValue(method.returnType!!), ";")
                    }
                    popIndent()
                    println("}")
                }

                printlnWithNoIndent()
            }

            private fun javacSignature(method: PsiMethod) = printToString {
                print(method.name, "(")
                method.parameterList.parameters.forEachIndexed{ index, parameter ->
                    if (index > 0) print(", ")
                    printTypeSignature(parameter.type)
                }
                print(")")
            }

            private fun reportIfIllegalTypeUsage(
                containingClass: PsiClass,
                type: PsiType,
            ) {
                val typeName = type.simpleNameOrNull ?: return
                if (typeName in importsFromRoot && reportedTypes.add(typeName)) {
                    onError("${containingClass.qualifiedName}: Can't reference type '${typeName}' from default package in Java stub.")
                }
            }

            private fun recordErrorTypes(type: PsiType) {
                if (type is PsiEllipsisType) {
                    recordErrorTypes(type.componentType)
                    return
                }
                if (type.qualifiedNameOrNull == null) {
                    recordUnresolvedQualifier(type.qualifiedName)
                }
                when (type) {
                    is PsiClassType -> type.typeArguments().forEach { (it as? PsiType)?.let { recordErrorTypes(it) } }
                    is PsiArrayType -> recordErrorTypes(type.componentType)
                }
            }

            private fun elementMapping(lightClass: PsiClass): Multimap<KtElement, PsiElement> =
                HashMultimap.create<KtElement, PsiElement>().apply {
                    (lightClass.methods.asSequence() + lightClass.fields.asSequence() + lightClass.constructors.asSequence()).forEach {
                        put((it as KtLightElement<*, *>).kotlinOrigin, it)
                    }
                }

            private fun Printer.printType(type: PsiType) {
                recordErrorTypes(type)
                type.annotations.forEach {
                    printAnnotation(it, false)
                }
                printTypeSignature(type)
            }

            private fun Printer.printTypeSignature(type: PsiType) {
                printWithNoIndent((if (type is PsiClassType && isErroneous(type)) type.rawType() else type).canonicalText.replace('$', '.'))
            }

            private fun Printer.printTypeParams(typeParameters: Array<PsiTypeParameter>) {
                if (typeParameters.isEmpty()) return
                printWithNoIndent("<")
                typeParameters.forEachIndexed { index, param ->
                    if (index > 0) printWithNoIndent(", ")
                    printWithNoIndent(param.name, " extends ")
                    if (param.extendsListTypes.isNotEmpty()) {
                        param.extendsListTypes.forEachIndexed { i, t ->
                            if (i > 0) printWithNoIndent(", ")
                            printType(t)
                        }
                    } else {
                        printWithNoIndent("java.lang.Object")
                    }
                }
                printWithNoIndent(">")
            }

            private fun Printer.printAnnotationMemberValue(psiAnnotationMemberValue: PsiAnnotationMemberValue) {
                when (psiAnnotationMemberValue) {
                    is PsiClassObjectAccessExpression ->
                        psiAnnotationMemberValue.text.takeIf { checkIfValidTypeName(psiAnnotationMemberValue.operand.type) }
                            ?.let { printWithNoIndent(it) }
                    is PsiArrayInitializerMemberValue -> {
                        printWithNoIndent("{")
                        psiAnnotationMemberValue.initializers.forEachIndexed { index, value ->
                            if (index > 0) printWithNoIndent(", ")
                            printAnnotationMemberValue(value)
                        }
                        printWithNoIndent("}")
                    }
                    is PsiAnnotation -> printAnnotation(psiAnnotationMemberValue, false)
                    else -> printWithNoIndent(psiAnnotationMemberValue.text)
                }
            }

            private fun convertDotQualifiedExpression(dotQualifiedExpression: KtDotQualifiedExpression): String? {
                val qualifier = dotQualifiedExpression.lastChild as? KtNameReferenceExpression ?: return null
                val name = qualifier.text.takeIf { isValidIdentifier(it) } ?: "InvalidFieldName"
                val lhs = when (val left = dotQualifiedExpression.firstChild) {
                    is KtNameReferenceExpression -> left.getReferencedName()
                    is KtDotQualifiedExpression -> convertDotQualifiedExpression(left) ?: return null
                    else -> return null
                }
                return "$lhs.$name"
            }

            private fun Printer.printModifiers(modifierListOwner: PsiModifierListOwner) {
                val withIndentation = modifierListOwner !is PsiParameter
                for (annotation in modifierListOwner.annotations) {
                    printAnnotation(annotation, withIndentation)
                }

                if (withIndentation) printIndent()
                if (!(modifierListOwner is PsiMethod && modifierListOwner.isConstructor && modifierListOwner.containingClass?.isEnum == true) && (modifierListOwner !is PsiEnumConstant)) {
                    for (modifier in PsiModifier.MODIFIERS.filter(modifierListOwner::hasModifierProperty)) {
                        if (modifier == PsiModifier.PRIVATE && (modifierListOwner as? PsiMember)?.containingClass?.isInterface == true) continue
                        if ((modifier != PsiModifier.FINAL && modifier != PsiModifier.ABSTRACT) || !(modifierListOwner is PsiClass && modifierListOwner.isEnum)) {
                            printWithNoIndent(modifier, " ")
                        }
                    }
                }
            }

            private fun Printer.printAnnotation(annotation: PsiAnnotation, separateLine: Boolean) {
                fun collectNameParts(node: ASTNode, builder: StringBuilder) {
                    when (node) {
                        is LeafPsiElement ->
                            when (node.elementType) {
                                KtTokens.IDENTIFIER, KtTokens.DOT -> builder.append((node as ASTNode).text)
                            }
                        else -> node.children().forEach { collectNameParts(it, builder) }
                    }
                }

                fun qualifiedName(node: ASTNode): String {
                    val callee = node.children().first { it.elementType == KtNodeTypes.CONSTRUCTOR_CALLEE }
                    return buildString {
                        collectNameParts(callee, this)
                    }
                }

                val rawQualifiedName = when (annotation.qualifiedName) {
                    // A temporary fix for KT-60482
                    "<error>" ->
                        (annotation as? KtLightElement<*, *>)?.kotlinOrigin?.node?.let { qualifiedName(it) }
                            ?.also { recordUnresolvedQualifier(it) }
                    else -> annotation.qualifiedName
                } ?: return


                val qname = if (rawQualifiedName.startsWith(packageName) && rawQualifiedName.lastIndexOf('.') == packageName.length)
                    rawQualifiedName.substring(packageName.length + 1)
                else rawQualifiedName
                if (separateLine) printIndent()
                printWithNoIndent("@", qname, "(")

                annotation.parameterList.attributes
                    .filter { it.name != null && isValidIdentifier(it.name!!) }
                    .forEachIndexed { index, attr ->
                        if (index > 0) printWithNoIndent(", ")
                        printAnnotationAttribute(attr)
                    }
                printWithNoIndent(")")
                if (separateLine) printlnWithNoIndent() else printWithNoIndent(" ")
            }

            private fun Printer.printAnnotationAttribute(attr: PsiNameValuePair) {
                val name = attr.name?.takeIf { isValidIdentifier(it) } ?: return
                val value = if (attr.value == null) {
                    ((attr as? KtLightElementBase)?.kotlinOrigin as? KtDotQualifiedExpression)?.let { convertDotQualifiedExpression(it) }
                } else {
                    when (val v = attr.value!!) {
                        is PsiClassObjectAccessExpression -> v.text.takeIf { checkIfValidTypeName(v.operand.type) }
                        is PsiArrayInitializerMemberValue ->
                            printToString {
                                printWithNoIndent("{")
                                v.initializers.forEachIndexed { index, value ->
                                    if (index > 0) printWithNoIndent(", ")
                                    printAnnotationMemberValue(value)
                                }
                                printWithNoIndent("}")
                            }
                        is PsiAnnotation -> printToString { printAnnotation(v, false) }
                        else -> v.text
                    }
                } ?: return
                printWithNoIndent(name, " = ", value)
            }

            private fun calculateMetadata(lightClass: PsiClass): Metadata? =
                if (stripMetadata) null
                else with(analysisSession) {
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

            private fun createMultifileClassMetadata(lightClass: KtLightClassForFacade, qualifiedName: String): Metadata =
                Metadata(
                    kind = KotlinClassHeader.Kind.MULTIFILE_CLASS.id,
                    metadataVersion = LanguageVersion.KOTLIN_2_0.toMetadataVersion().toArray(),
                    data1 = lightClass.files.map {
                        JvmFileClassUtil.manglePartName(qualifiedName.replace('.', '/'), it.name)
                    }.toTypedArray(),
                    extraInt = METADATA_JVM_IR_FLAG or METADATA_JVM_IR_STABLE_ABI_FLAG
                )

            private fun Printer.printMetadata(m: Metadata) {
                if (metadataRenderer != null) {
                    metadataRenderer.invoke(this, m)
                } else {
                    print("@kotlin.Metadata(k = ", m.kind, ", mv = {")
                    (overriddenMetadataVersion?.toArray() ?: m.metadataVersion).forEachIndexed { index, value ->
                        if (index > 0) printWithNoIndent(", ")
                        printWithNoIndent(value)
                    }
                    printWithNoIndent("}, d1 = {")
                    m.data1.forEachIndexed { i, s ->
                        if (i > 0) printWithNoIndent(", ")
                        printStringLiteral(s)

                    }
                    printWithNoIndent("}, d2 = {")
                    m.data2.forEachIndexed { i, s ->
                        if (i > 0) printWithNoIndent(", ")
                        printStringLiteral(s)

                    }
                    printWithNoIndent("}, xs= ")
                    printStringLiteral(m.extraString)
                    printWithNoIndent(", pn = ")
                    printStringLiteral(m.packageName)
                    printlnWithNoIndent(", xi = ", m.extraInt, ")")
                }
            }

            private fun checkIfValidTypeName(type: PsiType): Boolean {
                when (type) {
                    is PsiArrayType -> return checkIfValidTypeName(type.componentType)
                    is PsiPrimitiveType -> return true
                }

                val internalName = type.qualifiedName
                // Ignore type names with Java keywords in it
                if (internalName.split('/', '.').any { it in JAVA_KEYWORDS }) {
                    if (strictMode) {
                        onError("Can't generate a stub for '${internalName}'.\nType name '${type.qualifiedName}' contains a Java keyword.")
                    }

                    return false
                }

                val clazz = type.resolvedClass ?: return true

                if (doesInnerClassNameConflictWithOuter(clazz)) {
                    if (strictMode) {
                        onError(
                            "Can't generate a stub for '${clazz.qualifiedNameWithDollars}'.\n" +
                                    "Its name '${clazz.name}' is the same as one of the outer class names." +
                                    "\nJava forbids it. Please change one of the class names."
                        )
                    }

                    return false
                }

                reportIfIllegalTypeUsage(psiClass, type)

                return true
            }

            fun Printer.printStringLiteral(s: String) {
                printWithNoIndent('\"')
                s.forEach {
                    printWithNoIndent(
                        when (it) {
                            '\n' -> "\\n"
                            '\r' -> "\\r"
                            '\t' -> "\\t"
                            '"' -> "\\\""
                            '\\' -> "\\\\"
                            else -> if (it.code in 32..128) it else "\\u%04X".format(it.code)
                        }
                    )
                }
                printWithNoIndent('\"')
            }
        }
    }
}

private val JAVA_KEYWORDS = setOf(
    "_", "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default", "do",
    "double", "else", "enum", "extends", "false", "final", "finally", "float", "for", "goto", "if", "implements", "import",
    "instanceof", "int", "interface", "long", "native", "new", "null", "package", "private", "protected", "public", "return",
    "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "true", "try",
    "void", "volatile", "while"
)

private inline fun printToString(block: Printer.() -> Unit): String =
    buildString {
        Printer(this).block()
    }


private fun defaultValue(type: PsiType): String =
    when (type) {
        PsiType.BYTE -> "0"
        PsiType.BOOLEAN -> "false"
        PsiType.CHAR -> "\'\\u0000\'"
        PsiType.SHORT -> "0"
        PsiType.INT -> "0"
        PsiType.LONG -> "0L"
        PsiType.FLOAT -> "0.0F"
        PsiType.DOUBLE -> "0.0"
        else -> "null"
    }

private fun PsiMethod.isSyntheticStaticEnumMethod(): Boolean {
    if (!isStatic) return false
    return when (name) {
        StandardNames.ENUM_VALUES.asString() -> parameters.isEmpty()
        StandardNames.ENUM_VALUE_OF.asString() -> (parameters.singleOrNull()?.type as? PsiClassType)?.qualifiedName == "java.lang.String"
        else -> false
    }
}

// Java forbids outer and inner class names to be the same. Check if the names are different
private tailrec fun doesInnerClassNameConflictWithOuter(
    clazz: PsiClass,
    outerClass: PsiClass? = findContainingClassNode(clazz),
): Boolean {
    if (outerClass == null) return false
    if (clazz.name == outerClass.name) return true
    // Try to find the containing class for outerClassNode (to check the whole tree recursively)
    val containingClassForOuterClass = findContainingClassNode(outerClass) ?: return false
    return doesInnerClassNameConflictWithOuter(clazz, containingClassForOuterClass)
}

private fun findContainingClassNode(clazz: PsiClass): PsiClass? =
    clazz.parent as? PsiClass

private fun isValidQualifiedName(name: FqName) = name.pathSegments().all { isValidIdentifier(it.asString()) }

private fun isValidIdentifier(name: String): Boolean =
    !(name.isEmpty()
            || (name in JAVA_KEYWORDS)
            || !Character.isJavaIdentifierStart(name[0])
            || name.drop(1).any { !Character.isJavaIdentifierPart(it) })

private fun paramName(info: PsiParameter): String {
    val defaultName = info.name
    return when {
        isValidIdentifier(defaultName) -> defaultName
        defaultName == SpecialNames.IMPLICIT_SET_PARAMETER.asString() -> "p0"
        else -> "p${info.parameterIndex()}_${info.name.hashCode().ushr(1)}"
    }
}

private fun isErroneous(type: PsiType): Boolean {
    if (type.canonicalText == StandardNames.NON_EXISTENT_CLASS.asString()) return true
    if (type is PsiClassType) return type.parameters.any { isErroneous(it) }
    return false
}

private fun getKDocComment(psiElement: PsiElement): String? {
    val ktElement = psiElement.extractOriginalKtDeclaration() ?: return null
    if (psiElement is PsiField && ktElement is KtObjectDeclaration) {
        // Do not write KDoc on object instance field
        return null
    }

    val docComment = when {
        ktElement is KtProperty -> ktElement.docComment
        ktElement.docComment == null && ktElement is KtPropertyAccessor -> ktElement.property.docComment
        else -> ktElement.docComment
    } ?: return null

    if (psiElement is PsiMethod && psiElement.isConstructor && ktElement is KtClassOrObject) {
        // We don't want the class comment to be duplicated on <init>()
        return null
    }

    return extractComment(docComment)
}

private fun PsiElement.extractOriginalKtDeclaration(): KtDeclaration? {
    // This when is needed to avoid recursion
    val elementToExtract = when (this) {
        is KtLightParameter -> when (kotlinOrigin) {
            null -> method
            else -> return kotlinOrigin
        }
        else -> this
    }

    return when (elementToExtract) {
        is KtLightMember<*> -> {
            val origin = elementToExtract.lightMemberOrigin
            origin?.auxiliaryOriginalElement ?: origin?.originalElement ?: elementToExtract.kotlinOrigin
        }
        is KtLightElement<*, *> -> elementToExtract.kotlinOrigin
        else -> null
    } as? KtDeclaration
}