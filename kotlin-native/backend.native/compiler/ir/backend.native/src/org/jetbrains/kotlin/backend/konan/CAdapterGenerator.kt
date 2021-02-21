/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import kotlinx.cinterop.cValuesOf
import java.io.PrintWriter
import llvm.*
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.common.descriptors.explicitParameters
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.konan.ir.isOverridable
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.name.isChildOf
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.annotations.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.addIfNotNull

private enum class ScopeKind {
    TOP,
    CLASS,
    PACKAGE
}

private enum class ElementKind {
    FUNCTION,
    PROPERTY,
    TYPE
}

private enum class DefinitionKind {
    C_HEADER_DECLARATION,
    C_HEADER_STRUCT,
    C_SOURCE_DECLARATION,
    C_SOURCE_STRUCT
}

private enum class Direction {
    KOTLIN_TO_C,
    C_TO_KOTLIN
}

private operator fun String.times(count: Int): String {
    val builder = StringBuilder()
    repeat(count, { builder.append(this) })
    return builder.toString()
}

private val KotlinType.shortNameForPredefinedType
    get() = this.toString().split('.').last()


private val KotlinType.createNullableNameForPredefinedType
        get() = "createNullable${this.shortNameForPredefinedType}"

internal val cKeywords = setOf(
        // Actual C keywords.
        "auto", "break", "case",
        "char", "const", "continue",
        "default", "do", "double",
        "else", "enum", "extern",
        "float", "for", "goto",
        "if", "int", "long",
        "register", "return",
        "short", "signed", "sizeof", "static", "struct", "switch",
        "typedef", "union", "unsigned",
        "void", "volatile", "while",
        // C99-specific.
        "_Bool", "_Complex", "_Imaginary", "inline", "restrict",
        // C11-specific.
        "_Alignas", "_Alignof", "_Atomic", "_Generic", "_Noreturn", "_Static_assert", "_Thread_local",
        // Not exactly keywords, but reserved or standard-defined.
        "and", "not", "or", "xor",
        "bool", "complex", "imaginary",

         // C++ keywords not listed above.
        "alignas", "alignof", "and_eq", "asm",
        "bitand", "bitor", "bool",
        "catch", "char16_t", "char32_t", "class", "compl", "constexpr", "const_cast",
        "decltype", "delete", "dynamic_cast",
        "explicit", "export",
        "false", "friend",
        "inline",
        "mutable",
        "namespace", "new", "noexcept", "not_eq", "nullptr",
        "operator", "or_eq",
        "private", "protected", "public",
        "reinterpret_cast",
        "static_assert",
        "template", "this", "thread_local", "throw", "true", "try", "typeid", "typename",
        "using",
        "virtual",
        "wchar_t",
        "xor_eq"
)

private fun isExportedFunction(descriptor: FunctionDescriptor): Boolean {
    if (!descriptor.isEffectivelyPublicApi || !descriptor.kind.isReal || descriptor.isExpect)
        return false
    if (descriptor.isSuspend)
        return false
    return !descriptor.typeParameters.any()
}

private fun isExportedClass(descriptor: ClassDescriptor): Boolean {
    if (!descriptor.isEffectivelyPublicApi) return false
    // No sense to export annotations.
    if (DescriptorUtils.isAnnotationClass(descriptor)) return false
    // Do not export expect classes.
    if (descriptor.isExpect) return false
    // Do not export types with type parameters.
    // TODO: is it correct?
    if (!descriptor.declaredTypeParameters.isEmpty()) return false
    // Do not export inline classes for now. TODO: add proper support.
    if (descriptor.isInlined()) return false

    return true
}

internal fun AnnotationDescriptor.properValue(key: String) =
        this.argumentValue(key)?.toString()?.removeSurrounding("\"")

private fun functionImplName(descriptor: DeclarationDescriptor, default: String, shortName: Boolean): String {
    assert(descriptor is FunctionDescriptor)
    val annotation = descriptor.annotations.findAnnotation(RuntimeNames.cnameAnnotation) ?: return default
    val key = if (shortName) "shortName" else "externName"
    val value = annotation.properValue(key)
    return value.takeIf { value != null && value.isNotEmpty() } ?: default
}

internal data class SignatureElement(val name: String, val type: KotlinType)

private class ExportedElementScope(val kind: ScopeKind, val name: String) {
    val elements = mutableListOf<ExportedElement>()
    val scopes = mutableListOf<ExportedElementScope>()
    private val scopeNames = mutableSetOf<String>()
    private val scopeNamesMap = mutableMapOf<Pair<DeclarationDescriptor, Boolean>, String>()

    override fun toString(): String {
        return "$kind: $name ${elements.joinToString(", ")} ${scopes.joinToString("\n")}"
    }

    fun generateCAdapters() {
        elements.forEach {
            it.generateCAdapter()
        }
        scopes.forEach {
            it.generateCAdapters()
        }
    }

    fun scopeUniqueName(descriptor: DeclarationDescriptor, shortName: Boolean): String {
        scopeNamesMap[descriptor to shortName]?.apply { return this }
        var computedName = when (descriptor) {
            is ConstructorDescriptor -> "${descriptor.constructedClass.fqNameSafe.shortName().asString()}"
            is PropertyGetterDescriptor -> "get_${descriptor.correspondingProperty.name.asString()}"
            is PropertySetterDescriptor -> "set_${descriptor.correspondingProperty.name.asString()}"
            is FunctionDescriptor -> functionImplName(descriptor, descriptor.fqNameSafe.shortName().asString(), shortName)
            else -> descriptor.fqNameSafe.shortName().asString()
        }
        while (scopeNames.contains(computedName) || cKeywords.contains(computedName)) {
            computedName += "_"
        }
        scopeNames += computedName
        scopeNamesMap[descriptor to shortName] = computedName
        return computedName
    }
}

