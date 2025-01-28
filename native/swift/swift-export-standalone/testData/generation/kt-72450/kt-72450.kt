// KIND: STANDALONE
// MODULE: common(modulea)
// SWIFT_EXPORT_CONFIG: packageRoot=namespace
// FILE: main.kt
package namespace.demo

import namespace.modulea.ClassFromA

fun useClassFromA(): ClassFromA = TODO()


// MODULE: modulea()
// SWIFT_EXPORT_CONFIG: packageRoot=namespace.modulea
// FILE: moduleA.kt
package namespace.modulea

class ClassFromA