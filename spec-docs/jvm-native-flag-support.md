# Support for JVM's ACC_NATIVE flag

Goal: enable JNI interop for Kotlin, so that anything that works through JNI in Java could be reproduced 1-to-1 in Kotlin

## Syntax

The following definition is present in the library:

``` kotlin
package kotlin.jvm

Retention(RetentionPolicy.SOURCE)
public annotation class native
```

This annotation is applicable to
 - functions
 - property accessors

A declaration marked with this annotation is referred to as a *native declaration*.

Checks to perform:
 - a native declaration can not be abstract
 - a native declaration can not have a body
 - a native declaration can not be marked `inline`
 - a native declaration can not have `reified` type parameters
   NOTE: this is achieved through prohibiting `inline`, as `reified` is only allowed on inline-functions now
 - members of traits can not be native

NOTE: native members can override open (or abstract) members of supertypes

## Semantics on the JVM

Intuition: a JVM method whose source declaration is native is marked with the `ACC_NATIVE` flag, and has no `CODE` attribute.

### Interaction with \[platformStatic\]

A native member of an object marked `native` and `platformStatic` is translated in a straightforward way:
there is only one JVM method corresponding to it, and it is marked as `ACC_NATIVE`.

A member of a default object of class `C` marked `native` and `platformStatic` yields two JVM methods:
 - static member of `C` that is marked `ACC_NATIVE`;
 - instance member of `C$object` that is not marked `ACC_NATIVE` and its body delegates to the native static method.

### Top-level declarations

A native member of package `p` yields one JVM method:
 - member of a package-facade class `PPackage` which is marked with `ACC_NATIVE` flag.

### Native Property Accessors

A property can not be marked `native`.
A *property accessor* can be marked `native` if it has no body (i.e. it is a *default accessor*).
In this case the generated code is the same as for a native function defined in the same context as the property.

Example:
``` kotlin
val foo: Int
  [native] get
```

## Not implemented (yet)

- native property accessors
 - frontend: when accessors are default and native, don't require an initializer, don't allow a backing field
 - backend: when accessors are default and native, don't generate a backing field
- applicability checks: only functions and property accessors