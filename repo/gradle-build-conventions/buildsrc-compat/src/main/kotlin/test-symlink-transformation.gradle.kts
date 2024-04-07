import java.io.Serializable
import kotlin.io.path.createSymbolicLinkPointingTo

/**
 * This precompiled script plugin defines an artifact transformation that creates a symlink for each artifact.
 *
 * This should be used for testing logic against whitespaces and other escapable characters in classpath.
 *
 * Example usage:
 * ```
 * plugins {
 *     id("test-symlink-transformation")
 * }
 *
 * testSymlinkTransformation {
 *     rename {
 *         "myPrefix with spaces/$it" // if you need to override the default renaming
 *     }
 * }
 *
 * configurations {
 *     testRuntimeClasspath {
 *         testSymlinkTransformation.resolveAgainstSymlinkedArtifacts(this)
 *     }
 * }
 * ```
 */

plugins {
    `java-base`
}

fun interface NameTransformer : Serializable {
    fun transform(input: String): String
}

val transformationExtension = extensions.create<TestSymlinkTransformationExtension>("testSymlinkTransformation")

transformationExtension.nameTransformer.convention {
    "directory with \$trange c#aracters/$it"
}

dependencies {
    attributesSchema {
        attribute(TestSymlinkTransformationExtension.symlinked)
    }
    registerTransform(TestSymlinkTransformation::class) {
        from.attribute(TestSymlinkTransformationExtension.symlinked, false)
        to.attribute(TestSymlinkTransformationExtension.symlinked, true)
        parameters.nameTransformer.set(transformationExtension.nameTransformer)
    }
    artifactTypes.maybeCreate("jar").attributes.attribute(TestSymlinkTransformationExtension.symlinked, false)
}

internal abstract class TestSymlinkTransformation : TransformAction<TestSymlinkTransformation.Parameters> {
    interface Parameters : TransformParameters {
        @get:Input
        val nameTransformer: Property<NameTransformer>
    }

    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        val transformedName = parameters.nameTransformer.get().transform(input.name)
        val link = outputs.file(transformedName).toPath()
        link.createSymbolicLinkPointingTo(input.toPath())
    }
}

abstract class TestSymlinkTransformationExtension {
    internal abstract val nameTransformer: Property<NameTransformer>

    fun rename(transformer: NameTransformer) {
        nameTransformer.set(transformer)
    }

    fun resolveAgainstSymlinkedArtifacts(configuration: Configuration) {
        configuration.attributes.attribute(symlinked, true)
    }

    companion object {
        internal val symlinked = Attribute.of("kotlin.build.test.symlinked", Boolean::class.javaObjectType)
    }
}