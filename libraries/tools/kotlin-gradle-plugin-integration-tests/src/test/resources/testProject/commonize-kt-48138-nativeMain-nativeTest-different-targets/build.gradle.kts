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
    mingwX64()

    val commonMain = sourceSets.getByName("commonMain")
    val nativeMain = sourceSets.create("nativeMain")
    val linuxX64Main = sourceSets.getByName("linuxX64Main")
    val linuxArm64Main = sourceSets.getByName("linuxArm64Main")
    val mingwX64Main = sourceSets.getByName("mingwX64Main")

    nativeMain.dependsOn(commonMain)
    linuxX64Main.dependsOn(nativeMain)
    linuxArm64Main.dependsOn(nativeMain)
    mingwX64Main.dependsOn(nativeMain)

    val commonTest = sourceSets.getByName("commonTest")
    val nativeTest = sourceSets.create("nativeTest")
    val linuxX64Test = sourceSets.getByName("linuxX64Test")
    val linuxArm64Test = sourceSets.getByName("linuxArm64Test")
    val mingwX64Test = sourceSets.getByName("mingwX64Test")

    nativeTest.dependsOn(commonTest)
    linuxX64Test.dependsOn(nativeTest)
    linuxArm64Test.dependsOn(nativeTest)
    /* NOTE: mingwX64Test does not depend on nativeTest */

    targets.withType<KotlinNativeTarget>().forEach { target ->
        target.compilations.getByName("main").cinterops.create("dummy") {
            headers("libs/dummy.h")
        }
    }
}
