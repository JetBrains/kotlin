// IGNORE_BACKEND_K2: ANY
// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM

// MODULE: lib-common
package foo

expect annotation class MyAnnotation

// MODULE: lib-jvm()()(lib-common)
package foo

annotation class RealState

actual typealias MyAnnotation = RealState

// MODULE: main(lib-jvm)()()
package foo

@<!COMPILER_REQUIRED_ANNOTATION_AMBIGUITY!>MyAnnotation<!>
object CommonBenchmark

fun box() = "OK"
