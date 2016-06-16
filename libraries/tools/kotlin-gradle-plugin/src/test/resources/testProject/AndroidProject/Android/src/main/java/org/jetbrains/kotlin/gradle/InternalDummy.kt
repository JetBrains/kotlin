package org.jetbrains.kotlin.gradle

internal class InternalDummy(private val name: String) {
    internal val greeting: String
            get() = "Hello $name!"
}