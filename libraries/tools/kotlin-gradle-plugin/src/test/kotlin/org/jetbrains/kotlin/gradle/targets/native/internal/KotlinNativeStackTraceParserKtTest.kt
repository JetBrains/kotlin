/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.junit.Assert.assertEquals
import org.junit.Test

class KotlinNativeStackTraceParserKtTest {
    @Test
    fun testDebug() {
        assertEquals(
            """
KotlinNativeStackTrace(
message="kotlin.AssertionError: Expected <7>, actual <42>.",
stacktrace=[
KotlinNativeStackTraceElement(bin=test.kexe, address=0x00000001048f3c86, className=kotlin.Error, methodName=<init>, signature=(kotlin.String?)kotlin.Error, offset=70, fileName=/Users/teamcity/buildAgent/work/4d622a065c544371/runtime/src/main/kotlin/kotlin/Exceptions.kt, lineNumber=12, columnNumber=5)
KotlinNativeStackTraceElement(bin=test.kexe, address=0x00000001048f3ada, className=kotlin.AssertionError, methodName=<init>, signature=(kotlin.Any?)kotlin.AssertionError, offset=122, fileName=/Users/teamcity/buildAgent/work/4d622a065c544371/runtime/src/main/kotlin/kotlin/Exceptions.kt, lineNumber=128, columnNumber=5)
KotlinNativeStackTraceElement(bin=test.kexe, address=0x0000000104987939, className=kotlin.test.DefaultAsserter, methodName=fail, signature=(kotlin.String?)kotlin.Nothing, offset=137, fileName=/Users/teamcity/buildAgent/work/4d622a065c544371/backend.native/build/stdlib/kotlin/test/DefaultAsserter.kt, lineNumber=16, columnNumber=19)
KotlinNativeStackTraceElement(bin=test.kexe, address=0x0000000104987797, className=kotlin.test.Asserter, methodName=assertTrue, signature=(kotlin.Function0<kotlin.String?>;kotlin.Boolean), offset=263, fileName=/Users/teamcity/buildAgent/work/4d622a065c544371/backend.native/build/stdlib/kotlin/test/Assertions.kt, lineNumber=-1, columnNumber=-1)
KotlinNativeStackTraceElement(bin=test.kexe, address=0x000000010498840c, className=kotlin.test.Asserter, methodName=assertEquals, signature=(kotlin.String?;kotlin.Any?;kotlin.Any?), offset=380, fileName=/Users/teamcity/buildAgent/work/4d622a065c544371/backend.native/build/stdlib/kotlin/test/Assertions.kt, lineNumber=-1, columnNumber=-1)
KotlinNativeStackTraceElement(bin=test.kexe, address=0x000000010498b904, className=kotlin.test, methodName=assertEquals, signature=(#GENERIC;#GENERIC;kotlin.String?)Generic, offset=196, fileName=/Users/teamcity/buildAgent/work/4d622a065c544371/backend.native/build/stdlib/kotlin/test/Assertions.kt, lineNumber=-1, columnNumber=-1)
KotlinNativeStackTraceElement(bin=test.kexe, address=0x000000010498b7fe, className=kotlin.test, methodName=assertEquals${'$'}default, signature=(#GENERIC;#GENERIC;kotlin.String?;kotlin.Int)Generic, offset=158, fileName=/Users/teamcity/buildAgent/work/4d622a065c544371/backend.native/build/stdlib/kotlin/test/Assertions.kt, lineNumber=-1, columnNumber=-1)
KotlinNativeStackTraceElement(bin=test.kexe, address=0x000000010498b65c, className=sample.SampleTests, methodName=testMe, signature=(), offset=140, fileName=/Users/jetbrains/IdeaProjects/mpplib2/src/commonTest/kotlin/sample/SampleTests.kt, lineNumber=-1, columnNumber=-1)
KotlinNativeStackTraceElement(bin=test.kexe, address=0x000000010498b593, className=sample.${'$'}SampleTests${'$'}test${'$'}0.${'$'}testMe${'$'}FUNCTION_REFERENCE${'$'}0, methodName=invoke#internal, signature=(sample.${'$'}SampleTests${'$'}test${'$'}0.${'$'}testMe${'$'}FUNCTION_REFERENCE${'$'}0.invoke#internal, offset=67, fileName=/Users/jetbrains/IdeaProjects/mpplib2/src/commonTest/kotlin/sample/SampleTests.kt, lineNumber=-1, columnNumber=-1)
KotlinNativeStackTraceElement(bin=test.kexe, address=0x000000010498b4fb, className=sample.${'$'}SampleTests${'$'}test${'$'}0.${'$'}testMe${'$'}FUNCTION_REFERENCE${'$'}0, methodName=${'$'}<bridge-UNNN>invoke, signature=(#GENERIC)#internal, offset=75, fileName=/Users/jetbrains/IdeaProjects/mpplib2/src/commonTest/kotlin/sample/SampleTests.kt, lineNumber=10, columnNumber=6)
KotlinNativeStackTraceElement(bin=test.kexe, address=0x0000000104985c1c, className=kotlin.native.internal.test.BaseClassSuite.TestCase, methodName=run, signature=(), offset=492, fileName=/Users/teamcity/buildAgent/work/4d622a065c544371/backend.native/build/stdlib/generated/_Collections.kt, lineNumber=98, columnNumber=7)
KotlinNativeStackTraceElement(bin=test.kexe, address=0x000000010490649b, className=kotlin.native.internal.test.TestRunner, methodName=run#internal, signature=(kotlin.native.internal.test.TestRunner.run#internal, offset=1467, fileName=/Users/teamcity/buildAgent/work/4d622a065c544371/runtime/src/main/kotlin/kotlin/native/internal/test/TestRunner.kt, lineNumber=198, columnNumber=21)
KotlinNativeStackTraceElement(bin=test.kexe, address=0x0000000104905287, className=kotlin.native.internal.test.TestRunner, methodName=runIteration#internal, signature=(kotlin.native.internal.test.TestRunner.runIteration#internal, offset=1863, fileName=/Users/teamcity/buildAgent/work/4d622a065c544371/runtime/src/main/kotlin/kotlin/system/Timing.kt, lineNumber=33, columnNumber=12)
KotlinNativeStackTraceElement(bin=test.kexe, address=0x00000001049042d0, className=kotlin.native.internal.test.TestRunner, methodName=run, signature=()kotlin.Int, offset=816, fileName=/Users/teamcity/buildAgent/work/4d622a065c544371/runtime/src/main/kotlin/kotlin/native/internal/test/TestRunner.kt, lineNumber=232, columnNumber=17)
KotlinNativeStackTraceElement(bin=test.kexe, address=0x0000000104902eae, className=kotlin.native.internal.test, methodName=testLauncherEntryPoint, signature=(kotlin.Array<kotlin.String>)kotlin.Int, offset=110, fileName=/Users/teamcity/buildAgent/work/4d622a065c544371/runtime/src/main/kotlin/kotlin/native/internal/test/Launcher.kt, lineNumber=19, columnNumber=47)
KotlinNativeStackTraceElement(bin=test.kexe, address=0x0000000104902e12, className=kotlin.native.internal.test, methodName=main, signature=(kotlin.Array<kotlin.String>), offset=50, fileName=/Users/teamcity/buildAgent/work/4d622a065c544371/runtime/src/main/kotlin/kotlin/native/internal/test/Launcher.kt, lineNumber=23, columnNumber=5)
KotlinNativeStackTraceElement(bin=test.kexe, address=0x0000000104902d67, className=null, methodName=Konan_start, signature=(Konan_start, offset=71, fileName=/Users/teamcity/buildAgent/work/4d622a065c544371/runtime/src/launcher/kotlin/konan/start.kt, lineNumber=-1, columnNumber=-1)
KotlinNativeStackTraceElement(bin=test.kexe, address=0x0000000104902cf1, className=null, methodName=Konan_run_start, signature=(Konan_run_start, offset=113, fileName=null, lineNumber=-1, columnNumber=-1)
KotlinNativeStackTraceElement(bin=test.kexe, address=0x0000000104902c6b, className=null, methodName=Konan_main, signature=(Konan_main, offset=27, fileName=null, lineNumber=-1, columnNumber=-1)
KotlinNativeStackTraceElement(bin=libdyld.dylib, address=0x00007fff5ab4fed9, className=null, methodName=start, signature=(start, offset=1, fileName=null, lineNumber=-1, columnNumber=-1)
])
            """.trim(),
            parseKotlinNativeStackTrace(
                """
kotlin.AssertionError: Expected <7>, actual <42>.
        at 0   test.kexe                           0x00000001048f3c86 kfun:kotlin.Error.<init>(kotlin.String?)kotlin.Error + 70 (/Users/teamcity/buildAgent/work/4d622a065c544371/runtime/src/main/kotlin/kotlin/Exceptions.kt:12:5)
        at 1   test.kexe                           0x00000001048f3ada kfun:kotlin.AssertionError.<init>(kotlin.Any?)kotlin.AssertionError + 122 (/Users/teamcity/buildAgent/work/4d622a065c544371/runtime/src/main/kotlin/kotlin/Exceptions.kt:128:5)
        at 2   test.kexe                           0x0000000104987939 kfun:kotlin.test.DefaultAsserter.fail(kotlin.String?)kotlin.Nothing + 137 (/Users/teamcity/buildAgent/work/4d622a065c544371/backend.native/build/stdlib/kotlin/test/DefaultAsserter.kt:16:19)
        at 3   test.kexe                           0x0000000104987797 kfun:kotlin.test.Asserter.assertTrue(kotlin.Function0<kotlin.String?>;kotlin.Boolean) + 263 (/Users/teamcity/buildAgent/work/4d622a065c544371/backend.native/build/stdlib/kotlin/test/Assertions.kt:<unknown>)
        at 4   test.kexe                           0x000000010498840c kfun:kotlin.test.Asserter.assertEquals(kotlin.String?;kotlin.Any?;kotlin.Any?) + 380 (/Users/teamcity/buildAgent/work/4d622a065c544371/backend.native/build/stdlib/kotlin/test/Assertions.kt:<unknown>)
        at 5   test.kexe                           0x000000010498b904 kfun:kotlin.test.assertEquals(#GENERIC;#GENERIC;kotlin.String?)Generic + 196 (/Users/teamcity/buildAgent/work/4d622a065c544371/backend.native/build/stdlib/kotlin/test/Assertions.kt:<unknown>)
        at 6   test.kexe                           0x000000010498b7fe kfun:kotlin.test.assertEquals${'$'}default(#GENERIC;#GENERIC;kotlin.String?;kotlin.Int)Generic + 158 (/Users/teamcity/buildAgent/work/4d622a065c544371/backend.native/build/stdlib/kotlin/test/Assertions.kt:<unknown>)
        at 7   test.kexe                           0x000000010498b65c kfun:sample.SampleTests.testMe() + 140 (/Users/jetbrains/IdeaProjects/mpplib2/src/commonTest/kotlin/sample/SampleTests.kt:<unknown>)
        at 8   test.kexe                           0x000000010498b593 kfun:sample.${'$'}SampleTests${'$'}test${'$'}0.${'$'}testMe${'$'}FUNCTION_REFERENCE${'$'}0.invoke#internal + 67 (/Users/jetbrains/IdeaProjects/mpplib2/src/commonTest/kotlin/sample/SampleTests.kt:<unknown>)
        at 9   test.kexe                           0x000000010498b4fb kfun:sample.${'$'}SampleTests${'$'}test${'$'}0.${'$'}testMe${'$'}FUNCTION_REFERENCE${'$'}0.${'$'}<bridge-UNNN>invoke(#GENERIC)#internal + 75 (/Users/jetbrains/IdeaProjects/mpplib2/src/commonTest/kotlin/sample/SampleTests.kt:10:6)
        at 10  test.kexe                           0x0000000104985c1c kfun:kotlin.native.internal.test.BaseClassSuite.TestCase.run() + 492 (/Users/teamcity/buildAgent/work/4d622a065c544371/backend.native/build/stdlib/generated/_Collections.kt:98:7)
        at 11  test.kexe                           0x000000010490649b kfun:kotlin.native.internal.test.TestRunner.run#internal + 1467 (/Users/teamcity/buildAgent/work/4d622a065c544371/runtime/src/main/kotlin/kotlin/native/internal/test/TestRunner.kt:198:21)
        at 12  test.kexe                           0x0000000104905287 kfun:kotlin.native.internal.test.TestRunner.runIteration#internal + 1863 (/Users/teamcity/buildAgent/work/4d622a065c544371/runtime/src/main/kotlin/kotlin/system/Timing.kt:33:12)
        at 13  test.kexe                           0x00000001049042d0 kfun:kotlin.native.internal.test.TestRunner.run()kotlin.Int + 816 (/Users/teamcity/buildAgent/work/4d622a065c544371/runtime/src/main/kotlin/kotlin/native/internal/test/TestRunner.kt:232:17)
        at 14  test.kexe                           0x0000000104902eae kfun:kotlin.native.internal.test.testLauncherEntryPoint(kotlin.Array<kotlin.String>)kotlin.Int + 110 (/Users/teamcity/buildAgent/work/4d622a065c544371/runtime/src/main/kotlin/kotlin/native/internal/test/Launcher.kt:19:47)
        at 15  test.kexe                           0x0000000104902e12 kfun:kotlin.native.internal.test.main(kotlin.Array<kotlin.String>) + 50 (/Users/teamcity/buildAgent/work/4d622a065c544371/runtime/src/main/kotlin/kotlin/native/internal/test/Launcher.kt:23:5)
        at 16  test.kexe                           0x0000000104902d67 Konan_start + 71 (/Users/teamcity/buildAgent/work/4d622a065c544371/runtime/src/launcher/kotlin/konan/start.kt:<unknown>)
        at 17  test.kexe                           0x0000000104902cf1 Konan_run_start + 113
        at 18  test.kexe                           0x0000000104902c6b Konan_main + 27
        at 19  libdyld.dylib                       0x00007fff5ab4fed9 start + 1                
                """.trim()
            ).toString()
        )
    }

