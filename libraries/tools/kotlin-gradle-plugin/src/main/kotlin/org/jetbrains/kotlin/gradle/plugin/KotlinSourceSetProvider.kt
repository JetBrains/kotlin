package org.jetbrains.kotlin.gradle.plugin

internal interface KotlinSourceSetProvider {
    fun create(displayName: String): KotlinSourceSet
}