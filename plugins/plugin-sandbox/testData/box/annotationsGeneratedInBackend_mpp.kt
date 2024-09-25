// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// WITH_STDLIB

// MODULE: common
package test

import org.jetbrains.kotlin.fir.plugin.AddAnnotations
import kotlin.reflect.KClass

@AddAnnotations
class Some {
    fun foo() {}
}

// MODULE: platform()()(common)
package test

import org.jetbrains.kotlin.fir.plugin.AddAnnotations
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredFunctions

@AddAnnotations
class Other {
    fun foo() {}
}

fun checkClass(klass: KClass<*>): Boolean {
    val foo = klass.declaredFunctions.first { it.name == "foo" }
    return foo.annotations.any { it.annotationClass.simpleName == "AnnotationToAdd" }
}

fun box(): String {
    if (!checkClass(Some::class)) return "Fail: Some"
    if (!checkClass(Other::class)) return "Fail: Other"
    return "OK"
}
