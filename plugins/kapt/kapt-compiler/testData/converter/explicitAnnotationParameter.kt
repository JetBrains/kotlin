// CORRECT_ERROR_TYPES
// WITH_REFLECT

import kotlin.reflect.KClass

annotation class FooAnnotation(vararg val value: KClass<*>)

// To re-produce, both `TypeA` and `TypeB`have to
// be generated during annotation processing, unknown types during the java stubbing stage.
// Alternatively, it is possible to run KAPT in STUBS only mode.
interface FooAnnotationUser {
    // OK: class literals passed through the Implicit value parameter.
    @FooAnnotation(TypeA::class) fun usageA()

    // OK: class literals passed through the Implicit value parameter.
    @FooAnnotation(TypeA::class, TypeB::class) fun usageAB()

    // Error: class literals passed through the explicit value parameter.
    @FooAnnotation(value = [TypeA::class]) fun usageWithExplicitValueParamA()

    // Error: class literals passed through the explicit value parameter.
    @FooAnnotation(value = [TypeA::class, TypeB::class]) fun usageWithExplicitValueParamAB()
}