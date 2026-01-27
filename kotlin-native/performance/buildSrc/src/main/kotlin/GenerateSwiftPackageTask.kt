package org.jetbrains.kotlin

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import javax.inject.Inject

private const val PACKAGE_FILENAME = "Package.swift"

/**
 * Generate `Package.swift` for a given XCFramework
 */
open class GenerateSwiftPackageTask @Inject constructor(
        objectFactory: ObjectFactory,
) : DefaultTask() {
    /**
     * Path to the XCFramework for which to generate the package.
     *
     * The name of the package will be derived from the framework's name.
     */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE) // manual relative path calculation
    val xcFramework: DirectoryProperty = objectFactory.directoryProperty()

    /**
     * Directory where `Package.swift` will be located
     */
    @get:Internal("used for relative path calculation and as the parent directory for the output file")
    val packageDirectory: DirectoryProperty = objectFactory.directoryProperty()

    /**
     * Package file location.
     *
     * NOTE: this is not editable, use [packageDirectory] to select the alternative output directory; file name
     * is always fixed to be `Package.swift`
     */
    @get:OutputFile
    val packageFile = packageDirectory.file(PACKAGE_FILENAME)

    @get:Input
    protected val xcFrameworkRelativePath = packageDirectory.zip(xcFramework) { root, dir ->
        dir.asFile.toRelativeString(root.asFile)
    }

    /**
     * The minimum Swift compiler requirement.
     *
     * See [the official documentation](https://docs.swift.org/swiftpm/documentation/packagemanagerdocs/settingswifttoolsversion)
     * for details.
     */
    @get:Input
    val swiftToolsVersion: Property<String> = objectFactory.property(String::class.java)

    @TaskAction
    fun run() {
        val packageName = xcFramework.asFile.get().nameWithoutExtension
        val productName = packageName.capitalized
        val targetName = packageName.capitalized
        val text = """
            // swift-tools-version: ${swiftToolsVersion.get()}
            import PackageDescription

            let package = Package(
                name: "$packageName",
                products: [
                    .library(name: "$productName", targets: ["$targetName"])
                ],
                targets: [
                    .binaryTarget(
                        name: "$targetName",
                        path: "${xcFrameworkRelativePath.get()}"
                    )
               ]
            )
        """.trimIndent()
        packageFile.get().asFile.writeText(text)
    }
}