private class ExportedElement(val kind: ElementKind,
                              val scope: ExportedElementScope,
                              val declaration: DeclarationDescriptor,
                              val owner: CAdapterGenerator) : ContextUtils {
    init {
        scope.elements.add(this)
    }

    val name: String
        get() = declaration.fqNameSafe.shortName().asString()

    lateinit var cname: String

    override fun toString(): String {
        return "$kind: $name (aliased to ${if (::cname.isInitialized) cname.toString() else "<unknown>"})"
    }

    override val context = owner.context

    fun generateCAdapter() {
        when {
            isFunction -> {
                val function = declaration as FunctionDescriptor
                val irFunction = irSymbol.owner as IrFunction
                cname = "_konan_function_${owner.nextFunctionIndex()}"
                val llvmFunction = owner.codegen.llvmFunction(irFunction)
                // If function is virtual, we need to resolve receiver properly.
                val bridge = if (!DescriptorUtils.isTopLevelDeclaration(function) &&
                        irFunction.isOverridable) {
                    // We need LLVMGetElementType() as otherwise type is function pointer.
                    generateFunction(owner.codegen, LLVMGetElementType(llvmFunction.type)!!, cname) {
                        val receiver = param(0)
                        val numParams = LLVMCountParams(llvmFunction)
                        val args = (0 .. numParams - 1).map { index -> param(index) }
                        val callee = lookupVirtualImpl(receiver, irFunction)
                        val result = call(callee, args, exceptionHandler = ExceptionHandler.Caller, verbatim = true)
                        ret(result)
                    }
                } else {
                    LLVMAddAlias(context.llvmModule, llvmFunction.type, llvmFunction, cname)!!
                }
                LLVMSetLinkage(bridge, LLVMLinkage.LLVMExternalLinkage)
            }
            isClass -> {
                val irClass = irSymbol.owner as IrClass
                cname = "_konan_function_${owner.nextFunctionIndex()}"
                // Produce type getter.
                val getTypeFunction = addLlvmFunctionWithDefaultAttributes(
                        context,
                        context.llvmModule!!,
                        "${cname}_type",
                        owner.kGetTypeFuncType
                )
                val builder = LLVMCreateBuilderInContext(llvmContext)!!
                val bb = LLVMAppendBasicBlockInContext(llvmContext, getTypeFunction, "")!!
                LLVMPositionBuilderAtEnd(builder, bb)
                LLVMBuildRet(builder, irClass.typeInfoPtr.llvm)
                LLVMDisposeBuilder(builder)
                // Produce instance getter if needed.
                if (isSingletonObject) {
                    generateFunction(owner.codegen, owner.kGetObjectFuncType, "${cname}_instance") {
                        val value = getObjectValue(irClass, ExceptionHandler.Caller, null)
                        ret(value)
                    }
                }
            }
            isEnumEntry -> {
                // Produce entry getter.
                cname = "_konan_function_${owner.nextFunctionIndex()}"
                generateFunction(owner.codegen, owner.kGetObjectFuncType, cname) {
                    val irEnumEntry = irSymbol.owner as IrEnumEntry
                    val value = getEnumEntry(irEnumEntry, ExceptionHandler.Caller)
                    ret(value)
                }
            }
        }
    }

    fun uniqueName(descriptor: DeclarationDescriptor, shortName: Boolean) =
            scope.scopeUniqueName(descriptor, shortName)

    val isFunction = declaration is FunctionDescriptor
    val isTopLevelFunction: Boolean
        get() {
            if (declaration !is FunctionDescriptor ||
                    !declaration.annotations.hasAnnotation(RuntimeNames.cnameAnnotation))
                return false
            val annotation = declaration.annotations.findAnnotation(RuntimeNames.cnameAnnotation)!!
            val externName = annotation.properValue("externName")
            return externName != null && externName.isNotEmpty()
        }
    val isClass = declaration is ClassDescriptor && declaration.kind != ClassKind.ENUM_ENTRY
    val isEnumEntry = declaration is ClassDescriptor && declaration.kind == ClassKind.ENUM_ENTRY
    val isSingletonObject = declaration is ClassDescriptor && DescriptorUtils.isObject(declaration)

    private val irSymbol = when {
        isFunction -> owner.symbolTable.referenceFunction(declaration as FunctionDescriptor)
        isClass -> owner.symbolTable.referenceClass(declaration as ClassDescriptor)
        isEnumEntry -> owner.symbolTable.referenceEnumEntry(declaration as ClassDescriptor)
        else -> error("unexpected $kind element: $declaration")
    }

    fun KotlinType.includeToSignature() = !this.isUnit()

    fun makeCFunctionSignature(shortName: Boolean): List<SignatureElement> {
        if (!isFunction) {
            throw Error("only for functions")
        }
        val descriptor = declaration
        val original = descriptor.original as FunctionDescriptor
        val returned = when {
            original is ConstructorDescriptor ->
                SignatureElement(uniqueName(original, shortName), original.constructedClass.defaultType)
            else ->
                SignatureElement(uniqueName(original, shortName), original.returnType!!)
        }
        val uniqueNames = owner.paramsToUniqueNames(original.explicitParameters)
        val params = ArrayList(original.explicitParameters
                .filter { it.type.includeToSignature() }
                .map { SignatureElement(uniqueNames[it]!!, it.type) })
        return listOf(returned) + params
    }

    fun makeBridgeSignature(): List<String> {
        if (!isFunction) {
            throw Error("only for functions")
        }
        val descriptor = declaration
        val original = descriptor.original as FunctionDescriptor
        val returnedType = when {
            original is ConstructorDescriptor -> owner.context.builtIns.unitType
            else -> original.returnType!!
        }
        val params = ArrayList(original.allParameters
                .filter { it.type.includeToSignature() }
                .map {
                    owner.translateTypeBridge(it.type)
                })
        if (owner.isMappedToReference(returnedType) || owner.isMappedToString(returnedType)) {
            params += "KObjHeader**"
        }
        return listOf(owner.translateTypeBridge(returnedType)) + params
    }


    fun makeFunctionPointerString(): String {
        val signature = makeCFunctionSignature(true)
        return "${owner.translateType(signature[0])} (*${signature[0].name})(${signature.drop(1).map { it -> "${owner.translateType(it)} ${it.name}" }.joinToString(", ")});"
    }

    fun makeTopLevelFunctionString(): Pair<String, String> {
        val signature = makeCFunctionSignature(false)
        val name = signature[0].name
        return (name to
                "extern ${owner.translateType(signature[0])} $name(${signature.drop(1).map { it -> "${owner.translateType(it)} ${it.name}" }.joinToString(", ")});")
    }

    fun makeFunctionDeclaration(): String {
        assert(isFunction)
        val bridge = makeBridgeSignature()

        val builder = StringBuilder()
        builder.append("extern \"C\" ${bridge[0]} $cname")
        builder.append("(${bridge.drop(1).joinToString(", ")});\n")

        // Now the C function body.
        builder.append(translateBody(makeCFunctionSignature(false)))
        return builder.toString()
    }

    fun makeClassDeclaration(): String {
        assert(isClass)
        val typeGetter = "extern \"C\" ${owner.prefix}_KType* ${cname}_type(void);"
        val instanceGetter = if (isSingletonObject) {
            val objectClassC = owner.translateType((declaration as ClassDescriptor).defaultType)
            """
            |
            |extern "C" KObjHeader* ${cname}_instance(KObjHeader**);
            |static $objectClassC ${cname}_instance_impl(void) {
            |  Kotlin_initRuntimeIfNeeded();
            |  KObjHolder result_holder;
            |  KObjHeader* result = ${cname}_instance(result_holder.slot());
            |  return $objectClassC { .pinned = CreateStablePointer(result)};
            |}
            """.trimMargin()
        } else ""
        return "$typeGetter$instanceGetter"
    }

    fun makeEnumEntryDeclaration(): String {
        assert(isEnumEntry)
        val enumClass = declaration.containingDeclaration as ClassDescriptor
        val enumClassC = owner.translateType(enumClass.defaultType)

        return """
              |extern "C" KObjHeader* $cname(KObjHeader**);
              |static $enumClassC ${cname}_impl(void) {
              |  Kotlin_initRuntimeIfNeeded();
              |  KObjHolder result_holder;
              |  KObjHeader* result = $cname(result_holder.slot());
              |  return $enumClassC { .pinned = CreateStablePointer(result)};
              |}
              """.trimMargin()
    }

    private fun translateArgument(name: String, signatureElement: SignatureElement,
                                  direction: Direction, builder: StringBuilder): String {
        return when {
            owner.isMappedToString(signatureElement.type) ->
                if (direction == Direction.C_TO_KOTLIN) {
                    builder.append("  KObjHolder ${name}_holder;\n")
                    "CreateStringFromCString($name, ${name}_holder.slot())"
                } else {
                    "CreateCStringFromString($name)"
                }
            owner.isMappedToReference(signatureElement.type) ->
                if (direction == Direction.C_TO_KOTLIN) {
                    builder.append("  KObjHolder ${name}_holder2;\n")
                    "DerefStablePointer(${name}.pinned, ${name}_holder2.slot())"
                } else {
                    "((${owner.translateType(signatureElement.type)}){ .pinned = CreateStablePointer(${name})})"
                }
            else -> {
                assert(!signatureElement.type.binaryTypeIsReference()) {
                    println(signatureElement.toString())
                }
                name
            }
        }
    }

    val cnameImpl: String
        get() = if (isTopLevelFunction)
            functionImplName(declaration, "******" /* Default value must never be used. */, false)
        else
            "${cname}_impl"

    private fun translateBody(cfunction: List<SignatureElement>): String {
        val visibility = if (isTopLevelFunction) "RUNTIME_USED extern \"C\"" else "static"
        val builder = StringBuilder()
        builder.append("$visibility ${owner.translateType(cfunction[0])} ${cnameImpl}(${cfunction.drop(1).
                mapIndexed { index, it -> "${owner.translateType(it)} arg${index}" }.joinToString(", ")}) {\n")
        val args = ArrayList(cfunction.drop(1).mapIndexed { index, pair ->
            translateArgument("arg$index", pair, Direction.C_TO_KOTLIN, builder)
        })
        val isVoidReturned = owner.isMappedToVoid(cfunction[0].type)
        val isConstructor = declaration is ConstructorDescriptor
        val isObjectReturned = !isConstructor && owner.isMappedToReference(cfunction[0].type)
        val isStringReturned = owner.isMappedToString(cfunction[0].type)
        // TODO: do we really need that in every function?
        builder.append("  Kotlin_initRuntimeIfNeeded();\n")
        builder.append("   try {\n")
        if (isObjectReturned || isStringReturned) {
            builder.append("  KObjHolder result_holder;\n")
            args += "result_holder.slot()"
        }
        if (isConstructor) {
            builder.append("  KObjHolder result_holder;\n")
            val clazz = scope.elements[0]
            assert(clazz.kind == ElementKind.TYPE)
            builder.append("  KObjHeader* result = AllocInstance((const KTypeInfo*)${clazz.cname}_type(), result_holder.slot());\n")
            args.add(0, "result")
        }
        if (!isVoidReturned && !isConstructor) {
            builder.append("  auto result = ")
        }
        builder.append("  $cname(")
        builder.append(args.joinToString(", "))
        builder.append(");\n")

        if (!isVoidReturned) {
            val result = translateArgument(
                    "result", cfunction[0], Direction.KOTLIN_TO_C, builder)
            builder.append("  return $result;\n")
        }
        builder.append("   } catch (ExceptionObjHolder& e) { TerminateWithUnhandledException(e.GetExceptionObject()); } \n")

        builder.append("}\n")

        return builder.toString()
    }

    private fun addUsedType(type: KotlinType, set: MutableSet<ClassDescriptor>) {
        if (type.constructor.declarationDescriptor is TypeParameterDescriptor) return
        set.addIfNotNull(TypeUtils.getClassDescriptor(type))
    }

    fun addUsedTypes(set: MutableSet<ClassDescriptor>) {
        val descriptor = declaration
        when (descriptor) {
            is FunctionDescriptor -> {
                val original = descriptor.original
                original.allParameters.forEach { addUsedType(it.type, set) }
                original.returnType?.let { addUsedType(it, set) }
            }
            is PropertyAccessorDescriptor -> {
                val original = descriptor.original
                addUsedType(original.correspondingProperty.type, set)
            }
            is ClassDescriptor -> {
                set += descriptor
            }
        }
    }
}

