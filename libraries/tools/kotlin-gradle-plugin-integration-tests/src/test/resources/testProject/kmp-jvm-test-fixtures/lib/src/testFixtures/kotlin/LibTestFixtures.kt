package com.foo

import com.example.CommonMain
import com.example.JvmMain
import com.example.JvmTestFixtures
import com.example.JavaTestFixtures
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json

class LibTestFixtures(
    val commonMain: CommonMain,
    val jvmMain: JvmMain,
    val jvmTestFixtures: JvmTestFixtures,
    val libMain: LibMain,
)

suspend fun shouldWork() = coroutineScope {
}

fun shouldAlsoWork() {
    val s = Json.decodeFromString<String>("")
}

fun another() {
    JvmTestFixtures().helperForTest()
}

fun anotherJava() {
    JavaTestFixtures().createJsonSerializer()
}
