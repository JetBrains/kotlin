/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaMember
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition
import com.tngtech.archunit.library.freeze.FreezingArchRule
import org.junit.jupiter.api.Test

class DeprecatedAnnotationArchRuleTest {

    @Test
    fun `deprecated classes should have scheduled for removal message`() {
        FreezingArchRule.freeze(
            ArchRuleDefinition.classes()
                .that().areAnnotatedWith(Deprecated::class.java)
                .and().haveSimpleNameNotStartingWith("$")
                .and().resideOutsideOfPackages("*.internal.*")
                .should(classDeprecationCondition)
        ).check(kgpClasses)
    }

    @Test
    fun `deprecated members should have scheduled for removal message`() {
        FreezingArchRule.freeze(
            ArchRuleDefinition.members()
                .that()
                .areAnnotatedWith(Deprecated::class.java)
                .and().haveNameNotStartingWith("$")
                .and().areDeclaredInClassesThat().resideOutsideOfPackages("*.internal.*")
                .should(memberDeprecationCondition)
        ).check(kgpClasses)
    }

    companion object {
        private val kgpClasses by lazy {
            ClassFileImporter()
                .withImportOption(ImportOption.DoNotIncludeTests())
                .importPackages("org.jetbrains.kotlin.gradle")
        }

        private const val CONDITION_DESC =
            "have @Deprecated message ending with 'Scheduled for removal in Kotlin X.Y.'"

        private val classDeprecationCondition = object : ArchCondition<JavaClass>(CONDITION_DESC) {
            override fun check(item: JavaClass, events: ConditionEvents): Unit =
                checkDeprecation(item, item.fullName, item.tryGetAnnotationOfType(Deprecated::class.java).orElse(null), events)
        }

        private val memberDeprecationCondition = object : ArchCondition<JavaMember>(CONDITION_DESC) {
            override fun check(item: JavaMember, events: ConditionEvents): Unit =
                checkDeprecation(item, item.fullName, item.tryGetAnnotationOfType(Deprecated::class.java).orElse(null), events)
        }

        private val scheduledRemovalRegex = Regex(""".*(Scheduled|Removed) [A-Za-z ]+ *Kotlin \d+\.\d+\.?.*""")

        private fun checkDeprecation(item: Any, name: String, annotation: Deprecated?, events: ConditionEvents) {
            val message = annotation?.message ?: return

            when {
                "https://kotl.in/u1r8ln" in message
                        ||
                        "https://kotl.in/t6m3vu" in message
                        ||
                        "KT-56644" in message
                        ||
                        "https://kotl.in/native-targets-tiers" in message
                        ||
                        "This synthesized declaration should not be used directly" in message
                        ||
                        "Inserted into generated code and should not be used directly" in message
                    -> return

                !message.matches(scheduledRemovalRegex) ->
                    events.add(
                        SimpleConditionEvent.violated(
                            item,
                            "${name}: ${annotation.message}"
                        )
                    )
            }
        }
    }
}
