/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.extensions

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.CompilationException
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrIntrinsicExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.fileParent
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlinx.serialization.compiler.backend.ir.*
import org.jetbrains.kotlinx.serialization.compiler.backend.ir.SerializationJvmIrIntrinsicSupport
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationDependencies
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationJsDependenciesClassIds
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationPackages
import java.util.concurrent.ConcurrentHashMap

/**
 * Copy of [runOnFilePostfix], but this implementation first lowers declaration, then its children.
 */
fun ClassLoweringPass.runOnFileInOrder(irFile: IrFile) {
    irFile.acceptVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitClass(declaration: IrClass) {
            lower(declaration)
            declaration.acceptChildrenVoid(this)
        }
    })
}


class SerializationPluginContext(baseContext: IrPluginContext, val metadataPlugin: SerializationDescriptorSerializerPlugin?) :
    IrPluginContext by baseContext, SerializationBaseContext {

    internal val copiedStaticWriteSelf: MutableMap<IrSimpleFunction, IrSimpleFunction> = ConcurrentHashMap()

    // Kotlin built-in declarations
    internal val arrayValueGetter = irBuiltIns.arrayClass.owner.declarations.filterIsInstance<IrSimpleFunction>()
        .single { it.name.asString() == "get" }

    internal val intArrayOfFunctionSymbol =
        referenceFunctions(CallableId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier("intArrayOf"))).first()

    // Kotlin stdlib declarations
    internal val jvmFieldClassSymbol = referenceClass(JvmStandardClassIds.Annotations.JvmField)!!

    internal val lazyModeClass = referenceClass(ClassId.topLevel(SerializationDependencies.LAZY_MODE_FQ))!!.owner
    internal val lazyModePublicationEnumEntry =
        lazyModeClass.enumEntries().single { it.name == SerializationDependencies.LAZY_PUBLICATION_MODE_NAME }
    // There can be several transitive dependencies on kotlin-stdlib in IDE sources,
    // as well as several definitions of stdlib functions, including `kotlin.lazy`;
    // in that case `referenceFunctions` might return more than one valid definition of the same function.
    internal val lazyFunctionSymbol =
        referenceFunctions(CallableId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier("lazy"))).first {
            it.owner.valueParameters.size == 2 && it.owner.valueParameters[0].type == lazyModeClass.defaultType
        }
    internal val lazyClass = referenceClass(ClassId.topLevel(SerializationDependencies.LAZY_FQ))!!.owner
    internal val lazyValueGetter = lazyClass.getPropertyGetter("value")!!

    internal val jsExportIgnoreClass: IrClass? by lazy {
        val pkg = SerializationJsDependenciesClassIds.jsExportIgnore.packageFqName
        val jsExportName = SerializationJsDependenciesClassIds.jsExportIgnore.parentClassId!!.shortClassName
        val jsExportIgnoreFqName = SerializationJsDependenciesClassIds.jsExportIgnore.asSingleFqName()

        getClassFromRuntimeOrNull(jsExportName.identifier, pkg)
            ?.owner
            ?.findDeclaration { it.fqNameWhenAvailable == jsExportIgnoreFqName }
    }

    // serialization runtime declarations
    internal val enumSerializerFactoryFunc = baseContext.referenceFunctions(
        CallableId(
            SerializationPackages.internalPackageFqName,
            SerialEntityNames.ENUM_SERIALIZER_FACTORY_FUNC_NAME
        )
    ).singleOrNull()

    internal val annotatedEnumSerializerFactoryFunc = baseContext.referenceFunctions(
        CallableId(
            SerializationPackages.internalPackageFqName,
            SerialEntityNames.ANNOTATED_ENUM_SERIALIZER_FACTORY_FUNC_NAME
        )
    ).singleOrNull()

    /**
     * @return `null` if there is no serialization runtime in the classpath
     */
    internal val kSerializerClass = referenceClass(SerialEntityNames.KSERIALIZER_CLASS_ID)?.owner

    // evaluated properties
    override val runtimeHasEnumSerializerFactoryFunctions = enumSerializerFactoryFunc != null && annotatedEnumSerializerFactoryFunc != null

    override fun referenceClassId(classId: ClassId): IrClassSymbol? = referenceClass(classId)
}

