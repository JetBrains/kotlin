package org.jetbrains.testproject.ios

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
