# Annotations

Goals:
* Check targets of annotations
* Allow annotating fields
* Sort out problems of positional parameters and varargs in annotations
* Provide clear semantics for annotating derived elements (such as property accessors, `$default`-functions etc)
* Use Kotlin class literals in annotations

## TODO

* [ ] Naming conventions
* [ ] How to annotate anonymous classes generated for `object`-expressions?

## Declaration-Site Syntax 

``` kotlin
annotation class Example(val foo: String)
```

## Use-Site Syntax

``` kotlin
[Example] fun foo() {}
Example fun foo() {}
```

## Annotation Targets

Possible annotation targets in Kotlin are:
* PACKAGE
* CLASSIFIER (class, trait, object, annotation class, enum class, type parameter)
* CONSTRUCTOR
* FUNCTION
* PROPERTY (val, var)
* PROPERTY_ACCESSOR (getter, setter)
* TYPE (type usage)
* (?) TYPE_ARGUMENT (including `*`)
* VARIABLE (parameter, local variable)
* EXPRESSION (maybe have lambdas separately?)

Possible modifiers:
* LOCALS_ALLOWED
* LOCALCS_REQUIRED
 
Or have LOCAL_VARIABLE separately

Platform-specific targets
* SINGLETON_FIELD for objects
* PROPERTY_FIELD
* (?) DEFAULT_FUNCTION
* (?) LAMBDA_METHOD
* PACKAGE_FACADE
* PACKAGE_PART

> NOTE: Java has the following [targets](https://docs.oracle.com/javase/8/docs/api/java/lang/annotation/ElementType.html):
* ANNOTATION_TYPE - Annotation type declaration
* CONSTRUCTOR - Constructor declaration
* FIELD - Field declaration (includes enum constants)
* LOCAL_VARIABLE - Local variable declaration
* METHOD - Method declaration
* PACKAGE - Package declaration
* PARAMETER - Formal parameter declaration
* TYPE - Class, interface (including annotation type), or enum declaration
* TYPE_PARAMETER - Type parameter declaration
* TYPE_USE - Use of a type
