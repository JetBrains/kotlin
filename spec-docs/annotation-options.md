# Annotation Options

Goals:
* Support annotation options, such as retention policy and trageting

TODO
* Should we support `@Inherited`?
* Should we support `@Documented`?

## Examples

Option 0:

``` kotlin
import kotlin.AnnotationRetention.*
import kotlin.AnnotationTarget.*

@retention(SOURCE)
@targets(CLASSIFIER, FIELD)
annotation class example
```

Option 1:

``` kotlin
annotation(
    retention = SOURCE, 
    targets = array(CLASSIFIER, FIELD)
) 
class example
```

``` kotlin
package kotlin

annotation(retention = SOURCE, targets = array(CLASSIFIER)) class annotation(
    val targets: Array<AnnotationTarget>,
    val retention: AnnotationRetention
)
```

## Discussion

Having option as separate annotation is what Java has and seems more extensible, although it actually isn't (adding new parameters to one annotation is no better or worse than adding new annotation recognized by the compiler).

Having those as parameters is more discoverable, but has some syntactic shortcomings: no varargs can be used.
