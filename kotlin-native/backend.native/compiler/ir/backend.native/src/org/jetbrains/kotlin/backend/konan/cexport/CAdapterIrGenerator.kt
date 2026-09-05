/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.cexport

import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.isInlined
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getAnnotationArgumentValue
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.util.isStatic
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.FqName

/**
 * Walks the declaration of an enclosing declaration and all its containers, returning `true` iff every visibility
 * on the way is public-API (public/protected). This is the IR equivalent of the descriptor
 * `isEffectivelyPublicApi`.
 */
private fun IrDeclaration.isEffectivelyPublicApi(): Boolean {
    var declaration: IrDeclaration? = this
    while (declaration != null) {
        if (declaration is IrDeclarationWithVisibility && !declaration.visibility.isPublicAPI) {
            return false
        }
        declaration = declaration.parent as? IrDeclaration
    }
    return true
}

private fun isExportedFunction(function: IrFunction): Boolean {
    if (!function.isEffectivelyPublicApi()) return false
    // Synthetic enum members (`values`/`valueOf`) are not part of the exported surface. In the K1 mode they are
    // absent from the deserialized member scope; in IR they are present, so filter them here.
    if (function.origin == IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER) return false
    if (function is IrSimpleFunction && function.isFakeOverride) return false
    if (function.isExpect) return false
    if (function is IrSimpleFunction && function.isSuspend) return false
    if (function.parameters.any { it.kind == IrParameterKind.Context }) return false
    // Exclude generic functions — but NOT property accessors: a getter/setter has no type parameters of its own,
    // yet in IR it carries the property's ones. K1's accessor descriptor reports none, so it exports e.g. the
    // `KSerializer<T>.nullable` getter; match that by applying the exclusion to non-accessors only.
    val isAccessor = function is IrSimpleFunction && function.correspondingPropertySymbol != null
    if (!isAccessor && function.typeParameters.isNotEmpty()) return false
    // Companion members are not part of the exported surface (mirrors the K1
    // `isDeserializedAndHasCompanionExtensionReceiver`). Two IR shapes: companion-extension members carry a
    // companion-extension class, while companion-block members are compiled as static functions. This matches
    // Kotlin's own notion of a companion member: `isStatic || companionExtensionClass != null`.
    if (function is IrSimpleFunction && (function.isStatic || function.companionExtensionClass != null)) return false
    return true
}

private fun isExportedClass(irClass: IrClass): Boolean {
    if (!irClass.isEffectivelyPublicApi()) return false
    // No sense to export annotations.
    if (irClass.kind == ClassKind.ANNOTATION_CLASS) return false
    // Do not export expect classes.
    if (irClass.isExpect) return false
    // Do not export types with type parameters.
    if (irClass.typeParameters.isNotEmpty()) return false
    // Do not export inline classes for now.
    if (irClass.isInlined()) return false
    return true
}

private fun isExportedEnumEntry(entry: IrEnumEntry): Boolean = entry.isEffectivelyPublicApi()

private fun IrClass.hasSpecialName(): Boolean = name.isSpecial || name.asString().contains("<anonymous>")

