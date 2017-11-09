# Enums

Goals:
* Better syntax for passing constructor parameters when defining enum constants
* Resolve issues with annotation syntax for enum constants

## Example

Simple enum:
``` kotlin
enum class Foo {
    A
    B
    C {
        override fun foo() { ... }
    }
  
    open fun foo() {}
}
```

Enum with constructor:
``` kotlin
enum class Foo(val s: String) {
    A("a")
    B("b")
    C("c") {
        override fun foo() { ... }
    }
  
    open fun foo() {}
}
```

Issues
* Enum literals syntax clash with annotation syntax
    * Option 1.1: Forbid short annotation syntax in enums. **downside**: cannot annotate functions/properties/classes in this enum
    * Option 1.2: Add a separator between enum constants and members, and forbid short annotation syntax only on enum entries themselves. **downside**: separator is not intuitive, hard to think of when doing this for the first time (the error message will be rather clear and instructive, though)
    * Option 1.3: prefix each entry with a soft-keyword, e.g. `entry`. **downside**: verbosity
    * Option 1.4 **chosen**: Add a semicolon separator after the last enum constant, **and** a comma separator between different enum constants.
* How do we specify other supertypes for a constant (if any)
    * Option 2.1 **chosen**: Leave unsupported, use cases are very few, and Java does not support it
    * Option 2.2: `A("s"): OtherType`

Example for option 1.4:

``` kotlin
enum class Foo(val s: String) {
    A("a"), // semicolon CAN NOT be used here!
    B("b"), // comma is MANDATORY after each enum constant except the last one
    C("c") {
        override fun foo() { ... }
    }; // semicolon is MANDATORY here, even if no member follows
  
    open fun foo() {}
}
```

Notes:
* Almost no overhead in the most common case of no members at all: `enum class E {A, B, C; }
* Clear error message: if the parser sees no semicolon after the last constant:
    * it reports an error saying "There must be a semicolon after the last enum entry", which is rather instructive
    * a quick fix can even guess the right position for the semicolon most of the time
* Today, there's no way of naming an enum entry `public` (or any other soft-keyword used as a modifier), which is unfortunate
