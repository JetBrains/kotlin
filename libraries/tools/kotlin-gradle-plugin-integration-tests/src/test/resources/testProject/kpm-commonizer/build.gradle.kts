import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*

plugins {
    kotlin("multiplatform.pm20")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    main {
        val jvm by fragments.creating(GradleKpmJvmVariant::class)
        val targetA by fragments.creating(<targetA>::class)
        val targetB by fragments.creating(<targetB>::class)
        val native by fragments.creating

        targetA.refines(native)
        targetB.refines(native)
    }
}
