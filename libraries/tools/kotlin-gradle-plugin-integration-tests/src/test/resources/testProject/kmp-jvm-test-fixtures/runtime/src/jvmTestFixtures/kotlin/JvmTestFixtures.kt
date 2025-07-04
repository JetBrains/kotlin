package com.example

import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

class JvmTestFixtures {
    fun helperForTest() = runBlocking {
        CommonMain()
        JvmMain()
    }

    fun createJsonSerializer() = Json { ignoreUnknownKeys = true }
}
