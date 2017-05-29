# Annotation Targets

Goals:
* Check targets of annotations
* Allow annotating fields
* Provide clear semantics for annotating derived elements (such as property accessors, `$default`-functions etc)

## TODO

* [ ] Naming conventions
* [ ] How to annotate anonymous classes generated for `object`-expressions?



## Allowed Targets

To check applicability, we can use the following constants:

| Kotlin constant | Java constant | 
|-----------------|---------------|
| PACKAGE | \<same> |
| CLASSIFIER | TYPE |
| ANNOTATION_CLASS | ANNOTATION_TYPE |
| TYPE_PARAMETER | \<same>
| PROPERTY | \<no analog> |
| FIELD | \<same>
| LOCAL_VARIABLE | \<same> |
| VALUE_PARAMETER | PARAMETER |
| CONSTRUCTOR | \<same> |
| FUNCITON | METHOD |
| PROPERTY_GETTER | METHOD |
| PROPERTY_SETTER | METHOD |
| TYPE | TYPE_USE |
| EXPRESSION | \<no analog> |

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