    @Test
    fun testRelease() {
        assertEquals(
            """
KotlinNativeStackTrace(
message="Uncaught Kotlin exception: kotlin.Exception: Foo!",
stacktrace=[
KotlinNativeStackTraceElement(bin=program.kexe, address=0x000000010d6ad726, className=kotlin.Exception, methodName=<init>, signature=(kotlin.String?)kotlin.Exception, offset=70, fileName=null, lineNumber=-1, columnNumber=-1)
KotlinNativeStackTraceElement(bin=program.kexe, address=0x000000010d6bc7a9, className=org.test.A, methodName=<get-qux>, signature=()ValueType, offset=89, fileName=null, lineNumber=-1, columnNumber=-1)
KotlinNativeStackTraceElement(bin=program.kexe, address=0x000000010d6bc712, className=org.test.A, methodName=baz, signature=()ValueType, offset=50, fileName=null, lineNumber=-1, columnNumber=-1)
KotlinNativeStackTraceElement(bin=program.kexe, address=0x000000010d6bc633, className=org.test, methodName=bar, signature=()ValueType, offset=67, fileName=null, lineNumber=-1, columnNumber=-1)
KotlinNativeStackTraceElement(bin=program.kexe, address=0x000000010d6bc5e9, className=org.test, methodName=foo, signature=()ValueType, offset=9, fileName=null, lineNumber=-1, columnNumber=-1)
])
            """.trim(),
            parseKotlinNativeStackTrace(
                """
Uncaught Kotlin exception: kotlin.Exception: Foo!
        at 0   program.kexe                        0x000000010d6ad726 kfun:kotlin.Exception.<init>(kotlin.String?)kotlin.Exception + 70
        at 1   program.kexe                        0x000000010d6bc7a9 kfun:org.test.A.<get-qux>()ValueType + 89
        at 2   program.kexe                        0x000000010d6bc712 kfun:org.test.A.baz()ValueType + 50
        at 3   program.kexe                        0x000000010d6bc633 kfun:org.test.bar()ValueType + 67
        at 4   program.kexe                        0x000000010d6bc5e9 kfun:org.test.foo()ValueType + 9
               """.trim()
            ).toString()
        )
    }
}