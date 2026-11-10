/*
 * hibernate-jpamodelgen scenario (JPA static metamodel): the metamodel classes `MyEntity_` and
 * `SecondEntity_` are not in the source set — the annotation processor itself generates them
 * from the stubs of this very compilation. The `mappedBy` argument therefore can be neither
 * resolved nor const-evaluated during stub generation, and the stub must render the original
 * argument text back from FIR. The same unresolved reference inside a function body never
 * reaches the stub, as bodies are skipped in the stub generation mode.
 *
 * String-constant flavor of KT-82338; see kt82338.kt for the enum flavor.
 */

// FILE: Main.kt

package test

@Target(AnnotationTarget.FIELD)
annotation class Related(val mappedBy: String = "")

class MyEntity {
    @Related(mappedBy = SecondEntity_.FIRST)
    val second: MutableList<SecondEntity> = mutableListOf()
}

class SecondEntity(val first: MyEntity? = null)

fun main() {
    println(MyEntity_.SECOND)
}
