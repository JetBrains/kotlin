# Annotation Options

Goals:
* Support annotation options, such as retention policy and trageting

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

Option 
