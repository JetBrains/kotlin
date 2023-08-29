plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm()
    linuxX64()
    linuxArm64()


    /*
     Create custom 'refines' edges.
     The goal here is to have refines edges (dependsOn) listed in a way
     that linuxMain will see the 'commonMain' edge first
     */

    sourceSets.nativeMain.get().dependsOn(sourceSets.commonMain.get())

    sourceSets.linuxMain.get().dependsOn(sourceSets.commonMain.get())
    sourceSets.linuxMain.get().dependsOn(sourceSets.nativeMain.get())

    sourceSets.getByName("linuxX64Main").dependsOn(sourceSets.linuxMain.get())
    sourceSets.getByName("linuxArm64Main").dependsOn(sourceSets.linuxMain.get())
}
