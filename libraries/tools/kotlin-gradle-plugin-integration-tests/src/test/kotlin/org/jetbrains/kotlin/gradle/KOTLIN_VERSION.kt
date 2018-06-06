package org.jetbrains.kotlin.gradle

// constant is held in separate file intentionally for better discoverability
// and to prevent vcs conflicts (its value is 1.1-* in master branch, 0.1-* in 1.0.x branches)
val KOTLIN_VERSION get() = System.getProperty("kotlinVersion") ?: error("Required to specify kotlinVersion system property for tests")