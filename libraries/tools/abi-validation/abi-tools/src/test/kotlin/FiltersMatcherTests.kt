/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.abi.tools.api.AbiFilters
import org.jetbrains.kotlin.abi.tools.filtering.compileMatcher
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FiltersMatcherTests {

    @Test
    fun emptyMatchers() {
        // manual empty
        val emptyMatcherManual = compileMatcher(FiltersBuilder().build())
        assertTrue(emptyMatcherManual.isEmpty)
        assertFalse(emptyMatcherManual.isExcludedByName("some.Name"))
        assertFalse(emptyMatcherManual.isExcludedByAnnotations(listOf("some.Annotation")))

        // default empty
        val emptyMatcherDefault = compileMatcher(AbiFilters.EMPTY)
        assertTrue(emptyMatcherDefault.isEmpty)
        assertFalse(emptyMatcherDefault.isExcludedByName("some.Name"))
        assertFalse(emptyMatcherDefault.isExcludedByAnnotations(listOf("some.Annotation")))
    }

    @Test
    fun excludeFullName() {
        assertFilters { excludeClasses("foo.bar.Biz") }
            .isNotEmpty()
            .excludes("foo.bar.Biz")
            .notExcludes("foo.bar.Biz2", "xfoo.bar.Biz", "foo.bar.", "foo.bar", "Biz")
    }

    @Test
    fun excludeAnyPostfix() {
        assertFilters { excludeClasses("foo.bar.**") }
            .isNotEmpty()
            .excludes("foo.bar.", "foo.bar.bar.Biz", "foo.bar.Bar")
            .notExcludes("foo.bar", "foo")
    }

    @Test
    fun excludeAnyRightSegment() {
        assertFilters { excludeClasses("foo.bar.*") }
            .isNotEmpty()
            .excludes("foo.bar.", "foo.bar.Biz", "foo.bar.B")
            .notExcludes("foo.bar.biz.Biz", "foo", "foo.")
    }

    @Test
    fun excludeAnyPrefix() {
        assertFilters { excludeClasses("**Bar") }
            .isNotEmpty()
            .excludes("Bar", "foo.Bar", "foo.bar.Bar")
            .notExcludes("BarClass", "foo.Bar.", "foo.Bar.Biz")
    }

    @Test
    fun excludeRegex() {
        assertFilters { excludeClasses("*a**Bar?") }
            .isNotEmpty()
            .excludes("aBarr", "bar.foo.Barr", "a.Bar2")
            .notExcludes("aBar", "bar.Bar", "Bar")
    }

    @Test
    fun includeFullName() {
        assertFilters { includeClasses("foo.bar.Biz") }
            .isNotEmpty()
            .excludes("foo.bar.Biz2", "xfoo.bar.Biz", "foo.bar.", "foo.bar", "Biz")
            .notExcludes("foo.bar.Biz")
    }

    @Test
    fun includeAnyPostfix() {
        assertFilters { includeClasses("foo.bar.**") }
            .isNotEmpty()
            .excludes("foo.bar", "foo")
            .notExcludes("foo.bar.", "foo.bar.bar.Biz", "foo.bar.Bar")
    }

    @Test
    fun includeAnyRightSegment() {
        assertFilters { includeClasses("foo.bar.*") }
            .isNotEmpty()
            .excludes("foo.bar.biz.Biz", "foo", "foo.")
            .notExcludes("foo.bar.", "foo.bar.Biz", "foo.bar.B")
    }

    @Test
    fun includeAnyPrefix() {
        assertFilters { includeClasses("**Bar") }
            .isNotEmpty()
            .excludes("BarClass", "foo.Bar.", "foo.Bar.Biz")
            .notExcludes("Bar", "foo.Bar", "foo.bar.Bar")
    }

    @Test
    fun includeRegex() {
        assertFilters { includeClasses("*a**Bar?") }
            .isNotEmpty()
            .notExcludes("aBarr", "bar.foo.Barr", "a.Bar2")
            .excludes("aBar", "bar.Bar", "Bar")
    }

    @Test
    fun excludeClassNameAndAnnotatedWith() {
        assertFilters {
            excludeClasses("**Bar*")
            excludeAnnotatedWith("foo.Generated")
        }
            .isNotEmpty()
            // by name
            .excludesAnnotated("foo.Bar")
            // by annotation
            .excludesAnnotated("Other", "foo.A", "foo.Generated")
            // both
            .excludesAnnotated("FooBarBiz", "foo.A", "foo.Generated")

            .notExcludesAnnotated("Other", "foo.A")
            .notExcludesAnnotated("Bar.Biz", "foo.A")
    }

    @Test
    fun includeClassNameAndAnnotatedWith() {
        assertFilters {
            includeClasses("**Bar*")
            includeAnnotatedWith("foo.Generated", "Included")
        }
            .isNotEmpty()
            // only name
            .excludesAnnotated("foo.Bar")
            // only annotation
            .excludesAnnotated("Other", "foo.A", "foo.Generated")
            // both
            .notExcludesAnnotated("FooBarBiz", "foo.A", "foo.Generated")
            // both with other annotation
            .notExcludesAnnotated("FooBarBiz", "foo.A", "Included")

            .excludesAnnotated("Other", "foo.A")
            .excludesAnnotated("Bar.Biz", "foo.A")
    }

    @Test
    fun excludeAndIncludeByName() {
        assertFilters {
            // exclude have higher priority over include
            includeClasses("foo.bar.**")
            excludeClasses("**Biz*")
        }
            .isNotEmpty()
            .excludes("other.package.Foo", "other.package.Bizz", "foo.bar.Biz", "foo.Bar")
            .notExcludes("foo.bar.A", "foo.bar.B")
    }

    @Test
    fun excludeAndIncludeByAnnotation() {
        assertFilters {
            // included only if at least one of annotation is present
            includeAnnotatedWith("foo.Include", "Include")
            // excluded if at least one of annotation is present
            excludeAnnotatedWith("foo.Exclude", "Exclude")
        }
            .isNotEmpty()
            .notExcludesAnnotated("A", "foo.Include", "Extra")
            .notExcludesAnnotated("B", "Include", "Extra")
            .notExcludesAnnotated("C", "foo.Include", "Include", "Extra")
            .excludesAnnotated("D", "Include", "Exclude")
            .excludesAnnotated("E", "foo.Include", "foo.Exclude")
    }

    /**
     * Class not excluded if:
     * - class name matches one of the include class
     * - class name doesn't match no one of the exclude class
     * - class annotated with at least one of the include annotation
     * - class doesn't annotate with no one of the exclude annotation
     */
    @Test
    fun mixedFilters() {
        assertFilters {
            // included only if at least one of annotation is present and match any of class name pattern
            includeClasses("foo.bar.**", "com.**")
            includeAnnotatedWith("foo.Include", "Include")

            // excluded if at least one of annotation is present and match any of class name pattern
            excludeClasses("**Biz*", "com.example.*")
            excludeAnnotatedWith("foo.Exclude", "Exclude")
        }
            .isNotEmpty()
            .notExcludesAnnotated("foo.bar.MyClass", "foo.Include")
            .notExcludesAnnotated("foo.bar.MyClass", "Include", "Other")
            .notExcludesAnnotated("com.MyClass", "foo.Include")
            .notExcludesAnnotated("com.MyClass", "Include", "Extra")

            .excludesAnnotated("foo.bar.MyClass", "Extra")
            .excludesAnnotated("some.class", "foo.Include")
            .excludesAnnotated("foo.bar.MyClass", "foo.Include", "foo.Exclude")
            .excludesAnnotated("foo.bar.MyClass", "Include", "Exclude")
            .excludesAnnotated("com.example.MyClass", "foo.Include")
            .excludesAnnotated("foo.bar.Biz", "foo.Include")
    }

    private fun assertFilters(block: FiltersBuilder.() -> Unit): Asserter {
        val builder = FiltersBuilder().also(block)
        return Asserter(builder.build())
    }

    private class FiltersBuilder {
        private val includedClasses = mutableSetOf<String>()
        private val excludedClasses = mutableSetOf<String>()
        private val includedAnnotatedWith = mutableSetOf<String>()
        private val excludedAnnotatedWith = mutableSetOf<String>()

        fun excludeClasses(vararg filter: String): FiltersBuilder {
            excludedClasses.addAll(filter)
            return this
        }

        fun includeClasses(vararg filter: String): FiltersBuilder {
            includedClasses.addAll(filter)
            return this
        }

        fun excludeAnnotatedWith(vararg filter: String): FiltersBuilder {
            excludedAnnotatedWith.addAll(filter)
            return this
        }

        fun includeAnnotatedWith(vararg filter: String): FiltersBuilder {
            includedAnnotatedWith.addAll(filter)
            return this
        }

        fun build(): AbiFilters = AbiFilters(includedClasses, excludedClasses, includedAnnotatedWith, excludedAnnotatedWith)
    }

    private class Asserter(val filters: AbiFilters) {
        val matcher = compileMatcher(filters)

        fun isNotEmpty(): Asserter {
            assertFalse(matcher.isEmpty, "Filters ${filters.print()} should not be empty")
            return this
        }

        fun excludes(vararg names: String): Asserter {
            names.forEach { name ->
                assertTrue(matcher.isExcludedByName(name), "Class '$name' should be excluded by filters ${filters.print()}")
            }
            return this
        }

        fun excludesAnnotated(className: String, vararg annotations: String): Asserter {
            val excluded = matcher.isExcludedByName(className) || matcher.isExcludedByAnnotations(annotations.toList())
            assertTrue(
                excluded,
                "Class '$className' with annotations ${annotations.toList()} should be excluded by filters ${filters.print()}"
            )
            return this
        }

        fun notExcludesAnnotated(className: String, vararg annotations: String): Asserter {
            val excluded = matcher.isExcludedByName(className) || matcher.isExcludedByAnnotations(annotations.toList())
            assertFalse(
                excluded,
                "Class '$className' with annotations ${annotations.toList()} should not be excluded by filters ${filters.print()}"
            )
            return this
        }

        fun notExcludes(vararg names: String): Asserter {
            names.forEach { name ->
                assertFalse(matcher.isExcludedByName(name), "Class '$name' should be included by filters ${filters.print()}")
            }
            return this
        }

        private fun AbiFilters.print(): String {
            return "{includedClasses=$includedClasses, excludedClasses=$excludedClasses, includedAnnotatedWith=$includedAnnotatedWith, excludedAnnotatedWith=$excludedAnnotatedWith}"
        }
    }


    @JvmField
    @Rule
    val tempDir = TemporaryFolder()

    @Test
    fun test() {
        val output = tmpFile()
//        val classes = root.resolve("simple").walk().filter { it.isFile && it.name.endsWith(".class") }.toList()
//
//
//        AbiTools.jvm.dumpTo(output, listOf(JvmAbiSuit("jvm", classes)), AbiFilters.EMPTY)
//
//        println(output.readText())
    }

    fun tmpFile(): File {
        return tempDir.newFile()
    }

    private val root = File("src/test/resources/compiled")
}