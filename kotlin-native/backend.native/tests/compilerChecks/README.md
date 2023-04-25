These tests aren't invoked so far because there is no mechanism of checking compilation errors yet.

To run tests manually on MacOS,
- build platformLibs: `./gradlew :kotlin-native:platformLibs:macos_arm64Install` or `./gradlew :kotlin-native:platformLibs:macos_x64Install`
- run `runtests.sh` as described below. 

K1: `kotlin-native/backend.native/tests/compilerChecks/runtests.sh -language-version 1.9`
K2: `kotlin-native/backend.native/tests/compilerChecks/runtests.sh -language-version 2.0`

Reference output:
```text
kotlin-native/backend.native/tests/compilerChecks/t1.kt
kotlin-native/backend.native/tests/compilerChecks/t1.kt:10:5: error: variadic function pointers are not supported
kotlin-native/backend.native/tests/compilerChecks/t10.kt
kotlin-native/backend.native/tests/compilerChecks/t10.kt:6:5: error: type kotlin.Function1<*, kotlin.Int>  of callback parameter 1 is not supported here: * as 1 parameter type
kotlin-native/backend.native/tests/compilerChecks/t11.kt
kotlin-native/backend.native/tests/compilerChecks/t11.kt:6:5: error: type kotlin.Function1<in kotlin.Int, kotlin.Int>  of callback parameter 1 is not supported here: in-variance of 1 parameter type
kotlin-native/backend.native/tests/compilerChecks/t12.kt
kotlin-native/backend.native/tests/compilerChecks/t12.kt:6:5: error: type kotlinx.cinterop.CValue<*>?  of callback parameter 1 is not supported here: must not be nullable
kotlin-native/backend.native/tests/compilerChecks/t13.kt
kotlin-native/backend.native/tests/compilerChecks/t13.kt:7:5: error: type kotlinx.cinterop.CValue<T of <root>.bar>  of callback parameter 1 is not supported here: must be parameterized with concrete class
kotlin-native/backend.native/tests/compilerChecks/t14.kt
kotlin-native/backend.native/tests/compilerChecks/t14.kt:8:5: error: type kotlinx.cinterop.CValue<<root>.Z>  of callback parameter 1 is not supported here: not a structure or too complex
kotlin-native/backend.native/tests/compilerChecks/t15.kt
kotlin-native/backend.native/tests/compilerChecks/t15.kt:4:26: error: type kotlin.Function0<kotlin.Unit>  is not supported here: not supported as variadic argument
kotlin-native/backend.native/tests/compilerChecks/t16.kt
kotlin-native/backend.native/tests/compilerChecks/t16.kt:6:26: error: type <root>.Z  is not supported here: doesn't correspond to any C type
kotlin-native/backend.native/tests/compilerChecks/t17.kt
kotlin-native/backend.native/tests/compilerChecks/t17.kt:6:15: error: super calls to Objective-C protocols are not allowed
kotlin-native/backend.native/tests/compilerChecks/t18.kt
kotlin-native/backend.native/tests/compilerChecks/t18.kt:6:19: error: super calls to Objective-C meta classes are not supported yet
kotlin-native/backend.native/tests/compilerChecks/t2.kt
kotlin-native/backend.native/tests/compilerChecks/t2.kt:5:5: error: 'handleFailureInFunction' overrides nothing
    override fun handleFailureInFunction(functionName: String, file: String, lineNumber: NSInteger /* = Long */, description: String?, vararg args: Any?) { }
    ^
kotlin-native/backend.native/tests/compilerChecks/t20.kt
kotlin-native/backend.native/tests/compilerChecks/t20.kt:4:1: error: only classes are supported as subtypes of Objective-C types
kotlin-native/backend.native/tests/compilerChecks/t21.kt
kotlin-native/backend.native/tests/compilerChecks/t21.kt:4:1: error: non-final Kotlin subclasses of Objective-C classes are not yet supported
kotlin-native/backend.native/tests/compilerChecks/t22.kt
kotlin-native/backend.native/tests/compilerChecks/t22.kt:4:1: error: fields are not supported for Companion of subclass of ObjC type
kotlin-native/backend.native/tests/compilerChecks/t23.kt
kotlin-native/backend.native/tests/compilerChecks/t23.kt:6:1: error: mixing Kotlin and Objective-C supertypes is not supported
kotlin-native/backend.native/tests/compilerChecks/t24.kt
kotlin-native/backend.native/tests/compilerChecks/t24.kt:4:1: error: only companion objects of subclasses of Objective-C classes can inherit from Objective-C metaclasses
kotlin-native/backend.native/tests/compilerChecks/t25.kt
kotlin-native/backend.native/tests/compilerChecks/t25.kt:5:14: error: can't override 'toString', override 'description' instead
kotlin-native/backend.native/tests/compilerChecks/t26.kt
kotlin-native/backend.native/tests/compilerChecks/t26.kt:7:9: error: @kotlinx.cinterop.ObjCAction method must not have extension receiver
kotlin-native/backend.native/tests/compilerChecks/t27.kt
kotlin-native/backend.native/tests/compilerChecks/t27.kt:7:13: error: unexpected @kotlinx.cinterop.ObjCAction method parameter type: kotlin.String
Only Objective-C object types are supported here
kotlin-native/backend.native/tests/compilerChecks/t28.kt
kotlin-native/backend.native/tests/compilerChecks/t28.kt:7:5: error: unexpected @kotlinx.cinterop.ObjCAction method return type: kotlin.Int
Only 'Unit' is supported here
kotlin-native/backend.native/tests/compilerChecks/t29.kt
kotlin-native/backend.native/tests/compilerChecks/t29.kt:6:5: error: @kotlinx.cinterop.ObjCOutlet property must be var
kotlin-native/backend.native/tests/compilerChecks/t3.kt
kotlin-native/backend.native/tests/compilerChecks/t3.kt:4:13: error: callable references to variadic C functions are not supported
kotlin-native/backend.native/tests/compilerChecks/t30.kt
kotlin-native/backend.native/tests/compilerChecks/t30.kt:7:9: error: @kotlinx.cinterop.ObjCOutlet must not have extension receiver
kotlin-native/backend.native/tests/compilerChecks/t31.kt
kotlin-native/backend.native/tests/compilerChecks/t31.kt:6:5: error: unexpected @kotlinx.cinterop.ObjCOutlet type: kotlin.String
Only Objective-C object types are supported here
kotlin-native/backend.native/tests/compilerChecks/t32.kt
kotlin-native/backend.native/tests/compilerChecks/t32.kt:7:5: error: constructor with @kotlinx.cinterop.ObjCObjectBase.OverrideInit doesn't override any super class constructor.
It must completely match by parameter names and types.
kotlin-native/backend.native/tests/compilerChecks/t33.kt
kotlin-native/backend.native/tests/compilerChecks/t33.kt:7:5: error: constructor with @kotlinx.cinterop.ObjCObjectBase.OverrideInit overrides initializer that is already overridden explicitly
kotlin-native/backend.native/tests/compilerChecks/t34.kt
kotlin-native/backend.native/tests/compilerChecks/t34.kt:7:5: error: only 0, 1 or 2 parameters are supported here
kotlin-native/backend.native/tests/compilerChecks/t35.kt
kotlin-native/backend.native/tests/compilerChecks/t35.kt:5:13: error: unable to call non-designated initializer as super constructor
kotlin-native/backend.native/tests/compilerChecks/t36.kt
kotlin-native/backend.native/tests/compilerChecks/t36.kt:4:13: error: native interop types constructors must not be called directly
kotlin-native/backend.native/tests/compilerChecks/t37.kt
kotlin-native/backend.native/tests/compilerChecks/t37.kt:5:5: error: subclasses of NativePointed cannot have properties with backing fields
kotlin-native/backend.native/tests/compilerChecks/t38.kt
kotlin-native/backend.native/tests/compilerChecks/t38.kt:5:5: error: subclasses of NativePointed cannot have properties with backing fields
kotlin-native/backend.native/tests/compilerChecks/t4.kt
kotlin-native/backend.native/tests/compilerChecks/t4.kt:4:21: error: callable references to variadic Objective-C methods are not supported
kotlin-native/backend.native/tests/compilerChecks/t40.kt
kotlin-native/backend.native/tests/compilerChecks/t40.kt:8:5: error: kotlinx.cinterop.staticCFunction must take an unbound, non-capturing function or lambda, but captures at:
    kotlin-native/backend.native/tests/compilerChecks/t40.kt:8:21
kotlin-native/backend.native/tests/compilerChecks/t41.kt
kotlin-native/backend.native/tests/compilerChecks/t41.kt:7:5: error: kotlinx.cinterop.staticCFunction must take an unbound, non-capturing function or lambda, but captures at:
    kotlin-native/backend.native/tests/compilerChecks/t41.kt:5:17: x
kotlin-native/backend.native/tests/compilerChecks/t42.kt
kotlin-native/backend.native/tests/compilerChecks/t42.kt:6:5: error: c function signature element mismatch: expected 'kotlin.Any', got 'kotlin.String'
kotlin-native/backend.native/tests/compilerChecks/t43.kt
kotlin-native/backend.native/tests/compilerChecks/t43.kt:3:21: error: receiver's type kotlin.Float is not an integer type
kotlin-native/backend.native/tests/compilerChecks/t44.kt
kotlin-native/backend.native/tests/compilerChecks/t44.kt:3:21: error: type argument kotlin.Float is not an integer type
kotlin-native/backend.native/tests/compilerChecks/t45.kt
kotlin-native/backend.native/tests/compilerChecks/t45.kt:3:21: error: unable to sign extend kotlin.Int to kotlin.Short
kotlin-native/backend.native/tests/compilerChecks/t46.kt
kotlin-native/backend.native/tests/compilerChecks/t46.kt:3:21: error: unable to narrow kotlin.Int to kotlin.Long
kotlin-native/backend.native/tests/compilerChecks/t47.kt
kotlin-native/backend.native/tests/compilerChecks/t47.kt:3:21: error: unable to convert kotlin.Int to kotlin.String
kotlin-native/backend.native/tests/compilerChecks/t48.kt
kotlin-native/backend.native/tests/compilerChecks/t48.kt:9:12: error: kotlin.native.concurrent.Worker.execute must take an unbound, non-capturing function or lambda, but captures at:
    kotlin-native/backend.native/tests/compilerChecks/t48.kt:9:50
kotlin-native/backend.native/tests/compilerChecks/t49.kt
kotlin-native/backend.native/tests/compilerChecks/t49.kt:5:12: error: kotlin.native.concurrent.Worker.execute must take an unbound, non-capturing function or lambda, but captures at:
    kotlin-native/backend.native/tests/compilerChecks/t49.kt:5:61: x
kotlin-native/backend.native/tests/compilerChecks/t5.kt
kotlin-native/backend.native/tests/compilerChecks/t5.kt:4:83: error: passing String as variadic Objective-C argument is ambiguous; cast it to NSString or pass with '.cstr' as C string
kotlin-native/backend.native/tests/compilerChecks/t50.kt
kotlin-native/backend.native/tests/compilerChecks/t50.kt:10:16: error: kotlin.native.concurrent.Worker.execute must take an unbound, non-capturing function or lambda, but captures at:
    kotlin-native/backend.native/tests/compilerChecks/t50.kt:10:54
kotlin-native/backend.native/tests/compilerChecks/t51.kt
  MISSING EXPECTED ERROR FOR kotlin-native/backend.native/tests/compilerChecks/t51.kt
kotlin-native/backend.native/tests/compilerChecks/t52.kt
kotlin-native/backend.native/tests/compilerChecks/t52.kt:5:5: warning: 'createCleaner(T, (T) -> Unit): Cleaner' is deprecated. Use kotlin.native.ref.createCleaner instead.
    createCleaner(42) { println(x) }
    ^
  MISSING EXPECTED ERROR FOR kotlin-native/backend.native/tests/compilerChecks/t52.kt
kotlin-native/backend.native/tests/compilerChecks/t53.kt
kotlin-native/backend.native/tests/compilerChecks/t53.kt:9:5: warning: 'createCleaner(T, (T) -> Unit): Cleaner' is deprecated. Use kotlin.native.ref.createCleaner instead.
    createCleaner(42, C(x)::bar)
    ^
  MISSING EXPECTED ERROR FOR kotlin-native/backend.native/tests/compilerChecks/t53.kt
kotlin-native/backend.native/tests/compilerChecks/t54.kt
kotlin-native/backend.native/tests/compilerChecks/t54.kt:1:44: error: no spread elements allowed here
kotlin-native/backend.native/tests/compilerChecks/t55.kt
kotlin-native/backend.native/tests/compilerChecks/t55.kt:1:37: error: all elements of binary blob must be constants
kotlin-native/backend.native/tests/compilerChecks/t56.kt
kotlin-native/backend.native/tests/compilerChecks/t56.kt:1:29: error: incorrect value for binary data: 1000
kotlin-native/backend.native/tests/compilerChecks/t57.kt
kotlin-native/backend.native/tests/compilerChecks/t57.kt:1:13: error: expected at least one element
kotlin-native/backend.native/tests/compilerChecks/t58.kt
kotlin-native/backend.native/tests/compilerChecks/t58.kt:5:5: error: non-reified type parameters with recursive bounds are not supported yet: TYPE_PARAMETER name:T index:0 variance: superTypes:[kotlin.Comparable<T of <root>.foo>] reified:false
kotlin-native/backend.native/tests/compilerChecks/t59.kt
kotlin-native/backend.native/tests/compilerChecks/t59.kt:5:5: error: non-reified type parameters with recursive bounds are not supported yet: TYPE_PARAMETER name:T index:0 variance: superTypes:[kotlin.Comparable<T of <root>.foo>] reified:false
kotlin-native/backend.native/tests/compilerChecks/t6.kt
kotlin-native/backend.native/tests/compilerChecks/t6.kt:4:97: error: when calling variadic Objective-C methods spread operator is supported only for *arrayOf(...)
kotlin-native/backend.native/tests/compilerChecks/t60.kt
kotlin-native/backend.native/tests/compilerChecks/t60.kt:4:5: error: subclasses of NativePointed cannot have properties with backing fields
kotlin-native/backend.native/tests/compilerChecks/t61.kt
kotlin-native/backend.native/tests/compilerChecks/t61.kt:8:5: error: only companion objects of subclasses of Objective-C classes can inherit from Objective-C metaclasses
kotlin-native/backend.native/tests/compilerChecks/t62.kt
kotlin-native/backend.native/tests/compilerChecks/t62.kt:7:1: error: only companion objects of subclasses of Objective-C classes can inherit from Objective-C metaclasses
kotlin-native/backend.native/tests/compilerChecks/t63.kt
  MISSING EXPECTED ERROR FOR kotlin-native/backend.native/tests/compilerChecks/t63.kt
kotlin-native/backend.native/tests/compilerChecks/t64.kt
  MISSING EXPECTED ERROR FOR kotlin-native/backend.native/tests/compilerChecks/t64.kt
kotlin-native/backend.native/tests/compilerChecks/t7.kt
kotlin-native/backend.native/tests/compilerChecks/t7.kt:4:41: error: when calling variadic C functions spread operator is supported only for *arrayOf(...)
kotlin-native/backend.native/tests/compilerChecks/t8.kt
kotlin-native/backend.native/tests/compilerChecks/t8.kt:6:5: error: type kotlin.Function0<*>  of callback parameter 1 is not supported here: * as return type
kotlin-native/backend.native/tests/compilerChecks/t9.kt
kotlin-native/backend.native/tests/compilerChecks/t9.kt:6:5: error: type kotlin.Function0<out kotlin.Int>  of callback parameter 1 is not supported here: out-variance of return type
```
