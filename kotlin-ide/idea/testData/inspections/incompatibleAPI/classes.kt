package problem.api.kotlin.classes

import lib.LibClass

fun ktTest() {
    LibClass()
}

class KtUsage : LibClass()

fun ktTestSuppress() {
    @Suppress("IncompatibleAPI")
    LibClass()
}

@Suppress("IncompatibleAPI")
class KtUsageSuppress : LibClass()