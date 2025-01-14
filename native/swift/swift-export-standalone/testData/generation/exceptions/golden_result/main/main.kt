@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(Object::class, "4main6ObjectC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("__root___Object_init_allocate")
public fun __root___Object_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Object>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Object_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__")
public fun __root___Object_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__(__kt: kotlin.native.internal.NativePtr, arg: Int, __error: kotlinx.cinterop.COpaquePointerVar): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __arg = arg
    val ____error = __error
    try {
        val _result = kotlin.native.internal.initInstance(____kt, Object(__arg))
        return _result
    } catch (error: Throwable) {
        ____error.value = StableRef.create(error).asCPointer()
        return Unit
    }
}

@ExportedBridge("__root___Object_init_initialize__TypesOfArguments__Swift_UInt_Swift_Double__")
public fun __root___Object_init_initialize__TypesOfArguments__Swift_UInt_Swift_Double__(__kt: kotlin.native.internal.NativePtr, arg: Double, __error: kotlinx.cinterop.COpaquePointerVar): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __arg = arg
    val ____error = __error
    try {
        val _result = kotlin.native.internal.initInstance(____kt, Object(__arg))
        return _result
    } catch (error: Throwable) {
        ____error.value = StableRef.create(error).asCPointer()
        return Unit
    }
}

@ExportedBridge("__root___Object_init_initialize__TypesOfArguments__Swift_UInt_Swift_Bool__")
public fun __root___Object_init_initialize__TypesOfArguments__Swift_UInt_Swift_Bool__(__kt: kotlin.native.internal.NativePtr, arg: Boolean, __error: kotlinx.cinterop.COpaquePointerVar): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __arg = arg
    val ____error = __error
    try {
        val _result = kotlin.native.internal.initInstance(____kt, Object(__arg))
        return _result
    } catch (error: Throwable) {
        ____error.value = StableRef.create(error).asCPointer()
        return Unit
    }
}

@ExportedBridge("__root___Object_init_initialize__TypesOfArguments__Swift_UInt_Swift_Unicode_UTF16_CodeUnit__")
public fun __root___Object_init_initialize__TypesOfArguments__Swift_UInt_Swift_Unicode_UTF16_CodeUnit__(__kt: kotlin.native.internal.NativePtr, arg: Char, __error: kotlinx.cinterop.COpaquePointerVar): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __arg = arg
    val ____error = __error
    try {
        val _result = kotlin.native.internal.initInstance(____kt, Object(__arg))
        return _result
    } catch (error: Throwable) {
        ____error.value = StableRef.create(error).asCPointer()
        return Unit
    }
}

@ExportedBridge("__root___Object_init_initialize__TypesOfArguments__Swift_UInt_KotlinRuntime_KotlinBase__")
public fun __root___Object_init_initialize__TypesOfArguments__Swift_UInt_KotlinRuntime_KotlinBase__(__kt: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr, __error: kotlinx.cinterop.COpaquePointerVar): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as kotlin.Any
    val ____error = __error
    try {
        val _result = kotlin.native.internal.initInstance(____kt, Object(__arg))
        return _result
    } catch (error: Throwable) {
        ____error.value = StableRef.create(error).asCPointer()
        return Unit
    }
}

@ExportedBridge("__root___Object_init_initialize__TypesOfArguments__Swift_UInt_KotlinRuntime_KotlinBase_opt___")
public fun __root___Object_init_initialize__TypesOfArguments__Swift_UInt_KotlinRuntime_KotlinBase_opt___(__kt: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr, __error: kotlinx.cinterop.COpaquePointerVar): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __arg = if (arg == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as kotlin.Any
    val ____error = __error
    try {
        val _result = kotlin.native.internal.initInstance(____kt, Object(__arg))
        return _result
    } catch (error: Throwable) {
        ____error.value = StableRef.create(error).asCPointer()
        return Unit
    }
}

@ExportedBridge("__root___Object_init_initialize__TypesOfArguments__Swift_UInt_main_Object__")
public fun __root___Object_init_initialize__TypesOfArguments__Swift_UInt_main_Object__(__kt: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr, __error: kotlinx.cinterop.COpaquePointerVar): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as Object
    val ____error = __error
    try {
        val _result = kotlin.native.internal.initInstance(____kt, Object(__arg))
        return _result
    } catch (error: Throwable) {
        ____error.value = StableRef.create(error).asCPointer()
        return Unit
    }
}

