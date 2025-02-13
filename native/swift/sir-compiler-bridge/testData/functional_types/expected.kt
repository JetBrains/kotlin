@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("root_foo_consuming_closure_returning_closure__TypesOfArguments__function__")
public fun root_foo_consuming_closure_returning_closure__TypesOfArguments__function__(arg1: kotlin.native.internal.NativePtr): Int {
    val __arg1 = {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->kotlin.native.internal.NativePtr>(arg1);
        {
            {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Unit>(kotlinFun());
        {
            kotlinFun()
        }
    }()
        }
    }()
    val _result = foo_consuming_closure_returning_closure(__arg1)
    return _result
}

@ExportedBridge("root_foo_consuming_closure_with_param__TypesOfArguments__function_MyAnotherClass__")
public fun root_foo_consuming_closure_with_param__TypesOfArguments__function_MyAnotherClass__(arg1: kotlin.native.internal.NativePtr): Int {
    val __arg1 = {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->kotlin.native.internal.NativePtr>(arg1);
        { arg0: MyAnotherClass ->
            kotlin.native.internal.ref.dereferenceExternalRCRef(kotlinFun(kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))) as MyClass
        }
    }()
    val _result = foo_consuming_closure_with_param(__arg1)
    return _result
}

@ExportedBridge("root_foo_consuming_simple_closure__TypesOfArguments__function__")
public fun root_foo_consuming_simple_closure__TypesOfArguments__function__(arg1: kotlin.native.internal.NativePtr): Int {
    val __arg1 = {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Unit>(arg1);
        {
            kotlinFun()
        }
    }()
    val _result = foo_consuming_simple_closure(__arg1)
    return _result
}

@ExportedBridge("root_foo_returning_closure_returning_closure__TypesOfArguments__Int32__")
public fun root_foo_returning_closure_returning_closure__TypesOfArguments__Int32__(arg1: Int): kotlin.native.internal.NativePtr {
    val __arg1 = arg1
    val _result = foo_returning_closure_returning_closure(__arg1)
    return {
        val newClosure = { kotlin.native.internal.ref.createRetainedExternalRCRef(_result()).toLong() }
        newClosure.objcPtr()
    }()
}

@ExportedBridge("root_foo_returning_simple_closure__TypesOfArguments__Int32__")
public fun root_foo_returning_simple_closure__TypesOfArguments__Int32__(arg1: Int): kotlin.native.internal.NativePtr {
    val __arg1 = arg1
    val _result = foo_returning_simple_closure(__arg1)
    return {
        val newClosure = { kotlin.native.internal.ref.createRetainedExternalRCRef(_result()).toLong() }
        newClosure.objcPtr()
    }()
}
