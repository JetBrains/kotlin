plugins {
    kotlin("jvm")
    idea
}

val main by sourceSets.getting {
    dependencies {
        api(kotlinStdlib())
        implementation(project(":native:hair:sym"))
        implementation(project(":native:hair:utils"))
        implementation(project(":native:hair:ir:core"))
    }
    kotlin.srcDir("src/main/generated")
}

idea {
    module {
        generatedSourceDirs.add(file("src/main/generated"))
    }
}
