# ObjC Export mangling
After stubs are generated we mangle classifiers and members at [mangleObjCStubs](mangleObjCStubs.kt)

- [Protocols and interfaces](#protocols-and-interfaces)
- [Properties](#properties)
- [Methods](#methods)
- [Generics](#generics)
- [Extensions](#extensions)
## Protocols and interfaces
Since we merge all interfaces and protocols into single header we needs to rename interfaces and classes with similar names from different packages. See implementation at [mangleObjCInterface](mangleObjCInterface.kt) and [mangleObjCProtocol](mangleObjCProtocol.kt) 
```kotlin
package a
class Foo

package b
class Foo
```
```c
@interface Foo
@interface Foo_
```
## Properties
There is only one case when property needs to be mangled: when classifier has method with the same name. See implementation at [mangleObjCProperties](mangleObjCProperties.kt)
```kotlin
interface Foo {
    fun bar()
    val bar: Int
    val uniqProp: Int
}
```
```c
@protocol
    (void) bar
    @property (readonly, getter=bar_) bar
    @property (readonly) uniqProp
@end
```
## Methods
ObjC method has multiple selectors, parameters and `swift_name` attribute. Depending on amount of parameters we mangle methods differently. See implementation at [mangleObjCMethods](mangleObjCMethods.kt)
### 1 parameter: combine method name with parameter add `_`
```kotlin
class Foo {
    fun bar(value: Int)
    fun bar(value: String)
    fun bar(value: Boolean)
}
```
```c
@interface Foo
    (void) barValue:(Int) value __attribute__((swift_name("bar(value:)")));
    (void) barValue_:(String) value __attribute__((swift_name("bar(value_:)")));
    (void) barValue__:(Boolean) value __attribute__((swift_name("bar(value__:)")));
@end
```
### More than 1 parameter: add `_` to the last parameter
```kotlin
class Foo {
    fun bar(value1: Int, value2: String, value3: Boolean)
    fun bar(value1: Boolean, value2: Int, value3: String)
    fun bar(value1: String, value2: Boolean, value3: Int)
}
```
```c
@interface Foo
    (void) barValue:(Int) value1 (String) value2 (Boolean) value3 __attribute__((swift_name("bar(value1:value2:value3:)")));
    (void) barValue:(Boolean) value1 (Int) value2 (String) value3_ __attribute__((swift_name("bar(value1:value2:value3_:)")));
    (void) barValue:(String) value1 (Boolean) value2 (Int) value3__ __attribute__((swift_name("bar(value1:value2:value3__:)")));
@end
```

## Generics

Two groups of type parameters needs to be mangled:

- Predefined Kotlin types: `MutableSet`, `MutableMap`, `Base`
- Predefined Objective-C types: `id`, `NSObject`, `int16_t` and others. See [isReservedTypeParameterName.kt](isReservedTypeParameterName.kt)

```kotlin
class Foo<Base>
```

```chatinput
@interface Foo<Base_>
@end
```

## Extensions

Unique extension function/property signature is guaranteed by kotlin compiler, so extensions don't need to be mangled, but K1 does mangling.
And since CLI uses K1 we need to be aligned with K1 extensions mangling.

- K1 implements global extensions mangling, so method or property must has unique signature across all extensions, even though they split
  into different facades.
- K1 also implements mangling differently compared to mangling of regular properties and functions:
    - properties are not mangled with additional attribute, but by adding `_` to property name and swift_name attribute
    - functions with no parameters mangled unlike regular functions

### No parameters functions

```kotlin
class Foo
class Bar

fun Foo.funcName() = Unit
fun Bar.funcName() = Unit
```

```c
@interface Foo
- funcName __attribute__((swift_name("funcName()")));
@end
@interface Bar
- funcName_ __attribute__((swift_name("funcName_()")));
@end
```

### Properties

```kotlin
class Foo
class Bar

val Foo.prop: Int = 42
val Bar.prop: Int = 42
```

```c
@interface Foo
@property prop int __attribute__((swift_name("prop")));
@end
@interface Bar
@property prop_ int __attribute__((swift_name("prop_")));
@end
```