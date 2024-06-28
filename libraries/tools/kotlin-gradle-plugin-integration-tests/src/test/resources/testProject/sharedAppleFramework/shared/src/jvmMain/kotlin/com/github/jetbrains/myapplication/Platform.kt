package com.github.jetbrains.myapplication

actual class Platform actual constructor() {
    actual val platform: String = "JVM ${System.getProperty("java.version")}"
}