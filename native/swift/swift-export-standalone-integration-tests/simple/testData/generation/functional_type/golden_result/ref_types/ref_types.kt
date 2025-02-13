@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___consume_block_with_reftype_consumer__TypesOfArguments__U28data_FooU29202D_U20Swift_Void__")
public fun __root___consume_block_with_reftype_consumer__TypesOfArguments__U28data_FooU29202D_U20Swift_Void__(block: kotlin.native.internal.NativePtr): Unit {
    val __block = {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Unit>(block);
        { arg0: Foo ->
            kotlinFun(kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))
        }
    }()
    consume_block_with_reftype_consumer(__block)
}

@ExportedBridge("__root___consume_block_with_reftype_factory__TypesOfArguments__U2829202D_U20data_Foo__")
public fun __root___consume_block_with_reftype_factory__TypesOfArguments__U2829202D_U20data_Foo__(block: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __block = {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->kotlin.native.internal.NativePtr>(block);
        {
            kotlin.native.internal.ref.dereferenceExternalRCRef(kotlinFun()) as Foo
        }
    }()
    val _result = consume_block_with_reftype_factory(__block)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___consume_block_with_reftype_unzip__TypesOfArguments__U28data_BarU29202D_U20data_Foo__")
public fun __root___consume_block_with_reftype_unzip__TypesOfArguments__U28data_BarU29202D_U20data_Foo__(block: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __block = {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->kotlin.native.internal.NativePtr>(block);
        { arg0: Bar ->
            kotlin.native.internal.ref.dereferenceExternalRCRef(kotlinFun(kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))) as Foo
        }
    }()
    val _result = consume_block_with_reftype_unzip(__block)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___consume_block_with_reftype_zip__TypesOfArguments__U28data_Foo_U20data_FooU29202D_U20data_Bar__")
public fun __root___consume_block_with_reftype_zip__TypesOfArguments__U28data_Foo_U20data_FooU29202D_U20data_Bar__(block: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __block = {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr, kotlin.native.internal.NativePtr)->kotlin.native.internal.NativePtr>(block);
        { arg0: Foo, arg1: Foo ->
            kotlin.native.internal.ref.dereferenceExternalRCRef(kotlinFun(kotlin.native.internal.ref.createRetainedExternalRCRef(arg0), kotlin.native.internal.ref.createRetainedExternalRCRef(arg1))) as Bar
        }
    }()
    val _result = consume_block_with_reftype_zip(__block)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
