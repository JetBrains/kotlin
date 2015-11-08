package some

import bad.prefix.KotlinTestInBadPrefix
import good.prefix.KotlinTestInGoodPrefix
import good.prefix.JavaTest;
import test.JavaRef

val goodTest = KotlinTestInGoodPrefix()
val badTest = KotlinTestInBadPrefix()
val javaTest = JavaTest().bar()
val javaRef = JavaRef().foo(null)


