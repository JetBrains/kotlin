package pkg

import dep.*

class DependencyKotlinClass {
    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    val mode: Dependency = Dependency.GOO
}
