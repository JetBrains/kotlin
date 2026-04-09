plugins {
    kotlin("jvm")
}

description = "Common part of Swift and Objective-C exports."

val caffeine3: Configuration by configurations.creating {
    isTransitive = false
}

dependencies {
    compileOnly(kotlinStdlib())

    api(project(":analysis:analysis-api"))
    api(project(":core:compiler.common"))
    api(project(":compiler:psi:psi-api"))

    caffeine3(libs.caffeine3)
}

kotlin {
    explicitApi()
}

sourceSets {
    "main" { projectDefault() }
}

/*
Pack caffeine 3.x classes into META-INF/versions/25,
so that they shadow caffeine 2.x classes when running on JDK 25+.
Caffeine 2.x relies on sun.misc.Unsafe which is restricted starting from JDK 25.

See https://openjdk.org/jeps/238.
*/
tasks.named<Jar>("jar") {
    into("META-INF/versions/25") {
        from(caffeine3.map { zipTree(it) })
        exclude("META-INF/**")
    }
    manifest {
        attributes("Multi-Release" to true)
    }
}
