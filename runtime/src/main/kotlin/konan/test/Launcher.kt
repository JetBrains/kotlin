/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package konan.test

import kotlin.system.exitProcess

private val _generatedSuites = mutableListOf<TestSuite>()

internal fun registerSuite(suite: TestSuite): Unit {
    _generatedSuites.add(suite)
}

fun testLauncherEntryPoint(args: Array<String>): Int {
    return TestRunner(_generatedSuites, args).run()
}

fun main(args: Array<String>) {
    exitProcess(testLauncherEntryPoint(args))
}