private fun getPackagesFqNames(module: ModuleDescriptor): Set<FqName> {
    val result = mutableSetOf<FqName>()

    fun getSubPackages(fqName: FqName) {
        result.add(fqName)
        module.getSubPackagesOf(fqName) { true }.forEach { getSubPackages(it) }
    }

    getSubPackages(FqName.ROOT)
    return result
}

private fun ModuleDescriptor.getPackageFragments(): List<PackageFragmentDescriptor> =
        getPackagesFqNames(this).flatMap {
            getPackage(it).fragments.filter { it.module == this }
        }

internal class CAdapterGenerator(val context: Context) : DeclarationDescriptorVisitor<Boolean, Void?> {

    private val scopes = mutableListOf<ExportedElementScope>()
    internal val prefix = context.config.moduleId
    private lateinit var outputStreamWriter: PrintWriter
    private val paramNamesRecorded = mutableMapOf<String, Int>()

    private var codegenOrNull: CodeGenerator? = null
    internal val codegen get() = codegenOrNull!!

    private var symbolTableOrNull: SymbolTable? = null
    internal val symbolTable get() = symbolTableOrNull!!

    private val predefinedTypes = listOf(
            context.builtIns.byteType, context.builtIns.shortType,
            context.builtIns.intType, context.builtIns.longType,
            context.builtIns.floatType, context.builtIns.doubleType,
            context.builtIns.charType, context.builtIns.booleanType,
            context.builtIns.unitType)

