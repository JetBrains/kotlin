/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInAnalysisApi
import kotlin.test.*

@MustRunOnChangesInAnalysisApi
internal class OverriddenMembersTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
            }
        }
    }

    @Test
    fun `generic diamond overrides retain direct declarations and inherited relationships`() {
        testInline(
            """
            |/src/Test.kt
            |package sample
            |interface Root<T> {
            |    fun foo(value: T): T
            |    val bar: T
            |}
            |interface Left<T> : Root<T> {
            |    override fun foo(value: T): T
            |    override val bar: T
            |}
            |interface Right<T> : Root<T> {
            |    override fun foo(value: T): T
            |    override val bar: T
            |}
            |open class Derived : Left<String>, Right<String> {
            |    override fun foo(value: String): String = value
            |    override var bar: String = ""
            |        protected set
            |    fun unrelated() {}
            |}
            |class Inherited : Derived()
            """.trimMargin(),
            configuration,
        ) {
            documentablesCreationStage = { modules ->
                val derived = modules.flatMap { it.packages }.flatMap { it.classlikes }.single { it.name == "Derived" }
                val property = derived.properties.single { it.name == "bar" }
                assertNull(assertNotNull(property.setter).extra[OverriddenMembers])
            }
            documentablesMergingStage = { module ->
                val classes = module.packages.single().classlikes.associateBy { it.name }
                val root = classes.getValue("Root")
                val left = classes.getValue("Left")
                val right = classes.getValue("Right")
                val derived = classes.getValue("Derived")
                val inherited = classes.getValue("Inherited")
                val sourceSet = derived.sourceSets.single().sourceSetID.sourceSetName
                for (name in listOf("foo", "bar")) {
                    assertTrue(root.member(name).overridens(sourceSet).isEmpty())
                    assertEquals(listOf(root.member(name).dri), left.member(name).overridens(sourceSet))
                    assertEquals(listOf(root.member(name).dri), right.member(name).overridens(sourceSet))
                    assertEquals(
                        setOf(left.member(name).dri, right.member(name).dri),
                        derived.member(name).overridens(sourceSet).toSet(),
                    )
                    assertEquals(derived.member(name).dri, inherited.member(name).dri)
                    assertEquals(derived.member(name).overridens(sourceSet), inherited.member(name).overridens(sourceSet))
                }
                assertTrue(derived.member("unrelated").overridens(sourceSet).isEmpty())
                val property = derived.properties.single { it.name == "bar" }
                assertNull(property.getter?.extra?.get(OverriddenMembers))
                assertNull(property.setter?.extra?.get(OverriddenMembers))
            }
        }
    }

    @Test
    fun `Java overrides retain the nearest declaration`() {
        testInline(
            """
            |/src/Root.java
            |package sample;
            |public interface Root<T> { T foo(T value); }
            |
            |/src/Base.java
            |package sample;
            |public class Base implements Root<String> {
            |    @Override public String foo(String value) { return value; }
            |}
            |
            |/src/Child.java
            |package sample;
            |public class Child extends Base {
            |    @Override public String foo(String value) { return value; }
            |}
            """.trimMargin(),
            configuration,
        ) {
            documentablesMergingStage = { module ->
                val classes = module.packages.single().classlikes.associateBy { it.name }
                fun declaredFoo(owner: String): DFunction = classes.getValue(owner).functions.single {
                    it.name == "foo" && it.dri.classNames == owner
                }

                val root = declaredFoo("Root")
                val base = declaredFoo("Base")
                val child = declaredFoo("Child")
                val sourceSet = child.sourceSets.single().sourceSetID.sourceSetName
                assertTrue(root.overridens(sourceSet).isEmpty())
                assertEquals(listOf(root.dri), base.overridens(sourceSet))
                assertEquals(listOf(base.dri), child.overridens(sourceSet))
            }
        }
    }

    @Test
    fun `explicit overrides retain source set specific targets before and after merging`() {
        val multiplatformConfiguration = dokkaConfiguration {
            sourceSets {
                val common by sourceSet {
                    name = "common"
                    analysisPlatform = "common"
                    sourceRoots = listOf("src/common")
                }
                sourceSet {
                    name = "jvm"
                    analysisPlatform = "jvm"
                    sourceRoots = listOf("src/jvm")
                    dependentSourceSets = setOf(common.sourceSetID)
                }
            }
        }
        testInline(
            """
            |/src/common/Test.kt
            |package sample
            |interface Root {
            |    fun foo(): String
            |    val bar: String
            |}
            |expect abstract class Base() : Root
            |class Derived : Base() {
            |    override fun foo(): String = ""
            |    override val bar: String = ""
            |}
            |
            |/src/jvm/Test.kt
            |package sample
            |actual abstract class Base actual constructor() : Root {
            |    override fun foo(): String = ""
            |    override val bar: String = ""
            |}
            """.trimMargin(),
            multiplatformConfiguration,
        ) {
            fun checkTargets(modules: List<DModule>) {
                val derived = modules.flatMap { it.packages }.flatMap { it.classlikes }.filter { it.name == "Derived" }
                for (name in listOf("foo", "bar")) {
                    val members = derived.flatMap { it.functions + it.properties }.filter { it.name == name }
                    val common = members.single { member -> member.sourceSets.any { it.sourceSetID.sourceSetName == "common" } }
                    val jvm = members.single { member -> member.sourceSets.any { it.sourceSetID.sourceSetName == "jvm" } }
                    assertEquals(common.dri, jvm.dri)
                    assertEquals(listOf("Root"), common.overridens("common").map { it.classNames })
                    assertEquals(listOf("Base"), jvm.overridens("jvm").map { it.classNames })
                }
            }
            documentablesCreationStage = { checkTargets(it) }
            documentablesMergingStage = { checkTargets(listOf(it)) }
        }
    }

    private fun DClasslike.member(name: String): Documentable =
        (functions + properties).single { it.name == name }

    private fun Documentable.overridens(sourceSetName: String): List<DRI> {
        val extra = when (this) {
            is DFunction -> extra[OverriddenMembers]
            is DProperty -> extra[OverriddenMembers]
            else -> error("Expected a function or property: $this")
        }
        val members = assertNotNull(extra).members.entries.single { it.key.sourceSetID.sourceSetName == sourceSetName }
        return members.value
    }
}
