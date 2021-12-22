@file:FileAnnotation
package b

import a.A
import a.FileAnnotation
import a.ClassAnnotation

@ClassAnnotation
class B

fun b(param: a.A) {
    a.a()
}
