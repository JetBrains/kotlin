These tests aren't invoked automatically so far because there is no mechanism of checking compilation errors yet.
To run tests manually, 
- build platformLibs: `./gradlew :kotlin-native:platformLibs:macos_arm64Install` or `./gradlew :kotlin-native:platformLibs:macos_x64Install`
- run `runtests.sh` as described below. 

Known issues: missing expected error for tests:
K1: `kotlin-native/backend.native/tests/compilerChecks/runtests.sh -language-version 1.9`
- t51.kt: missing error
- t63.kt: missing error
- t64.kt: missing error

K2: `kotlin-native/backend.native/tests/compilerChecks/runtests.sh -language-version 2.0`
- t3.kt: missing `error: callable references to variadic C functions are not supported`
- t18.kt: missing `error: super calls to Objective-C meta classes are not supported yet`
- t20.kt: missing `error: only classes are supported as subtypes of Objective-C types`
- t21.kt: missing `error: non-final Kotlin subclasses of Objective-C classes are not yet supported`
- t22.kt: missing `error: fields are not supported for Companion of subclass of ObjC type`
- t23.kt: missing `error: mixing Kotlin and Objective-C supertypes is not supported`
- t24.kt: missing `error: only companion objects of subclasses of Objective-C classes can inherit from Objective-C metaclasses`
- t25.kt: missing `error: can't override 'toString', override 'description' instead`
- t26.kt: missing `error: @ObjCAction method must not have extension receiver`
- t27.kt: missing `error: unexpected @ObjCAction method parameter type: String`
- t28.kt: missing `error: unexpected @ObjCAction method return type: Int`
- t29.kt: missing `error: @ObjCOutlet property must be var`
- t30.kt: missing `error: @ObjCOutlet must not have extension receiver`
- t31.kt: missing `error: unexpected @ObjCOutlet type: String`
- t32.kt: missing `error: constructor with @OverrideInit doesn't override any super class constructor. \nIt must completely match by parameter names and types.`
- t33.kt: missing `error: constructor with @OverrideInit overrides initializer that is already overridden explicitly`
- t34.kt: missing `error: only 0, 1 or 2 parameters are supported here`
- t35.kt: missing `error: unable to call non-designated initializer as super constructor`
- t36.kt: missing `error: native interop types constructors must not be called directly` 
          with literal `42`:  wrong `error: symbol @GCUnsafeCall(...) fun malloc(size: Long, align: Int): NativePtr is invisible`, since "KT-56583 K1: Implement opt-in for integer cinterop conversions" is not implemented in K2
          with literal `42u`: wrong `error: type kotlinx.cinterop.CPointer<out kotlinx.cinterop.CPointed>?  of return value is not supported here: doesn't correspond to any C type`
- t37.kt: missing `error: subclasses of NativePointed cannot have properties with backing fields`
- t38.kt: missing `error: subclasses of NativePointed cannot have properties with backing fields`
- t51.kt: missing error. K1 has no error as well
- t54.kt: missing `error: no spread elements allowed here`
- t55.kt: missing `error: all elements of binary blob must be constants`
- t56.kt: missing `error: incorrect value for binary data: 1000`
- t57.kt: missing `error: expected at least one element`
- t60.kt: missing `error: subclasses of NativePointed cannot have properties with backing fields`
- t61.kt: missing `error: only companion objects of subclasses of Objective-C classes can inherit from Objective-C metaclasses`
- t62.kt: missing `error: only companion objects of subclasses of Objective-C classes can inherit from Objective-C metaclasses` - t25.kt: missing `error: can't override 'toString', override 'description' instead`
- t63.kt: missing error. K1 has no error as well
- t64.kt: missing error. K1 has no error as well
