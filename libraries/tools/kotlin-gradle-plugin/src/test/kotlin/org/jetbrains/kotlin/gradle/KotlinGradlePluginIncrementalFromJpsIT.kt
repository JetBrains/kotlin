package org.jetbrains.kotlin.gradle

import org.junit.Test

class KotlinGradleIncrementalFromJpsIT: BaseIncrementalGradleIT() {

    @Test
    fun testClassSignatureChanged() {
        val project = JpsTestProject("pureKotlin/classSignatureChanged", "2.4")

        project.build("build") {
            assertSuccessful()
            assertReportExists()
            assertCompiledKotlinSources("class.kt", "usage.kt")
        }

        project.modify()

        project.build("build") {
            assertSuccessful()
            assertCompiledKotlinSources("class.kt", "usage.kt")
        }
    }
//
//    @Test
//    fun testKotlinInJavaFunRenamed() {
//        val project = JpsTestProject("withJava/kotlinUsedInJava/funRenamed", "2.4")
//
//        project.build("build") {
//            assertSuccessful()
//            assertReportExists()
//            assertCompiledKotlinSources("fun.kt")
//            assertCompiledJavaSources("WillBeUnresolved.java")
//        }
//
//        project.modify()
//
//        project.build("build") {
//            assertCompiledKotlinSources("fun.kt")
//            assertCompiledJavaSources("WillBeUnresolved.java")
//            assertFailed()
//        }
//    }
//
//    @Test
//    fun testJavaInKotlinChangeSignature() {
//        val project = JpsTestProject("withJava/javaUsedInKotlin/changeSignature", "2.4")
//
//        project.build("build") {
//            assertSuccessful()
//            assertReportExists()
//            assertCompiledKotlinSources("usage.kt")
//            assertCompiledJavaSources("JavaClass.java")
//        }
//
//        project.modify()
//
//        project.build("build") {
//            assertSuccessful()
//            assertCompiledKotlinSources("usage.kt")
//            assertCompiledJavaSources("JavaClass.java")
//        }
//    }

}
