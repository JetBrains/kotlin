/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.ir.addExtensionReceiver
import org.jetbrains.kotlin.backend.jvm.lower.JvmAnnotationImplementationTransformer
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames

class SerialInfoImplJvmIrGenerator(
    private val context: SerializationPluginContext,
    private val moduleFragment: IrModuleFragment,
) {
    private val javaLangClass = createClass(createPackage("java.lang"), "Class", ClassKind.CLASS)

    private val jvmName: IrClassSymbol = createClass(createPackage("kotlin.jvm"), "JvmName", ClassKind.ANNOTATION_CLASS) { klass ->
        klass.addConstructor().apply {
            addValueParameter("name", context.irBuiltIns.stringType)
        }
    }

    private val jvmExpose: IrClassSymbol = createClass(createPackage("kotlin.jvm"), "JvmExpose", ClassKind.ANNOTATION_CLASS) { klass ->
        klass.addConstructor().apply {
            addValueParameter("name", context.irBuiltIns.stringType)
        }
    }

    private val kClassJava: IrPropertySymbol =
        IrFactoryImpl.buildProperty {
            name = Name.identifier("java")
        }.apply {
            parent = createClass(createPackage("kotlin.jvm"), "JvmClassMappingKt", ClassKind.CLASS).owner
            addGetter().apply {
                annotations = listOf(
                    IrConstructorCallImpl.fromSymbolOwner(jvmName.typeWith(), jvmName.constructors.single()).apply {
                        putValueArgument(
                            0,
                            IrConstImpl.string(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                context.irBuiltIns.stringType,
                                "getJavaClass"
                            )
                        )
                    }
                )
                addExtensionReceiver(context.irBuiltIns.kClassClass.starProjectedType)
                returnType = javaLangClass.starProjectedType
            }
        }.symbol

    private val implementor = JvmAnnotationImplementationTransformer.AnnotationPropertyImplementor(
        context.irFactory,
        context.irBuiltIns,
        context.symbols,
        javaLangClass,
        kClassJava.owner.getter!!.symbol,
        SERIALIZATION_PLUGIN_ORIGIN
    )

    fun generateImplementationFor(annotationClass: IrClass) {

        val properties = annotationClass.declarations.filterIsInstance<IrProperty>()

        val subclass = context.irFactory.buildClass {
            startOffset = UNDEFINED_OFFSET
            endOffset = UNDEFINED_OFFSET
            name = SerialEntityNames.IMPL_NAME
            origin = SERIALIZATION_PLUGIN_ORIGIN
            visibility = DescriptorVisibilities.PUBLIC
        }.apply {
            parent = annotationClass
            createImplicitParameterDeclarationWithWrappedDescriptor()
            superTypes = listOf(annotationClass.defaultType)
        }
        annotationClass.declarations.add(subclass)

        val ctor = subclass.addConstructor {
            startOffset = UNDEFINED_OFFSET
            endOffset = UNDEFINED_OFFSET
            visibility = DescriptorVisibilities.PUBLIC
        }

        implementor.implementAnnotationPropertiesAndConstructor(properties, subclass, ctor, null)
    }

    private fun createPackage(packageName: String): IrPackageFragment =
        IrExternalPackageFragmentImpl.createEmptyExternalPackageFragment(
            moduleFragment.descriptor,
            FqName(packageName)
        )

    private fun createClass(
        irPackage: IrPackageFragment,
        shortName: String,
        classKind: ClassKind,
        block: (IrClass) -> Unit = {}
    ): IrClassSymbol = IrFactoryImpl.buildClass {
        name = Name.identifier(shortName)
        kind = classKind
        modality = Modality.FINAL
    }.apply {
        parent = irPackage
        createImplicitParameterDeclarationWithWrappedDescriptor()
        block(this)
    }.symbol
}
