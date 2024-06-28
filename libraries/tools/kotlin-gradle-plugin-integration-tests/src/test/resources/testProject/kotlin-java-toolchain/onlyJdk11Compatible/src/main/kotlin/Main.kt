/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


/**
 * A main function that could only compile on JDK 11-13, due to use of both new api and removed api
 */
fun main() {
    // The function, that was only available until JDK 13 (https://bugs.openjdk.org/browse/JDK-8205131)
    Runtime.getRuntime().traceInstructions(true)

    // The new overload for toArray that was added in JDK 11 (https://bugs.openjdk.org/browse/JDK-8060192)
    val array: Array<String> = listOf("").toArray { arrayOf("other") }
}