@ExportedBridge("__root___throwing_fun_any")
public fun __root___throwing_fun_any(_out_error: kotlinx.cinterop.COpaquePointerVar): kotlin.native.internal.NativePtr {
    val ___out_error = _out_error
    try {
        val _result = throwing_fun_any()
        return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
    } catch (error: Throwable) {
        ___out_error.value = StableRef.create(error).asCPointer()
        return kotlin.native.internal.NativePtr.NULL
    }
}

@ExportedBridge("__root___throwing_fun_any__TypesOfArguments__KotlinRuntime_KotlinBase__")
public fun __root___throwing_fun_any__TypesOfArguments__KotlinRuntime_KotlinBase__(arg: kotlin.native.internal.NativePtr, _out_error: kotlinx.cinterop.COpaquePointerVar): kotlin.native.internal.NativePtr {
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as kotlin.Any
    val ___out_error = _out_error
    try {
        val _result = throwing_fun_any(__arg)
        return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
    } catch (error: Throwable) {
        ___out_error.value = StableRef.create(error).asCPointer()
        return kotlin.native.internal.NativePtr.NULL
    }
}

@ExportedBridge("__root___throwing_fun_boolean")
public fun __root___throwing_fun_boolean(_out_error: kotlinx.cinterop.COpaquePointerVar): Boolean {
    val ___out_error = _out_error
    try {
        val _result = throwing_fun_boolean()
        return _result
    } catch (error: Throwable) {
        ___out_error.value = StableRef.create(error).asCPointer()
        return false
    }
}

@ExportedBridge("__root___throwing_fun_boolean__TypesOfArguments__Swift_Bool__")
public fun __root___throwing_fun_boolean__TypesOfArguments__Swift_Bool__(arg: Boolean, _out_error: kotlinx.cinterop.COpaquePointerVar): Boolean {
    val __arg = arg
    val ___out_error = _out_error
    try {
        val _result = throwing_fun_boolean(__arg)
        return _result
    } catch (error: Throwable) {
        ___out_error.value = StableRef.create(error).asCPointer()
        return false
    }
}

@ExportedBridge("__root___throwing_fun_char")
public fun __root___throwing_fun_char(_out_error: kotlinx.cinterop.COpaquePointerVar): Char {
    val ___out_error = _out_error
    try {
        val _result = throwing_fun_char()
        return _result
    } catch (error: Throwable) {
        ___out_error.value = StableRef.create(error).asCPointer()
        return '\u0000'
    }
}

@ExportedBridge("__root___throwing_fun_char__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit__")
public fun __root___throwing_fun_char__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit__(arg: Char, _out_error: kotlinx.cinterop.COpaquePointerVar): Char {
    val __arg = arg
    val ___out_error = _out_error
    try {
        val _result = throwing_fun_char(__arg)
        return _result
    } catch (error: Throwable) {
        ___out_error.value = StableRef.create(error).asCPointer()
        return '\u0000'
    }
}

@ExportedBridge("__root___throwing_fun_double")
public fun __root___throwing_fun_double(_out_error: kotlinx.cinterop.COpaquePointerVar): Double {
    val ___out_error = _out_error
    try {
        val _result = throwing_fun_double()
        return _result
    } catch (error: Throwable) {
        ___out_error.value = StableRef.create(error).asCPointer()
        return 0.0
    }
}

@ExportedBridge("__root___throwing_fun_double__TypesOfArguments__Swift_Double__")
public fun __root___throwing_fun_double__TypesOfArguments__Swift_Double__(arg: Double, _out_error: kotlinx.cinterop.COpaquePointerVar): Double {
    val __arg = arg
    val ___out_error = _out_error
    try {
        val _result = throwing_fun_double(__arg)
        return _result
    } catch (error: Throwable) {
        ___out_error.value = StableRef.create(error).asCPointer()
        return 0.0
    }
}

