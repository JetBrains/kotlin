package org.jetbrains.kotlin.maven

import org.jfrog.maven.annomojo.annotations.*

/**
 * Base class for Kotlin compiler plugins
 */
import org.apache.maven.plugin.AbstractMojo
import java.lang.String

abstract class KotlinMojoSupport : AbstractMojo() {

    [MojoParameter(required = false)]
    public var src: String? = null
}