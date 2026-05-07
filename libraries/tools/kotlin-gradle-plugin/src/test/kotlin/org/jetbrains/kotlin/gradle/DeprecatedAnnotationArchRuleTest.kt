/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaField
import com.tngtech.archunit.core.domain.JavaMethod
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition
import org.junit.jupiter.api.Test

class DeprecatedAnnotationArchRuleTest {

    @Test
    fun `deprecated classes should have scheduled for removal message`() {
        ArchRuleDefinition.classes()
            .that().areAnnotatedWith(Deprecated::class.java)
            .should(classDeprecationCondition)
            .check(kgpClasses)
    }

    @Test
    fun `deprecated methods should have scheduled for removal message`() {
        ArchRuleDefinition.methods()
            .that().areAnnotatedWith(Deprecated::class.java)
            .should(methodDeprecationCondition)
            .check(kgpClasses)
    }

    @Test
    fun `deprecated fields should have scheduled for removal message`() {
        ArchRuleDefinition.fields()
            .that().areAnnotatedWith(Deprecated::class.java)
            .should(fieldDeprecationCondition)
            .check(kgpClasses)
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

        private val methodDeprecationCondition = object : ArchCondition<JavaMethod>(CONDITION_DESC) {
            override fun check(item: JavaMethod, events: ConditionEvents): Unit =
                checkDeprecation(item, item.fullName, item.tryGetAnnotationOfType(Deprecated::class.java).orElse(null), events)
        }

        private val fieldDeprecationCondition = object : ArchCondition<JavaField>(CONDITION_DESC) {
            override fun check(item: JavaField, events: ConditionEvents): Unit =
                checkDeprecation(item, item.fullName, item.tryGetAnnotationOfType(Deprecated::class.java).orElse(null), events)
        }

        private val scheduledRemovalRegex = Regex(""".*(Scheduled|Removed)\w*Kotlin \d+\.\d+\.?.*""")

        private fun checkDeprecation(item: Any, name: String, annotation: Deprecated?, events: ConditionEvents) {
            val message = annotation?.message ?: return
            if (!message.matches(scheduledRemovalRegex)) {
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
