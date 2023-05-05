/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.backend.konan.ir.getAnnotationArgumentValue
import org.jetbrains.kotlin.backend.konan.ir.parentDeclarationsWithSelf
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBasedClassConstructorDescriptor
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.ExternalOverridabilityCondition
import org.jetbrains.kotlin.resolve.constants.BooleanValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.supertypes

internal val interopPackageName = InteropFqNames.packageName
internal val objCObjectFqName = interopPackageName.child(Name.identifier("ObjCObject"))
private val objCClassFqName = interopPackageName.child(Name.identifier("ObjCClass"))
private val objCProtocolFqName = interopPackageName.child(Name.identifier("ObjCProtocol"))
internal val externalObjCClassFqName = interopPackageName.child(Name.identifier("ExternalObjCClass"))
internal val objCDirectFqName = interopPackageName.child(Name.identifier("ObjCDirect"))
internal val objCMethodFqName = interopPackageName.child(Name.identifier("ObjCMethod"))
internal val objCConstructorFqName = FqName("kotlinx.cinterop.ObjCConstructor")
internal val objCFactoryFqName = interopPackageName.child(Name.identifier("ObjCFactory"))
private val objcnamesForwardDeclarationsPackageName = Name.identifier("objcnames")

fun ClassDescriptor.isObjCClass(): Boolean =
                this.containingDeclaration.fqNameSafe != interopPackageName &&
        this.getAllSuperClassifiers().any { it.fqNameSafe == objCObjectFqName } // TODO: this is not cheap. Cache me!

fun KotlinType.isObjCObjectType(): Boolean =
        (this.supertypes() + this).any { TypeUtils.getClassDescriptor(it)?.fqNameSafe == objCObjectFqName }

private fun IrClass.selfOrAnySuperClass(pred: (IrClass) -> Boolean): Boolean {
    if (pred(this)) return true

    return superTypes.any { it.classOrNull!!.owner.selfOrAnySuperClass(pred) }
}

internal fun IrClass.isObjCClass() = this.packageFqName != interopPackageName &&
        selfOrAnySuperClass { it.hasEqualFqName(objCObjectFqName) }

fun ClassDescriptor.isExternalObjCClass(): Boolean = this.isObjCClass() &&
        this.parentsWithSelf.filterIsInstance<ClassDescriptor>().any {
            it.annotations.findAnnotation(externalObjCClassFqName) != null
        }
fun IrClass.isExternalObjCClass(): Boolean = this.isObjCClass() &&
        (this as IrDeclaration).parentDeclarationsWithSelf.filterIsInstance<IrClass>().any {
            it.annotations.hasAnnotation(externalObjCClassFqName)
        }

fun ClassDescriptor.isObjCForwardDeclaration(): Boolean =
        this.findPackage().fqName.startsWith(objcnamesForwardDeclarationsPackageName)

fun ClassDescriptor.isObjCMetaClass(): Boolean = this.getAllSuperClassifiers().any {
    it.fqNameSafe == objCClassFqName
}

fun IrClass.isObjCMetaClass(): Boolean = selfOrAnySuperClass {
    it.hasEqualFqName(objCClassFqName)
}

fun IrClass.isObjCProtocolClass(): Boolean = hasEqualFqName(objCProtocolFqName)

fun ClassDescriptor.isObjCProtocolClass(): Boolean =
        this.fqNameSafe == objCProtocolFqName

fun FunctionDescriptor.isObjCClassMethod() =
        this.containingDeclaration.let { it is ClassDescriptor && it.isObjCClass() }

fun IrFunction.isObjCClassMethod() =
        this.parent.let { it is IrClass && it.isObjCClass() }

fun FunctionDescriptor.isExternalObjCClassMethod() =
        this.containingDeclaration.let { it is ClassDescriptor && it.isExternalObjCClass() }

internal fun IrFunction.isExternalObjCClassMethod() =
    this.parent.let {it is IrClass && it.isExternalObjCClass()}

// Special case: methods from Kotlin Objective-C classes can be called virtually from bridges.
fun FunctionDescriptor.canObjCClassMethodBeCalledVirtually(overriddenDescriptor: FunctionDescriptor) =
        overriddenDescriptor.isOverridable && this.kind.isReal && !this.isExternalObjCClassMethod()

internal fun IrFunction.canObjCClassMethodBeCalledVirtually(overridden: IrFunction) =
    overridden.isOverridable && !this.isFakeOverride && !this.isExternalObjCClassMethod()

fun ClassDescriptor.isKotlinObjCClass(): Boolean = this.isObjCClass() && !this.isExternalObjCClass()

fun IrClass.isKotlinObjCClass(): Boolean = this.isObjCClass() && !this.isExternalObjCClass()


data class ObjCMethodInfo(val selector: String,
                          val encoding: String,
                          val isStret: Boolean,
                          val directSymbol: String?)