private inline fun IrClass.runPluginSafe(block: () -> Unit) {
    try {
        block()
    } catch (e: Throwable) {
        throw when (e) {
            is VirtualMachineError, is ThreadDeath -> e
            else -> CompilationException(
                "kotlinx.serialization compiler plugin internal error: unable to transform declaration, see cause",
                this.fileParent,
                this,
                e
            )
        }
    }
}

private class SerializerClassLowering(
    baseContext: IrPluginContext,
    metadataPlugin: SerializationDescriptorSerializerPlugin?,
    moduleFragment: IrModuleFragment
) : IrElementTransformerVoid(), ClassLoweringPass {
    val context: SerializationPluginContext = SerializationPluginContext(baseContext, metadataPlugin)

    // Lazy to avoid creating generator in non-JVM backends
    private val serialInfoJvmGenerator by lazy(LazyThreadSafetyMode.NONE) { SerialInfoImplJvmIrGenerator(context, moduleFragment) }

    override fun lower(irClass: IrClass) {
        irClass.runPluginSafe {
            SerializableIrGenerator.generate(irClass, context)
            SerializerIrGenerator.generate(irClass, context, context.metadataPlugin)
            SerializableCompanionIrGenerator.generate(irClass, context)

            if (context.platform.isJvm() && irClass.isSerialInfoAnnotation) {
                serialInfoJvmGenerator.generateImplementationFor(irClass)
            }
        }
    }
}

private class SerializerClassPreLowering(
    baseContext: IrPluginContext
) : IrElementTransformerVoid(), ClassLoweringPass {
    val context: SerializationPluginContext = SerializationPluginContext(baseContext, null)

    override fun lower(irClass: IrClass) {
        irClass.runPluginSafe {
            IrPreGenerator.generate(irClass, context)
        }
    }
}

enum class SerializationIntrinsicsState {
    NORMAL, // depends on whether we have noCompiledSerializer function in runtime
    DISABLED, // disabled if corresponding CLI flag passed
    FORCE_ENABLED // used for test purposes ONLY
}

open class SerializationLoweringExtension @JvmOverloads constructor(
    private val metadataPlugin: SerializationDescriptorSerializerPlugin? = null
) : IrGenerationExtension {

    private var intrinsicsState = SerializationIntrinsicsState.NORMAL

    constructor(metadataPlugin: SerializationDescriptorSerializerPlugin, intrinsicsState: SerializationIntrinsicsState) : this(
        metadataPlugin
    ) {
        this.intrinsicsState = intrinsicsState
    }

    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        val pass1 = SerializerClassPreLowering(pluginContext)
        val pass2 = SerializerClassLowering(pluginContext, metadataPlugin, moduleFragment)
        moduleFragment.files.forEach(pass1::runOnFileInOrder)
        moduleFragment.files.forEach(pass2::runOnFileInOrder)
    }

    override fun getPlatformIntrinsicExtension(backendContext: BackendContext): IrIntrinsicExtension? {
        val ctx = backendContext as? JvmBackendContext ?: return null
        if (!canEnableIntrinsics(ctx)) return null
        return SerializationJvmIrIntrinsicSupport(
            ctx,
            requireNotNull(ctx.irPluginContext) { "Intrinsics can't be enabled with null irPluginContext, check `canEnableIntrinsics` function for bugs." })
    }

    private fun canEnableIntrinsics(ctx: JvmBackendContext): Boolean {
        return when (intrinsicsState) {
            SerializationIntrinsicsState.FORCE_ENABLED -> true
            SerializationIntrinsicsState.DISABLED -> false
            SerializationIntrinsicsState.NORMAL -> {
                val requiredFunctionsFromRuntime = ctx.irPluginContext?.referenceFunctions(
                    CallableId(
                        SerializationPackages.packageFqName,
                        Name.identifier(SerializationJvmIrIntrinsicSupport.noCompiledSerializerMethodName)
                    )
                ).orEmpty()
                requiredFunctionsFromRuntime.isNotEmpty() && requiredFunctionsFromRuntime.all { it.isBound }
            }
        }
    }
}


