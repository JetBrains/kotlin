# Annotations

Goals:
* Check targets of annotations
* Allow annotating fields
* Sort out problems of positional parameters and varargs in annotations
* Provide clear semantics for annotating derived elements (such as property accessors, `$default`-functions etc)
* Use Kotlin class literals in annotations

## TODO

* [ ] Naming conventions

## Declaration-Site Syntax 

``` kotlin
annotation class Example(val foo: String)
```

## Use-Site Syntax

``` kotlin
[Example] fun foo() {}
Example fun foo() {}
```
