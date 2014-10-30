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
  
```
// getter and setter
A getA()
void setA(A)
->
var a: A
```
  
```
// only setter (write-only)
void setA(A)
->
var a: A
  // no get
  set(v) {...}
```
  
```
// private getter (effectively write-only)  
private A getA()
public void setA(A)
->
var a: A
  private get() = ...
  public set(v) = ...
```
  
```
// Different setter type  
A getA()
void setA(B)
->
var a: A
  get() = ...
  set(v: B) = ...
```
  
```
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