    internal fun paramsToUniqueNames(params: List<ParameterDescriptor>): Map<ParameterDescriptor, String> {
        paramNamesRecorded.clear()
        return params.associate {
            val name = translateName(it.name.asString()) 
            val count = paramNamesRecorded.getOrDefault(name, 0)
            paramNamesRecorded[name] = count + 1
            if (count == 0) {
                it to name
            } else {
                it to "$name${count.toString()}"
            }
        }
    }

    private fun visitChildren(descriptors: Collection<DeclarationDescriptor>) {
        for (descriptor in descriptors) {
            descriptor.accept(this, null)
        }
    }

    private fun visitChildren(descriptor: DeclarationDescriptor) {
        descriptor.accept(this, null)
    }

    override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, ignored: Void?): Boolean {
        if (!isExportedFunction(descriptor)) return true
        ExportedElement(ElementKind.FUNCTION, scopes.last(), descriptor, this)
        return true
    }

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, ignored: Void?): Boolean {
        if (!isExportedFunction(descriptor)) return true
        ExportedElement(ElementKind.FUNCTION, scopes.last(), descriptor, this)
        return true
    }

    override fun visitClassDescriptor(descriptor: ClassDescriptor, ignored: Void?): Boolean {
        if (!isExportedClass(descriptor)) return true
        // TODO: fix me!
        val shortName = descriptor.fqNameSafe.shortName()
        if (shortName.isSpecial || shortName.asString().contains("<anonymous>"))
            return true
        val classScope = ExportedElementScope(ScopeKind.CLASS, shortName.asString())
        scopes.last().scopes += classScope
        scopes.push(classScope)
        // Add type getter.
        ExportedElement(ElementKind.TYPE, scopes.last(), descriptor, this)
        visitChildren(descriptor.getConstructors())
        visitChildren(DescriptorUtils.getAllDescriptors(descriptor.getDefaultType().memberScope))
        scopes.pop()
        return true
    }

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, ignored: Void?): Boolean {
        if (descriptor.isExpect) return true
        descriptor.getter?.let { visitChildren(it) }
        descriptor.setter?.let { visitChildren(it) }
        return true
    }

    override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, ignored: Void?): Boolean {
        if (!isExportedFunction(descriptor)) return true
        ExportedElement(ElementKind.FUNCTION, scopes.last(), descriptor, this)
        return true
    }

    override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, ignored: Void?): Boolean {
        if (!isExportedFunction(descriptor)) return true
        ExportedElement(ElementKind.FUNCTION, scopes.last(), descriptor, this)
        return true
    }

    override fun visitScriptDescriptor(descriptor: ScriptDescriptor, ignored: Void?) = true

    override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, ignored: Void?): Boolean {
        if (descriptor.module !in moduleDescriptors) return true
        val fragments = descriptor.module.getPackage(FqName.ROOT).fragments.filter {
            it.module in moduleDescriptors }
        visitChildren(fragments)
        return true
    }

    override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, ignored: Void?): Boolean {
        TODO("visitValueParameterDescriptor() shall not be seen")
    }

    override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor?, ignored: Void?): Boolean {
        TODO("visitReceiverParameterDescriptor() shall not be seen")
    }

    override fun visitVariableDescriptor(descriptor: VariableDescriptor, ignored: Void?) = true

    override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, ignored: Void?) = true

    private val seenPackageFragments = mutableSetOf<PackageFragmentDescriptor>()
    private var currentPackageFragments: List<PackageFragmentDescriptor> = emptyList()
    private val packageScopes = mutableMapOf<FqName, ExportedElementScope>()

    override fun visitModuleDeclaration(descriptor: ModuleDescriptor, ignored: Void?): Boolean {
        TODO("Shall not be called directly")
    }

    override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, ignored: Void?) = true

    override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, ignored: Void?): Boolean {
        val fqName = descriptor.fqName
        val packageScope = packageScopes.getOrPut(fqName) {
            val name = if (fqName.isRoot) "root" else translateName(fqName.shortName().asString())
            val scope = ExportedElementScope(ScopeKind.PACKAGE, name)
            scopes.last().scopes += scope
            scope
        }
        scopes.push(packageScope)
        visitChildren(DescriptorUtils.getAllDescriptors(descriptor.getMemberScope()))
        for (currentPackageFragment in currentPackageFragments) {
            if (!seenPackageFragments.contains(currentPackageFragment) &&
                    currentPackageFragment.fqName.isChildOf(descriptor.fqName)) {
                seenPackageFragments += currentPackageFragment
                visitChildren(currentPackageFragment)
            }
        }
        scopes.pop()
        return true
    }


    private val moduleDescriptors = mutableSetOf<ModuleDescriptor>()

    fun buildExports(symbolTable: SymbolTable) {
        this.symbolTableOrNull = symbolTable
        try {
            buildExports()
        } finally {
            this.symbolTableOrNull = null
        }
    }

    fun generateBindings(codegen: CodeGenerator) {
        this.codegenOrNull = codegen
        try {
            generateBindings()
        } finally {
            this.codegenOrNull = null
        }
    }

    private fun buildExports() {
        scopes.push(ExportedElementScope(ScopeKind.TOP, "kotlin"))
        moduleDescriptors += context.moduleDescriptor
        moduleDescriptors += context.getExportedDependencies()

        currentPackageFragments = moduleDescriptors.flatMap { it.getPackageFragments() }.toSet().sortedWith(
                Comparator { o1, o2 ->
                    o1.fqName.toString().compareTo(o2.fqName.toString())
                })

        context.moduleDescriptor.getPackage(FqName.ROOT).accept(this, null)
    }

    private fun generateBindings() {
        val top = scopes.pop()
        assert(scopes.isEmpty() && top.kind == ScopeKind.TOP)

        // Now, let's generate C world adapters for all functions.
        top.generateCAdapters()

        // Then generate data structure, describing generated adapters.
        makeGlobalStruct(top)
    }

    private fun output(string: String, indent: Int = 0) {
        if (indent != 0) outputStreamWriter.print("  " * indent)
        outputStreamWriter.println(string)
    }

    private fun makeElementDefinition(element: ExportedElement, kind: DefinitionKind, indent: Int) {
        when (kind) {
            DefinitionKind.C_HEADER_DECLARATION -> {
                when {
                    element.isTopLevelFunction -> {
                        val (name, declaration) = element.makeTopLevelFunctionString()
                        exportedSymbols += name
                        output(declaration, 0)
                    }
                }
            }

            DefinitionKind.C_HEADER_STRUCT -> {
                when {
                    element.isFunction ->
                        output(element.makeFunctionPointerString(), indent)
                    element.isClass -> {
                        output("${prefix}_KType* (*_type)(void);", indent)
                        if (element.isSingletonObject) {
                            output("${translateType((element.declaration as ClassDescriptor).defaultType)} (*_instance)();", indent)
                        }
                    }
                    element.isEnumEntry -> {
                        val enumClass = element.declaration.containingDeclaration as ClassDescriptor
                        output("${translateType(enumClass.defaultType)} (*get)(); /* enum entry for ${element.name}. */", indent)
                    }
                // TODO: handle properties.
                }
            }

            DefinitionKind.C_SOURCE_DECLARATION -> {
                when {
                    element.isFunction ->
                        output(element.makeFunctionDeclaration(), 0)
                    element.isClass ->
                        output(element.makeClassDeclaration(), 0)
                    element.isEnumEntry ->
                        output(element.makeEnumEntryDeclaration(), 0)
                // TODO: handle properties.
                }
            }

            DefinitionKind.C_SOURCE_STRUCT -> {
                when {
                    element.isFunction ->
                        output("/* ${element.name} = */ ${element.cnameImpl}, ", indent)
                    element.isClass -> {
                        output("/* Type for ${element.name} = */  ${element.cname}_type, ", indent)
                        if (element.isSingletonObject)
                            output("/* Instance for ${element.name} = */ ${element.cname}_instance_impl, ", indent)
                    }
                    element.isEnumEntry ->
                        output("/* enum entry getter ${element.name} = */  ${element.cname}_impl,", indent)
                // TODO: handle properties.
                }
            }
        }
    }

    private fun makeScopeDefinitions(scope: ExportedElementScope, kind: DefinitionKind, indent: Int) {
        if (kind == DefinitionKind.C_HEADER_STRUCT) output("struct {", indent)
        if (kind == DefinitionKind.C_SOURCE_STRUCT) output(".${scope.name} = {", indent)
        scope.elements.forEach { makeElementDefinition(it, kind, indent + 1) }
        scope.scopes.forEach { makeScopeDefinitions(it, kind, indent + 1) }
        if (kind == DefinitionKind.C_HEADER_STRUCT) output("} ${scope.name};", indent)
        if (kind == DefinitionKind.C_SOURCE_STRUCT) output("},", indent)
    }

    private fun defineUsedTypesImpl(scope: ExportedElementScope, set: MutableSet<ClassDescriptor>) {
        scope.elements.forEach {
            it.addUsedTypes(set)
        }
        scope.scopes.forEach {
            defineUsedTypesImpl(it, set)
        }
    }

    private fun defineUsedTypes(scope: ExportedElementScope, indent: Int) {
        val set = mutableSetOf<ClassDescriptor>()
        defineUsedTypesImpl(scope, set)
        // Add nullable primitives.
        predefinedTypes.forEach {
            val nullableIt = it.makeNullable()
            output("typedef struct {", indent)
            output("${prefix}_KNativePtr pinned;", indent + 1)
            output("} ${translateType(nullableIt)};", indent)
        }
        set.forEach {
            val type = it.defaultType
            if (isMappedToReference(type) && !it.isInlined()) {
                output("typedef struct {", indent)
                output("${prefix}_KNativePtr pinned;", indent + 1)
                output("} ${translateType(type)};", indent)
            }
        }
    }

    val exportedSymbols = mutableListOf<String>()

    private fun makeGlobalStruct(top: ExportedElementScope) {
        val headerFile = context.config.outputFiles.cAdapterHeader
        outputStreamWriter = headerFile.printWriter()

        val exportedSymbol = "${prefix}_symbols"
        exportedSymbols += exportedSymbol

        output("#ifndef KONAN_${prefix.toUpperCase()}_H")
        output("#define KONAN_${prefix.toUpperCase()}_H")
        // TODO: use namespace for C++ case?
        output("""
        #ifdef __cplusplus
        extern "C" {
        #endif""".trimIndent())
        output("""
        #ifdef __cplusplus
        typedef bool            ${prefix}_KBoolean;
        #else
        typedef _Bool           ${prefix}_KBoolean;
        #endif
        """.trimIndent())
        output("typedef unsigned short     ${prefix}_KChar;")
        output("typedef signed char        ${prefix}_KByte;")
        output("typedef short              ${prefix}_KShort;")
        output("typedef int                ${prefix}_KInt;")
        output("typedef long long          ${prefix}_KLong;")
        output("typedef unsigned char      ${prefix}_KUByte;")
        output("typedef unsigned short     ${prefix}_KUShort;")
        output("typedef unsigned int       ${prefix}_KUInt;")
        output("typedef unsigned long long ${prefix}_KULong;")
        output("typedef float              ${prefix}_KFloat;")
        output("typedef double             ${prefix}_KDouble;")

        val typedef_KVector128 = "typedef float __attribute__ ((__vector_size__ (16))) ${prefix}_KVector128;"
        if (context.config.target.family == Family.MINGW) {
            // Separate `output` for each line to ensure Windows EOL (LFCR), otherwise generated file will have inconsistent line ending.
            output("#ifndef _MSC_VER")
            output(typedef_KVector128)
            output("#else")
            output("#include <xmmintrin.h>")
            output("typedef __m128 ${prefix}_KVector128;")
            output("#endif")
        } else {
            output(typedef_KVector128)
        }

        output("typedef void*              ${prefix}_KNativePtr;")
        output("struct ${prefix}_KType;")
        output("typedef struct ${prefix}_KType ${prefix}_KType;")

        output("")
        defineUsedTypes(top, 0)

        output("")
        makeScopeDefinitions(top, DefinitionKind.C_HEADER_DECLARATION, 0)

        output("")
        output("typedef struct {")
        output("/* Service functions. */", 1)
        output("void (*DisposeStablePointer)(${prefix}_KNativePtr ptr);", 1)
        output("void (*DisposeString)(const char* string);", 1)
        output("${prefix}_KBoolean (*IsInstance)(${prefix}_KNativePtr ref, const ${prefix}_KType* type);", 1)
        predefinedTypes.forEach {
            val nullableIt = it.makeNullable()
            val argument = if (!it.isUnit()) translateType(it) else "void"
            output("${translateType(nullableIt)} (*${it.createNullableNameForPredefinedType})($argument);", 1)
        }

        output("")
        output("/* User functions. */", 1)
        makeScopeDefinitions(top, DefinitionKind.C_HEADER_STRUCT, 1)
        output("} ${prefix}_ExportedSymbols;")

        output("extern ${prefix}_ExportedSymbols* $exportedSymbol(void);")
        output("""
        #ifdef __cplusplus
        }  /* extern "C" */
        #endif""".trimIndent())

        output("#endif  /* KONAN_${prefix.toUpperCase()}_H */")

        outputStreamWriter.close()
        println("Produced library API in ${prefix}_api.h")

        outputStreamWriter = context.config.tempFiles
                .cAdapterCpp
                .printWriter()

        // Include header into C++ source.
        headerFile.forEachLine { it -> output(it) }

        output("""
        |struct KObjHeader;
        |typedef struct KObjHeader KObjHeader;
        |struct KTypeInfo;
        |typedef struct KTypeInfo KTypeInfo;
        |
        |#define RUNTIME_NOTHROW __attribute__((nothrow))
        |#define RUNTIME_USED __attribute__((used))
        |#define RUNTIME_NORETURN __attribute__((noreturn))
        |
        |extern "C" {
        |void UpdateStackRef(KObjHeader**, const KObjHeader*) RUNTIME_NOTHROW;
        |KObjHeader* AllocInstance(const KTypeInfo*, KObjHeader**) RUNTIME_NOTHROW;
        |KObjHeader* DerefStablePointer(void*, KObjHeader**) RUNTIME_NOTHROW;
        |void* CreateStablePointer(KObjHeader*) RUNTIME_NOTHROW;
        |void DisposeStablePointer(void*) RUNTIME_NOTHROW;
        |${prefix}_KBoolean IsInstance(const KObjHeader*, const KTypeInfo*) RUNTIME_NOTHROW;
        |void EnterFrame(KObjHeader** start, int parameters, int count) RUNTIME_NOTHROW;
        |void LeaveFrame(KObjHeader** start, int parameters, int count) RUNTIME_NOTHROW;
        |void Kotlin_initRuntimeIfNeeded();
        |void TerminateWithUnhandledException(KObjHeader*) RUNTIME_NORETURN;
        |
        |KObjHeader* CreateStringFromCString(const char*, KObjHeader**);
        |char* CreateCStringFromString(const KObjHeader*);
        |void DisposeCString(char* cstring);
        |}  // extern "C"
        |
        |struct ${prefix}_FrameOverlay {
        |  void* arena;
        |  ${prefix}_FrameOverlay* previous;
        |  ${prefix}_KInt parameters;
        |  ${prefix}_KInt count;
        |};
        |
        |class KObjHolder {
        |public:
        |  KObjHolder() : obj_(nullptr) {
        |    EnterFrame(frame(), 0, sizeof(*this)/sizeof(void*));
        |  }
        |  explicit KObjHolder(const KObjHeader* obj) : obj_(nullptr) {
        |    EnterFrame(frame(), 0, sizeof(*this)/sizeof(void*));
        |    UpdateStackRef(&obj_, obj);
        |  }
        |  ~KObjHolder() {
        |    LeaveFrame(frame(), 0, sizeof(*this)/sizeof(void*));
        |  }
        |  KObjHeader* obj() { return obj_; }
        |  KObjHeader** slot() { return &obj_; }
        | private:
        |  ${prefix}_FrameOverlay frame_;
        |  KObjHeader* obj_;
        |
        |  KObjHeader** frame() { return reinterpret_cast<KObjHeader**>(&frame_); }
        |};
        |
        |class ExceptionObjHolder {
        |public:
        |    virtual ~ExceptionObjHolder() = default;
        |
        |    KObjHeader* GetExceptionObject() noexcept;
        |};
        |
        |static void DisposeStablePointerImpl(${prefix}_KNativePtr ptr) {
        |  DisposeStablePointer(ptr);
        |}
        |static void DisposeStringImpl(const char* ptr) {
        |  DisposeCString((char*)ptr);
        |}
        |static ${prefix}_KBoolean IsInstanceImpl(${prefix}_KNativePtr ref, const ${prefix}_KType* type) {
        |  KObjHolder holder;
        |  return IsInstance(DerefStablePointer(ref, holder.slot()), (const KTypeInfo*)type);
        |}
        """.trimMargin())
        predefinedTypes.forEach {
            assert(!it.isNothing())
            val nullableIt = it.makeNullable()
            val needArgument = !it.isUnit()
            val (parameter, maybeComma) = if (needArgument)
                ("${translateType(it)} value" to ",") else ("" to "")
            val argument = if (needArgument) "value, " else ""
            output("extern \"C\" KObjHeader* Kotlin_box${it.shortNameForPredefinedType}($parameter$maybeComma KObjHeader**);")
            output("static ${translateType(nullableIt)} ${it.createNullableNameForPredefinedType}Impl($parameter) {")
            output("Kotlin_initRuntimeIfNeeded();", 1)
            output("KObjHolder result_holder;", 1)
            output("KObjHeader* result = Kotlin_box${it.shortNameForPredefinedType}($argument result_holder.slot());", 1)
            output("return ${translateType(nullableIt)} { .pinned = CreateStablePointer(result) };", 1)
            output("}")
        }
        makeScopeDefinitions(top, DefinitionKind.C_SOURCE_DECLARATION, 0)
        output("static ${prefix}_ExportedSymbols __konan_symbols = {")
        output(".DisposeStablePointer = DisposeStablePointerImpl,", 1)
        output(".DisposeString = DisposeStringImpl,", 1)
        output(".IsInstance = IsInstanceImpl,", 1)
        predefinedTypes.forEach {
            output(".${it.createNullableNameForPredefinedType} = ${it.createNullableNameForPredefinedType}Impl,", 1)
        }

        makeScopeDefinitions(top, DefinitionKind.C_SOURCE_STRUCT, 1)
        output("};")
        output("RUNTIME_USED ${prefix}_ExportedSymbols* $exportedSymbol(void) { return &__konan_symbols;}")
        outputStreamWriter.close()

        if (context.config.target.family == Family.MINGW) {
            outputStreamWriter = context.config.outputFiles
                    .cAdapterDef
                    .printWriter()
            output("EXPORTS")
            exportedSymbols.forEach { output(it) }
            outputStreamWriter.close()
        }
    }

    private val simpleNameMapping = mapOf(
            "<this>" to "thiz",
            "<set-?>" to "set"
    )

    private val primitiveTypeMapping = KonanPrimitiveType.values().associate {
        it to when (it) {
            KonanPrimitiveType.BOOLEAN -> "${prefix}_KBoolean"
            KonanPrimitiveType.CHAR -> "${prefix}_KChar"
            KonanPrimitiveType.BYTE -> "${prefix}_KByte"
            KonanPrimitiveType.SHORT -> "${prefix}_KShort"
            KonanPrimitiveType.INT -> "${prefix}_KInt"
            KonanPrimitiveType.LONG -> "${prefix}_KLong"
            KonanPrimitiveType.FLOAT -> "${prefix}_KFloat"
            KonanPrimitiveType.DOUBLE -> "${prefix}_KDouble"
            KonanPrimitiveType.NON_NULL_NATIVE_PTR -> "void*"
            KonanPrimitiveType.VECTOR128 -> "${prefix}_KVector128"
        }
    }

    private val unsignedTypeMapping = UnsignedType.values().associate {
        it.classId to when (it) {
            UnsignedType.UBYTE -> "${prefix}_KUByte"
            UnsignedType.USHORT -> "${prefix}_KUShort"
            UnsignedType.UINT -> "${prefix}_KUInt"
            UnsignedType.ULONG -> "${prefix}_KULong"
        }
    }

    internal fun isMappedToString(type: KotlinType): Boolean =
            isMappedToString(type.computeBinaryType())

    private fun isMappedToString(binaryType: BinaryType<ClassDescriptor>): Boolean =
            when (binaryType) {
                is BinaryType.Primitive -> false
                is BinaryType.Reference -> binaryType.types.first() == context.builtIns.string
            }

    internal fun isMappedToReference(type: KotlinType) =
            !isMappedToVoid(type) && !isMappedToString(type) &&
                    type.binaryTypeIsReference()

    internal fun isMappedToVoid(type: KotlinType): Boolean {
        return type.isUnit() || type.isNothing()
    }

    fun translateName(name: String): String {
        return when {
            simpleNameMapping.contains(name) -> simpleNameMapping[name]!!
            cKeywords.contains(name) -> "${name}_"
            else -> name
        }
    }

    private fun translateTypeFull(type: KotlinType): Pair<String, String> =
            if (isMappedToVoid(type)) {
                "void" to "void"
            } else {
                translateNonVoidTypeFull(type)
            }

    private fun translateNonVoidTypeFull(type: KotlinType): Pair<String, String> = type.unwrapToPrimitiveOrReference(
            eachInlinedClass = { inlinedClass, _ ->
                unsignedTypeMapping[inlinedClass.classId]?.let {
                    return it to it
                }
            },
            ifPrimitive = { primitiveType, _ ->
                primitiveTypeMapping[primitiveType]!!.let { it to it }
            },
            ifReference = {
                val clazz = (it.computeBinaryType() as BinaryType.Reference).types.first()
                if (clazz == context.builtIns.string) {
                    "const char*" to "KObjHeader*"
                } else {
                    "${prefix}_kref_${translateTypeFqName(clazz.fqNameSafe.asString())}" to "KObjHeader*"
                }
            }
    )

    fun translateType(element: SignatureElement): String =
            translateTypeFull(element.type).first

    fun translateType(type: KotlinType): String
            = translateTypeFull(type).first

    fun translateTypeBridge(type: KotlinType): String = translateTypeFull(type).second

    fun translateTypeFqName(name: String): String {
        return name.replace('.', '_')
    }

    private var functionIndex = 0
    fun nextFunctionIndex() = functionIndex++

    internal val kGetTypeFuncType get() =
            LLVMFunctionType(codegen.kTypeInfoPtr, null, 0, 0)!!
    // Abstraction leak for slot :(.
    internal val kGetObjectFuncType get() =
            LLVMFunctionType(codegen.kObjHeaderPtr, cValuesOf(codegen.kObjHeaderPtrPtr), 1, 0)!!
}
