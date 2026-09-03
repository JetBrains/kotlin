@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("__root___foo")
public fun __root___foo(): kotlin.native.internal.NativePtr {
    val _result = run { foo() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___testListAny__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun __root___testListAny__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_anyU20KotlinRuntimeSupport__KotlinBridgeable___(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as kotlin.collections.List<kotlin.Any>
    val _result = run { testListAny(__l) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___testListInt__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_Int32___")
public fun __root___testListInt__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_Int32___(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as kotlin.collections.List<kotlin.Int>
    val _result = run { testListInt(__l) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___testListListInt__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_anyU20KotlinRuntimeSupport_TypedList_Swift_Int32____")
public fun __root___testListListInt__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_anyU20KotlinRuntimeSupport_TypedList_Swift_Int32____(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as kotlin.collections.List<kotlin.collections.List<kotlin.Int>>
    val _result = run { testListListInt(__l) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___testListNothing__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_Never___")
public fun __root___testListNothing__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_Never___(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as kotlin.collections.List<kotlin.Nothing>
    val _result = run { testListNothing(__l) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___testListOptAny__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____")
public fun __root___testListOptAny__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as kotlin.collections.List<kotlin.Any?>
    val _result = run { testListOptAny(__l) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___testListOptInt__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_Optional_Swift_Int32____")
public fun __root___testListOptInt__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_Optional_Swift_Int32____(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as kotlin.collections.List<kotlin.Int?>
    val _result = run { testListOptInt(__l) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___testListOptListInt__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_Optional_anyU20KotlinRuntimeSupport_TypedList_Swift_Int32_____")
public fun __root___testListOptListInt__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_Optional_anyU20KotlinRuntimeSupport_TypedList_Swift_Int32_____(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as kotlin.collections.List<kotlin.collections.List<kotlin.Int>?>
    val _result = run { testListOptListInt(__l) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___testListOptNothing__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_Optional_Swift_Never____")
public fun __root___testListOptNothing__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_Optional_Swift_Never____(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as kotlin.collections.List<kotlin.Nothing?>
    val _result = run { testListOptNothing(__l) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___testListOptString__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_Optional_Swift_String____")
public fun __root___testListOptString__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_Optional_Swift_String____(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as kotlin.collections.List<kotlin.String?>
    val _result = run { testListOptString(__l) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___testListShort__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_Int16___")
public fun __root___testListShort__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_Int16___(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as kotlin.collections.List<kotlin.Short>
    val _result = run { testListShort(__l) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___testListString__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_String___")
public fun __root___testListString__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_String___(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as kotlin.collections.List<kotlin.String>
    val _result = run { testListString(__l) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___testOptListInt__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport_TypedList_Swift_Int32____")
public fun __root___testOptListInt__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport_TypedList_Swift_Int32____(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = if (l == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(l) as kotlin.collections.List<kotlin.Int>
    val _result = run { testOptListInt(__l) }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___testStarList__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_List__")
public fun __root___testStarList__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_List__(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as kotlin.collections.List<kotlin.Any?>
    val _result = run { testStarList(__l) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
