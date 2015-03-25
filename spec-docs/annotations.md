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

To check applicability, we can use the following constants:

------
| Kotlin constant | Java constant | 
------
| ANNOTATION_CLASS | ANNOTATION_TYPE |
| CONSTRUCTOR | <same> |
| FIELD | <same>
| LOCAL_VARIABLE | <same> |
| FUNCITON | METHOD |
| PROPERTY_GETTER | METHOD |
| PROPERTY_SETTER | METHOD |
| PACKAGE | <same> |
| VALUE_PARAMETER | PARAMETER |
| CLASSIFIER | TYPE |
| TYPE_PARAMETER | <same>
| TYPE | TYPE_USE |
| PROPERTY | <no analog> |
| EXPRESSION | <no analog> |

**TODO** Open question: what about traits/classes/objects?  
**TODO** local variables are just like properties, but local  

> Consider: we could have modifiers (parameters) to the `Target` annotation, that regulate applicability, e.g.: LOCALS_ALLOWED/REQUIRED
> TYPE_ARGUMENTS_ALLOWED/REQUIRED, TRAIT_FORBIDDEN/REQUIRED 
 
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
