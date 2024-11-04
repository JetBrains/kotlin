repositories {
    when (kotlinBuildProperties.getOrNull("attachedIntellijVersion")) {
        null -> {}
        "master" -> {
            maven { setUrl("https://www.jetbrains.com/intellij-repository/snapshots") }
        }

        else -> {
            kotlinBuildLocalRepo(project)
        }
    }

    //mirrorRepo?.let(::maven)

    maven(intellijRepo) {
        content {
            includeGroupByRegex("com\\.jetbrains\\.intellij(\\..+)?")
        }
    }

    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") {
        content {
            includeGroupByRegex("org\\.jetbrains\\.intellij\\.deps(\\..+)?")
            includeVersion("org.jetbrains.jps", "jps-javac-extension", "7")
            includeVersion("com.google.protobuf", "protobuf-parent", "3.24.4-jb.2")
            includeVersion("com.google.protobuf", "protobuf-java", "3.24.4-jb.2")
            includeVersion("com.google.protobuf", "protobuf-bom", "3.24.4-jb.2")
        }
    }

    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies") {
        content {
            includeModule("org.jetbrains.dukat", "dukat")
            includeModule("org.jetbrains.kotlin", "android-dx")
            includeModule("org.jetbrains.kotlin", "jcabi-aether")
            includeModule("org.jetbrains.kotlin", "kotlin-build-gradle-plugin")
            includeModule("org.jetbrains.kotlin", "protobuf-lite")
            includeModule("org.jetbrains.kotlin", "protobuf-relocated")
            includeModule("org.jetbrains.kotlinx", "kotlinx-metadata-klib")
        }
    }

    maven("https://download.jetbrains.com/teamcity-repository") {
        content {
            includeModule("org.jetbrains.teamcity", "serviceMessages")
            includeModule("org.jetbrains.teamcity.idea", "annotations")
        }
    }

    maven("https://dl.google.com/dl/android/maven2") {
        content {
            includeGroup("com.android.tools")
            includeGroup("com.android.tools.build")
            includeGroup("com.android.tools.layoutlib")
            includeGroup("com.android")
            includeGroup("androidx.test")
            includeGroup("androidx.annotation")
        }
    }

    mavenCentral()
}