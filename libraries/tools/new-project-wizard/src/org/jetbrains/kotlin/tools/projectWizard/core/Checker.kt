package org.jetbrains.kotlin.tools.projectWizard.core

typealias Checker = Reader.() -> Boolean

val ALWAYS_AVAILABLE_CHECKER = checker { true }

fun checker(check: Checker) = check

interface ContextOwner {
    val context: Context
}

interface ActivityCheckerOwner {
    val isAvailable: Checker

    fun isActive(reader: Reader) = isAvailable(reader)
}