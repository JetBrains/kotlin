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

package org.jetbrains.kotlin.kapt.stubs

import com.intellij.psi.PsiElement
import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.parser.Tokens
import com.sun.tools.javac.tree.TreeMaker
import com.sun.tools.javac.util.Context
import kotlinx.kapt.KaptIgnored
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.fileParent
import org.jetbrains.kotlin.backend.jvm.mapping.MethodSignatureMapper
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.functions.isBuiltin
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.constant.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.backend.FirAnnotationSourceElement
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.impl.FirPropertyFromParameterResolvedNamedReference
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.references.toResolvedEnumEntrySymbol
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.PackageResolutionResult
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirArrayOfCallTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.resolveToPackageOrClass
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.kapt.KaptContextForStubGeneration
import org.jetbrains.kotlin.kapt.base.*
import org.jetbrains.kotlin.kapt.base.javac.kaptError
import org.jetbrains.kotlin.kapt.base.javac.reportKaptError
import org.jetbrains.kotlin.kapt.base.stubs.KaptStubLineInformation
import org.jetbrains.kotlin.kapt.base.stubs.KotlinPosition
import org.jetbrains.kotlin.kapt.javac.KaptTreeMaker
import org.jetbrains.kotlin.kapt.stubs.ErrorTypeCorrector.TypeKind.*
import org.jetbrains.kotlin.kapt.stubs.SignatureParser.ClassGenericSignature
import org.jetbrains.kotlin.kapt.util.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.util.ArrayLiteralResolution
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import java.io.File
import java.lang.Deprecated
import java.util.*
import javax.lang.model.element.ElementKind
import kotlin.collections.emptyList

abstract class KaptStubConverter(val kaptContext: KaptContextForStubGeneration, val generateNonExistentClass: Boolean) {
    internal companion object {
        val NON_EXISTENT_CLASS_NAME = FqName("error.NonExistentClass")

        fun create(kaptContext: KaptContextForStubGeneration, generateNonExistentClass: Boolean): KaptStubConverter =
            when (kaptContext.options.stubGenerationScheme) {
                StubGenerationScheme.JTREE -> KaptStubJTreeConverter(kaptContext, generateNonExistentClass)
                StubGenerationScheme.DIRECT -> KaptStubDirectConverter(kaptContext, generateNonExistentClass)
            }
    }

    abstract fun convert(): List<KaptStub>

    abstract class KaptStub(
        private val kaptMetadata: ByteArray? = null,
    ) {
        internal abstract fun packageName(): String

        internal abstract fun simpleClassName(): String

        internal abstract fun sourceFileName(): String

        abstract fun getText(context: Context): String

        fun writeMetadataIfNeeded(forSource: File, report: ((File) -> Unit)? = null) {
            if (kaptMetadata == null) {
                return
            }

            val metadataFile = File(
                forSource.parentFile,
                forSource.nameWithoutExtension + KaptStubLineInformation.KAPT_METADATA_EXTENSION
            )

            report?.invoke(metadataFile)
            metadataFile.writeBytes(kaptMetadata)
        }
    }

}

