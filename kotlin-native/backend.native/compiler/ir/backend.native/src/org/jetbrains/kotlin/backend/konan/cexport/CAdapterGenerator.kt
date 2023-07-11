/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.cexport

import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.driver.phases.PsiToIrContext
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.isChildOf
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPublicApi
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isUnit

internal enum class ScopeKind {
    TOP,
    CLASS,
    PACKAGE
}

internal enum class ElementKind {
    FUNCTION,
    PROPERTY,
    TYPE
}

internal enum class DefinitionKind {
    C_HEADER_DECLARATION,
    C_HEADER_STRUCT,
    C_SOURCE_DECLARATION,
    C_SOURCE_STRUCT
}

private enum class Direction {
    KOTLIN_TO_C,
    C_TO_KOTLIN
}

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

internal class ExportedElementScope(val kind: ScopeKind, val name: String) {
    val elements = mutableListOf<ExportedElement>()
    val scopes = mutableListOf<ExportedElementScope>()
    private val scopeNames = mutableSetOf<String>()
    private val scopeNamesMap = mutableMapOf<Pair<DeclarationDescriptor, Boolean>, String>()

    override fun toString(): String {
        return "$kind: $name ${elements.joinToString(", ")} ${scopes.joinToString("\n")}"
    }

    // collects names of inner scopes to make sure function<->scope name clashes would be detected, and functions would be mangled with "_" suffix
    fun collectInnerScopeName(innerScope: ExportedElementScope) {
        scopeNames += innerScope.name
    }

