import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

operator fun KotlinSourceSet.invoke(builder: SourceSetHierarchyBuilder.() -> Unit): KotlinSourceSet {
    SourceSetHierarchyBuilder(this).builder()
    return this
}

class SourceSetHierarchyBuilder(private val node: KotlinSourceSet) {
    operator fun KotlinSourceSet.unaryMinus() = this.dependsOn(node)
}

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    linuxX64()
    linuxArm64()
    @Suppress("DEPRECATION_ERROR")
    linuxArm32Hfp()

    val commonMain by sourceSets.getting
    val nativeMain by sourceSets.creating
    val linuxX64Main by sourceSets.getting
    val linuxArm64Main by sourceSets.getting
    val linuxArm32HfpMain by sourceSets.getting

    nativeMain.dependsOn(commonMain)
    linuxX64Main.dependsOn(nativeMain)
    linuxArm64Main.dependsOn(nativeMain)
    linuxArm32HfpMain.dependsOn(nativeMain)

    val commonTest by sourceSets.getting
    val nativeTest by sourceSets.creating
    val linuxX64Test by sourceSets.getting
    val linuxArm64Test by sourceSets.getting

    nativeTest.dependsOn(commonTest)
    linuxX64Test.dependsOn(nativeTest)
    linuxArm64Test.dependsOn(nativeTest)
    /* NOTE: linuxArm32HfpTest does not depend on nativeTest */

    targets.withType<KotlinNativeTarget>().forEach { target ->
        target.compilations.getByName("main").cinterops.create("dummy") {
            headers("libs/dummy.h")
        }
    }
}