abstract class ParameterizedKaptStubConverter<
        Element,
        Expression : Element,
        Statement : Element,
        ClassDecl : Statement,
        VariableDecl : Statement,
        Annotation : Expression,
        TypeParameter : Element,
        MethodDecl : Element,
        Modifiers : Element,
        Block : Statement,
        >(
    kaptContext: KaptContextForStubGeneration,
    generateNonExistentClass: Boolean,
) :
    KaptStubConverter(kaptContext, generateNonExistentClass) {

    internal companion object {
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

        private val JAVA_KEYWORD_FILTER_REGEX = "[a-z]+".toRegex()

        @Suppress("UselessCallOnNotNull") // nullable toString(), KT-27724
        private val JAVA_KEYWORDS = Tokens.TokenKind.entries
            .filter { JAVA_KEYWORD_FILTER_REGEX.matches(it.toString().orEmpty()) }
            .mapTo(hashSetOf(), Any::toString)
    }

    private val correctErrorTypes = kaptContext.options[KaptFlag.CORRECT_ERROR_TYPES]
    private val strictMode = kaptContext.options[KaptFlag.STRICT]
    private val stripMetadata = kaptContext.options[KaptFlag.STRIP_METADATA]

    private val typeMapper = KaptTypeMapper

    val treeMaker = TreeMaker.instance(kaptContext.context) as KaptTreeMaker

    private val signatureParser = SignatureParser(this)

    private val importsFromRoot by lazy(::collectImportsFromRootPackage)

    private val compiledClassByName = kaptContext.compiledClassByName

    private var done = false

    internal val typeReferenceToFirType = mutableMapOf<KtTypeReference, ConeKotlinType>().apply {
        for (file in kaptContext.firFiles) {
            file.accept(object : FirDefaultVisitorVoid() {
                override fun visitElement(element: FirElement) {
                    element.acceptChildren(this)
                }

                override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
                    val psi = resolvedTypeRef.psi
                    if (psi is KtTypeReference) {
                        this@apply[psi] = resolvedTypeRef.coneType
                    }
                }

                override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef) {
                    visitResolvedTypeRef(errorTypeRef)
                }
            })
        }
    }

    private val firJvmTypeMapper: FirJvmTypeMapper? = kaptContext.firSession?.let(::FirJvmTypeMapper)

    private val irTypeSystem = IrTypeSystemContextImpl(kaptContext.irBuiltIns)

    private val legacyFunctionTypeKindProjector: LegacyFunctionTypeKindProjector? =
        kaptContext.firSession?.let(::LegacyFunctionTypeKindProjector)

    private fun projectLegacyFunctionTypeKindsIfNeeded(
        typeReference: KtTypeReference?,
        typeMappingMode: TypeMappingMode,
    ): Expression? {
        val firType = typeReference?.let(typeReferenceToFirType::get) ?: return null
        val projectedType = legacyFunctionTypeKindProjector?.projectIfNeeded(firType) ?: return null
        val typeMapper = firJvmTypeMapper ?: return null
        val signatureWriter = BothSignatureWriter(BothSignatureWriter.Mode.TYPE)
        val asmType: Type = typeMapper.mapType(projectedType, typeMappingMode, signatureWriter)
        val signature = signatureWriter.makeJavaGenericSignature()
        return parseFieldSignatureOrUseAsmType(signature, asmType)
    }

    private fun parseFieldSignatureOrUseAsmType(signature: String?, asmType: Type): Expression =
        if (signature == null) {
            makeType(asmType)
        } else {
            signatureParser.parseFieldSignature(signature)
        }

    override fun convert(): List<KaptStub> {
        if (kaptContext.logger.isVerbose) {
            dumpDeclarationOrigins()
        }

        if (done) error(KaptStubConverter::class.java.simpleName + " can convert classes only once")
        done = true

        val stubs = kaptContext.compiledClasses.mapNotNullTo(mutableListOf()) { convertTopLevelClass(it) }

        if (generateNonExistentClass) {
            stubs += makeNonExistentClassStub()
        }

        return stubs
    }

    private fun dumpDeclarationOrigins() {
        kaptContext.logger.info("Declaration origins:")
        for ([key, value] in kaptContext.origins) {
            val element = when (key) {
                is ClassNode -> "class ${key.name}"
                is FieldNode -> "field ${key.name}:${key.desc}"
                is MethodNode -> "method ${key.name}${key.desc}"
                else -> key.javaClass.toString()
            }
            kaptContext.logger.info("$element -> $value")
        }
    }

    private fun convertTopLevelClass(clazz: ClassNode): KaptStub? {
        val declaration = kaptContext.origins[clazz]?.declaration ?: return null

        // Nested classes will be processed during the outer classes conversion
        if (declaration is IrClass && declaration.parent is IrClass) return null

        val lineMappings = KaptLineMappingCollector(kaptContext)

        val packageName = declaration.fileParent.packageFqName.asString()

        return makeStubForTopLevelClass(declaration, lineMappings, packageName, clazz)
    }

    protected fun findFirFile(irClass: IrDeclaration): FirFile? =
        when (val metadata = (irClass as? IrClass)?.metadata) {
            is FirMetadataSource.Class -> kaptContext.firSession?.firProvider?.getFirClassifierContainerFile(metadata.fir.symbol)
            is FirMetadataSource.File -> metadata.fir
            else -> null
        }

    protected fun convertImports(firFile: FirFile?, classSimpleName: String): List<Element> {
        if (!correctErrorTypes) return emptyList()

        val imports = mutableListOf<Element>()
        val importedShortNames = mutableSetOf<String>()

        val addImport = fun(fqName: FqName, isAllUnder: Boolean) {
            if (isAllUnder) {
                imports += makeStarImport(fqName)
            } else {
                if (importedShortNames.add(fqName.shortName().asString())) {
                    imports += makeSingleImport(fqName)
                }
            }
        }

        if (firFile != null) {
            convertImportsFir(firFile, classSimpleName, addImport)
        }

        return imports
    }

    private fun convertImportsFir(
        file: FirFile,
        classSimpleName: String,
        addImport: (fqName: FqName, isAllUnder: Boolean) -> Unit,
    ) {
        val firSession = file.moduleData.session

        // We prefer ordinary imports over aliased ones.
        val sortedImportDirectives = file.imports.partition { it.aliasName == null }.run { first + second }

        for (importDirective in sortedImportDirectives) {
            // Qualified name should be valid Java fq-name
            val importedFqName = importDirective.importedFqName?.takeIf { it.pathSegments().size > 1 } ?: continue
            if (!isValidQualifiedName(importedFqName)) continue

            val shortName = importedFqName.shortName()
            if (shortName.asString() == classSimpleName) continue

            val isTopLevelCallable = firSession.symbolProvider.getTopLevelCallableSymbols(importedFqName.parent(), shortName).isNotEmpty()
            if (isTopLevelCallable) continue

            val resolvedParentClassId = (importDirective as? FirResolvedImport)?.resolvedParentClassId
            val importedClass = resolvedParentClassId?.let { resolveToPackageOrClass(firSession.symbolProvider, it) }
            if (importedClass is PackageResolutionResult.PackageOrClass) {
                val classSymbol = importedClass.classSymbol
                val isEnumEntry = classSymbol?.classKind == ClassKind.ENUM_CLASS
                if (isEnumEntry) continue

                if (classSymbol is FirClassSymbol<*>) {
                    if (importDirective.isAllUnder) continue
                    if (shortName in firSession.declaredMemberScope(classSymbol, null).getCallableNames()) continue
                }
            }

            addImport(importedFqName, importDirective.isAllUnder)
        }
    }

    protected fun convertClass(clazz: ClassNode, lineMappings: KaptLineMappingCollector, packageFqName: String): Pair<String, ClassDecl>? {
        if (isSynthetic(clazz.access)) return null
        if (!checkIfValidTypeName(clazz, Type.getObjectType(clazz.name))) return null

        val declaration = kaptContext.origins[clazz]?.declaration as? IrClass ?: return null

        val flags = getClassAccessFlags(clazz, declaration)

        val isEnum = clazz.isEnum()
        val isAnnotation = clazz.isAnnotation()

        val modifiers = convertModifiers(
            clazz,
            flags.toLong(),
            if (isEnum) ElementKind.ENUM else ElementKind.CLASS,
            packageFqName, clazz.visibleAnnotations, clazz.invisibleAnnotations, declaration.annotations
        )

        val simpleName = declaration.name.asString()
        if (!isValidIdentifier(simpleName)) return null

        val rawSuperClass = makeQualifiedName(clazz.superName)
        val rawInterfaces = clazz.interfaces.mapNotNull {
            if (isAnnotation && it == "java/lang/annotation/Annotation") return@mapNotNull null
            makeQualifiedName(it)
        }

        lineMappings.registerClass(clazz)
        val classPosition = lineMappings.getPosition(clazz)

        val genericType = if (clazz.signature != null)
            signatureParser.parseClassSignature(clazz.signature)
        else ClassGenericSignature(emptyList(), rawSuperClass, rawInterfaces)

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

        val enumValues: List<VariableDecl> = enumValuesData.mapNotNull { data ->
            // Historically, the first available constructor is used for all values
            // First two arguments are synthetic and are dropped
            val constructorArguments = Type.getArgumentTypes(clazz.methods.firstOrNull {
                it.name == "<init>" && Type.getArgumentCount(it.desc) >= 2
            }?.desc ?: "()Z")

            val args = constructorArguments.drop(2).mapNotNull {
                convertLiteral(clazz, getDefaultValue(it))
            }

            val def = data.correspondingClass?.let { convertClass(it, lineMappings, packageFqName)?.second }
            val initializer = makeEnumValueInitializer(data.field.name, args, def)

            convertField(
                data.field, clazz, lineMappings, packageFqName, initializer
            )
        }

        val convertedFieldsWithNode: List<Pair<FieldNode, VariableDecl>> =
            clazz.fields.filter { !it.isEnumValue() }.mapNotNull { fieldNode ->
                convertField(fieldNode, clazz, lineMappings, packageFqName)?.let { fieldNode to it }
            }
        val sortedConvertedFields: List<VariableDecl> = sortClassMembers(convertedFieldsWithNode, classPosition) {
            val fieldNode = it.first
            MemberData(fieldNode.name, fieldNode.desc, lineMappings.getPosition(clazz, fieldNode))
        }.map { it.second }

        fun MethodNode.isImplicitEnumMethod() = isEnum && (
                name == "values" && desc == "()[L${clazz.name};" ||
                        name == "valueOf" && desc == "(Ljava/lang/String;)L${clazz.name};")

        val convertedMethodsWithNode: List<Pair<MethodNode, MethodDecl>> =
            clazz.methods.filter { !it.isImplicitEnumMethod() }.mapNotNull { methodNode ->
                convertMethod(methodNode, clazz, lineMappings, packageFqName, declaration)?.let { methodNode to it }
            }
        val sortedConvertedMethods: List<MethodDecl> = sortClassMembers(convertedMethodsWithNode, classPosition) {
            val methodNode = it.first
            MemberData(methodNode.name, methodNode.desc, lineMappings.getPosition(clazz, methodNode))
        }.map { it.second }

        val nestedClasses: List<ClassDecl> = clazz.innerClasses.mapNotNull { innerClass ->
            if (enumValuesData.any { it.innerClass == innerClass }) return@mapNotNull null
            if (innerClass.outerName != clazz.name) return@mapNotNull null
            val innerClassNode = compiledClassByName[innerClass.name] ?: return@mapNotNull null
            convertClass(innerClassNode, lineMappings, packageFqName)?.second
        }

        val superTypes = calculateSuperTypes(clazz, genericType, declaration)

        return simpleName to makeClassDecl(
            clazz,
            simpleName,
            modifiers,
            genericType,
            superTypes,
            enumValues,
            sortedConvertedFields,
            sortedConvertedMethods,
            nestedClasses
        )
    }

    private inline fun <T> sortClassMembers(
        members: List<T>,
        classPosition: KotlinPosition?,
        memberDataProvider: (T) -> MemberData,
    ): List<T> {
        val positionsMap = IdentityHashMap<T, MemberData>(members.size).also {
            members.associateWithTo(it, memberDataProvider)
        }
        return members.sortedWith(MembersPositionComparator(classPosition, positionsMap))
    }

    protected class ClassSupertypes<Expr>(val superClass: Expr?, val interfaces: List<Expr>)
    private class SuperTypeCalculationFailure : RuntimeException()

    private fun calculateSuperTypes(
        clazz: ClassNode, genericType: ClassGenericSignature<Expression, TypeParameter>, declaration: IrDeclaration,
    ): ClassSupertypes<Expression> {
        val hasSuperClass = clazz.superName != "java/lang/Object" && !clazz.isEnum()

        val defaultSuperTypes = ClassSupertypes(
            if (hasSuperClass) genericType.superClass else null,
            genericType.interfaces
        )

        if (!correctErrorTypes) {
            return defaultSuperTypes
        }

        val psiClass = kaptContext.origins[clazz]?.element as? KtClassOrObject ?: return defaultSuperTypes
        if (psiClass.computeJvmInternalName() != clazz.name) return defaultSuperTypes

        val firClass = ((declaration as? IrClass)?.metadata as? FirMetadataSource.Class)?.fir
        val [superClass, superInterfaces] = partitionSuperTypes(psiClass, firClass) ?: return defaultSuperTypes

        val sameSuperClassCount = (superClass == null) == (defaultSuperTypes.superClass == null)
        val sameSuperInterfaceCount = superInterfaces.size == defaultSuperTypes.interfaces.size

        // Note: if the number of supertypes is different, it might mean either that one of them is unresolved, or that backend generated
        // additional supertypes which were not present in the PSI.
        // In the former case, the subsequent code behaves as expected, trying to recover the types from the PSI.
        // In the latter case, ideally we shouldn't do anything, but most of the time invoking error type correction is harmless because
        // it will be a no-op. However, it might lead to problems for non-trivial types such as `kotlin.FunctionN` which are mapped to
        // `kotlin.jvm.functions.FunctionN`, because the Java source requires a new import, unlike the Kotlin source.
        if (sameSuperClassCount && sameSuperInterfaceCount) {
            return defaultSuperTypes
        }

        fun nonErrorType(ref: () -> KtTypeReference?): Expression {
            assert(correctErrorTypes)

            return getNonErrorType<Expression>(true, SUPER_TYPE, ref) { throw SuperTypeCalculationFailure() }
        }

        return try {
            ClassSupertypes(
                superClass?.let { nonErrorType { it } },
                superInterfaces.map { nonErrorType { it } }
            )
        } catch (_: SuperTypeCalculationFailure) {
            defaultSuperTypes
        }
    }

    private fun partitionSuperTypes(declaration: KtClassOrObject, firClass: FirClass?): Pair<KtTypeReference?, List<KtTypeReference>>? {
        val superTypeEntries = declaration.superTypeListEntries
            .takeIf { it.isNotEmpty() }
            ?: return Pair(null, emptyList())

        val classEntries = mutableListOf<KtSuperTypeListEntry>()
        val interfaceEntries = mutableListOf<KtSuperTypeListEntry>()
        val otherEntries = mutableListOf<KtSuperTypeListEntry>()

        for (entry in superTypeEntries) {
            val isInterface = isSuperTypeDefinitelyInterface(entry, firClass)
            val container = when {
                isInterface != null -> if (isInterface) interfaceEntries else classEntries
                entry is KtSuperTypeCallEntry -> classEntries
                else -> otherEntries
            }
            container += entry
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

    private fun isSuperTypeDefinitelyInterface(entry: KtSuperTypeListEntry, firClass: FirClass?): Boolean? {
        if (firClass != null) {
            val firSuperTypeRef = firClass.superTypeRefs.firstOrNull { (it.source as? KtPsiSourceElement)?.psi == entry.typeReference }
            val symbolProvider = kaptContext.firSession?.symbolProvider
            if (firSuperTypeRef != null && symbolProvider != null) {
                val superFirClass = firSuperTypeRef.coneTypeOrNull?.classId?.let(symbolProvider::getClassLikeSymbolByClassId)
                if (superFirClass != null) {
                    return superFirClass.classKind == ClassKind.INTERFACE
                }
            }
        }
        return null
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
        outerClass: ClassNode? = findContainingClassNode(clazz),
    ): Boolean {
        if (outerClass == null) return false
        if (treeMaker.getSimpleName(clazz) == treeMaker.getSimpleName(outerClass)) return true
        // Try to find the containing class for outerClassNode (to check the whole tree recursively)
        val containingClassForOuterClass = findContainingClassNode(outerClass) ?: return false
        return doesInnerClassNameConflictWithOuter(clazz, containingClassForOuterClass)
    }

    private fun getClassAccessFlags(clazz: ClassNode, irClass: IrClass): Int {
        var access = clazz.access
        if (irClass.isEnumClass) {
            // Enums are final in the bytecode, but "final enum" is not allowed in Java.
            access = access and Opcodes.ACC_FINAL.inv()
        }
        if ((irClass.parent as? IrClass)?.kind == ClassKind.INTERFACE) {
            // Classes inside interfaces should always be public and static.
            // See com.sun.tools.javac.comp.Enter.visitClassDef for more information.
            return (access or Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC) and
                    Opcodes.ACC_PRIVATE.inv() and Opcodes.ACC_PROTECTED.inv() // Remove private and protected modifiers
        }
        if (irClass.parent is IrClass && !irClass.isInner) {
            access = access or Opcodes.ACC_STATIC
        }
        return access
    }

    private fun convertField(
        field: FieldNode,
        containingClass: ClassNode,
        lineMappings: KaptLineMappingCollector,
        packageFqName: String,
        explicitInitializer: Expression? = null,
    ): VariableDecl? {
        if (isSynthetic(field.access) || isIgnored(field.invisibleAnnotations)) return null
        val declaration = kaptContext.origins[field]?.declaration

        val modifiers = convertModifiers(
            containingClass,
            field.access.toLong(), ElementKind.FIELD, packageFqName,
            field.visibleAnnotations, field.invisibleAnnotations,
            declaration?.annotations.orEmpty()
        )

        val isEnumField = isEnum(field.access)
        val name = field.name
        if (!isValidIdentifier(name)) return null

        val asmType = Type.getType(field.desc)
        val irField = declaration as? IrField

        if (!checkIfValidTypeName(containingClass, asmType)) {
            return null
        }

        val fieldTypeReference =
            (kaptContext.origins[field]?.element as? KtCallableDeclaration)
                ?.takeIf { it !is KtFunction }
                ?.typeReference

        val fieldTypeMappingMode = irField?.let {
            if (it.correspondingPropertySymbol?.owner?.isVar == true) {
                MethodSignatureMapper.getTypeMappingModeForParameter(irTypeSystem, it, it.type)
            } else {
                MethodSignatureMapper.getTypeMappingModeForReturnType(irTypeSystem, it, it.type)
            }
        }

        // Enum type must be an identifier (Javac requirement)
        val convertedType = if (isEnumField) {
            val name = treeMaker.getQualifiedName(asmType).substringAfterLast('.')
            makeSimpleName(name)
        } else {
            getNonErrorType(
                irField?.type?.containsErrorTypes() == true,
                RETURN_TYPE,
                ktTypeProvider = { fieldTypeReference },
                ifNonError = {
                    fieldTypeMappingMode?.let { projectLegacyFunctionTypeKindsIfNeeded(fieldTypeReference, it) }
                        ?: parseFieldSignatureOrUseAsmType(field.signature, asmType)
                }
            )
        }

        lineMappings.registerField(containingClass, field)

        val initializer = explicitInitializer ?: convertPropertyInitializer(containingClass, field)

        return makeField(field, modifiers, initializer, convertedType)
    }

    private fun convertPropertyInitializer(containingClass: ClassNode, field: FieldNode): Expression? {
        val value = field.value

        val irField = kaptContext.origins[field]?.declaration as? IrField
        val firInitializer = when (val metadata = irField?.metadata) {
            is FirMetadataSource.Field -> metadata.fir.initializer
            is FirMetadataSource.Property -> metadata.fir.initializer
            else -> null
        }
        if (value != null && firInitializer != null)
            return convertConstantValueArgumentsFir(containingClass, value, listOf(firInitializer))

        // Work-around for enum classes in companions.
        // In expressions "Foo.Companion.EnumClass", Java prefers static field over a type name, making the reference invalid.
        if (irField?.type?.classOrNull?.owner?.let { it.isEnumClass && it.isInsideCompanionObject() } == true) {
            return null
        }

        val firProperty = (irField?.metadata as? FirMetadataSource.Property)?.fir
        if (firProperty != null) {
            convertNonConstPropertyInitializerFir(firProperty, containingClass)?.let { return it }
        }

        if (isFinal(field.access)) {
            val type = Type.getType(field.desc)
            return convertLiteral(containingClass, getDefaultValue(type))
        }

        return null
    }

    @OptIn(SymbolInternals::class)
    private fun convertNonConstPropertyInitializerFir(property: FirProperty, containingClass: ClassNode): Expression? {
        val propertyInitializer = property.initializer ?: return null
        val reference = propertyInitializer.toReference(kaptContext.firSession!!)
        val expression =
            if (kaptContext.options[KaptFlag.DUMP_DEFAULT_PARAMETER_VALUES])
                ((reference as? FirPropertyFromParameterResolvedNamedReference)?.resolvedSymbol?.fir as? FirValueParameter)
                    ?.defaultValue ?: propertyInitializer
            else propertyInitializer
        val asmValue = evaluateFirExpression(expression) ?: return null
        return convertConstantValueArgumentsFir(containingClass, asmValue, listOf(expression))
    }

    private fun evaluateFirExpression(initialExpression: FirExpression): Any? {
        val session = kaptContext.firSession!!
        val expression =
            if (initialExpression is FirFunctionCall && withSession(session) { useArrayLiteralResolution() }) {
                @OptIn(ArrayLiteralResolution::class)
                FirArrayOfCallTransformer().transformFunctionCall(initialExpression, session)
            } else initialExpression

        val result = try {
            expression.evaluateAs<FirElement>(session)
        } catch (_: Exception) {
            null
        } ?: return null

        return when (result) {
            is FirLiteralExpression -> result.value?.let { constValue ->
                when (result.kind) {
                    ConstantValueKind.Int -> convertTo<Int>(constValue)
                    ConstantValueKind.UnsignedInt -> convertTo<Int>(constValue)
                    ConstantValueKind.Byte -> convertTo<Byte>(constValue)
                    ConstantValueKind.UnsignedByte -> convertTo<Byte>(constValue)
                    ConstantValueKind.Short -> convertTo<Short>(constValue)
                    ConstantValueKind.UnsignedShort -> convertTo<Short>(constValue)
                    else -> constValue
                }
            }
            is FirPropertyAccessExpression -> result.calleeReference.toResolvedEnumEntrySymbol()?.let { enumEntry ->
                val enumType = AsmUtil.asmTypeByClassId(enumEntry.callableId.classId!!)
                arrayOf(enumType.descriptor, enumEntry.name.asString())
            }
            is FirCollectionLiteral -> {
                result.argumentList.arguments.map(::evaluateFirExpression)
            }
            else -> null
        }
    }

    @Suppress("RedundantIf")
    private fun IrElement.isInsideCompanionObject(): Boolean {
        val parent = (this as? IrDeclaration)?.parent ?: return false
        if (parent is IrClass && parent.isCompanion) return true
        return parent.isInsideCompanionObject()
    }

    // @JvmExposeBoxed generates an exposed function, which is not an override.
    private fun IrSimpleFunction.isJvmOverride(): Boolean = when {
        overriddenSymbols.isEmpty() -> false
        !hasAnnotation(JvmStandardClassIds.JVM_EXPOSE_BOXED_ANNOTATION_FQ_NAME) -> true
        else -> overriddenSymbols.any { it.owner.name == name }
    }

    private fun convertMethod(
        method: MethodNode,
        containingClass: ClassNode,
        lineMappings: KaptLineMappingCollector,
        packageFqName: String,
        irClass: IrClass,
    ): MethodDecl? {
        if (isIgnored(method.invisibleAnnotations)) return null
        val declaration = kaptContext.origins[method]?.declaration as? IrFunction ?: return null

        val isAnnotationHolderForProperty =
            isSynthetic(method.access) && isStatic(method.access) && method.name.endsWith(JvmAbi.ANNOTATED_PROPERTY_METHOD_NAME_SUFFIX)

        if (isSynthetic(method.access) && !isAnnotationHolderForProperty) return null

        val isOverridden = declaration is IrSimpleFunction && declaration.isJvmOverride()
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
            ElementKind.METHOD, packageFqName, visibleAnnotations, method.invisibleAnnotations, declaration.annotations
        )

        val asmReturnType = Type.getReturnType(method.desc)
        val returnType = if (isConstructor) null else makeType(asmReturnType)

        val parametersInfo = method.getParametersInfo(containingClass, irClass.isInner, declaration)

        if (!checkIfValidTypeName(containingClass, asmReturnType)
            || parametersInfo.any { !checkIfValidTypeName(containingClass, it.type) }
        ) {
            return null
        }

        val parameterTypes = parametersInfo.map { info -> makeType(info.type) }

        // Kotlin @Throws arguments are class literals, so cannot be generic and do not need refinement from the generic signature.
        val exceptionTypes = method.exceptions.map { makeQualifiedName(it) }

        val genericSignature = extractMethodSignatureTypes(declaration, exceptionTypes, returnType, method, parameterTypes)

        val parameters: List<VariableDecl> = parametersInfo.mapIndexed { index, info ->
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
                emptyList() /* TODO */
            )

            val name = when {
                info.name == "_" -> "p$index"
                isValidIdentifier(info.name) -> info.name
                else -> "p" + index + "_" + info.name.hashCode().ushr(1)
            }
            val type = genericSignature.parameterTypes[index]

            makeParameter(name, type, modifiers, varargs != 0L)
        }

        val defaultValue = method.annotationDefault?.let { convertLiteral(containingClass, it) }

        val body: Block? = if (defaultValue != null) {
            null
        } else if (isAbstract(method.access)) {
            null
        } else if (isConstructor && containingClass.isEnum()) {
            makeBlock(emptyList())
        } else if (isConstructor) {
            val superClass = declaration.parentAsClass.getNonErrorSuperClassNotAny()
            val superClassConstructor =
                superClass.constructors.find {
                    (it.visibility == DescriptorVisibilities.PUBLIC || it.visibility == DescriptorVisibilities.PROTECTED) &&
                            it.origin != JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR_FOR_HIDDEN_CONSTRUCTOR
                }

            val superClassConstructorCall: List<Statement> = if (superClassConstructor != null) {
                val args = superClassConstructor.parameters.map { param ->
                    val defaultValue = IrConstImpl.defaultValueForType(UNDEFINED_OFFSET, UNDEFINED_OFFSET, param.type)
                    convertLiteral(containingClass, defaultValue.value)
                }
                listOf(makeSimpleCallStatement("super", args))
            } else {
                emptyList()
            }

            makeBlock(superClassConstructorCall)
        } else if (asmReturnType == Type.VOID_TYPE) {
            makeBlock(emptyList())
        } else {
            val convertedDefaultValue = convertLiteral(containingClass, getDefaultValue(asmReturnType))
            val returnStatement = makeReturn(convertedDefaultValue)
            makeBlock(listOf(returnStatement))
        }

        lineMappings.registerMethod(containingClass, method)

        val paramSignatureTypes = List(parametersInfo.size) { index ->
            genericSignature.parameterTypes[index].toString()
        }
        val signature = buildString {
            append(name)
            paramSignatureTypes.joinTo(this, prefix = "(", postfix = ")")
        }
        lineMappings.registerSignature(signature, method)

        return makeMethod(method, irClass, modifiers, genericSignature, parameters, exceptionTypes, body, defaultValue)
    }

    private fun IrClass.getNonErrorSuperClassNotAny(): IrClass {
        // Based on `ClassDescriptor.getSuperClassNotAny`, but filters out error types because in K2 kapt, FIR classes (and thus IR, and
        // IR-based descriptors) still have error supertypes, while in K1 kapt they are filtered out on the frontend level.
        for (supertype in superTypes) {
            if (supertype !is IrErrorType && !supertype.isAny()) {
                val superclass = supertype.classOrNull?.owner ?: continue
                if (superclass.isClass || superclass.isEnumClass) return superclass
            }
        }
        return kaptContext.irBuiltIns.anyClass.owner
    }

    private fun isIgnored(annotations: List<AnnotationNode>?): Boolean {
        val kaptIgnoredAnnotationFqName = KaptIgnored::class.java.name
        return annotations?.any { Type.getType(it.desc).className == kaptIgnoredAnnotationFqName } ?: false
    }

    private fun extractMethodSignatureTypes(
        declaration: IrFunction,
        rawExceptionTypes: List<Expression>,
        returnType: Expression?,
        method: MethodNode,
        parameterTypes: List<Expression>,
    ): SignatureParser.MethodGenericSignature<Expression, TypeParameter> {
        val irValueParameters = declaration.parameters.filter { it.kind == IrParameterKind.Regular }
        val contextParameters = declaration.parameters.filter { it.kind == IrParameterKind.Context }
        val extensionReceiver = declaration.parameters.find { it.kind == IrParameterKind.ExtensionReceiver }
        val psiElement = kaptContext.origins[method]?.element
        val returnTypeReference =
            when (psiElement) {
                is KtFunction -> psiElement.typeReference
                is KtProperty -> if (declaration.isGetter) psiElement.typeReference else null
                is KtPropertyAccessor -> if (declaration.isGetter) psiElement.property.typeReference else null
                is KtParameter -> if (declaration.isGetter) psiElement.typeReference else null
                else -> null
            }
        val returnTypeMappingMode = MethodSignatureMapper.getTypeMappingModeForReturnType(irTypeSystem, declaration, declaration.returnType)

        fun nonErrorParameterTypeProvider(index: Int, lazyType: () -> Expression): Expression {
            fun getNonErrorMethodParameterType(type: IrType, ktTypeProvider: () -> KtTypeReference?): Expression {
                val typeReference = ktTypeProvider()
                val typeMappingMode = MethodSignatureMapper.getTypeMappingModeForParameter(irTypeSystem, declaration, type)
                return getNonErrorType(
                    type.containsErrorTypes(),
                    METHOD_PARAMETER_TYPE,
                    ktTypeProvider = { typeReference },
                    ifNonError = { projectLegacyFunctionTypeKindsIfNeeded(typeReference, typeMappingMode) ?: lazyType() }
                )
            }

            fun PsiElement.getCallableDeclaration(): KtCallableDeclaration? = when (this) {
                is KtCallableDeclaration -> if (this is KtFunction) null else this
                is KtPropertyAccessor -> property
                else -> null
            }

            return when {
                declaration.isGetter -> {
                    when {
                        index < contextParameters.size -> {
                            getNonErrorMethodParameterType(contextParameters[index].type) {
                                psiElement?.getCallableDeclaration()?.contextParameters?.get(index)?.typeReference
                            }
                        }
                        irValueParameters.isEmpty() && index == contextParameters.size -> {
                            getNonErrorMethodParameterType(extensionReceiver?.type ?: declaration.returnType) {
                                psiElement?.getCallableDeclaration()?.receiverTypeReference
                            }
                        }
                        else -> {
                            lazyType()
                        }
                    }
                }
                declaration.isSetter -> {
                    when {
                        index < contextParameters.size -> {
                            getNonErrorMethodParameterType(contextParameters[index].type) {
                                psiElement?.getCallableDeclaration()?.contextParameters?.get(index)?.typeReference
                            }
                        }
                        index == contextParameters.size && extensionReceiver != null ->
                            getNonErrorMethodParameterType(extensionReceiver.type) {
                                psiElement?.getCallableDeclaration()?.receiverTypeReference
                            }
                        irValueParameters.size != 1 -> lazyType()
                        index == (if (extensionReceiver == null) 0 else 1) + contextParameters.size -> {
                            getNonErrorMethodParameterType(irValueParameters[0].type) {
                                psiElement?.getCallableDeclaration()?.typeReference
                            }
                        }
                        else -> lazyType()
                    }
                }
                else -> {
                    val offset = (if (extensionReceiver == null) 0 else 1) + contextParameters.size
                    when {
                        index < contextParameters.size -> {
                            getNonErrorMethodParameterType(contextParameters[index].type) {
                                (psiElement as? KtCallableDeclaration)?.contextParameters?.get(index)?.typeReference
                            }
                        }
                        extensionReceiver != null && index == contextParameters.size -> {
                            getNonErrorMethodParameterType(extensionReceiver.type) {
                                (psiElement as? KtCallableDeclaration)?.receiverTypeReference
                            }
                        }
                        irValueParameters.size + offset == parameterTypes.size -> {
                            val valueParameterIndex = index - offset
                            val irParameter = irValueParameters[valueParameterIndex]
                            val sourceElement = when {
                                psiElement is KtFunction -> psiElement
                                declaration is IrConstructor && declaration.isPrimary -> {
                                    (psiElement as? KtClassOrObject)?.primaryConstructor
                                        ?: ((psiElement as? KtParameterList)?.parent as? KtFunction)
                                }
                                else -> null
                            }
                            if (sourceElement != null && sourceElement.hasDeclaredReturnType() && isContinuationParameter(irParameter)) {
                                val typeMappingMode =
                                    MethodSignatureMapper.getTypeMappingModeForParameter(irTypeSystem, declaration, irParameter.type)
                                val argument = getNonErrorType(
                                    irParameter.type.containsErrorTypes(),
                                    METHOD_PARAMETER_TYPE,
                                    ktTypeProvider = { sourceElement.typeReference },
                                    ifNonError = { projectLegacyFunctionTypeKindsIfNeeded(sourceElement.typeReference, typeMappingMode) },
                                )
                                if (argument == null) {
                                    lazyType()
                                } else {
                                    makeTypeApply(makeQualifiedName(StandardNames.CONTINUATION_INTERFACE_FQ_NAME), listOf(argument))
                                }
                            } else getNonErrorMethodParameterType(irParameter.type) {
                                sourceElement?.valueParameters?.getOrNull(valueParameterIndex)?.typeReference
                            }
                        }
                        else -> {
                            lazyType()
                        }
                    }
                }
            }
        }

        val genericSignature = if (method.signature == null) {
            val nonErrorParameterTypes = parameterTypes.mapIndexed { index, parameterType ->
                nonErrorParameterTypeProvider(index) { parameterType }
            }
            SignatureParser.MethodGenericSignature(emptyList(), nonErrorParameterTypes, rawExceptionTypes, returnType)
        } else {
            signatureParser.parseMethodSignature(
                method.signature, parameterTypes, hasReturnType = returnType != null,
                ::nonErrorParameterTypeProvider
            )
        }

        val refinedReturnType = getNonErrorType(
            declaration.returnType.containsErrorTypes(), RETURN_TYPE,
            ktTypeProvider = { returnTypeReference },
            ifNonError = {
                projectLegacyFunctionTypeKindsIfNeeded(returnTypeReference, returnTypeMappingMode) ?: genericSignature.returnType
            }
        )

        return genericSignature.withRefinedReturnType(refinedReturnType)
    }

    private fun isContinuationParameter(parameter: IrValueParameter): Boolean =
        parameter.name.asString() == SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME &&
                parameter.origin == JvmLoweredDeclarationOrigin.CONTINUATION_CLASS

    private fun <T : Expression?> getNonErrorType(
        containsErrorTypes: Boolean,
        kind: ErrorTypeCorrector.TypeKind,
        ktTypeProvider: () -> KtTypeReference?,
        ifNonError: () -> T,
    ): T {
        if (!correctErrorTypes) {
            return ifNonError()
        }

        if (containsErrorTypes) {
            val typeFromSource = ktTypeProvider()?.typeElement
            val ktFile = typeFromSource?.containingKtFile
            if (ktFile != null) {
                @Suppress("UNCHECKED_CAST")
                return ErrorTypeCorrector(this, kind, ktFile).convert(typeFromSource) as T
            }
        }

        val nonErrorType = ifNonError()
        if (nonErrorType != null && isNonExistentClass(nonErrorType)) {
            @Suppress("UNCHECKED_CAST")
            return makeQualifiedName("java.lang.Object") as T
        }

        return nonErrorType
    }

    private fun FirAnnotation.convertNonErrorAnnotationType(type: IrType): Expression? =
        if (correctErrorTypes && type.containsErrorTypes())
            convertFirType(resolvedType)
        else null

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

    private fun convertModifiers(
        containingClass: ClassNode,
        access: Long,
        kind: ElementKind,
        packageFqName: String,
        visibleAnnotations: List<AnnotationNode>?,
        invisibleAnnotations: List<AnnotationNode>?,
        irAnnotations: List<IrAnnotation>,
    ): Modifiers {
        val convertedAnnotations = mutableListOf<Annotation>()
        var seenOverride = false
        val seenAnnotations = mutableSetOf<IrAnnotation>()
        fun convertAndAdd(annotation: AnnotationNode) {
            if (annotation.desc == "Ljava/lang/Override;") {
                if (seenOverride) return  // KT-34569: skip duplicate @Override annotations
                seenOverride = true
            }
            // Missing annotation classes can match against multiple annotation descriptors
            val irAnnotation = irAnnotations.firstOrNull {
                it !in seenAnnotations && checkIfAnnotationValueMatches(annotation, it.toConstantValue())
            }?.also {
                seenAnnotations += it
            }
            filterAndConvertAnnotation(containingClass, annotation, packageFqName, irAnnotation)?.let { convertedAnnotations.add(it) }
        }

        visibleAnnotations?.forEach { convertAndAdd(it) }
        invisibleAnnotations?.forEach { convertAndAdd(it) }

        if (isDeprecated(access)) {
            convertedAnnotations.add(makeAnnotation(makeType(Type.getType(Deprecated::class.java)), emptyList()))
        }

        val flags = when (kind) {
            ElementKind.ENUM -> access and CLASS_MODIFIERS and Opcodes.ACC_ABSTRACT.inv().toLong()
            ElementKind.CLASS -> access and CLASS_MODIFIERS
            ElementKind.METHOD if isDefaultInterfaceMethod(containingClass, access.toInt()) ->
                (access and METHOD_MODIFIERS) or Flags.DEFAULT
            ElementKind.METHOD -> access and METHOD_MODIFIERS
            ElementKind.FIELD -> access and FIELD_MODIFIERS
            ElementKind.PARAMETER -> access and PARAMETER_MODIFIERS
            else -> throw IllegalArgumentException("Invalid element kind: $kind")
        }

        val isEnumField = kind == ElementKind.FIELD && isEnum(access.toInt())
        return makeModifiers(kind, isEnumField, flags, convertedAnnotations)
    }

    private fun filterAndConvertAnnotation(
        containingClass: ClassNode,
        annotation: AnnotationNode,
        packageFqName: String? = "",
        irAnnotation: IrAnnotation? = null
    ): Annotation? {
        val annotationType = Type.getType(annotation.desc)
        val fqName = treeMaker.getQualifiedName(annotationType)
        val filterOut = BLACKLISTED_ANNOTATIONS.any { fqName.startsWith(it) } ||
                (stripMetadata && fqName == KOTLIN_METADATA_ANNOTATION)
        if (filterOut) {
            reportIfIllegalTypeUsage(containingClass, annotationType)
            return null
        }
        return convertAnnotation(containingClass, annotation, packageFqName, irAnnotation)
    }

    private fun convertAnnotation(
        containingClass: ClassNode,
        annotation: AnnotationNode,
        packageFqName: String? = "",
        irAnnotation: IrAnnotation? = null
    ): Annotation {
        val annotationType = Type.getType(annotation.desc)
        val fqName = treeMaker.getQualifiedName(annotationType)
        reportIfIllegalTypeUsage(containingClass, annotationType)

        val nameFromNode = if ('.' in fqName && fqName.substringBeforeLast('.', "") == packageFqName) {
            // simple name is preferred
            fqName.substring(packageFqName.length + 1)
        } else {
            fqName
        }

        val ktAnnotation = irAnnotation?.source?.getPsi() as? KtAnnotationEntry
        val firSource = irAnnotation?.source as? FirAnnotationSourceElement
        val firAnnotation = firSource?.fir
        val convertedAnnotationType: Expression =
            firAnnotation?.convertNonErrorAnnotationType(irAnnotation.type) ?: getNonErrorType(
                irAnnotation?.type?.containsErrorTypes() == true,
                ANNOTATION,
                { ktAnnotation?.typeReference },
                {
                    makeQualifiedName(nameFromNode)
                }
            )

        val firArgMapping = firAnnotation?.argumentMapping?.mapping ?: emptyMap()
        val firOriginalArgMapping =
            (firAnnotation as? FirAnnotationCall)?.resolvedArgumentMapping?.map { it.value.name to it.key }?.toMap().orEmpty()

        val constantValues = pairedListToMap(annotation.values)

        val convertedArguments: List<Expression> = when {
            firArgMapping.isNotEmpty() -> {
                val allParameterNames = firArgMapping.keys.mapTo(mutableSetOf()) { it.asString() } + constantValues.keys
                allParameterNames.mapNotNull { strName ->
                    val name = Name.identifier(strName)
                    val firArg = firOriginalArgMapping[name] ?: firArgMapping[name]
                    convertAnnotationArgumentWithNameFir(containingClass, constantValues[strName], firArg, strName)
                }
            }
            else -> constantValues.mapNotNull { [parameterName, arg] ->
                convertAnnotationArgumentWithName(containingClass, arg, parameterName)
            }
        }

        return makeAnnotation(convertedAnnotationType, convertedArguments)
    }

    private fun convertAnnotationArgumentWithNameFir(
        containingClass: ClassNode,
        constantValue: Any?,
        value: FirExpression?,
        name: String,
    ): Expression? {
        if (!isValidIdentifier(name)) return null
        val expr = when (value) {
            is FirCollectionLiteral -> {
                convertConstantValueArgumentsFir(containingClass, constantValue, value.arguments)
            }
            is FirVarargArgumentsExpression -> {
                convertConstantValueArgumentsFir(containingClass, constantValue, value.arguments)
            }
            is FirFunctionCall if (value.isArrayOfOrArrayDotOfCall()) -> {
                convertConstantValueArgumentsFir(containingClass, constantValue, value.unwrapArgumentsOfArrayOfCall())
            }
            is FirGetClassCall -> {
                convertFirGetClassCall(value)
            }
            is FirPropertyAccessExpression if (value.resolvedType is ConeErrorType) -> {
                // Unresolved enum value
                convertFirType(value.resolvedType)
            }
            else -> {
                convertConstantValueArgumentsFir(containingClass, constantValue, listOfNotNull(value))
            }
        } ?: return null

        return makeAssignExpression(name, expr)
    }

    private fun convertConstantValueArgumentsFir(
        containingClass: ClassNode,
        constantValue: Any?,
        args: List<FirExpression>
    ): Expression {
        if (constantValue is List<*>) {
            if (args.size > constantValue.size) {
                // the expected reason for "extra" args is class literals with error (missing) types

                if (args.size == 1 && args[0] is FirSpreadArgumentExpression) {
                    return convertFirSpreadArgumentExpression(args[0] as FirSpreadArgumentExpression)
                }

                val convertedLiterals = args.mapNotNull(::convertFirGetClassCall)
                if (convertedLiterals.size == args.size) {
                    return makeArray(convertedLiterals)
                }
            }
        }

        if (constantValue.isOfPrimitiveType() && args.size == 1) {
            // Do not inline primitive constants
            tryParseReferenceToIntConstant(args.single())?.let { return it }
        } else if (constantValue is List<*> &&
            constantValue.isNotEmpty() &&
            args.isNotEmpty() &&
            constantValue.all { it.isOfPrimitiveType() }
        ) {
            val parsed = args.mapNotNull(::tryParseReferenceToIntConstant)
            if (parsed.size == args.size) {
                return makeArray(parsed)
            }
        }

        return convertLiteral(containingClass, constantValue)
    }

    private fun tryParseReferenceToIntConstant(expression: FirExpression): Expression? {
        if (expression !is FirPropertyAccessExpression) return null
        val field = expression.calleeReference.resolved?.resolvedSymbol as? FirFieldSymbol ?: return null
        if (!field.isJavaOrEnhancement || field.dispatchReceiverType != null) return null
        val containingClass = field.containingClassLookupTag() ?: return null
        val fqName = containingClass.classId.asSingleFqName().child(field.name)
        return makeQualifiedName(fqName)
    }

    private fun convertFirSpreadArgumentExpression(argumentExpression: FirSpreadArgumentExpression): Expression {
        val arguments = when (val expression = argumentExpression.expression) {
            is FirCollectionLiteral -> expression.arguments
            is FirFunctionCall if (expression.isArrayOfOrArrayDotOfCall()) -> expression.unwrapArgumentsOfArrayOfCall()
            else -> return makeArray([])
        }
        val converted = arguments.mapNotNull(::convertFirGetClassCall)
        return makeArray(converted)
    }

    private fun convertFirGetClassCall(expression: FirExpression): Expression? {
        if (expression !is FirGetClassCall) return null
        val kClassType = expression.resolvedType
        val type = kClassType.typeArguments.single().type ?: return null
        val convertedType = convertFirType(type) ?: return null
        return makeSelect(convertedType, "class")
    }

    private fun convertFirType(originalType: ConeKotlinType): Expression? {
        val possiblyArrayType = originalType.fullyExpandedType(kaptContext.firSession!!)
        var type = possiblyArrayType
        var arrayDimensions = 0
        while (type.isNonPrimitiveArray) {
            type = type.typeArguments[0].type ?: StandardClassIds.Any.constructClassLikeType()
            arrayDimensions++
        }
        val result = convertFirNonArrayType(type) ?: return null
        return makeArrayType(result, arrayDimensions)
    }

    private fun convertFirNonArrayType(type: ConeKotlinType): Expression? {
        if (type is ConeErrorType) {
            val diagnostic = type.diagnostic as? ConeUnresolvedError
            val simpleName = diagnostic?.qualifier ?: return null
            val outerType = (diagnostic as? ConeUnresolvedNameError)?.receiverInfo?.type
            return if (outerType == null) {
                makeSimpleName(simpleName)
            } else {
                val convertedOuterType = convertFirType(outerType) ?: return null
                makeSelect(convertedOuterType, simpleName)
            }
        }
        if (type !is ConeLookupTagBasedType) return null
        val classId = (type.lookupTag as? ConeClassLikeLookupTag)?.classId ?: return null
        val fqName = classId.asSingleFqName()
        if (classId.packageFqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME) {
            val primitiveType = PrimitiveType.getByShortName(classId.relativeClassName.asString())
            if (primitiveType != null) {
                val asmType = Type.getType(JvmPrimitiveType.get(primitiveType).desc)
                return makeType(asmType)
            }
            val primitiveArrayType = PrimitiveType.getByShortArrayName(classId.relativeClassName.asString())
            if (primitiveArrayType != null) {
                val asmType = Type.getType("[" + JvmPrimitiveType.get(primitiveArrayType).desc)
                return makeType(asmType)
            }
        }
        val javaFqName = JavaToKotlinClassMap.mapKotlinToJava(fqName.toUnsafe())?.asSingleFqName() ?: fqName
        return makeQualifiedName(javaFqName)
    }

    private fun convertAnnotationArgumentWithName(
        containingClass: ClassNode,
        constantValue: Any?,
        name: String,
    ): Expression? {
        if (!isValidIdentifier(name)) return null
        val initializer = convertLiteral(containingClass, constantValue)
        return makeAssignExpression(name, initializer)
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
                        && asm.zip(desc.value).all { [eAsm, eDesc] -> checkIfAnnotationValueMatches(eAsm, eDesc) }
            }

            is Type -> desc is KClassValue && typeMapper.mapKClassValue(desc) == asm
            is AnnotationNode -> {
                val annotationValue = (desc as? AnnotationValue)?.value ?: return false
                val arguments = annotationValue.argumentsMapping
                if (typeMapper.mapAnnotationClassId(annotationValue) != Type.getType(asm.desc)) return false
                val asmAnnotationArgs = pairedListToMap(asm.values)
                if (arguments.size != asmAnnotationArgs.size) return false

                for ([descName, descValue] in arguments) {
                    val asmValue = asmAnnotationArgs[descName.asString()] ?: return false
                    if (!checkIfAnnotationValueMatches(asmValue, descValue)) return false
                }

                true
            }

            else -> false
        }
    }

    private fun convertLiteral(containingClass: ClassNode, value: Any?): Expression {
        fun convertArray(elements: List<*>): Expression =
            makeArray(elements.map { convertLiteral(containingClass, it) })

        makeValueOfPrimitiveTypeOrString(value)?.let { return it }

        return when (value) {
            null -> makeNullLiteral()

            is ByteArray -> convertArray(value.asList())
            is BooleanArray -> convertArray(value.asList())
            is CharArray -> convertArray(value.asList())
            is ShortArray -> convertArray(value.asList())
            is IntArray -> convertArray(value.asList())
            is LongArray -> convertArray(value.asList())
            is FloatArray -> convertArray(value.asList())
            is DoubleArray -> convertArray(value.asList())
            is Array<*> -> { // Two-element String array for enumerations ([desc, fieldName])
                assert(value.size == 2)
                val enumType = Type.getType(value[0] as String)
                val valueName = (value[1] as String).takeIf { isValidIdentifier(it) } ?: run {
                    kaptContext.compiler.log.report(kaptContext.kaptError("'${value[1]}' is an invalid Java enum value name"))
                    "InvalidFieldName"
                }

                makeSelect(makeType(enumType), valueName)
            }

            is List<*> -> convertArray(value)

            is Type -> {
                checkIfValidTypeName(containingClass, value)
                makeSelect(makeType(value), "class")
            }

            is AnnotationNode ->
                convertAnnotation(containingClass, value, packageFqName = null)

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
        kaptContext.compiledClasses.mapNotNull(::getFileForClass).distinct().associateWith { file ->
            val importsFromRoot =
                file.importDirectives
                    .filter { !it.isAllUnder }
                    .mapNotNull { im -> im.importPath?.fqName?.takeIf { it.isOneSegmentFQN() } }
            importsFromRoot.mapTo(mutableSetOf()) { it.asString() }
        }

    //
    // making of stub elements
    //

    protected abstract fun makeNonExistentClassStub(): KaptStub

    protected abstract fun isNonExistentClass(type: Expression): Boolean

    protected abstract fun makeAnnotation(type: Expression, arguments: List<Expression>): Annotation

    internal abstract fun makeQualifiedName(javaName: String): Expression

    internal abstract fun makeQualifiedName(fqName: FqName): Expression

    internal abstract fun makeType(asmType: Type): Expression

    internal abstract fun makeSelect(left: Expression, simpleName: String): Expression

    internal abstract fun makeSimpleName(simpleName: String): Expression

    internal abstract fun makeArrayType(baseType: Expression, arrayDimensions: Int): Expression

    internal abstract fun makePrimitiveType(primitiveTypeSig: Char): Expression

    protected abstract fun makeArray(expressions: List<Expression>): Expression

    protected abstract fun makeField(
        field: FieldNode,
        modifiers: Modifiers,
        initializer: Expression?,
        convertedType: Expression
    ): VariableDecl

    protected abstract fun makeEnumValueInitializer(
        name: String,
        args: List<Expression>,
        def: ClassDecl?,
    ): Expression

    protected abstract fun makeClassDecl(
        clazz: ClassNode,
        simpleName: String,
        modifiers: Modifiers,
        genericType: ClassGenericSignature<Expression, TypeParameter>,
        superTypes: ClassSupertypes<Expression>,
        enumValues: List<VariableDecl>,
        sortedConvertedFields: List<VariableDecl>,
        sortedConvertedMethods: List<MethodDecl>,
        nestedClasses: List<ClassDecl>,
    ): ClassDecl

    protected abstract fun makeValueOfPrimitiveTypeOrString(value: Any?): Expression?

    protected abstract fun makeNullLiteral(): Expression

    protected abstract fun makeStubForTopLevelClass(
        declaration: IrDeclaration,
        lineMappings: KaptLineMappingCollector,
        packageName: String,
        clazz: ClassNode,
    ): KaptStub?

    internal abstract fun makeTypeApply(type: Expression, typeArguments: List<Expression>): Expression

    protected abstract fun makeAssignExpression(name: String, expression: Expression): Expression

    protected abstract fun makeSingleImport(fqName: FqName): Element
    protected abstract fun makeStarImport(fqName: FqName): Element

    protected abstract fun makeParameter(name: String, type: Expression, modifiers: Modifiers, isVararg: Boolean): VariableDecl
    protected abstract fun makeBlock(statements: List<Statement>): Block
    protected abstract fun makeSimpleCallStatement(name: String, args: List<Expression>): Statement
    protected abstract fun makeReturn(arg: Expression): Statement

    protected abstract fun makeMethod(
        method: MethodNode,
        irClass: IrClass,
        modifiers: Modifiers,
        genericSignature: SignatureParser.MethodGenericSignature<Expression, TypeParameter>,
        parameters: List<VariableDecl>,
        exceptionTypes: List<Expression>,
        body: Block?,
        defaultValue: Expression?,
    ): MethodDecl

    protected abstract fun makeModifiers(kind: ElementKind, isEnumField: Boolean, flags: Long, annotations: List<Annotation>): Modifiers

    internal abstract fun makeTypeParameter(name: String, allBounds: List<Expression>): TypeParameter
    internal abstract fun makeUnboundWildcard(): Expression
    internal abstract fun makeWildcard(asmWilcard: Char, bound: Expression): Expression
}

private class LegacyFunctionTypeKindProjector(private val session: FirSession) : AbstractConeSubstitutor(session.typeContext) {
    fun projectIfNeeded(type: ConeKotlinType): ConeKotlinType? =
        substituteOrNull(type)?.takeUnless { it === type }

    override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
        val classLikeType = type as? ConeClassLikeType ?: return null
        val functionTypeKind = classLikeType.functionTypeKind(session) ?: return null
        if (functionTypeKind.isBuiltin) return null

        val legacySerializationUntil =
            LanguageVersion.fromVersionString(functionTypeKind.serializeAsFunctionWithAnnotationUntil) ?: return null
        if (session.languageVersionSettings.languageVersion >= legacySerializationUntil) return null

        return classLikeType.customFunctionTypeToSimpleFunctionType(session)
    }
}

private fun Any?.isOfPrimitiveType(): Boolean = when (this) {
    is Boolean, is Byte, is Int, is Long, is Short, is Char, is Float, is Double, is UByte, is UShort, is UInt, is ULong -> true
    else -> false
}
