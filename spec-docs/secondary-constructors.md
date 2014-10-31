# Secondary constructors

## Goal

Compatibility with Java hierarchies that demand multiple constructors, such as
- Android Views
- Swing dialogs

## Examples

With a primary constructor:
``` kotlin
class Foo(a: Bar): MySuper() {
  // when there's a primary constructor, (direct or indirect) delegation to it is required
  constructor() : this(Bar()) { ... } // can't call super() here
  constructor(s: String) : this() { ... }
}
```

No primary constructor:
``` kotlin
class Foo: MySuper { // initialization of superclass is not allowed
  constructor(a: Int): super(a + 1) { ... } // must call super() here
}
```

No primary constructor + two overloaded constructors
``` kotlin
class Foo: MySuper { // initialization of superclass is not allowed
  constructor(a: Int): super(a + 1) { ... } 
  constructor() : this(1) { ... } // either super() or delegate to another constructor
}
```

## TODO

- [ ] is delegation allowed when no primary constructor is present?
- [ ] Allow omitting parameterless delegating calls?

## Syntax for primary constructor

- There's a primary constructor if
  - parentheses after class name, or
  - there're no secondary constructors (default primary constructor)
- No parentheses after name and an explicit constructor present => no primary constructor

No primary constructor => no supertype initialization allowed in the class header:
``` kotlin
class Foo : Bar() { // Error
  constructor(x: Int): this() {}
}
```

When a primary constructor is present, explicit constructors are called *secondary*.

Every class **must** have a constructor. the following is an error:
``` kotlin
class Parent
class Child : Parent { }
```
The error is: "superclass must be initialized". This class has a primary constructor, but does not initialize its superclass in teh class header.
## Syntax for explicit constructors

```
constructor
  : modifiers "constructor" valueParameters (":" constructorDelegationCall) block
  ;
  
constructorDelegationCall
  : "this" valueArguments
  | "super" valueArguments
  ;
```

Passing lambdas outside parentheses is not allowed in `constructorDelegationCall`.

## Rules for delegating calls

The only situation when an explicit constructor may not have an explicit delegating call is
- when there's no primary constructor **and** teh superclass has a constructor that can be called with no parameters passed to it

``` kotlin
class Parent {}
class Child: Parent {
  constructor() { ... } // implicitly calls `super()`
}
```

If there's a primary constructor, all explicit constructors must have explicit delegating calls that (directly or indirectly) call the primary constructor.

``` kotlin
class Parent {}
class Child(): Parent() {
  constructor(a: Int) : this() { ... }
}
```

## Initialization code outside constructors

The primary constructor's body consists of
- super class intialization from class header
- assignments to properties from constructor parameters declared with `val` or `var`
- property initializers and bodies of anonymous initializers following in the order of appearence in the class body

If the primary constructor is not present, property initializers and anonymous initializers are conceptually "prepended" to the body 
of each explicit constructor that has a delegating call to super class, and their contents are checked accordingly for definite
initialization of properties etc.

## Syntax for anonymous initializers

Anonymous initializer in the class body must be prefixed with the `init` keyword, without parentheses:

``` kotlin
class C {
  init {
    ... // anonymous initializer
  }
}
```

## Checks for constructors

All constructors must be checked for
- absence of circular delegation
- overload compatibility
- definite initialization of all properties that must be initialized
- absence of non-empty super call for enum constructors

No secondary constructors can be declared for
- traits
- objects (named, anonymous, and default)
- bodies of enum literals

Data classes should have a primary constructor
