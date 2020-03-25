/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import com.google.gson.GsonBuilder
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import java.io.File

/**
 * Creates fake NodeJS module directory from given composite [dependency].
 */
internal class CompositeNodeModuleBuilder(
    val project: Project,
    val dependency: ResolvedDependency,
    val srcDir: File,
    val cache: CompositeNodeModulesCache
) {
    var srcPackageJsonFile: File = srcDir

    fun rebuild(): File? {
        check(srcPackageJsonFile.isFile) {
            "Unable to read package.json of composite build"
        }

        val packageJson = fromSrcPackageJson(srcPackageJsonFile)!!

        packageJson.main = srcDir.parentFile.resolve(packageJson.main!!).canonicalPath

        // yarn requires semver
        packageJson.version = fixSemver(packageJson.version)

        return makeNodeModule2(cache.dir, packageJson)
    }
}

fun makeNodeModule2(
    container: File,
    packageJson: PackageJson
): File {
    val dir = importedPackageDir(container, packageJson.name, packageJson.version)

    if (dir.exists()) dir.deleteRecursively()

    check(dir.mkdirs()) {
        "Cannot create directory: $dir"
    }

    val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    dir.resolve("package.json").writer().use {
        gson.toJson(packageJson, it)
    }

    return dir
}
