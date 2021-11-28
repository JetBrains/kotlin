/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import java.io.File

/**
 * Creates fake NodeJS module directory from given composite [dependency].
 */
internal class CompositeNodeModuleBuilder(
    val srcDir: File,
    val cacheDir: File
) {
    var srcPackageJsonFile: File = srcDir

    fun rebuild(): File? {
        check(srcPackageJsonFile.isFile) {
            "Unable to read package.json of composite build"
        }

        val packageJson = fromSrcPackageJson(srcPackageJsonFile)!!

        // yarn requires semver
        packageJson.version = fixSemver(packageJson.version)

        val importedPackageDir = importedPackageDir(cacheDir, packageJson.name, packageJson.version)

        packageJson.main = srcDir.parentFile.resolve(packageJson.main!!)
            .relativeToOrNull(importedPackageDir)
            ?.path
            ?: throw IllegalStateException("Unable to link composite builds for Kotlin/JS which have different roots")

        return makeNodeModule(cacheDir, packageJson)
    }
}

private fun makeNodeModule(
    container: File,
    packageJson: PackageJson
): File {
    return makeNodeModule(container, packageJson) {}
}
