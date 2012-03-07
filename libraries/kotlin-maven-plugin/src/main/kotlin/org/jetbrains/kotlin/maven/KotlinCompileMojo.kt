package org.jetbrains.kotlin.maven

import org.jfrog.maven.annomojo.annotations.*
import org.apache.maven.plugin.AbstractMojo

/**
 * Kotlin compiler mojo
 */
[MojoGoal("compile")]
[MojoPhase( "compile" )]
[MojoRequiresDependencyResolution( "compile" )]
class KotlinCompileMojo() : KotlinMojoSupport() {

    override fun execute() {
        println("===== Kotlin maven compiler!!! src = $src")
    }
}