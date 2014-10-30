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
