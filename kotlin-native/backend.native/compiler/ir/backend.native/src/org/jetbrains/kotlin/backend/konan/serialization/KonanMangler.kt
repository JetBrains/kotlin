package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorBasedKotlinManglerImpl
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorExportCheckerVisitor
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrBasedKotlinManglerImpl
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrExportCheckerVisitor
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrMangleComputer
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.hasAnnotation

abstract class AbstractKonanIrMangler(private val withReturnType: Boolean) : IrBasedKotlinManglerImpl() {
    override fun getExportChecker(): IrExportCheckerVisitor = KonanIrExportChecker()

    override fun getMangleComputer(mode: MangleMode): IrMangleComputer =
            KonanIrManglerComputer(StringBuilder(256), mode, withReturnType)

    private class KonanIrExportChecker : IrExportCheckerVisitor() {
        override fun IrDeclaration.isPlatformSpecificExported(): Boolean {
            if (this is IrSimpleFunction) if (isFakeOverride) return false

            // TODO: revise
            if (annotations.hasAnnotation(RuntimeNames.symbolNameAnnotation)) {
                // Treat any `@SymbolName` declaration as exported.
                return true
            }
            if (annotations.hasAnnotation(RuntimeNames.exportForCppRuntime)) {
                // Treat any `@ExportForCppRuntime` declaration as exported.
                return true
            }
            if (annotations.hasAnnotation(RuntimeNames.cnameAnnotation)) {
                // Treat `@CName` declaration as exported.
                return true
            }
            if (annotations.hasAnnotation(RuntimeNames.exportForCompilerAnnotation)) {
                return true
            }

            return false
        }
    }

    private class KonanIrManglerComputer(builder: StringBuilder, mode: MangleMode, private val withReturnType: Boolean) : IrMangleComputer(builder, mode) {
        override fun copy(newMode: MangleMode): IrMangleComputer = KonanIrManglerComputer(builder, newMode, withReturnType)

        override fun addReturnType(): Boolean = withReturnType

        override fun IrFunction.platformSpecificFunctionName(): String? {
            (if (this is IrConstructor && this.isObjCConstructor) this.getObjCInitMethod() else this)?.getObjCMethodInfo()
                    ?.let {
                        return buildString {
                            if (extensionReceiverParameter != null) {
                                append(extensionReceiverParameter!!.type.getClass()!!.name)
                                append(".")
                            }

                            append("objc:")
                            append(it.selector)
                            if (this@platformSpecificFunctionName is IrConstructor && this@platformSpecificFunctionName.isObjCConstructor) append("#Constructor")

                            if ((this@platformSpecificFunctionName as? IrSimpleFunction)?.correspondingPropertySymbol != null) {
                                append("#Accessor")
                            }
                        }
                    }
            return null
        }

        override fun IrFunction.specialValueParamPrefix(param: IrValueParameter): String {
            // TODO: there are clashes originating from ObjectiveC interop.
            // kotlinx.cinterop.ObjCClassOf<T>.create(format: kotlin.String): T defined in platform.Foundation in file Foundation.kt
            // and
            // kotlinx.cinterop.ObjCClassOf<T>.create(string: kotlin.String): T defined in platform.Foundation in file Foundation.kt

            return if (this.hasObjCMethodAnnotation || this.hasObjCFactoryAnnotation || this.isObjCClassMethod()) "${param.name}:" else ""
        }
    }
}

object KonanManglerIr : AbstractKonanIrMangler(false)

abstract class AbstractKonanDescriptorMangler : DescriptorBasedKotlinManglerImpl() {
    override fun getExportChecker(): DescriptorExportCheckerVisitor = KonanDescriptorExportChecker()

    override fun getMangleComputer(mode: MangleMode): DescriptorMangleComputer =
            KonanDescriptorMangleComputer(StringBuilder(256), mode)

    private class KonanDescriptorExportChecker : DescriptorExportCheckerVisitor() {
        override fun DeclarationDescriptor.isPlatformSpecificExported(): Boolean {
            if (this is SimpleFunctionDescriptor) {
                if (kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) return false
            }
            // TODO: revise
            if (annotations.hasAnnotation(RuntimeNames.symbolNameAnnotation)) {
                // Treat any `@SymbolName` declaration as exported.
                return true
            }
            if (annotations.hasAnnotation(RuntimeNames.exportForCppRuntime)) {
                // Treat any `@ExportForCppRuntime` declaration as exported.
                return true
            }
            if (annotations.hasAnnotation(RuntimeNames.cnameAnnotation)) {
                // Treat `@CName` declaration as exported.
                return true
            }
            if (annotations.hasAnnotation(RuntimeNames.exportForCompilerAnnotation)) {
                return true
            }

            return false
        }
    }

    private class KonanDescriptorMangleComputer(builder: StringBuilder, mode: MangleMode) : DescriptorMangleComputer(builder, mode) {
        override fun copy(newMode: MangleMode): DescriptorMangleComputer = KonanDescriptorMangleComputer(builder, newMode)

        override fun FunctionDescriptor.platformSpecificFunctionName(): String? {
            (if (this is ConstructorDescriptor && this.isObjCConstructor) this.getObjCInitMethod() else this)?.getObjCMethodInfo()
                    ?.let {
                        return buildString {
                            if (extensionReceiverParameter != null) {
                                append(extensionReceiverParameter!!.type.constructor.declarationDescriptor!!.name)
                                append(".")
                            }

                            append("objc:")
                            append(it.selector)
                            if (this@platformSpecificFunctionName is ConstructorDescriptor && this@platformSpecificFunctionName.isObjCConstructor) append("#Constructor")

                            if (this@platformSpecificFunctionName is PropertyAccessorDescriptor) {
                                append("#Accessor")
                            }
                        }
                    }
            return null
        }

        override fun FunctionDescriptor.specialValueParamPrefix(param: ValueParameterDescriptor): String {
            return if (this.hasObjCMethodAnnotation || this.hasObjCFactoryAnnotation || this.isObjCClassMethod()) "${param.name}:" else ""
        }
    }
}

object KonanManglerDesc : AbstractKonanDescriptorMangler()