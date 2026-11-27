/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.jvm.lower.JvmAnnotationImplementationTransformer
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.v2.irClass
import org.jetbrains.kotlin.ir.builders.declarations.v2.irConstructor
import org.jetbrains.kotlin.ir.builders.declarations.v2.irProperty
import org.jetbrains.kotlin.ir.builders.expressions.v2.buildAnnotation
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
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
        klass.irConstructor {
            valueParameter("name", context.irBuiltIns.stringType)
        }
    }

    private val kClassJava: IrPropertySymbol =
        IrFactoryImpl.irProperty {
            name = Name.identifier("java")
            parent = createClass(createPackage("kotlin.jvm"), "JvmClassMappingKt", ClassKind.CLASS).owner
            getter {
                returnType = javaLangClass.starProjectedType
                extensionReceiver {
                    name = "receiver".synthesizedName
                    type = context.irBuiltIns.kClassClass.starProjectedType
                }
                annotations = listOf(
                    buildAnnotation(jvmName.constructors.single()) {
                        type = jvmName.typeWith()
                        arguments[0] = IrConstImpl.string(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            context.irBuiltIns.stringType,
                            "getJavaClass"
                        )
                    }
                )
            }
        }.symbol

    private val implementor = JvmAnnotationImplementationTransformer.AnnotationPropertyImplementor(
        context.irFactory,
        context.irBuiltIns,
        javaLangClass,
        kClassJava.owner.getter!!.symbol,
        SERIALIZATION_PLUGIN_ORIGIN
    )

    fun generateImplementationFor(annotationClass: IrClass) {

        val properties = annotationClass.declarations.filterIsInstance<IrProperty>()

        val subclass = context.irFactory.irClass {
            startOffset = UNDEFINED_OFFSET
            endOffset = UNDEFINED_OFFSET
            name = SerialEntityNames.IMPL_NAME
            origin = SERIALIZATION_PLUGIN_ORIGIN
            visibility = DescriptorVisibilities.PUBLIC
            parent = annotationClass
            superTypes = listOf(annotationClass.defaultType)
            thisReceiver()
        }
        annotationClass.declarations.add(subclass)

        val ctor = subclass.irConstructor {
            startOffset = UNDEFINED_OFFSET
            endOffset = UNDEFINED_OFFSET
            visibility = DescriptorVisibilities.PUBLIC
        }

        implementor.implementAnnotationPropertiesAndConstructor(annotationClass, properties, subclass, ctor, null)
    }

    private fun createPackage(packageName: String): IrPackageFragment =
        createEmptyExternalPackageFragment(
            // TODO(KT-87315): Use `stdlib` module instead of this one.
            moduleFragment,
            FqName(packageName)
        )

    private fun createClass(
        irPackage: IrPackageFragment,
        shortName: String,
        classKind: ClassKind,
        block: (IrClass) -> Unit = {}
    ): IrClassSymbol = IrFactoryImpl.irClass {
        name = Name.identifier(shortName)
        kind = classKind
        modality = Modality.FINAL
        parent = irPackage
        thisReceiver()
    }.apply {
        block(this)
    }.symbol
}