private fun FunctionDescriptor.decodeObjCMethodAnnotation(): ObjCMethodInfo? {
    assert (this.kind.isReal)

    val methodInfo = this.annotations.findAnnotation(objCMethodFqName)?.let {
        ObjCMethodInfo(
                selector = it.getStringValue("selector"),
                encoding = it.getStringValue("encoding"),
                isStret = it.getArgumentValueOrNull<Boolean>("isStret") ?: false,
                directSymbol = this.annotations.findAnnotation(objCDirectFqName)?.getStringValue("symbol"),
        )
    }

    return methodInfo
}

private fun IrFunction.decodeObjCMethodAnnotation(): ObjCMethodInfo? {
    assert (this.isReal)

    val methodInfo = this.annotations.findAnnotation(objCMethodFqName)?.let {
        ObjCMethodInfo(
                selector = it.getAnnotationStringValue("selector"),
                encoding = it.getAnnotationStringValue("encoding"),
                isStret = it.getAnnotationValueOrNull<Boolean>("isStret") ?: false,
                directSymbol = this.annotations.findAnnotation(objCDirectFqName)?.getAnnotationStringValue("symbol"),
        )
    }

    return methodInfo
}

private fun objCMethodInfo(annotation: AnnotationDescriptor) = ObjCMethodInfo(
        selector = annotation.getStringValue("selector"),
        encoding = annotation.getStringValue("encoding"),
        isStret = annotation.getArgumentValueOrNull<Boolean>("isStret") ?: false,
        directSymbol = null,
)

private fun objCMethodInfo(annotation: IrConstructorCall) = ObjCMethodInfo(
        selector = annotation.getAnnotationStringValue("selector"),
        encoding = annotation.getAnnotationStringValue("encoding"),
        isStret = annotation.getAnnotationValueOrNull<Boolean>("isStret") ?: false,
        directSymbol = null,
)

/**
 * @param onlyExternal indicates whether to accept overriding methods from Kotlin classes
 */
private fun FunctionDescriptor.getObjCMethodInfo(onlyExternal: Boolean): ObjCMethodInfo? {
    if (this.kind.isReal) {
        this.decodeObjCMethodAnnotation()?.let { return it }

        if (onlyExternal) {
            return null
        }
    }

    return overriddenDescriptors.firstNotNullOfOrNull { it.getObjCMethodInfo(onlyExternal) }
}

/**
 * @param onlyExternal indicates whether to accept overriding methods from Kotlin classes
 */
private fun IrSimpleFunction.getObjCMethodInfo(onlyExternal: Boolean): ObjCMethodInfo? {
    if (this.isReal) {
        this.decodeObjCMethodAnnotation()?.let { return it }

        if (onlyExternal) {
            return null
        }
    }

    return overriddenSymbols.firstNotNullOfOrNull {
        assert(it.owner != this) { "Function ${it.owner.descriptor.fqNameSafe}() is wrongly contained in its own overriddenSymbols"}
        it.owner.getObjCMethodInfo(onlyExternal)
    }
}

fun FunctionDescriptor.getExternalObjCMethodInfo(): ObjCMethodInfo? = this.getObjCMethodInfo(onlyExternal = true)

fun IrFunction.getExternalObjCMethodInfo(): ObjCMethodInfo? = (this as? IrSimpleFunction)?.getObjCMethodInfo(onlyExternal = true)

fun FunctionDescriptor.getObjCMethodInfo(): ObjCMethodInfo? = this.getObjCMethodInfo(onlyExternal = false)

fun IrFunction.getObjCMethodInfo(): ObjCMethodInfo? = (this as? IrSimpleFunction)?.getObjCMethodInfo(onlyExternal = false)

fun IrFunction.isObjCBridgeBased(): Boolean {
    assert(this.isReal)

    return this.annotations.hasAnnotation(objCMethodFqName) ||
            this.annotations.hasAnnotation(objCFactoryFqName) ||
            this.annotations.hasAnnotation(objCConstructorFqName)
}

/**
 * Describes method overriding rules for Objective-C methods.
 *
 * This class is applied at [org.jetbrains.kotlin.resolve.OverridingUtil] as configured with
 * `META-INF/services/org.jetbrains.kotlin.resolve.ExternalOverridabilityCondition` resource.
 */
class ObjCOverridabilityCondition : ExternalOverridabilityCondition {

    override fun getContract() = ExternalOverridabilityCondition.Contract.BOTH

    override fun isOverridable(
            superDescriptor: CallableDescriptor,
            subDescriptor: CallableDescriptor,
            subClassDescriptor: ClassDescriptor?
    ): ExternalOverridabilityCondition.Result {
        if (superDescriptor.name == subDescriptor.name) { // Slow path:
            if (superDescriptor is FunctionDescriptor && subDescriptor is FunctionDescriptor) {
                superDescriptor.getExternalObjCMethodInfo()?.let { superInfo ->
                    val subInfo = subDescriptor.getExternalObjCMethodInfo()
                    if (subInfo != null) {
                        // Overriding Objective-C method by Objective-C method in interop stubs.
                        // Don't even check method signatures:
                        return if (superInfo.selector == subInfo.selector) {
                            ExternalOverridabilityCondition.Result.OVERRIDABLE
                        } else {
                            ExternalOverridabilityCondition.Result.INCOMPATIBLE
                        }
                    } else {
                        // Overriding Objective-C method by Kotlin method.
                        if (!parameterNamesMatch(superDescriptor, subDescriptor)) {
                            return ExternalOverridabilityCondition.Result.INCOMPATIBLE
                        }
                    }
                }
            } else if (superDescriptor.isExternalObjCClassProperty() && subDescriptor.isExternalObjCClassProperty()) {
                return ExternalOverridabilityCondition.Result.OVERRIDABLE
            }
        }

        return ExternalOverridabilityCondition.Result.UNKNOWN
    }

