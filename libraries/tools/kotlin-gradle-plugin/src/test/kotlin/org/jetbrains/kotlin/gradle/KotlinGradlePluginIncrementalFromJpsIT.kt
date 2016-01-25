package org.jetbrains.kotlin.gradle

import org.junit.Test
import java.io.File

class KotlinGradleIncrementalFromJpsIT(): BaseIncrementalGradleIT() {

    companion object {
        val jpsResourcesPath = File("../../../jps-plugin/testData/incremental")
    }

    override fun defaultBuildOptions(): BuildOptions = BuildOptions(withDaemon = true)

    @Test
    fun testClassSignatureChanged() {
        val project = JpsTestProject(jpsResourcesPath, "pureKotlin/classSignatureChanged", "2.4")

        project.build("build") {
            assertSuccessful()
            assertReportExists()
            assertCompiledKotlinSources("src/class.kt", "src/usage.kt")
        }

        project.modify()

        project.build("build") {
            assertSuccessful()
            assertCompiledKotlinSources("src/class.kt", "src/usage.kt")
        }
    }

    @Test
    fun testKotlinInJavaFunRenamed() {
        val project = JpsTestProject(jpsResourcesPath, "withJava/kotlinUsedInJava/funRenamed", "2.4")

        project.build("build") {
            assertSuccessful()
            assertReportExists()
            assertCompiledKotlinSources("src/fun.kt")
            assertCompiledJavaSources("src/WillBeUnresolved.java")
        }

        project.modify()

        project.build("build") {
            assertCompiledKotlinSources("src/fun.kt")
            assertFailed()
        }
    }

    @Test
    fun testJavaInKotlinChangeSignature() {
        val project = JpsTestProject(jpsResourcesPath, "withJava/javaUsedInKotlin/changeSignature", "2.4")

        project.build("build") {
            assertSuccessful()
            assertReportExists()
            assertCompiledKotlinSources("src/usage.kt")
            assertCompiledJavaSources("src/JavaClass.java")
        }

        project.modify()

        project.build("build") {
            assertSuccessful()
            assertCompiledKotlinSources("src/usage.kt")
            assertCompiledJavaSources("src/JavaClass.java")
        }
    }

    @Test
    fun testClassSignatureChangedAuto() {
        val project = JpsTestProject(jpsResourcesPath, "pureKotlin/classSignatureChanged", "2.4")

        project.performAndAssertBuildStages()
    }

    @Test
    fun testAnnotationListChanged() {
        val project = JpsTestProject(jpsResourcesPath, "classHierarchyAffected/annotationListChanged", "2.4")

        project.performAndAssertBuildStages()
    }

    @Test
    fun testClassBecameFinal() {
        val project = JpsTestProject(jpsResourcesPath, "classHierarchyAffected/classBecameFinal", "2.4")

        project.performAndAssertBuildStages()
    }

    @Test
    fun testSupertypesListChanged() {
        val project = JpsTestProject(jpsResourcesPath, "classHierarchyAffected/supertypesListChanged", "2.4")

        project.performAndAssertBuildStages()
    }
}
