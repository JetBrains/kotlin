package com.example

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform