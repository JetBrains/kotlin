@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(funinterface.FunctorClass::class, "22ExportedKotlinPackages12funinterfaceO12funinterfaceE12FunctorClassC")
@file:kotlin.native.internal.objc.BindClassToObjCName(funinterface.FunctionalInterface::class, "_FunctionalInterface")
@file:kotlin.native.internal.objc.BindClassToObjCName(funinterface.XMLFunctionalInterfaceWithLeadingAbbreviation::class, "_XMLFunctionalInterfaceWithLeadingAbbreviation")
@file:kotlin.native.internal.objc.BindClassToObjCName(funinterface._123FunctionalInterfaceWithLeadingNumbers::class, "__123FunctionalInterfaceWithLeadingNumbers")
@file:kotlin.native.internal.objc.BindClassToObjCName(funinterface._123XMLFunctionalInterfaceWithLeadingUnderscoreNumbersAndAbbreviation::class, "__123XMLFunctionalInterfaceWithLeadingUnderscoreNumbersAndAbbreviation")
@file:kotlin.native.internal.objc.BindClassToObjCName(funinterface._FunctionalInterfaceWithLeadingUnderscore::class, "__FunctionalInterfaceWithLeadingUnderscore")
@file:kotlin.native.internal.objc.BindClassToObjCName(funinterface.functionalInterfaceWithAlreadyLowercaseLeading::class, "_functionalInterfaceWithAlreadyLowercaseLeading")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("funinterface_FunctionalInterface__TypesOfArguments__U2829202D_U20Swift_Int32__")
public fun funinterface_FunctionalInterface__TypesOfArguments__U2829202D_U20Swift_Int32__(function: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __function = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Int>(function);
        {
            kotlinFun()
        }
    }
    val _result = funinterface.FunctionalInterface(__function)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("funinterface_FunctorClass_init_allocate")
public fun funinterface_FunctorClass_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<funinterface.FunctorClass>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("funinterface_FunctorClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun funinterface_FunctorClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, funinterface.FunctorClass())
}

@ExportedBridge("funinterface_XMLFunctionalInterfaceWithLeadingAbbreviation__TypesOfArguments__U2829202D_U20Swift_Int32__")
public fun funinterface_XMLFunctionalInterfaceWithLeadingAbbreviation__TypesOfArguments__U2829202D_U20Swift_Int32__(function: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __function = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Int>(function);
        {
            kotlinFun()
        }
    }
    val _result = funinterface.XMLFunctionalInterfaceWithLeadingAbbreviation(__function)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("funinterface__123FunctionalInterfaceWithLeadingNumbers__TypesOfArguments__U2829202D_U20Swift_Int32__")
public fun funinterface__123FunctionalInterfaceWithLeadingNumbers__TypesOfArguments__U2829202D_U20Swift_Int32__(function: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __function = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Int>(function);
        {
            kotlinFun()
        }
    }
    val _result = funinterface._123FunctionalInterfaceWithLeadingNumbers(__function)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("funinterface__123XMLFunctionalInterfaceWithLeadingUnderscoreNumbersAndAbbreviation__TypesOfArguments__U2829202D_U20Swift_Int32__")
public fun funinterface__123XMLFunctionalInterfaceWithLeadingUnderscoreNumbersAndAbbreviation__TypesOfArguments__U2829202D_U20Swift_Int32__(function: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __function = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Int>(function);
        {
            kotlinFun()
        }
    }
    val _result = funinterface._123XMLFunctionalInterfaceWithLeadingUnderscoreNumbersAndAbbreviation(__function)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("funinterface__FunctionalInterfaceWithLeadingUnderscore__TypesOfArguments__U2829202D_U20Swift_Int32__")
public fun funinterface__FunctionalInterfaceWithLeadingUnderscore__TypesOfArguments__U2829202D_U20Swift_Int32__(function: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __function = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Int>(function);
        {
            kotlinFun()
        }
    }
    val _result = funinterface._FunctionalInterfaceWithLeadingUnderscore(__function)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("funinterface_functionalInterfaceWithAlreadyLowercaseLeading__TypesOfArguments__U2829202D_U20Swift_Int32__")
public fun funinterface_functionalInterfaceWithAlreadyLowercaseLeading__TypesOfArguments__U2829202D_U20Swift_Int32__(function: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __function = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Int>(function);
        {
            kotlinFun()
        }
    }
    val _result = funinterface.functionalInterfaceWithAlreadyLowercaseLeading(__function)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
