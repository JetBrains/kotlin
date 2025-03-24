/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.abi

import groovy.lang.Closure
import org.jetbrains.kotlin.gradle.dsl.NamedDomainImmutableCollection
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationVariantSpec
import org.jetbrains.kotlin.gradle.dsl.abi.VariantConfigurator
import org.jetbrains.kotlin.gradle.util.assertIsInstance
import kotlin.test.*


internal abstract class AbstractAbiValidationVariantsTest {
    protected open fun testNames() {
        val variants = buildVariants()
        // test variant configurator
        doTestNamesMain(variants)
        // test in immutable collection
        doTestNamesMain(variants.matching { true })
        variants.register("extra")
        // test variant configurator
        doTestNamesExtra(variants)
        // test in immutable collection
        doTestNamesExtra(variants.matching { true })
    }

    protected open fun testNamed() {
        val variants = buildVariants()
        variants.register("extra")
        // test variant configurator
        assertNamed(variants)
        // test in immutable collection
        assertNamed(variants.matching { true })
    }

    protected open fun testConfigureEach() {
        val variants = buildVariants()
        variants.register("extra")
        // test variant configurator
        assertConfigureEach(variants)
        // test in immutable collection
        assertConfigureEach(variants.matching { true })
    }

    protected open fun testMatching() {
        val variants = buildVariants()

        variants.register("extra")
        // test variant configurator
        assertMatching(variants)
        // test in immutable collection
        assertMatching(variants.matching { true })
    }

    protected open fun testWithType() {
        val variants = buildVariants()
        variants.register("extra")
        // test variant configurator
        assertWithType(variants)
        // test in immutable collection
        assertWithType(variants.matching { true })
    }

    private fun doTestNamesMain(variants: NamedDomainImmutableCollection<AbiValidationVariantSpec>) {
        assertEquals(setOf("main"), variants.getNames())
    }

    private fun doTestNamesExtra(variants: NamedDomainImmutableCollection<AbiValidationVariantSpec>) {
        assertEquals(setOf("main", "extra"), variants.getNames())
    }

    private fun assertConfigureEach(variants: NamedDomainImmutableCollection<AbiValidationVariantSpec>) {
        val filter = "excluded filters"

        variants.configureEach {
            it.filters.excluded.classes.add(filter)
        }

        val mainVariant = variants.named("main").get()
        val extraVariant = variants.named("extra").get()

        assertEquals(setOf(filter), mainVariant.filters.excluded.classes.get())
        assertEquals(setOf(filter), extraVariant.filters.excluded.classes.get())
    }

    private fun assertNamed(variants: NamedDomainImmutableCollection<AbiValidationVariantSpec>) {
        val variantMain1 = variants.named("main").get()
        assertEquals("main", variantMain1.name)

        val variantMain2 = variants.named("main") {
            it.filters.excluded.classes.empty()
            it.filters.excluded.classes.add("named(name,config)")
        }.get()
        assertEquals(setOf("named(name,config)"), variantMain2.filters.excluded.classes.get())
        assertEquals("main", variantMain2.name)

        val variantMain3 = variants.named("main", AbiValidationVariantSpec::class.java).get()
        assertEquals("main", variantMain3.name)

        val variantMain4 = variants.named(
            "main",
            AbiValidationVariantSpec::class.java
        ) {
            it.filters.excluded.classes.empty()
            it.filters.excluded.classes.add("named(name,type,config)")
        }.get()
        assertEquals(setOf("named(name,type,config)"), variantMain4.filters.excluded.classes.get())
        assertEquals("main", variantMain4.name)

        val variantExtra = variants.named("extra").get()
        assertEquals("extra", variantExtra.name)
        assertEquals(emptySet(), variantExtra.filters.excluded.classes.get())
    }

    private fun assertMatching(variants: NamedDomainImmutableCollection<AbiValidationVariantSpec>) {
        // initialize variants because getNames() is lazy and always
        variants.named("main").get()
        variants.named("extra").get()

        assertEquals(setOf("main"), variants.matching { it.name == "main" }.getNames())
        assertEquals(emptySet(), variants.matching { false }.getNames())

        assertEquals(setOf("main"), variants.matching(object : Closure<Boolean>(variants) {
            override fun call(vararg arguments: Any): Boolean {
                assertEquals(1, arguments.size)
                assertIsInstance<AbiValidationVariantSpec>(arguments[0])
                return (arguments[0] as AbiValidationVariantSpec).name == "main"
            }
        }).getNames())
        assertEquals(emptySet(), variants.matching { false }.getNames())
    }

    private fun assertWithType(variants: NamedDomainImmutableCollection<AbiValidationVariantSpec>) {
        // withType(type)
        assertEquals(emptySet(), variants.withType(ChildVariant::class.java).getNames())
        assertEquals(setOf("main", "extra"), variants.withType(AbiValidationVariantSpec::class.java).getNames())

        // withType(type,action)
        variants.withType(AbiValidationVariantSpec::class.java) {
            it.filters.excluded.classes.empty()
            it.filters.excluded.classes.add("withType(type,action)")
        }
        assertEquals(setOf("withType(type,action)"), variants.named("main").get().filters.excluded.classes.get())
        assertEquals(setOf("withType(type,action)"), variants.named("extra").get().filters.excluded.classes.get())

        variants.withType(ChildVariant::class.java) {
            it.filters.excluded.classes.empty()
        }
        assertNotEquals(emptySet(), variants.named("main").get().filters.excluded.classes.get())
        assertNotEquals(emptySet(), variants.named("extra").get().filters.excluded.classes.get())

        // withType(type,closure)
        variants.withType(AbiValidationVariantSpec::class.java, object : Closure<Void>(variants) {
            override fun call(vararg arguments: Any): Void? {
                assertIsInstance<AbiValidationVariantSpec>(delegate)
                val variant = delegate as AbiValidationVariantSpec
                variant.filters.excluded.classes.empty()
                variant.filters.excluded.classes.add("withType(type,closure)")
                return null
            }
        })
        assertEquals(setOf("withType(type,closure)"), variants.named("main").get().filters.excluded.classes.get())
        assertEquals(setOf("withType(type,closure)"), variants.named("extra").get().filters.excluded.classes.get())


        variants.withType(ChildVariant::class.java, object : Closure<Void>(variants) {
            override fun call(vararg arguments: Any): Void? {
                assertIsInstance<AbiValidationVariantSpec>(delegate)
                val variant = delegate as AbiValidationVariantSpec
                variant.filters.excluded.classes.empty()
                return null
            }
        })
        assertNotEquals(emptySet(), variants.named("main").get().filters.excluded.classes.get())
        assertNotEquals(emptySet(), variants.named("extra").get().filters.excluded.classes.get())
    }

    private interface ChildVariant : AbiValidationVariantSpec

    protected abstract fun buildVariants(): VariantConfigurator<AbiValidationVariantSpec>
}
