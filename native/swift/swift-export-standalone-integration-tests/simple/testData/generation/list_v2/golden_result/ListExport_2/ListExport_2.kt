@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(list2.MyList::class, "_ExportedKotlinPackages_list2_MyList")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("list2_testListAny__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun list2_testListAny__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_anyU20KotlinRuntimeSupport__KotlinBridgeable___(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as list2.MyList<kotlin.Any>
    val _result = run { list2.testListAny(__l) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("list2_testListInt__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Int32___")
public fun list2_testListInt__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Int32___(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as list2.MyList<kotlin.Int>
    val _result = run { list2.testListInt(__l) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("list2_testListListInt__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Int32____")
public fun list2_testListListInt__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Int32____(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as list2.MyList<list2.MyList<kotlin.Int>>
    val _result = run { list2.testListListInt(__l) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("list2_testListNothing__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Never___")
public fun list2_testListNothing__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Never___(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as list2.MyList<kotlin.Nothing>
    val _result = run { list2.testListNothing(__l) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("list2_testListOptAny__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____")
public fun list2_testListOptAny__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as list2.MyList<kotlin.Any?>
    val _result = run { list2.testListOptAny(__l) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("list2_testListOptInt__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Optional_Swift_Int32____")
public fun list2_testListOptInt__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Optional_Swift_Int32____(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as list2.MyList<kotlin.Int?>
    val _result = run { list2.testListOptInt(__l) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("list2_testListOptListInt__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Optional_anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Int32_____")
public fun list2_testListOptListInt__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Optional_anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Int32_____(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as list2.MyList<list2.MyList<kotlin.Int>?>
    val _result = run { list2.testListOptListInt(__l) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("list2_testListOptNothing__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Optional_Swift_Never____")
public fun list2_testListOptNothing__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Optional_Swift_Never____(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as list2.MyList<kotlin.Nothing?>
    val _result = run { list2.testListOptNothing(__l) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("list2_testListOptString__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Optional_Swift_String____")
public fun list2_testListOptString__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Optional_Swift_String____(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as list2.MyList<kotlin.String?>
    val _result = run { list2.testListOptString(__l) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("list2_testListShort__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Int16___")
public fun list2_testListShort__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Int16___(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as list2.MyList<kotlin.Short>
    val _result = run { list2.testListShort(__l) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("list2_testListString__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_String___")
public fun list2_testListString__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_String___(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as list2.MyList<kotlin.String>
    val _result = run { list2.testListString(__l) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("list2_testOptListInt__TypesOfArguments__Swift_Optional_anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Int32____")
public fun list2_testOptListInt__TypesOfArguments__Swift_Optional_anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Int32____(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = if (l == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(l) as list2.MyList<kotlin.Int>
    val _result = run { list2.testOptListInt(__l) }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("list2_testStarList__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList__")
public fun list2_testStarList__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList__(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as list2.MyList<kotlin.Any?>
    val _result = run { list2.testStarList(__l) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