/** IR-backed [ExportedDeclarationKey]; keys [ExportedElementScope]'s name cache by the declaration. */
private data class IrDeclarationNameKey(val declaration: IrDeclaration) : ExportedDeclarationKey

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class ExportedElementIr(
        override val kind: ElementKind,
        override val scope: ExportedElementScope,
        private val declaration: IrDeclaration,
        private val generator: CAdapterIrGenerator,
        private val typeTranslator: CAdapterIrTypeTranslator,
) : ExportedElement {
    init {
        scope.elements.add(this)
    }

    override val owner: CAdapterModelOwner get() = generator

    override lateinit var cname: String

    override val name: String
        get() = (declaration as IrDeclarationWithName).name.asString()

    override fun toString(): String =
            "$kind: $name (aliased to ${if (::cname.isInitialized) cname else "<unknown>"})"

    override val isFunction = declaration is IrFunction
    override val isConstructor = declaration is IrConstructor
    override val isClass = declaration is IrClass
    override val isEnumEntry = declaration is IrEnumEntry
    override val isSingletonObject = declaration is IrClass && declaration.isObject

    override val isTopLevelFunction: Boolean
        get() = (declaration as? IrFunction)?.cNameValue("externName") != null

    override val irSymbol: IrSymbol = when (declaration) {
        is IrFunction -> declaration.symbol
        is IrClass -> declaration.symbol
        is IrEnumEntry -> declaration.symbol
        else -> error("unexpected $kind element: $declaration")
    }

    override val cnameImpl: String
        get() = if (isTopLevelFunction) {
            (declaration as IrFunction).cNameValue("externName")!!
        } else {
            "${cname}_impl"
        }

    override val classType: String
        get() = typeTranslator.translateType((declaration as IrClass).defaultType)

    override val enumEntryContainingType: String
        get() = typeTranslator.translateType((declaration as IrEnumEntry).parentAsClass.defaultType)

    private fun IrFunction.cNameValue(key: String): String? =
            getAnnotationArgumentValue<String>(RuntimeNames.cnameAnnotation, key)?.takeIf { it.isNotEmpty() }

    // Parameters that participate in the exported ("public") C signature: everything but the context parameters.
    private fun IrFunction.explicitCParameters(): List<IrValueParameter> =
            parameters.filter { it.kind != IrParameterKind.Context }

    private fun IrFunction.cParameterTypes(): List<IrType> = buildList {
        if (this@cParameterTypes is IrConstructor) add(parentAsClass.defaultType)
        explicitCParameters().forEach { add(it.type) }
    }

    private val IrFunction.cReturnType: IrType
        get() = if (this is IrConstructor) parentAsClass.defaultType else returnType

    private fun uniqueName(function: IrFunction, shortName: Boolean): String {
        val property = (function as? IrSimpleFunction)?.correspondingPropertySymbol?.owner
        val baseName = when {
            function is IrConstructor -> function.parentAsClass.name.asString()
            property != null && property.getter == function -> "get_${property.name.asString()}"
            property != null && property.setter == function -> "set_${property.name.asString()}"
            else -> function.cNameValue(if (shortName) "shortName" else "externName") ?: function.name.asString()
        }
        return scope.scopeUniqueName(IrDeclarationNameKey(function), shortName, baseName)
    }

    override fun makeCFunctionSignature(shortName: Boolean): List<SignatureElement> {
        val function = declaration as? IrFunction ?: throw Error("only for functions")
        val returned = SignatureElement(uniqueName(function, shortName), typeTranslator.exportedType(function.cReturnType))

        val explicitParameters = function.explicitCParameters()
        val uniqueNames = generator.paramsToUniqueNames(explicitParameters)
        val params = explicitParameters
                .filter { !it.type.isUnit() }
                .map { SignatureElement(uniqueNames.getValue(it), typeTranslator.exportedType(it.type)) }
        return listOf(returned) + params
    }

    override fun makeBridgeSignature(): List<String> {
        val function = declaration as? IrFunction ?: throw Error("only for functions")
        // A constructor's bridge returns Unit (the instance is passed in, not returned), so it does not use cReturnType.
        val returnType = if (function is IrConstructor) generator.unitType else function.returnType

        val params = function.cParameterTypes()
                .filter { !it.isUnit() }
                .map { typeTranslator.translateTypeBridge(it) }
                .toMutableList()
        if (typeTranslator.isMappedToReference(returnType) || typeTranslator.isMappedToString(returnType)) {
            params += "KObjHeader**"
        }
        return listOf(typeTranslator.translateTypeBridge(returnType)) + params
    }

    private fun addUsedType(type: IrType, set: MutableSet<CExportedType>) {
        if (type.classifierOrNull is IrTypeParameterSymbol) return
        set.add(typeTranslator.exportedType(type))
    }

    override fun addUsedTypes(set: MutableSet<CExportedType>) {
        when (val decl = declaration) {
            is IrFunction -> {
                // Accessors are handled here too (not specially): in K1 `PropertyAccessorDescriptor` is a
                // `FunctionDescriptor`, so it takes the function branch — recording all parameter types (including
                // the extension receiver) plus the return type.
                decl.cParameterTypes().forEach {
                    addUsedType(it, set)
                }
                addUsedType(decl.cReturnType, set)
            }
            is IrClass -> addUsedType(decl.defaultType, set)
            is IrEnumEntry -> {
                // The entry's own synthetic type has no dedicated IrType, but K1 exposes it as a `kref`.
                val fqName = decl.parentAsClass.fqNameWhenAvailable!!.child(decl.name).asString()
                set.add(CExportedReferenceTypeByFqName(generator.prefix, fqName))
            }
        }
    }
}

