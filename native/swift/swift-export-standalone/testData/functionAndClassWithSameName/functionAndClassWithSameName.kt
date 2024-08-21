// KIND: STANDALONE
// MODULE: main(dep,flattened)
// FILE: main.kt
// https://youtrack.jetbrains.com/issue/KT-70063
class UtcOffset

fun UtcOffset(x: Int): UtcOffset = UtcOffset()

class ClassWithFactoryWithoutParameters(val value: Int)
fun ClassWithFactoryWithoutParameters() = ClassWithFactoryWithoutParameters(123)

// FILE: with_package.kt
package test.factory

class ClassWithFactoryInAPackage
fun ClassWithFactoryInAPackage(arg: Any) = ClassWithFactoryInAPackage()

class Outer {
    class Nested

    // The function is not static, so it is in a different scope => no mangling is performed.
    fun Nested(x: Any) = Nested()

    // The function is a member, the class is top-level => no mangling is performed.
    fun ClassWithFactoryInAPackage(arg: Any) = ClassWithFactoryInAPackage()
}

// The class is not top-level => no mangling is performed.
fun Nested() = Outer.Nested()

// FILE: with_different_package.kt
package test.not.factory

import test.factory.ClassWithFactoryInAPackage

// Located in different package => no mangling is performed.
fun ClassWithFactoryInAPackage(arg: Any) = ClassWithFactoryInAPackage()

// FILE: factory_for_class_from_dependency.kt
package test.factory.modules

fun ClassFromDependency(arg: Any) = ClassFromDependency()

// FILE: no_clash_with_flattened_package.kt
import flattenedPackage.FlattenedPackageClass

// This function is not mangled with the current implementation, but apparently swiftc is fine with that,
// and doesn't consider it clashing with `typealias FlattenedPackageClass` in `flattened` module.
fun FlattenedPackageClass(f: Float) = FlattenedPackageClass()

// FILE: object.kt
object ObjectWithFactory
fun ObjectWithFactory() = ObjectWithFactory

// FILE: typealias.kt
package typealiases

typealias TypealiasWithFactoryWithoutParameters = ClassWithFactoryWithoutParameters
fun TypealiasWithFactoryWithoutParameters(): TypealiasWithFactoryWithoutParameters =
    TypealiasWithFactoryWithoutParameters(123)

typealias TypealiasWithFactoryWithoutParameters2 = ClassWithFactoryWithoutParameters

// This won't work, because the implementation checks that return type and function FQ name match.
// TODO KT-70758
//fun TypealiasWithFactoryWithoutParameters2(): ClassWithFactoryWithoutParameters =
//    TypealiasWithFactoryWithoutParameters2(321)

// FILE: unsupported.kt
// These cases should work fine once the declarations get supported.
interface InterfaceWithFactory
fun InterfaceWithFactory(): InterfaceWithFactory = TODO()
fun InterfaceWithFactory(arg: Any): InterfaceWithFactory = TODO()

enum class EnumWithFactory {
    ONE
}
fun EnumWithFactory(x: Int): EnumWithFactory = EnumWithFactory.ONE

annotation class AnnotationWithFactory
fun AnnotationWithFactory(arg: Any) = AnnotationWithFactory()

// MODULE: dep
// EXPORT_TO_SWIFT
// FILE: dep.kt
package test.factory.modules

class ClassFromDependency

// MODULE: flattened
// SWIFT_EXPORT_CONFIG: packageRoot=flattenedPackage
// FILE: flattenedPackage.kt
package flattenedPackage

class FlattenedPackageClass()
fun FlattenedPackageClass(i: Int) = FlattenedPackageClass()

// FILE: root.kt
import flattenedPackage.FlattenedPackageClass

// This doesn't work:
//  the implementation doesn't mangle this function since it is located in a different package,
//  but it ends up being top-level, just as the package-flattening "trampoline" for the class,
//  so they clash in Swift.
// TODO KT-70758
//fun FlattenedPackageClass(d: Double) = FlattenedPackageClass()