    fun scopeUniqueName(descriptor: DeclarationDescriptor, shortName: Boolean): String {
        scopeNamesMap[descriptor to shortName]?.apply { return this }
        var computedName = when (descriptor) {
            is ConstructorDescriptor -> descriptor.constructedClass.fqNameSafe.shortName().asString()
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

@OptIn(ObsoleteDescriptorBasedAPI::class)
internal class ExportedElement(
        val kind: ElementKind,
        val scope: ExportedElementScope,
        val declaration: DeclarationDescriptor,
        val owner: CAdapterGenerator,
        val typeTranslator: CAdapterTypeTranslator,
) {
    init {
        scope.elements.add(this)
    }

    val name: String
        get() = declaration.fqNameSafe.shortName().asString()

    lateinit var cname: String

    override fun toString(): String {
        return "$kind: $name (aliased to ${if (::cname.isInitialized) cname else "<unknown>"})"
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

    val irSymbol = when {
        isFunction -> owner.symbolTable.referenceFunction(declaration as FunctionDescriptor)
        isClass -> owner.symbolTable.descriptorExtension.referenceClass(declaration as ClassDescriptor)
        isEnumEntry -> owner.symbolTable.descriptorExtension.referenceEnumEntry(declaration as ClassDescriptor)
        else -> error("unexpected $kind element: $declaration")
    }

    private fun KotlinType.includeToSignature() = !this.isUnit()

    private fun makeCFunctionSignature(shortName: Boolean): List<SignatureElement> {
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
            original is ConstructorDescriptor -> typeTranslator.builtIns.unitType
            else -> original.returnType!!
        }
        val params = ArrayList(original.allParameters
                .filter { it.type.includeToSignature() }
                .map {
                    typeTranslator.translateTypeBridge(it.type)
                })
        if (typeTranslator.isMappedToReference(returnedType) || typeTranslator.isMappedToString(returnedType)) {
            params += "KObjHeader**"
        }
        return listOf(typeTranslator.translateTypeBridge(returnedType)) + params
    }


    fun makeFunctionPointerString(): String {
        val signature = makeCFunctionSignature(true)
        return "${typeTranslator.translateType(signature[0])} (*${signature[0].name})(${signature.drop(1).map { "${typeTranslator.translateType(it)} ${it.name}" }.joinToString(", ")});"
    }

    fun makeTopLevelFunctionString(): Pair<String, String> {
        val signature = makeCFunctionSignature(false)
        val name = signature[0].name
        return (name to
                "extern ${typeTranslator.translateType(signature[0])} $name(${signature.drop(1).map { "${typeTranslator.translateType(it)} ${it.name}" }.joinToString(", ")});")
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
            val objectClassC = typeTranslator.translateType((declaration as ClassDescriptor).defaultType)
            """
            |
            |extern "C" KObjHeader* ${cname}_instance(KObjHeader**);
            |static $objectClassC ${cname}_instance_impl(void) {
            |  Kotlin_initRuntimeIfNeeded();
            |  ScopedRunnableState stateGuard;
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
        val enumClassC = typeTranslator.translateType(enumClass.defaultType)

        return """
              |extern "C" KObjHeader* $cname(KObjHeader**);
              |static $enumClassC ${cname}_impl(void) {
              |  Kotlin_initRuntimeIfNeeded();
              |  ScopedRunnableState stateGuard;
              |  KObjHolder result_holder;
              |  KObjHeader* result = $cname(result_holder.slot());
              |  return $enumClassC { .pinned = CreateStablePointer(result)};
              |}
              """.trimMargin()
    }

    private fun translateArgument(name: String, signatureElement: SignatureElement,
                                  direction: Direction, builder: StringBuilder): String {
        return when {
            typeTranslator.isMappedToString(signatureElement.type) ->
                if (direction == Direction.C_TO_KOTLIN) {
                    builder.append("  KObjHolder ${name}_holder;\n")
                    "CreateStringFromCString($name, ${name}_holder.slot())"
                } else {
                    "CreateCStringFromString($name)"
                }
            typeTranslator.isMappedToReference(signatureElement.type) ->
                if (direction == Direction.C_TO_KOTLIN) {
                    builder.append("  KObjHolder ${name}_holder2;\n")
                    "DerefStablePointer(${name}.pinned, ${name}_holder2.slot())"
                } else {
                    "((${typeTranslator.translateType(signatureElement.type)}){ .pinned = CreateStablePointer(${name})})"
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
        builder.append("$visibility ${typeTranslator.translateType(cfunction[0])} ${cnameImpl}(${cfunction.drop(1).
                mapIndexed { index, it -> "${typeTranslator.translateType(it)} arg${index}" }.joinToString(", ")}) {\n")
        // TODO: do we really need that in every function?
        builder.append("  Kotlin_initRuntimeIfNeeded();\n")
        builder.append("  ScopedRunnableState stateGuard;\n")
        builder.append("  FrameOverlay* frame = getCurrentFrame();")
        val args = ArrayList(cfunction.drop(1).mapIndexed { index, pair ->
            translateArgument("arg$index", pair, Direction.C_TO_KOTLIN, builder)
        })
        val isVoidReturned = typeTranslator.isMappedToVoid(cfunction[0].type)
        val isConstructor = declaration is ConstructorDescriptor
        val isObjectReturned = !isConstructor && typeTranslator.isMappedToReference(cfunction[0].type)
        val isStringReturned = typeTranslator.isMappedToString(cfunction[0].type)
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
        builder.append("   } catch (...) {")
        builder.append("       SetCurrentFrame(reinterpret_cast<KObjHeader**>(frame));\n")
        builder.append("       HandleCurrentExceptionWhenLeavingKotlinCode();\n")
        builder.append("   } \n")

        builder.append("}\n")

        return builder.toString()
    }

    private fun addUsedType(type: KotlinType, set: MutableSet<KotlinType>) {
        if (type.constructor.declarationDescriptor is TypeParameterDescriptor) return
        set.add(type)
    }

    fun addUsedTypes(set: MutableSet<KotlinType>) {
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
                set += descriptor.defaultType
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

/**
 * First phase of C export: walk given declaration descriptors and create [CAdapterExportedElements] from them.
 */
internal class CAdapterGenerator(
        private val context: PsiToIrContext,
        private val configuration: CompilerConfiguration,
        private val typeTranslator: CAdapterTypeTranslator,
) : DeclarationDescriptorVisitor<Boolean, Void?> {
    private val scopes = mutableListOf<ExportedElementScope>()
    internal val prefix = typeTranslator.prefix
    private val paramNamesRecorded = mutableMapOf<String, Int>()

    internal val symbolTable get() = context.symbolTable!!

    internal fun paramsToUniqueNames(params: List<ParameterDescriptor>): Map<ParameterDescriptor, String> {
        paramNamesRecorded.clear()
        return params.associate {
            val name = translateName(it.name)
            val count = paramNamesRecorded.getOrPut(name) { 0 }
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
        ExportedElement(ElementKind.FUNCTION, scopes.last(), descriptor, this, typeTranslator)
        return true
    }

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, ignored: Void?): Boolean {
        if (!isExportedFunction(descriptor)) return true
        ExportedElement(ElementKind.FUNCTION, scopes.last(), descriptor, this, typeTranslator)
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
        ExportedElement(ElementKind.TYPE, scopes.last(), descriptor, this, typeTranslator)
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
        ExportedElement(ElementKind.FUNCTION, scopes.last(), descriptor, this, typeTranslator)
        return true
    }

    override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, ignored: Void?): Boolean {
        if (!isExportedFunction(descriptor)) return true
        ExportedElement(ElementKind.FUNCTION, scopes.last(), descriptor, this, typeTranslator)
        return true
    }

    override fun visitScriptDescriptor(descriptor: ScriptDescriptor, ignored: Void?) = true

    override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, ignored: Void?): Boolean {
        if (descriptor.module !in moduleDescriptors) return true
        val fragments = descriptor.module.getPackage(FqName.ROOT).fragments.filter {
            it.module in moduleDescriptors }
        visitChildren(fragments)

        // K2 does not serialize empty package fragments, thus breaking the scope chain.
        // The following traverse definitely reaches every subpackage fragment.
        scopes.push(getPackageScope(FqName.ROOT))
        val subfragments = descriptor.module.getSubPackagesOf(FqName.ROOT) { true }
                .flatMap {
                    descriptor.module.getPackage(it).fragments.filter {
                        it.module in moduleDescriptors
                    }
                }
        visitChildren(subfragments)
        scopes.pop()

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
        val packageScope = getPackageScope(fqName)
        scopes.push(packageScope)
        if (!seenPackageFragments.contains(descriptor))
            visitChildren(DescriptorUtils.getAllDescriptors(descriptor.getMemberScope()))
        for (currentPackageFragment in currentPackageFragments) {
            if (!seenPackageFragments.contains(currentPackageFragment) &&
                    currentPackageFragment.fqName.isChildOf(descriptor.fqName)) {
                visitChildren(currentPackageFragment)
                seenPackageFragments += currentPackageFragment
            }
        }
        scopes.pop()
        return true
    }

    private fun getPackageScope(fqName: FqName) = packageScopes.getOrPut(fqName) {
        val name = if (fqName.isRoot) "root" else translateName(fqName.shortName())
        val scope = ExportedElementScope(ScopeKind.PACKAGE, name)
        scopes.last().scopes += scope
        scope
    }


    private val moduleDescriptors = mutableSetOf<ModuleDescriptor>()

    fun buildExports(moduleDescriptor: ModuleDescriptor): CAdapterExportedElements {
        scopes.push(ExportedElementScope(ScopeKind.TOP, "kotlin"))
        moduleDescriptors += moduleDescriptor
        moduleDescriptors += moduleDescriptor.getExportedDependencies(context.config)

        currentPackageFragments = moduleDescriptors.flatMap { it.getPackageFragments() }.toSet().sortedWith(
                Comparator { o1, o2 ->
                    o1.fqName.toString().compareTo(o2.fqName.toString())
                })

        moduleDescriptor.getPackage(FqName.ROOT).accept(this, null)
        return CAdapterExportedElements(typeTranslator, scopes)
    }

    private val simpleNameMapping = mapOf(
            "<this>" to "thiz",
            "<set-?>" to "set"
    )

    private fun translateName(name: Name): String {
        val nameString = name.asString()
        return when {
            simpleNameMapping.contains(nameString) -> simpleNameMapping[nameString]!!
            cKeywords.contains(nameString) -> "${nameString}_"
            name.isSpecial -> nameString.replace("[<> ]".toRegex(), "_")
            else -> nameString
        }
    }

    private var functionIndex = 0
    fun nextFunctionIndex() = functionIndex++
}
