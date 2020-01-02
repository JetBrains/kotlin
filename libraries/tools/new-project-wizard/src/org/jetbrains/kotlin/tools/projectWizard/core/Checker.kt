package org.jetbrains.kotlin.tools.projectWizard.core

import org.jetbrains.kotlin.tools.projectWizard.core.entity.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.SettingType


sealed class CheckerRule {
    abstract fun check(context: ValuesReadingContext): Boolean
}

data class RuleBySettingValue(
    val settingReference: SettingReference<Any, SettingType<Any>>,
    val expectedValue: Any
) : CheckerRule() {
    override fun check(context: ValuesReadingContext): Boolean = with(context) {
        settingReference.notRequiredSettingValue() == expectedValue
    }
}

data class OrRule(
    val left: CheckerRule,
    val right: CheckerRule
) : CheckerRule() {
    override fun check(context: ValuesReadingContext): Boolean = left.check(context) || right.check(context)
}

data class Checker(val rules: List<CheckerRule>) {
    fun check(context: ValuesReadingContext) =
        rules.all { rule -> rule.check(context) }

    class Builder {
        private val rules = mutableListOf<CheckerRule>()

        infix fun <V : Any, T: SettingType<V>> SettingReference<V, T>.shouldBeEqual(value: V) =
            RuleBySettingValue(this, value)

        infix fun CheckerRule.or(other: CheckerRule) =
            OrRule(this, other)

        fun extend(parent: Checker) {
            this.rules += parent.rules
        }

        fun rule(rule: CheckerRule) {
            rules += rule
        }

        fun build() = Checker(rules)
    }

    companion object {
        val ALWAYS_AVAILABLE = Checker(emptyList())
    }
}

fun checker(init: Checker.Builder.() -> Unit) =
    Checker.Builder().apply(init).build()


interface ContextOwner {
    val context: Context
}

interface ActivityCheckerOwner {
    val activityChecker: Checker

    fun isActive(valuesReadingContext: ValuesReadingContext) = activityChecker.check(valuesReadingContext)
}