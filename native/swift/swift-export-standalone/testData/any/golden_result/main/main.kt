import kotlin.native.internal.ExportedBridge

@ExportedBridge("__root___MyObject_get")
public fun __root___MyObject_get(): kotlin.native.internal.NativePtr {
    val _result = MyObject
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___getMainObject")
public fun __root___getMainObject(): kotlin.native.internal.NativePtr {
    val _result = getMainObject()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___isMainObject__TypesOfArguments__uintptr_t__")
public fun __root___isMainObject(obj: kotlin.native.internal.NativePtr): Boolean {
    val __obj = kotlin.native.internal.ref.dereferenceExternalRCRef(obj) as kotlin.Any
    val _result = isMainObject(__obj.autoCast())
    return _result
}

@ExportedBridge("opaque_produce_ABSTRACT_CLASS")
public fun opaque_produce_ABSTRACT_CLASS(): kotlin.native.internal.NativePtr {
    val _result = opaque.produce_ABSTRACT_CLASS()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("opaque_produce_DATA_CLASS")
public fun opaque_produce_DATA_CLASS(): kotlin.native.internal.NativePtr {
    val _result = opaque.produce_DATA_CLASS()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("opaque_produce_DATA_OBJECT")
public fun opaque_produce_DATA_OBJECT(): kotlin.native.internal.NativePtr {
    val _result = opaque.produce_DATA_OBJECT()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("opaque_produce_ENUM")
public fun opaque_produce_ENUM(): kotlin.native.internal.NativePtr {
    val _result = opaque.produce_ENUM()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("opaque_produce_INTERFACE")
public fun opaque_produce_INTERFACE(): kotlin.native.internal.NativePtr {
    val _result = opaque.produce_INTERFACE()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("opaque_produce_OPEN_CLASS")
public fun opaque_produce_OPEN_CLASS(): kotlin.native.internal.NativePtr {
    val _result = opaque.produce_OPEN_CLASS()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("opaque_produce_VALUE_CLASS")
public fun opaque_produce_VALUE_CLASS(): kotlin.native.internal.NativePtr {
    val _result = opaque.produce_VALUE_CLASS()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("opaque_recieve_ABSTRACT_CLASS__TypesOfArguments__uintptr_t__")
public fun opaque_recieve_ABSTRACT_CLASS(x: kotlin.native.internal.NativePtr): Unit {
    val __x = kotlin.native.internal.ref.dereferenceExternalRCRef(x) as kotlin.Any
    opaque.recieve_ABSTRACT_CLASS(__x.autoCast())
}

@ExportedBridge("opaque_recieve_DATA_CLASS__TypesOfArguments__uintptr_t__")
public fun opaque_recieve_DATA_CLASS(x: kotlin.native.internal.NativePtr): Unit {
    val __x = kotlin.native.internal.ref.dereferenceExternalRCRef(x) as kotlin.Any
    opaque.recieve_DATA_CLASS(__x.autoCast())
}

@ExportedBridge("opaque_recieve_DATA_OBJECT__TypesOfArguments__uintptr_t__")
public fun opaque_recieve_DATA_OBJECT(x: kotlin.native.internal.NativePtr): Unit {
    val __x = kotlin.native.internal.ref.dereferenceExternalRCRef(x) as kotlin.Any
    opaque.recieve_DATA_OBJECT(__x.autoCast())
}

@ExportedBridge("opaque_recieve_ENUM__TypesOfArguments__uintptr_t__")
public fun opaque_recieve_ENUM(x: kotlin.native.internal.NativePtr): Unit {
    val __x = kotlin.native.internal.ref.dereferenceExternalRCRef(x) as kotlin.Any
    opaque.recieve_ENUM(__x.autoCast())
}

@ExportedBridge("opaque_recieve_INTERFACE__TypesOfArguments__uintptr_t__")
public fun opaque_recieve_INTERFACE(x: kotlin.native.internal.NativePtr): Unit {
    val __x = kotlin.native.internal.ref.dereferenceExternalRCRef(x) as kotlin.Any
    opaque.recieve_INTERFACE(__x.autoCast())
}

@ExportedBridge("opaque_recieve_OPEN_CLASS__TypesOfArguments__uintptr_t__")
public fun opaque_recieve_OPEN_CLASS(x: kotlin.native.internal.NativePtr): Unit {
    val __x = kotlin.native.internal.ref.dereferenceExternalRCRef(x) as kotlin.Any
    opaque.recieve_OPEN_CLASS(__x.autoCast())
}

@ExportedBridge("opaque_recieve_VALUE_CLASS__TypesOfArguments__uintptr_t__")
public fun opaque_recieve_VALUE_CLASS(x: kotlin.native.internal.NativePtr): Unit {
    val __x = kotlin.native.internal.ref.dereferenceExternalRCRef(x) as kotlin.Any
    opaque.recieve_VALUE_CLASS(__x.autoCast())
}

private inline fun <reified T, reified U> T.autoCast(): U = this as U
