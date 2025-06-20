package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.Test
import kotlin.test.assertEquals

class AllKotlinSourceSetsTest {

    @Test
    fun `allKotlinSourceSets - contains dependsOn closure but not source sets from associated compilations`() {
        val project = buildProjectWithMPP {
            kotlin {
                val second = jvm().compilations.create("second")
                val first = jvm().compilations.create("first")
                first.associateWith(second)
                val a = sourceSets.create("a")
                val b = sourceSets.create("b")
                val c = sourceSets.create("c")
                second.kotlinSourceSets.single().dependsOn(a)
                first.kotlinSourceSets.single().dependsOn(b)
                a.dependsOn(c)
            }
        }.evaluate()

        val first = project.multiplatformExtension.jvm().compilations.getByName("first")
        val second = project.multiplatformExtension.jvm().compilations.getByName("second")

        assertEquals(
            setOf(
                first.kotlinSourceSets.single(),
                project.multiplatformExtension.sourceSets.getByName("b"),
            ),
            first.allKotlinSourceSets,
        )
        assertEquals(
            setOf(
                second.kotlinSourceSets.single(),
                project.multiplatformExtension.sourceSets.getByName("a"),
                project.multiplatformExtension.sourceSets.getByName("c"),
            ),
            second.allKotlinSourceSets,
        )
    }
}