/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addExtensionReceiver
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.createImplicitParameterDeclarationWithWrappedDescriptor
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

    private val androidOsBundle: IrClassSymbol =
        createClass(androidOs, "Bundle", ClassKind.CLASS, Modality.FINAL)

    private val androidOsIBinder: IrClassSymbol =
        createClass(androidOs, "IBinder", ClassKind.INTERFACE, Modality.ABSTRACT)

    val androidOsParcel: IrClassSymbol =
        createClass(androidOs, "Parcel", ClassKind.CLASS, Modality.FINAL)

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

    val kotlinUByte: IrClassSymbol =
        createClass(kotlin, "UByte", ClassKind.CLASS, Modality.FINAL, true).apply {
            owner.valueClassRepresentation = InlineClassRepresentation(Name.identifier("data"), irBuiltIns.byteType as IrSimpleType)
        }

    val kotlinUShort: IrClassSymbol =
        createClass(kotlin, "UShort", ClassKind.CLASS, Modality.FINAL, true).apply {
            owner.valueClassRepresentation = InlineClassRepresentation(Name.identifier("data"), irBuiltIns.shortType as IrSimpleType)
        }

    val kotlinUInt: IrClassSymbol =
        createClass(kotlin, "UInt", ClassKind.CLASS, Modality.FINAL, true).apply {
            owner.valueClassRepresentation = InlineClassRepresentation(Name.identifier("data"), irBuiltIns.intType as IrSimpleType)
        }

    val kotlinULong: IrClassSymbol =
        createClass(kotlin, "ULong", ClassKind.CLASS, Modality.FINAL, true).apply {
            owner.valueClassRepresentation = InlineClassRepresentation(Name.identifier("data"), irBuiltIns.longType as IrSimpleType)
        }

    val kotlinUByteArray: IrClassSymbol =
        createClass(kotlin, "UByteArray", ClassKind.CLASS, Modality.FINAL, true).apply {
            owner.valueClassRepresentation = InlineClassRepresentation(
                Name.identifier("storage"),
                irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.byteType).owner.defaultType
            )
        }

    val kotlinUShortArray: IrClassSymbol =
        createClass(kotlin, "UShortArray", ClassKind.CLASS, Modality.FINAL, true).apply {
            owner.valueClassRepresentation = InlineClassRepresentation(
                Name.identifier("storage"),
                irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.shortType).owner.defaultType
            )
        }

    val kotlinUIntArray: IrClassSymbol =
        createClass(kotlin, "UIntArray", ClassKind.CLASS, Modality.FINAL, true).apply {
            owner.valueClassRepresentation = InlineClassRepresentation(
                Name.identifier("storage"),
                irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.intType).owner.defaultType
            )
        }

    val kotlinULongArray: IrClassSymbol =
        createClass(kotlin, "ULongArray", ClassKind.CLASS, Modality.FINAL, true).apply {
            owner.valueClassRepresentation = InlineClassRepresentation(
                Name.identifier("storage"),
                irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.longType).owner.defaultType
            )
        }

    val androidOsParcelableCreator: IrClassSymbol = irFactory.buildClass {
        name = Name.identifier("Creator")
        kind = ClassKind.INTERFACE
        modality = Modality.ABSTRACT
    }.apply {
        createImplicitParameterDeclarationWithWrappedDescriptor()
        val t = addTypeParameter("T", irBuiltIns.anyNType)
        parent = androidOsParcelable.owner

        addFunction(CREATE_FROM_PARCEL_NAME.identifier, t.defaultType, Modality.ABSTRACT).apply {
            addValueParameter("source", androidOsParcel.defaultType)
        }

        addFunction(
            NEW_ARRAY_NAME.identifier, irBuiltIns.arrayClass.typeWith(t.defaultType.makeNullable()),
            Modality.ABSTRACT
        ).apply {
            addValueParameter("size", irBuiltIns.intType)
        }
    }.symbol

    val kotlinTimeDuration: IrClassSymbol = createClass(
        kotlinTime, "Duration", ClassKind.CLASS, Modality.FINAL, true
    ).apply {
        owner.valueClassRepresentation = InlineClassRepresentation(Name.identifier("rawValue"), irBuiltIns.longType as IrSimpleType)
    }

    val kotlinKClassJava: IrPropertySymbol = irFactory.buildProperty {
        name = Name.identifier("java")
    }.apply {
        parent = kotlinJvm
        addGetter().apply {
            addExtensionReceiver(irBuiltIns.kClassClass.starProjectedType)
            returnType = javaLangClass.defaultType
        }
    }.symbol

    val parcelCreateBinderArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("createBinderArray", irBuiltIns.arrayClass.typeWith(androidOsIBinder.defaultType)).symbol

    val parcelCreateBinderArrayList: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("createBinderArrayList", javaUtilArrayList.defaultType).symbol

    val parcelCreateBooleanArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction(
            "createBooleanArray",
            irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.booleanType).defaultType
        ).symbol

    val parcelCreateByteArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction(
            "createByteArray",
            irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.byteType).defaultType
        ).symbol

    val parcelCreateCharArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction(
            "createCharArray",
            irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.charType).defaultType
        ).symbol

    val parcelCreateDoubleArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction(
            "createDoubleArray",
            irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.doubleType).defaultType
        ).symbol

    val parcelCreateFloatArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction(
            "createFloatArray",
            irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.floatType).defaultType
        ).symbol

    val parcelCreateIntArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction(
            "createIntArray",
            irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.intType).defaultType
        ).symbol

    val parcelCreateLongArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction(
            "createLongArray",
            irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.longType).defaultType
        ).symbol

    val parcelCreateStringArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("createStringArray", irBuiltIns.arrayClass.typeWith(irBuiltIns.stringType)).symbol

    val parcelCreateStringArrayList: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("createStringArrayList", javaUtilArrayList.defaultType).symbol

    val parcelReadBundle: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("readBundle", androidOsBundle.defaultType).apply {
            addValueParameter("loader", javaLangClassLoader.defaultType)
        }.symbol

    val parcelReadByte: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("readByte", irBuiltIns.byteType).symbol

    val parcelReadDouble: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("readDouble", irBuiltIns.doubleType).symbol

    val parcelReadFileDescriptor: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("readFileDescriptor", androidOsParcelFileDescriptor.defaultType).symbol

    val parcelReadFloat: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("readFloat", irBuiltIns.floatType).symbol

    val parcelReadInt: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("readInt", irBuiltIns.intType).symbol

    val parcelReadLong: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("readLong", irBuiltIns.longType).symbol

    val parcelReadParcelable: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("readParcelable", androidOsParcelable.defaultType).apply {
            addValueParameter("loader", javaLangClassLoader.defaultType)
        }.symbol

    val parcelReadPersistableBundle: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("readPersistableBundle", androidOsPersistableBundle.defaultType).apply {
            addValueParameter("loader", javaLangClassLoader.defaultType)
        }.symbol

    val parcelReadSerializable: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("readSerializable", javaIoSerializable.defaultType).symbol

    val parcelReadSize: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("readSize", androidUtilSize.defaultType).symbol

    val parcelReadSizeF: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("readSizeF", androidUtilSizeF.defaultType).symbol

    val parcelReadSparseBooleanArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("readSparseBooleanArray", androidUtilSparseBooleanArray.defaultType).symbol

    val parcelReadString: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("readString", irBuiltIns.stringType).symbol

    val parcelReadStrongBinder: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("readStrongBinder", androidOsIBinder.defaultType).symbol

    val parcelReadValue: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("readValue", irBuiltIns.anyNType).apply {
            addValueParameter("loader", javaLangClassLoader.defaultType)
        }.symbol

    val parcelWriteBinderArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writeBinderArray", irBuiltIns.unitType).apply {
            addValueParameter("val", irBuiltIns.arrayClass.typeWith(androidOsIBinder.defaultType))
        }.symbol

    val parcelWriteBinderList: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writeBinderList", irBuiltIns.unitType).apply {
            addValueParameter("val", javaUtilList.defaultType)
        }.symbol

    val parcelWriteBooleanArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writeBooleanArray", irBuiltIns.unitType).apply {
            addValueParameter("val", irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.booleanType).defaultType)
        }.symbol

    val parcelWriteBundle: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writeBundle", irBuiltIns.unitType).apply {
            addValueParameter("val", androidOsBundle.defaultType)
        }.symbol

    val parcelWriteByte: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writeByte", irBuiltIns.unitType).apply {
            addValueParameter("val", irBuiltIns.byteType)
        }.symbol

    val parcelWriteByteArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writeByteArray", irBuiltIns.unitType).apply {
            addValueParameter("b", irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.byteType).defaultType)
        }.symbol

    val parcelWriteCharArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writeCharArray", irBuiltIns.unitType).apply {
            addValueParameter("val", irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.charType).defaultType)
        }.symbol

    val parcelWriteDouble: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writeDouble", irBuiltIns.unitType).apply {
            addValueParameter("val", irBuiltIns.doubleType)
        }.symbol

    val parcelWriteDoubleArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writeDoubleArray", irBuiltIns.unitType).apply {
            addValueParameter("val", irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.doubleType).defaultType)
        }.symbol

    val parcelWriteFileDescriptor: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writeFileDescriptor", irBuiltIns.unitType).apply {
            addValueParameter("val", javaIoFileDescriptor.defaultType)
        }.symbol

    val parcelWriteFloat: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writeFloat", irBuiltIns.unitType).apply {
            addValueParameter("val", irBuiltIns.floatType)
        }.symbol

    val parcelWriteFloatArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writeFloatArray", irBuiltIns.unitType).apply {
            addValueParameter("val", irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.floatType).defaultType)
        }.symbol

    val parcelWriteInt: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writeInt", irBuiltIns.unitType).apply {
            addValueParameter("val", irBuiltIns.intType)
        }.symbol

    val parcelWriteIntArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writeIntArray", irBuiltIns.unitType).apply {
            addValueParameter("val", irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.intType).defaultType)
        }.symbol

    val parcelWriteLong: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writeLong", irBuiltIns.unitType).apply {
            addValueParameter("val", irBuiltIns.longType)
        }.symbol

    val parcelWriteLongArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writeLongArray", irBuiltIns.unitType).apply {
            addValueParameter("val", irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.longType).defaultType)
        }.symbol

    val parcelWriteParcelable: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writeParcelable", irBuiltIns.unitType).apply {
            addValueParameter("p", androidOsParcelable.defaultType)
            addValueParameter("parcelableFlags", irBuiltIns.intType)
        }.symbol

    val parcelWritePersistableBundle: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writePersistableBundle", irBuiltIns.unitType).apply {
            addValueParameter("val", androidOsPersistableBundle.defaultType)
        }.symbol

    val parcelWriteSerializable: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writeSerializable", irBuiltIns.unitType).apply {
            addValueParameter("s", javaIoSerializable.defaultType)
        }.symbol

    val parcelWriteSize: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writeSize", irBuiltIns.unitType).apply {
            addValueParameter("val", androidUtilSize.defaultType)
        }.symbol

    val parcelWriteSizeF: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writeSizeF", irBuiltIns.unitType).apply {
            addValueParameter("val", androidUtilSizeF.defaultType)
        }.symbol

    val parcelWriteSparseBooleanArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writeSparseBooleanArray", irBuiltIns.unitType).apply {
            addValueParameter("val", androidUtilSparseBooleanArray.defaultType)
        }.symbol

    val parcelWriteString: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writeString", irBuiltIns.unitType).apply {
            addValueParameter("val", irBuiltIns.stringType)
        }.symbol

    val parcelWriteStringArray: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writeStringArray", irBuiltIns.unitType).apply {
            addValueParameter("val", irBuiltIns.arrayClass.typeWith(irBuiltIns.stringType))
        }.symbol

    val parcelWriteStringList: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writeStringList", irBuiltIns.unitType).apply {
            addValueParameter("val", javaUtilList.defaultType)
        }.symbol

    val parcelWriteStrongBinder: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writeStrongBinder", irBuiltIns.unitType).apply {
            addValueParameter("val", androidOsIBinder.defaultType)
        }.symbol

    val parcelWriteValue: IrSimpleFunctionSymbol =
        androidOsParcel.owner.addFunction("writeValue", irBuiltIns.unitType).apply {
            addValueParameter("v", irBuiltIns.anyNType)
        }.symbol

    val textUtilsWriteToParcel: IrSimpleFunctionSymbol =
        androidTextTextUtils.owner.addFunction(WRITE_TO_PARCEL_NAME.identifier, irBuiltIns.unitType, isStatic = true).apply {
            addValueParameter("cs", irBuiltIns.charSequenceClass.defaultType)
            addValueParameter("p", androidOsParcel.defaultType)
            addValueParameter("parcelableFlags", irBuiltIns.intType)
        }.symbol

    val classGetClassLoader: IrSimpleFunctionSymbol =
        javaLangClass.owner.addFunction("getClassLoader", javaLangClassLoader.defaultType).symbol

    val arrayListConstructor: IrConstructorSymbol = javaUtilArrayList.owner.addConstructor().apply {
        addValueParameter("p_0", irBuiltIns.intType)
    }.symbol

    val arrayListAdd: IrSimpleFunctionSymbol =
        javaUtilArrayList.owner.addFunction("add", irBuiltIns.booleanType).apply {
            addValueParameter("p_0", irBuiltIns.anyNType)
        }.symbol

    val linkedHashMapConstructor: IrConstructorSymbol =
        javaUtilLinkedHashMap.owner.addConstructor().apply {
            addValueParameter("p_0", irBuiltIns.intType)
        }.symbol

    val linkedHashMapPut: IrSimpleFunctionSymbol =
        javaUtilLinkedHashMap.owner.addFunction("put", irBuiltIns.anyNType).apply {
            addValueParameter("p_0", irBuiltIns.anyNType)
            addValueParameter("p_1", irBuiltIns.anyNType)
        }.symbol

    val linkedHashSetConstructor: IrConstructorSymbol =
        javaUtilLinkedHashSet.owner.addConstructor().apply {
            addValueParameter("p_0", irBuiltIns.intType)
        }.symbol

    val linkedHashSetAdd: IrSimpleFunctionSymbol =
        javaUtilLinkedHashSet.owner.addFunction("add", irBuiltIns.booleanType).apply {
            addValueParameter("p_0", irBuiltIns.anyNType)
        }.symbol

    val treeMapConstructor: IrConstructorSymbol = javaUtilTreeMap.owner.addConstructor().symbol

    val treeMapPut: IrSimpleFunctionSymbol =
        javaUtilTreeMap.owner.addFunction("put", irBuiltIns.anyNType).apply {
            addValueParameter("p_0", irBuiltIns.anyNType)
            addValueParameter("p_1", irBuiltIns.anyNType)
        }.symbol

    val treeSetConstructor: IrConstructorSymbol = javaUtilTreeSet.owner.addConstructor().symbol

    val treeSetAdd: IrSimpleFunctionSymbol =
        javaUtilTreeSet.owner.addFunction("add", irBuiltIns.booleanType).apply {
            addValueParameter("p_0", irBuiltIns.anyNType)
        }.symbol

    val textUtilsCharSequenceCreator: IrFieldSymbol = androidTextTextUtils.owner.addField {
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
        return pluginContext.referenceFunctions(callableId)
            .firstOrNull {
                it.owner.extensionReceiverParameter?.type?.classFqName == receiver &&
                        it.owner.valueParameters.isEmpty()
            }
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
        irFactory.buildFun {
            name = Name.special("<unsafe-coerce>")
            origin = IrDeclarationOrigin.IR_BUILTINS_STUB
        }.apply {
            parent = kotlinJvmInternalPackage
            val src = addTypeParameter("T", irBuiltIns.anyNType)
            val dst = addTypeParameter("R", irBuiltIns.anyNType)
            addValueParameter("v", src.defaultType)
            returnType = dst.defaultType
        }.symbol

    private fun createPackage(packageName: String): IrPackageFragment =
        IrExternalPackageFragmentImpl.createEmptyExternalPackageFragment(
            moduleFragment.descriptor,
            FqName(packageName)
        )

    private fun createClass(
        irPackage: IrPackageFragment,
        shortName: String,
        classKind: ClassKind,
        classModality: Modality,
        isValueClass: Boolean = false,
    ): IrClassSymbol = irFactory.buildClass {
        name = Name.identifier(shortName)
        kind = classKind
        modality = classModality
        isValue = isValueClass
    }.apply {
        parent = irPackage
        createImplicitParameterDeclarationWithWrappedDescriptor()
    }.symbol

    fun createBuilder(
        symbol: IrSymbol,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET
    ) = AndroidIrBuilder(this, symbol, startOffset, endOffset)
}