@ExportedBridge("__root___throwing_fun_int")
public fun __root___throwing_fun_int(_out_error: kotlinx.cinterop.COpaquePointerVar): Int {
    val ___out_error = _out_error
    try {
        val _result = throwing_fun_int()
        return _result
    } catch (error: Throwable) {
        ___out_error.value = StableRef.create(error).asCPointer()
        return 0
    }
}

@ExportedBridge("__root___throwing_fun_int__TypesOfArguments__Swift_Int32__")
public fun __root___throwing_fun_int__TypesOfArguments__Swift_Int32__(arg: Int, _out_error: kotlinx.cinterop.COpaquePointerVar): Int {
    val __arg = arg
    val ___out_error = _out_error
    try {
        val _result = throwing_fun_int(__arg)
        return _result
    } catch (error: Throwable) {
        ___out_error.value = StableRef.create(error).asCPointer()
        return 0
    }
}

@ExportedBridge("__root___throwing_fun_never")
public fun __root___throwing_fun_never(_out_error: kotlinx.cinterop.COpaquePointerVar): kotlin.native.internal.NativePtr {
    val ___out_error = _out_error
    try {
        val _result = throwing_fun_never()
        return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
    } catch (error: Throwable) {
        ___out_error.value = StableRef.create(error).asCPointer()
        return kotlin.native.internal.NativePtr.NULL
    }
}

@ExportedBridge("__root___throwing_fun_nullable")
public fun __root___throwing_fun_nullable(_out_error: kotlinx.cinterop.COpaquePointerVar): kotlin.native.internal.NativePtr {
    val ___out_error = _out_error
    try {
        val _result = throwing_fun_nullable()
        return if (_result == null) return kotlin.native.internal.NativePtr.NULL else return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
    } catch (error: Throwable) {
        ___out_error.value = StableRef.create(error).asCPointer()
        return kotlin.native.internal.NativePtr.NULL
    }
}

@ExportedBridge("__root___throwing_fun_nullable__TypesOfArguments__KotlinRuntime_KotlinBase_opt___")
public fun __root___throwing_fun_nullable__TypesOfArguments__KotlinRuntime_KotlinBase_opt___(arg: kotlin.native.internal.NativePtr, _out_error: kotlinx.cinterop.COpaquePointerVar): kotlin.native.internal.NativePtr {
    val __arg = if (arg == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as kotlin.Any
    val ___out_error = _out_error
    try {
        val _result = throwing_fun_nullable(__arg)
        return if (_result == null) return kotlin.native.internal.NativePtr.NULL else return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
    } catch (error: Throwable) {
        ___out_error.value = StableRef.create(error).asCPointer()
        return kotlin.native.internal.NativePtr.NULL
    }
}

@ExportedBridge("__root___throwing_fun_object")
public fun __root___throwing_fun_object(_out_error: kotlinx.cinterop.COpaquePointerVar): kotlin.native.internal.NativePtr {
    val ___out_error = _out_error
    try {
        val _result = throwing_fun_object()
        return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
    } catch (error: Throwable) {
        ___out_error.value = StableRef.create(error).asCPointer()
        return kotlin.native.internal.NativePtr.NULL
    }
}

@ExportedBridge("__root___throwing_fun_object__TypesOfArguments__KotlinRuntime_KotlinBase__")
public fun __root___throwing_fun_object__TypesOfArguments__KotlinRuntime_KotlinBase__(arg: kotlin.native.internal.NativePtr, _out_error: kotlinx.cinterop.COpaquePointerVar): kotlin.native.internal.NativePtr {
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as kotlin.Any
    val ___out_error = _out_error
    try {
        val _result = throwing_fun_object(__arg)
        return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
    } catch (error: Throwable) {
        ___out_error.value = StableRef.create(error).asCPointer()
        return kotlin.native.internal.NativePtr.NULL
    }
}

@ExportedBridge("__root___throwing_fun_void")
public fun __root___throwing_fun_void(_out_error: kotlinx.cinterop.COpaquePointerVar): Unit {
    val ___out_error = _out_error
    try {
        val _result = throwing_fun_void()
        return _result
    } catch (error: Throwable) {
        ___out_error.value = StableRef.create(error).asCPointer()
        return Unit
    }
}

