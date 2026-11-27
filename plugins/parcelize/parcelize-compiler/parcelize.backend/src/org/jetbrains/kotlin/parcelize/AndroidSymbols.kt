/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addTypeParameter
import org.jetbrains.kotlin.ir.builders.declarations.v2.irClass
import org.jetbrains.kotlin.ir.builders.declarations.v2.irConstructor
import org.jetbrains.kotlin.ir.builders.declarations.v2.irField
import org.jetbrains.kotlin.ir.builders.declarations.v2.irFunction
import org.jetbrains.kotlin.ir.builders.declarations.v2.irProperty
import org.jetbrains.kotlin.ir.builders.declarations.v2.irStaticFunction
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.parcelize.ParcelizeNames.CREATE_FROM_PARCEL_NAME
import org.jetbrains.kotlin.parcelize.ParcelizeNames.NEW_ARRAY_NAME
import org.jetbrains.kotlin.parcelize.ParcelizeNames.WRITE_TO_PARCEL_NAME

// All of the IR declarations needed by the parcelize plugin. Note that the declarations are generated based on JVM descriptors and
// hence contain just enough information to produce correct JVM bytecode for *calls*. In particular, we omit generic types and
// supertypes, which are not needed to produce correct bytecode.
class AndroidSymbols(
    private val pluginContext: IrPluginContext,
    private val moduleFragment: IrModuleFragment,
) {
    private val irFactory: IrFactory = IrFactoryImpl
    val irBuiltIns: IrBuiltIns = pluginContext.irBuiltIns

    private val javaIo: IrPackageFragment = createPackage("java.io")
    private val javaLang: IrPackageFragment = createPackage("java.lang")
    private val javaUtil: IrPackageFragment = createPackage("java.util")

    private val kotlin: IrPackageFragment = createPackage("kotlin")
    private val kotlinJvm: IrPackageFragment = createPackage("kotlin.jvm")
    private val kotlinJvmInternalPackage: IrPackageFragment = createPackage("kotlin.jvm.internal")
    private val kotlinTime: IrPackageFragment = createPackage("kotlin.time")

    private val androidOs: IrPackageFragment = createPackage("android.os")
    private val androidUtil: IrPackageFragment = createPackage("android.util")
    private val androidText: IrPackageFragment = createPackage("android.text")
    private val runtimePackage: IrPackageFragment = createPackage("kotlinx.parcelize")

    private val androidOsBundle: IrClassSymbol =
        createClass(androidOs, "Bundle", ClassKind.CLASS, Modality.FINAL)

    val androidOsIBinder: IrClassSymbol =
        createClass(androidOs, "IBinder", ClassKind.INTERFACE, Modality.ABSTRACT)

    val androidOsParcel: IrClassSymbol =
        createClass(androidOs, "Parcel", ClassKind.CLASS, Modality.FINAL)

    val directInitializerMarker: IrClassSymbol =
        createClass(runtimePackage,"DirectInitializerMarker", ClassKind.OBJECT, Modality.FINAL)

    private val androidOsParcelFileDescriptor: IrClassSymbol =
        createClass(androidOs, "ParcelFileDescriptor", ClassKind.CLASS, Modality.OPEN)

    private val androidOsParcelable: IrClassSymbol =
        createClass(androidOs, "Parcelable", ClassKind.INTERFACE, Modality.ABSTRACT)

    private val androidOsPersistableBundle: IrClassSymbol =
        createClass(androidOs, "PersistableBundle", ClassKind.CLASS, Modality.FINAL)

    private val androidTextTextUtils: IrClassSymbol =
        createClass(androidText, "TextUtils", ClassKind.CLASS, Modality.OPEN)

    private val androidUtilSize: IrClassSymbol =
        createClass(androidUtil, "Size", ClassKind.CLASS, Modality.FINAL)

    private val androidUtilSizeF: IrClassSymbol =
        createClass(androidUtil, "SizeF", ClassKind.CLASS, Modality.FINAL)

    private val androidUtilSparseBooleanArray: IrClassSymbol =
        createClass(androidUtil, "SparseBooleanArray", ClassKind.CLASS, Modality.OPEN)

    private val javaIoFileDescriptor: IrClassSymbol =
        createClass(javaIo, "FileDescriptor", ClassKind.CLASS, Modality.FINAL)

    private val javaIoSerializable: IrClassSymbol =
        createClass(javaIo, "Serializable", ClassKind.INTERFACE, Modality.ABSTRACT)

    val javaLangClass: IrClassSymbol =
        createClass(javaLang, "Class", ClassKind.CLASS, Modality.FINAL)

    private val javaLangClassLoader: IrClassSymbol =
        createClass(javaLang, "ClassLoader", ClassKind.CLASS, Modality.ABSTRACT)

    private val javaUtilArrayList: IrClassSymbol =
        createClass(javaUtil, "ArrayList", ClassKind.CLASS, Modality.OPEN)

    private val javaUtilLinkedHashMap: IrClassSymbol =
        createClass(javaUtil, "LinkedHashMap", ClassKind.CLASS, Modality.OPEN)

    private val javaUtilLinkedHashSet: IrClassSymbol =
        createClass(javaUtil, "LinkedHashSet", ClassKind.CLASS, Modality.OPEN)

    private val javaUtilList: IrClassSymbol =
        createClass(javaUtil, "List", ClassKind.INTERFACE, Modality.ABSTRACT)

    private val javaUtilTreeMap: IrClassSymbol =
        createClass(javaUtil, "TreeMap", ClassKind.CLASS, Modality.OPEN)

    private val javaUtilTreeSet: IrClassSymbol =
        createClass(javaUtil, "TreeSet", ClassKind.CLASS, Modality.OPEN)

    val kotlinUByte: IrClassSymbol = createClass(
        kotlin, "UByte", ClassKind.CLASS, Modality.FINAL,
        InlineClassRepresentation(Name.identifier("data"), irBuiltIns.byteType as IrSimpleType),
    )

    val kotlinUShort: IrClassSymbol = createClass(
        kotlin, "UShort", ClassKind.CLASS, Modality.FINAL,
        InlineClassRepresentation(Name.identifier("data"), irBuiltIns.shortType as IrSimpleType),
    )

    val kotlinUInt: IrClassSymbol = createClass(
        kotlin, "UInt", ClassKind.CLASS, Modality.FINAL,
        InlineClassRepresentation(Name.identifier("data"), irBuiltIns.intType as IrSimpleType),
    )

    val kotlinULong: IrClassSymbol = createClass(
        kotlin, "ULong", ClassKind.CLASS, Modality.FINAL,
        InlineClassRepresentation(Name.identifier("data"), irBuiltIns.longType as IrSimpleType),
    )

    val kotlinUByteArray: IrClassSymbol = createClass(
        kotlin, "UByteArray", ClassKind.CLASS, Modality.FINAL,
        InlineClassRepresentation(
            Name.identifier("storage"),
            irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.byteType).owner.defaultType,
        ),
    )

    val kotlinUShortArray: IrClassSymbol = createClass(
        kotlin, "UShortArray", ClassKind.CLASS, Modality.FINAL,
        InlineClassRepresentation(
            Name.identifier("storage"),
            irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.shortType).owner.defaultType,
        ),
    )

    val kotlinUIntArray: IrClassSymbol = createClass(
        kotlin, "UIntArray", ClassKind.CLASS, Modality.FINAL,
        InlineClassRepresentation(
            Name.identifier("storage"),
            irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.intType).owner.defaultType,
        ),
    )

    val kotlinULongArray: IrClassSymbol = createClass(
        kotlin, "ULongArray", ClassKind.CLASS, Modality.FINAL,
        InlineClassRepresentation(
            Name.identifier("storage"),
            irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.longType).owner.defaultType,
        ),
    )

    val androidOsParcelableCreator: IrClassSymbol = irFactory.irClass {
        name = Name.identifier("Creator")
        kind = ClassKind.INTERFACE
        modality = Modality.ABSTRACT
        parent = androidOsParcelable.owner
        thisReceiver()
    }.apply {
        val t = addTypeParameter("T", irBuiltIns.anyNType)

        irFunction {
            name = CREATE_FROM_PARCEL_NAME
            returnType = t.defaultType
            modality = Modality.ABSTRACT
            valueParameter("source", androidOsParcel.defaultType)
        }

        irFunction {
            name = NEW_ARRAY_NAME
            returnType = irBuiltIns.arrayClass.typeWith(t.defaultType.makeNullable())
            modality = Modality.ABSTRACT
            valueParameter("size", irBuiltIns.intType)
        }
    }.symbol

    val kotlinTimeDuration: IrClassSymbol = createClass(
        kotlinTime, "Duration", ClassKind.CLASS, Modality.FINAL,
        InlineClassRepresentation(Name.identifier("rawValue"), irBuiltIns.longType as IrSimpleType),
    )

    val kotlinKClassJava: IrPropertySymbol = irFactory.irProperty {
        name = Name.identifier("java")
        parent = kotlinJvm
        getter {
            returnType = javaLangClass.defaultType
            extensionReceiver {
                name = "receiver".synthesizedName
                type = irBuiltIns.kClassClass.starProjectedType
            }
        }
    }.symbol

    val parcelCreateBinderArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("createBinderArray")
            returnType = irBuiltIns.arrayClass.typeWith(androidOsIBinder.defaultType)
        }.symbol

    val parcelCreateBinderArrayList: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("createBinderArrayList")
            returnType = javaUtilArrayList.defaultType
        }.symbol

    val parcelCreateBooleanArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("createBooleanArray")
            returnType = irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.booleanType).defaultType
        }.symbol

    val parcelCreateByteArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("createByteArray")
            returnType = irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.byteType).defaultType
        }.symbol

    val parcelCreateCharArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("createCharArray")
            returnType = irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.charType).defaultType
        }.symbol

    val parcelCreateDoubleArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("createDoubleArray")
            returnType = irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.doubleType).defaultType
        }.symbol

    val parcelCreateFloatArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("createFloatArray")
            returnType = irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.floatType).defaultType
        }.symbol

    val parcelCreateIntArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("createIntArray")
            returnType = irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.intType).defaultType
        }.symbol

    val parcelCreateLongArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("createLongArray")
            returnType = irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.longType).defaultType
        }.symbol

    val parcelCreateStringArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("createStringArray")
            returnType = irBuiltIns.arrayClass.typeWith(irBuiltIns.stringType)
        }.symbol

    val parcelCreateStringArrayList: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("createStringArrayList")
            returnType = javaUtilArrayList.defaultType
        }.symbol

    val parcelReadBundle: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("readBundle")
            returnType = androidOsBundle.defaultType
            valueParameter("loader", javaLangClassLoader.defaultType)
        }.symbol

    val parcelReadByte: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("readByte")
            returnType = irBuiltIns.byteType
        }.symbol

    val parcelReadDouble: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("readDouble")
            returnType = irBuiltIns.doubleType
        }.symbol

    val parcelReadFileDescriptor: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("readFileDescriptor")
            returnType = androidOsParcelFileDescriptor.defaultType
        }.symbol

    val parcelReadFloat: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("readFloat")
            returnType = irBuiltIns.floatType
        }.symbol

    val parcelReadInt: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("readInt")
            returnType = irBuiltIns.intType
        }.symbol

    val parcelReadLong: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("readLong")
            returnType = irBuiltIns.longType
        }.symbol

    val parcelReadParcelable: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("readParcelable")
            returnType = androidOsParcelable.defaultType
            valueParameter("loader", javaLangClassLoader.defaultType)
        }.symbol

    val parcelReadPersistableBundle: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("readPersistableBundle")
            returnType = androidOsPersistableBundle.defaultType
            valueParameter("loader", javaLangClassLoader.defaultType)
        }.symbol

    val parcelReadSerializable: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("readSerializable")
            returnType = javaIoSerializable.defaultType
        }.symbol

    val parcelReadSize: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("readSize")
            returnType = androidUtilSize.defaultType
        }.symbol

    val parcelReadSizeF: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("readSizeF")
            returnType = androidUtilSizeF.defaultType
        }.symbol

    val parcelReadSparseBooleanArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("readSparseBooleanArray")
            returnType = androidUtilSparseBooleanArray.defaultType
        }.symbol

    val parcelReadString: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("readString")
            returnType = irBuiltIns.stringType
        }.symbol

    val parcelReadStrongBinder: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("readStrongBinder")
            returnType = androidOsIBinder.defaultType
        }.symbol

    val parcelReadValue: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("readValue")
            returnType = irBuiltIns.anyNType
            valueParameter("loader", javaLangClassLoader.defaultType)
        }.symbol

    val parcelWriteBinderArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writeBinderArray")
            returnType = irBuiltIns.unitType
            valueParameter("val", irBuiltIns.arrayClass.typeWith(androidOsIBinder.defaultType))
        }.symbol

    val parcelWriteBinderList: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writeBinderList")
            returnType = irBuiltIns.unitType
            valueParameter("val", javaUtilList.defaultType)
        }.symbol

    val parcelWriteBooleanArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writeBooleanArray")
            returnType = irBuiltIns.unitType
            valueParameter("val", irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.booleanType).defaultType)
        }.symbol

    val parcelWriteBundle: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writeBundle")
            returnType = irBuiltIns.unitType
            valueParameter("val", androidOsBundle.defaultType)
        }.symbol

    val parcelWriteByte: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writeByte")
            returnType = irBuiltIns.unitType
            valueParameter("val", irBuiltIns.byteType)
        }.symbol

    val parcelWriteByteArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writeByteArray")
            returnType = irBuiltIns.unitType
            valueParameter("b", irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.byteType).defaultType)
        }.symbol

    val parcelWriteCharArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writeCharArray")
            returnType = irBuiltIns.unitType
            valueParameter("val", irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.charType).defaultType)
        }.symbol

    val parcelWriteDouble: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writeDouble")
            returnType = irBuiltIns.unitType
            valueParameter("val", irBuiltIns.doubleType)
        }.symbol

    val parcelWriteDoubleArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writeDoubleArray")
            returnType = irBuiltIns.unitType
            valueParameter("val", irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.doubleType).defaultType)
        }.symbol

    val parcelWriteFileDescriptor: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writeFileDescriptor")
            returnType = irBuiltIns.unitType
            valueParameter("val", javaIoFileDescriptor.defaultType)
        }.symbol

    val parcelWriteFloat: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writeFloat")
            returnType = irBuiltIns.unitType
            valueParameter("val", irBuiltIns.floatType)
        }.symbol

    val parcelWriteFloatArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writeFloatArray")
            returnType = irBuiltIns.unitType
            valueParameter("val", irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.floatType).defaultType)
        }.symbol

    val parcelWriteInt: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writeInt")
            returnType = irBuiltIns.unitType
            valueParameter("val", irBuiltIns.intType)
        }.symbol

    val parcelWriteIntArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writeIntArray")
            returnType = irBuiltIns.unitType
            valueParameter("val", irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.intType).defaultType)
        }.symbol

    val parcelWriteLong: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writeLong")
            returnType = irBuiltIns.unitType
            valueParameter("val", irBuiltIns.longType)
        }.symbol

    val parcelWriteLongArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writeLongArray")
            returnType = irBuiltIns.unitType
            valueParameter("val", irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.longType).defaultType)
        }.symbol

    val parcelWriteParcelable: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writeParcelable")
            returnType = irBuiltIns.unitType
            valueParameter("p", androidOsParcelable.defaultType)
            valueParameter("parcelableFlags", irBuiltIns.intType)
        }.symbol

    val parcelWritePersistableBundle: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writePersistableBundle")
            returnType = irBuiltIns.unitType
            valueParameter("val", androidOsPersistableBundle.defaultType)
        }.symbol

    val parcelWriteSerializable: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writeSerializable")
            returnType = irBuiltIns.unitType
            valueParameter("s", javaIoSerializable.defaultType)
        }.symbol

    val parcelWriteSize: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writeSize")
            returnType = irBuiltIns.unitType
            valueParameter("val", androidUtilSize.defaultType)
        }.symbol

    val parcelWriteSizeF: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writeSizeF")
            returnType = irBuiltIns.unitType
            valueParameter("val", androidUtilSizeF.defaultType)
        }.symbol

    val parcelWriteSparseBooleanArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writeSparseBooleanArray")
            returnType = irBuiltIns.unitType
            valueParameter("val", androidUtilSparseBooleanArray.defaultType)
        }.symbol

    val parcelWriteString: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writeString")
            returnType = irBuiltIns.unitType
            valueParameter("val", irBuiltIns.stringType)
        }.symbol

    val parcelWriteStringArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writeStringArray")
            returnType = irBuiltIns.unitType
            valueParameter("val", irBuiltIns.arrayClass.typeWith(irBuiltIns.stringType))
        }.symbol

    val parcelWriteStringList: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writeStringList")
            returnType = irBuiltIns.unitType
            valueParameter("val", javaUtilList.defaultType)
        }.symbol

    val parcelWriteStrongBinder: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writeStrongBinder")
            returnType = irBuiltIns.unitType
            valueParameter("val", androidOsIBinder.defaultType)
        }.symbol

    val parcelWriteValue: IrSimpleFunctionSymbol =
        androidOsParcel.owner.irFunction {
            name = Name.identifier("writeValue")
            returnType = irBuiltIns.unitType
            valueParameter("v", irBuiltIns.anyNType)
        }.symbol

    val textUtilsWriteToParcel: IrSimpleFunctionSymbol =
        androidTextTextUtils.owner.irStaticFunction {
            name = WRITE_TO_PARCEL_NAME
            returnType = irBuiltIns.unitType
            valueParameter("cs", irBuiltIns.charSequenceClass.defaultType)
            valueParameter("p", androidOsParcel.defaultType)
            valueParameter("parcelableFlags", irBuiltIns.intType)
        }.symbol

    val classGetClassLoader: IrSimpleFunctionSymbol =
        javaLangClass.owner.irFunction {
            name = Name.identifier("getClassLoader")
            returnType = javaLangClassLoader.defaultType
        }.symbol

    val arrayListConstructor: IrConstructorSymbol = javaUtilArrayList.owner.irConstructor {
        valueParameter("p_0", irBuiltIns.intType)
    }.symbol

    val arrayListAdd: IrSimpleFunctionSymbol =
        javaUtilArrayList.owner.irFunction {
            name = Name.identifier("add")
            returnType = irBuiltIns.booleanType
            valueParameter("p_0", irBuiltIns.anyNType)
        }.symbol

    val linkedHashMapConstructor: IrConstructorSymbol =
        javaUtilLinkedHashMap.owner.irConstructor {
            valueParameter("p_0", irBuiltIns.intType)
        }.symbol

    val linkedHashMapPut: IrSimpleFunctionSymbol =
        javaUtilLinkedHashMap.owner.irFunction {
            name = Name.identifier("put")
            returnType = irBuiltIns.anyNType
            valueParameter("p_0", irBuiltIns.anyNType)
            valueParameter("p_1", irBuiltIns.anyNType)
        }.symbol

    val linkedHashSetConstructor: IrConstructorSymbol =
        javaUtilLinkedHashSet.owner.irConstructor {
            valueParameter("p_0", irBuiltIns.intType)
        }.symbol

    val linkedHashSetAdd: IrSimpleFunctionSymbol =
        javaUtilLinkedHashSet.owner.irFunction {
            name = Name.identifier("add")
            returnType = irBuiltIns.booleanType
            valueParameter("p_0", irBuiltIns.anyNType)
        }.symbol

    val treeMapConstructor: IrConstructorSymbol = javaUtilTreeMap.owner.irConstructor().symbol

    val treeMapPut: IrSimpleFunctionSymbol =
        javaUtilTreeMap.owner.irFunction {
            name = Name.identifier("put")
            returnType = irBuiltIns.anyNType
            valueParameter("p_0", irBuiltIns.anyNType)
            valueParameter("p_1", irBuiltIns.anyNType)
        }.symbol

    val treeSetConstructor: IrConstructorSymbol = javaUtilTreeSet.owner.irConstructor().symbol

    val treeSetAdd: IrSimpleFunctionSymbol =
        javaUtilTreeSet.owner.irFunction {
            name = Name.identifier("add")
            returnType = irBuiltIns.booleanType
            valueParameter("p_0", irBuiltIns.anyNType)
        }.symbol

    val textUtilsCharSequenceCreator: IrFieldSymbol = androidTextTextUtils.owner.irField {
        name = Name.identifier("CHAR_SEQUENCE_CREATOR")
        type = androidOsParcelableCreator.defaultType
        isStatic = true
    }.symbol

    private val kotlinxCollectionsImmutable = FqName(kotlinxImmutable())
    private val kotlinCollections = FqName("kotlin.collections")
    private val kotlinIterable: FqName = kotlinCollections.child(Name.identifier("Iterable"))
    private val kotlinMap: FqName = kotlinCollections.child(Name.identifier("Map"))

    private fun findKotlinxImmutableCollectionExtensionFunction(
        receiver: FqName,
        functionName: String,
    ): IrSimpleFunctionSymbol {
        val callableId = CallableId(kotlinxCollectionsImmutable, Name.identifier(functionName))
        return pluginContext.finderForBuiltins().findFunctions(callableId)
            .firstOrNull { it.owner.parameters.singleOrNull()?.type?.classFqName == receiver }
            ?: error("Function from kotlinx.collections.immutable is not found on classpath: $callableId")
    }

    val kotlinIterableToPersistentListExtension: IrSimpleFunctionSymbol by lazy {
        findKotlinxImmutableCollectionExtensionFunction(kotlinIterable, "toPersistentList")
    }

    val kotlinIterableToPersistentSetExtension: IrSimpleFunctionSymbol by lazy {
        findKotlinxImmutableCollectionExtensionFunction(kotlinIterable, "toPersistentSet")
    }

    val kotlinMapToPersistentMapExtension: IrSimpleFunctionSymbol by lazy {
        findKotlinxImmutableCollectionExtensionFunction(kotlinMap, "toPersistentMap")
    }

    val unsafeCoerceIntrinsic: IrSimpleFunctionSymbol =
        irFactory.irFunction {
            name = Name.special("<unsafe-coerce>")
            origin = IrDeclarationOrigin.IR_BUILTINS_STUB
            parent = kotlinJvmInternalPackage
            val src = typeParameter("T", irBuiltIns.anyNType)
            val dst = typeParameter("R", irBuiltIns.anyNType)
            valueParameter("v", src.defaultType)
            returnType = dst.defaultType
        }.symbol

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
        classModality: Modality,
        inlineClassRepresentation: InlineClassRepresentation<IrSimpleType>? = null,
    ): IrClassSymbol = irFactory.irClass {
        name = Name.identifier(shortName)
        kind = classKind
        modality = classModality
        isValue = inlineClassRepresentation != null
        valueClassRepresentation = inlineClassRepresentation
        parent = irPackage
        thisReceiver()
    }.symbol

    fun createBuilder(
        symbol: IrSymbol,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET
    ) = AndroidIrBuilder(this, symbol, startOffset, endOffset)
}
