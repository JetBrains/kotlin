/**
 * The Build Tools API implementation with its whole run-time classpath, for the Kotlin JPS builder of the IDE.
 *
 * The IDE loads the implementation from a directory of jars, so the published jar holds these jars at its root. The
 * `btaImplHomeElements` configuration gives the same directory to the JPS tests of this repository.
 */
plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
}

val btaImpl = configurations.dependencyScope("btaImpl")
val btaImplClasspath = configurations.resolvable("btaImplClasspath") {
    extendsFrom(btaImpl.get())
}

dependencies {
    btaImpl(project(":compiler:build-tools:kotlin-build-tools-impl"))
}

val btaImplHome = tasks.register<Sync>("btaImplHome") {
    description = "Collects the Build Tools API implementation and its run-time classpath into one directory."
    from(btaImplClasspath)
    into(layout.buildDirectory.dir("btaImplHome"))
}

configurations.consumable("btaImplHomeElements") {
    outgoing.artifact(btaImplHome.map { it.destinationDir }) {
        builtBy(btaImplHome)
    }
}

idePluginPublishingLatch {
    apply<JavaPlugin>()

    publish()

    tasks.named<Jar>("jar") {
        from(btaImplClasspath)
    }
}
