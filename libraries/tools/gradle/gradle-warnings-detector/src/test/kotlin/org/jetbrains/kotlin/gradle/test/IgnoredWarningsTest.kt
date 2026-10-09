/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.test

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class IgnoredWarningsTest {

    @Test
    fun `AGP boolean is-property warning is ignored as Gradle 8_14 words it`() {
        val ignoredWarning = KNOWN_THIRD_PARTY_WARNINGS.firstMatching(
            agpBooleanIsPropertyOnGradle814,
            stackTraceClassNames = listOf("com.android.something")
        )
        assertNotNull(ignoredWarning, "The AGP 'is-' property warning should be ignored")
        assertEquals(agpBooleanIsPropertyReason, ignoredWarning.reason)
    }

    @Test
    fun `AGP boolean is-property warning is ignored as Gradle 9_7 words it`() {
        val ignoredWarning = KNOWN_THIRD_PARTY_WARNINGS.firstMatching(
            agpBooleanIsPropertyOnGradle97,
            stackTraceClassNames = listOf("com.android.something")
        )
        assertNotNull(ignoredWarning, "The AGP 'is-' property warning should be ignored")
        assertEquals(agpBooleanIsPropertyReason, ignoredWarning.reason)
    }

    @Test
    fun `the same boolean is-property warning caused by Kotlin is reported`() {
        assertNull(
            KNOWN_THIRD_PARTY_WARNINGS.firstMatching(
                agpBooleanIsPropertyOnGradle814.replace(
                    "com.android.build.gradle.internal.dsl.BuildType",
                    "org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension",
                )
            ),
            "A boolean 'is-' property declared by Kotlin should be reported",
        )
        assertNull(
            KNOWN_THIRD_PARTY_WARNINGS.firstMatching(
                agpBooleanIsPropertyOnGradle97.replace(
                    "com.android.build.gradle.internal.dsl.BuildType",
                    "org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension",
                )
            ),
            "A boolean 'is-' property declared by Kotlin should be reported",
        )
    }

    @Test
    fun `AGP null attribute key warning is ignored when raised from AGP code`() {
        val ignoredWarning = KNOWN_THIRD_PARTY_WARNINGS.firstMatching(nullAttributeKey, agpLintStackTrace)
        assertNotNull(ignoredWarning, "The AGP null attribute key warning should be ignored")
        assertEquals(agpNullAttributeKeyReason, ignoredWarning.reason)
    }

    @Test
    fun `the same null attribute key warning is reported when AGP is not in the stack trace`() {
        assertNull(
            KNOWN_THIRD_PARTY_WARNINGS.firstMatching(
                nullAttributeKey,
                listOf(
                    "org.gradle.api.internal.attributes.AbstractAttributeContainer",
                    "org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCompilationNpmResolver",
                ),
            ),
            "A null attribute key lookup made by Kotlin should be reported",
        )
    }

    @Test
    fun `the null attribute key warning is reported when no stack trace was captured`() {
        assertNull(KNOWN_THIRD_PARTY_WARNINGS.firstMatching(nullAttributeKey))
    }

    @Test
    fun `a stack trace alone does not ignore an unrelated warning`() {
        assertNull(
            KNOWN_THIRD_PARTY_WARNINGS.firstMatching(
                "The Project.getConvention() method has been deprecated.",
                agpLintStackTrace,
            ),
            "Being raised from AGP code is not on its own a reason to ignore a warning",
        )
    }

    @Test
    fun `an unrelated warning is reported`() {
        assertNull(
            KNOWN_THIRD_PARTY_WARNINGS.firstMatching(
                "The Project.getConvention() method has been deprecated. " +
                        "This is scheduled to be removed in Gradle 9.0."
            )
        )
    }

    @Test
    fun `a warning mentioning AGP for an unrelated reason is reported`() {
        assertNull(
            KNOWN_THIRD_PARTY_WARNINGS.firstMatching(
                "The com.android.build.gradle.AppPlugin type has been deprecated."
            ),
            "Only the deprecations listed as known should be ignored, not everything AGP touches",
        )
    }

    @Test
    fun `every known warning declares at least one pattern`() {
        for (ignoredWarning in KNOWN_THIRD_PARTY_WARNINGS) {
            assertNotNull(
                ignoredWarning.patterns.firstOrNull(),
                "'${ignoredWarning.reason}' declares no patterns",
            )
        }
    }

    @Test
    fun `fragments are joined in order, skipping the empty ones`() {
        assertEquals(
            "Summary. Removal details. Contextual advice. Advice. https://example.com",
            describeDeprecationWarning(
                "Summary.",
                "Removal details.",
                "Contextual advice.",
                "Advice.",
                "https://example.com",
            ),
        )
        assertEquals("Summary. Advice.", describeDeprecationWarning("Summary.", null, "", "Advice.", null))
        assertEquals("", describeDeprecationWarning(null, null))
    }

    @Test
    fun `whitespace runs within a fragment are collapsed`() {
        assertEquals(
            "A multi line summary. Advice.",
            describeDeprecationWarning("  A multi\n  line\tsummary.  ", "Advice."),
        )
    }

    private val agpNullAttributeKeyReason =
        "AGP looks up null attribute keys while writing the lint model: " +
                "https://issuetracker.google.com/issues/408334529"

    private val nullAttributeKey = describeDeprecationWarning(
        "Retrieving attribute with a null key. This behavior has been deprecated.",
        "This will fail with an error in Gradle 10.0.",
        null,
        "Don't request attributes from attribute containers using null keys.",
        "https://docs.gradle.org/8.14.5/userguide/upgrading_version_8.html#null-attribute-lookup",
    )

    private val agpLintStackTrace = listOf(
        "org.gradle.api.internal.attributes.AbstractAttributeContainer",
        "org.gradle.api.internal.attributes.DefaultImmutableAttributesContainer",
        "com.android.build.gradle.internal.ide.dependencies.ArtifactUtils",
        "com.android.build.gradle.internal.lint.LintModelWriterTask",
    )

    private val agpBooleanIsPropertyReason =
        "AGP declares Boolean 'is-' properties: https://issuetracker.google.com/issues/399393875"

    private val agpBooleanIsPropertyOnGradle814 = describeDeprecationWarning(
        "Declaring an 'is-' property with a Boolean type has been deprecated.",
        "Starting with Gradle 9.0, this property will be ignored by Gradle.",
        "The combination of method name and return type is not consistent with Java Bean property rules " +
                "and will become unsupported in future versions of Groovy.",
        "Add a method named 'getMinifyEnabled' with the same behavior and mark the old one with @Deprecated, " +
                "or change the type of " +
                "'com.android.build.gradle.internal.dsl.BuildType.isMinifyEnabled' (and the setter) to 'boolean'.",
        "https://docs.gradle.org/8.14.5/userguide/upgrading_version_8.html#groovy_boolean_properties",
    )

    private val agpBooleanIsPropertyOnGradle97 = describeDeprecationWarning(
        "Declaring 'minifyEnabled' as a property using an 'is-' method with a Boolean type on " +
                "com.android.build.gradle.internal.dsl.BuildType has been deprecated.",
        "Starting with Gradle 10, this property will no longer be treated like a property.",
        "The combination of method name and return type is not consistent with Java Bean property rules.",
        "Add a method named 'getMinifyEnabled' with the same behavior and mark the old one with @Deprecated, " +
                "or change the type of " +
                "'com.android.build.gradle.internal.dsl.BuildType.isMinifyEnabled' (and the setter) to 'boolean'.",
        "https://docs.gradle.org/9.7.1/userguide/upgrading_version_8.html#groovy_boolean_properties",
    )
}