    private fun CallableDescriptor.isExternalObjCClassProperty() = this is PropertyDescriptor &&
            (this.containingDeclaration as? ClassDescriptor)?.isExternalObjCClass() == true

    private fun parameterNamesMatch(first: FunctionDescriptor, second: FunctionDescriptor): Boolean {
        // The original Objective-C method selector is represented as
        // function name and parameter names (except first).

        if (first.valueParameters.size != second.valueParameters.size) {
            return false
        }

        first.valueParameters.forEachIndexed { index, parameter ->
            if (index > 0 && parameter.name != second.valueParameters[index].name) {
                return false
            }
        }

        return true
    }

}

fun IrConstructor.objCConstructorIsDesignated(): Boolean =
    this.getAnnotationArgumentValue<Boolean>(objCConstructorFqName, "designated")
        ?: error("Could not find 'designated' argument")

fun ConstructorDescriptor.objCConstructorIsDesignated(): Boolean {
    val annotation = this.annotations.findAnnotation(objCConstructorFqName)!!
    val value = annotation.allValueArguments[Name.identifier("designated")]!!

    return (value as BooleanValue).value
}


val IrConstructor.isObjCConstructor get() = this.annotations.hasAnnotation(objCConstructorFqName)
val ConstructorDescriptor.isObjCConstructor get() = this.annotations.hasAnnotation(objCConstructorFqName)

// TODO-DCE-OBJC-INIT: Selector should be preserved by DCE.
fun IrConstructor.getObjCInitMethod(): IrSimpleFunction? {
    return this.annotations.findAnnotation(objCConstructorFqName)?.let {
        val initSelector = it.getAnnotationStringValue("initSelector")
        this.constructedClass.declarations.asSequence()
                .filterIsInstance<IrSimpleFunction>()
                .single { it.getExternalObjCMethodInfo()?.selector == initSelector }
    }
}

fun ConstructorDescriptor.getObjCInitMethod(): FunctionDescriptor? {
    if (this is IrBasedClassConstructorDescriptor) {
        // E.g. in case of K2.
        // The constructedClass has empty member scope, so we have to delegate to IR to find the init method.
        return this.owner.getObjCInitMethod()?.descriptor
    }

    return this.annotations.findAnnotation(objCConstructorFqName)?.let {
        val initSelector = it.getAnnotationStringValue("initSelector")
        val memberScope = constructedClass.unsubstitutedMemberScope
        val functionNames = memberScope.getFunctionNames()
        for (name in functionNames) {
            val functions = memberScope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND)
            for (function in functions) {
                val objectInfo = function.getExternalObjCMethodInfo() ?: continue
                if (objectInfo.selector == initSelector) return function
            }
        }
        error("Cannot find ObjInitMethod for $this")
    }
}

val IrFunction.hasObjCFactoryAnnotation get() = this.annotations.hasAnnotation(objCFactoryFqName)
val FunctionDescriptor.hasObjCFactoryAnnotation get() = this.annotations.hasAnnotation(objCFactoryFqName)

val IrFunction.hasObjCMethodAnnotation get() = this.annotations.hasAnnotation(objCMethodFqName)
val FunctionDescriptor.hasObjCMethodAnnotation get() = this.annotations.hasAnnotation(objCMethodFqName)

fun FunctionDescriptor.getObjCFactoryInitMethodInfo(): ObjCMethodInfo? {
    val factoryAnnotation = this.annotations.findAnnotation(objCFactoryFqName) ?: return null
    return objCMethodInfo(factoryAnnotation)
}

fun IrFunction.getObjCFactoryInitMethodInfo(): ObjCMethodInfo? {
    val factoryAnnotation = this.annotations.findAnnotation(objCFactoryFqName) ?: return null
    return objCMethodInfo(factoryAnnotation)
}

fun inferObjCSelector(descriptor: FunctionDescriptor): String = if (descriptor.valueParameters.isEmpty()) {
    descriptor.name.asString()
} else {
    buildString {
        append(descriptor.name)
        append(':')
        descriptor.valueParameters.drop(1).forEach {
            append(it.name)
            append(':')
        }
    }
}

fun ClassDescriptor.getExternalObjCClassBinaryName(): String =
        this.getExplicitExternalObjCClassBinaryName()
                ?: this.name.asString()

fun ClassDescriptor.getExternalObjCMetaClassBinaryName(): String =
        this.getExplicitExternalObjCClassBinaryName()
                ?: this.name.asString().removeSuffix("Meta")

private fun ClassDescriptor.getExplicitExternalObjCClassBinaryName() =
        this.annotations.findAnnotation(externalObjCClassFqName)!!.getStringValueOrNull("binaryName")
