# ObjC Export mangling
After stubs are generated we mangle classifiers and members at [mangleObjCStubs](mangleObjCStubs.kt)
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