/**
 * First phase of C export, IR variant: walks the linked IR of the exported modules and builds
 * [CAdapterExportedElements]. It mirrors the descriptor-based [CAdapterGenerator].
 */
internal class CAdapterIrGenerator(
        override val prefix: String,
        private val irBuiltIns: IrBuiltIns,
) : CAdapterModelOwner {
    private val typeTranslator = CAdapterIrTypeTranslator(prefix)

    val unitType: IrType get() = irBuiltIns.unitType

    private var functionIndex = 0
    override fun nextFunctionIndex(): Int = functionIndex++

    fun buildExports(fragments: List<IrModuleFragment>): CAdapterExportedElements {
        val top = ExportedElementScope(ScopeKind.TOP, "kotlin")

        // Group each package's top-level declarations by source file, preserving file order. K1 processes a package
        // one metadata fragment (source file) at a time, so ordering is per-file, not merged across the package.
        val filesByPackage = LinkedHashMap<FqName, MutableList<List<IrDeclaration>>>()
        for (fragment in fragments) {
            for (file in fragment.files) {
                filesByPackage.getOrPut(file.packageFqName) { mutableListOf() }
                        .add(file.declarations)
            }
        }

        // The "root" scope is the single child of the top scope and holds the whole package hierarchy. In K1,
        // `getPackageScope(FqName.ROOT)` is named "root"; root-package declarations and every named subpackage
        // are nested under it (the top "kotlin" scope itself never gets declarations directly).
        val rootScope = ExportedElementScope(ScopeKind.PACKAGE, "root")
        top.scopes += rootScope
        packageScopes[FqName.ROOT] = rootScope

        val namedPackages = filesByPackage.keys
                .filter { !it.isRoot }
                .sortedBy { it.asString() }

        // Two passes, so within every scope the subpackages precede the package's own classes (as in K1). Pass 1
        // materializes the whole package-scope tree in fq-name order; pass 2 appends each package's declarations.
        for (packageFqName in namedPackages) {
            getPackageScope(packageFqName)
        }

        filesByPackage[FqName.ROOT]?.let {
            populateScope(rootScope, it, isClassScope = false)
        }
        for (packageFqName in namedPackages) {
            val scope = getPackageScope(packageFqName)
            populateScope(scope, filesByPackage.getValue(packageFqName), isClassScope = false)
        }

        return CAdapterExportedElements(prefix, mutableListOf(top))
    }

    private val packageScopes = mutableMapOf<FqName, ExportedElementScope>()

    private fun getPackageScope(fqName: FqName): ExportedElementScope = packageScopes.getOrPut(fqName) {
        val parent = fqName.parent()
        val parentScope = if (parent.isRoot) packageScopes.getValue(FqName.ROOT) else getPackageScope(parent)
        val scope = ExportedElementScope(ScopeKind.PACKAGE, translateName(fqName.shortName()))
        parentScope.scopes += scope
        scope
    }

    private fun buildClassScope(irClass: IrClass, parentScope: ExportedElementScope) {
        val classScope = ExportedElementScope(ScopeKind.CLASS, irClass.name.asString())
        parentScope.scopes += classScope
        // Type getter (also produces the `_instance` getter for singleton objects).
        ExportedElementIr(ElementKind.TYPE, classScope, irClass, this, typeTranslator)
        populateScope(classScope, listOf(irClass.declarations), isClassScope = true)
    }

    private fun buildEnumEntryScope(entry: IrEnumEntry, parentScope: ExportedElementScope) {
        val entryScope = ExportedElementScope(ScopeKind.CLASS, entry.name.asString())
        parentScope.scopes += entryScope
        ExportedElementIr(ElementKind.TYPE, entryScope, entry, this, typeTranslator)
    }

    /**
     * Populates [scope] from declarations grouped per source file ([fileGroups]), reproducing the K1 order:
     *  - sub-scopes: nested classes and enum entries, in declaration order across the files;
     *  - constructors (class scope only), in declaration order;
     *  - callables: within each file sorted by [memberComparator] (matching the deserializer's
     *    `NameAndTypeMemberComparator`), the files then concatenated in order.
     */
    private fun populateScope(scope: ExportedElementScope, fileGroups: List<List<IrDeclaration>>, isClassScope: Boolean) {
        for (declarations in fileGroups) {
            for (declaration in declarations) {
                when {
                    declaration is IrClass && isExportedClass(declaration) && !declaration.hasSpecialName() ->
                        buildClassScope(declaration, scope)
                    declaration is IrEnumEntry && isExportedEnumEntry(declaration) ->
                        buildEnumEntryScope(declaration, scope)
                }
            }
        }
        if (isClassScope) {
            // K1 reads constructors via `getConstructors()`, whose deserialized order is
            // `computeSecondaryConstructors() + primaryConstructor` — i.e. secondaries first (in declaration order),
            // then the primary. IR lists the primary first, so move it last (stable keeps the secondaries' order).
            val constructors = fileGroups.flatMap { group -> group.filterIsInstance<IrConstructor>() }
                    .filter { isExportedFunction(it) }
                    .sortedBy { it.isPrimary }
            for (constructor in constructors) {
                ExportedElementIr(ElementKind.FUNCTION, scope, constructor, this, typeTranslator)
            }
        }
        for (declarations in fileGroups) {
            val members = declarations.mapNotNull { declaration ->
                when (declaration) {
                    is IrProperty -> declaration.takeUnless { it.isExpect || it.origin == IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER }
                    is IrSimpleFunction ->
                        declaration.takeIf { it.correspondingPropertySymbol == null && isExportedFunction(it) }
                    else -> null
                }
            }.sortedWith(memberComparator)
            for (member in members) {
                when (member) {
                    is IrProperty -> {
                        member.getter?.let { if (isExportedFunction(it)) ExportedElementIr(ElementKind.FUNCTION, scope, it, this, typeTranslator) }
                        member.setter?.let { if (isExportedFunction(it)) ExportedElementIr(ElementKind.FUNCTION, scope, it, this, typeTranslator) }
                    }
                    is IrSimpleFunction -> ExportedElementIr(ElementKind.FUNCTION, scope, member, this, typeTranslator)
                }
            }
        }
    }

    // Callable ordering within a source file, matching the deserializer's `NameAndTypeMemberComparator`: by
    // declaration priority, then name.
    // See core/descriptors/src/org/jetbrains/kotlin/resolve/MemberComparator.java
    private val memberComparator: Comparator<IrDeclaration> =
            compareByDescending<IrDeclaration> { memberPriority(it) }.thenBy { memberName(it) }

    private fun memberPriority(declaration: IrDeclaration): Int = when (declaration) {
        is IrProperty -> if (declaration.hasExtensionReceiver()) 5 else 6
        is IrSimpleFunction -> if (declaration.hasExtensionReceiver()) 3 else 4
        else -> 0
    }

    private fun memberName(declaration: IrDeclaration): String =
            (declaration as IrDeclarationWithName).name.asString()

    fun paramsToUniqueNames(params: List<IrValueParameter>): Map<IrValueParameter, String> {
        val paramNames = mutableMapOf<String, Int>()
        return params.associateWith { param ->
            val name = translateName(param.name)
            val count = paramNames.getOrPut(name) { 0 }
            paramNames[name] = count + 1
            if (count == 0) name else "$name$count"
        }
    }
}

private fun IrProperty.hasExtensionReceiver(): Boolean =
        (getter ?: setter)?.hasExtensionReceiver() == true

private fun IrSimpleFunction.hasExtensionReceiver(): Boolean =
        parameters.any { it.kind == IrParameterKind.ExtensionReceiver }
