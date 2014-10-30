# Java Beans as Kotlin Properties

## Goal

Make Java get/set pairs accessible as properties in Kotlin

## Examples

``` kotlin
// simple getter
A getA()
->
val a: A
```
  
``` kotlin
// getter and setter
A getA()
void setA(A)
->
var a: A
```
  
``` kotlin
// only setter (write-only)
void setA(A)
->
var a: A
  // no get
  set(v) {...}
```
  
``` kotlin
// private getter (effectively write-only)  
private A getA()
public void setA(A)
->
var a: A
  private get() = ...
  public set(v) = ...
```
  
``` kotlin
// Different setter type  
A getA()
void setA(B)
->
var a: A
  get() = ...
  set(v: B) = ...
```
  
``` kotlin
// Overloaded setters
A getA()
void setA(B)
void setA(C)
->
var a: A
  get() = ...
  set(v: B) = ...
  set(v: C) = ...
```

## TODO

- field access (may clash with property)
- static field access (critical, syntax can't be changed)
- How much old code does this break?
  - usages of Java APIs with getters/setters
  - inheritors of Java classes overriding getters/setters

## Changes in Kotlin properties

- allow many setters (overloaded)
- allow no getter (no backing field + no explicit getter => no getter at all)
- allow different modifiers on getters and properties
  - property modifiers are lub(get, set)
  - property is no less visible than its accessors
  - property is no "less final" than its accessors (`final` > `open` > `abstract`)

## Changes in JavaDescriptorResolver

**Definition**. Getter of name N is // TODO: recognize "is" prefix?

**Definition**. Setter of name N is // TODO

- All getters and setters of the same name are merged into (replaced by) a single property.
- Its visibility/modality is computed as lub of all getters' and setters' visibilities/modalities.
- (MAYBE) the respective functions (getters/setters) are also created as synthesized members (to enable calling them from Kotlin the Java way)
  - see difficult cases below (two abstracts in J1 and J2)

## Difficult cases of inheritance

Two abstracts of incompatible shapes:
```
class J {
  abstract void setA(A)
}

class K {
  abstract fun setA(A)
}

class K1: J, K {
  // can't override both var J.a and fun K.setA
  // but the same problem exists in pure Kotlin code
  
  // but if we create a concrete synthetic setA() in J it will override K.setA()
  // but what if there are two setA()'s coming from J1 nad J2?
}

// Let's say we extend J and K with J1:

class J1 extend J, K {
  @Override
  void setA(A)
}

// and then extend it in K1

class K1 : J1 {
  // we can override either val a or fun setA(), but not both, which is fine
}

// If setA() were abstract in J1, we shouldn't be able to override only `var`, because it breaks the substitution principle
```

Abstract is J and concrete fun in K
```
class J {
  abstract void setA(A)
}

class K {
  fun setA(A) {}
}

class K1: J, K {
  override var a: A ...
}
```


Abstract is J and val in K is OK.

Abstract in J1 and J2:
```
class J1 {
  abstract void setA(A)
}

class J2 {
  abstract void setA(A)
}

class K : J1, J2 {
  // if there are two final setA(A) synthesized for supertypes, it's a conflict :(
}
```
