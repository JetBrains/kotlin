/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.serialization.js

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object KotlinJavascriptSerializedResourcePaths {
    private val CLASSES_FILE_EXTENSION = "kotlin_classes"
    private val STRING_TABLE_FILE_EXTENSION = "kotlin_string_table"

    fun getClassesInPackageFilePath(fqName: FqName): String =
            fqName.toPath().withSepIfNotEmpty() + shortName(fqName) + "." + CLASSES_FILE_EXTENSION


    fun getClassMetadataPath(classId: ClassId): String {
        return classId.packageFqName.toPath().withSepIfNotEmpty() + classId.relativeClassName.asString() +
               "." + KotlinJavascriptSerializationUtil.CLASS_METADATA_FILE_EXTENSION
    }

    fun getPackageFilePath(fqName: FqName): String =
            getPackageClassFqName(fqName).toPath() + "." + KotlinJavascriptSerializationUtil.CLASS_METADATA_FILE_EXTENSION

    fun getStringTableFilePath(fqName: FqName): String =
            fqName.toPath().withSepIfNotEmpty() + shortName(fqName) + "." + STRING_TABLE_FILE_EXTENSION

    private fun FqName.toPath() = this.asString().replace('.', '/')

    private fun String.withSepIfNotEmpty() = if (this.isEmpty()) this else this + "/"

    private fun shortName(fqName: FqName): String =
            if (fqName.isRoot) "default-package" else fqName.shortName().asString()

}

private val PACKAGE_CLASS_NAME_SUFFIX: String = "Package"
private val DEFAULT_PACKAGE_CLASS_NAME: String = "_Default" + PACKAGE_CLASS_NAME_SUFFIX
private val DEFAULT_PACKAGE_METAFILE_NAME: String = DEFAULT_PACKAGE_CLASS_NAME + "." + KotlinJavascriptSerializationUtil.CLASS_METADATA_FILE_EXTENSION

fun FqName.isPackageClassFqName(): Boolean = !this.isRoot && getPackageClassFqName(this.parent()) == this

fun isDefaultPackageMetafile(fileName: String): Boolean = fileName == DEFAULT_PACKAGE_METAFILE_NAME

fun isPackageMetadataFile(fileName: String): Boolean =
        KotlinJavascriptSerializedResourcePaths.getPackageFilePath(getPackageFqName(fileName)) == fileName

fun isStringTableFile(fileName: String): Boolean =
        KotlinJavascriptSerializedResourcePaths.getStringTableFilePath(getPackageFqName(fileName)) == fileName

fun isClassesInPackageFile(fileName: String): Boolean =
        KotlinJavascriptSerializedResourcePaths.getClassesInPackageFilePath(getPackageFqName(fileName)) == fileName

private fun getPackageFqName(fileName: String): FqName = FqName(getPackageName(fileName))

private fun getPackageName(filePath: String): String =
        if (filePath.indexOf('/') >= 0) filePath.substringBeforeLast('/').replace('/', '.') else ""

private fun getPackageClassFqName(packageFQN: FqName): FqName {
    return packageFQN.child(Name.identifier(getPackageClassName(packageFQN)))
}

private fun getPackageClassName(packageFQN: FqName): String {
    return if (packageFQN.isRoot) DEFAULT_PACKAGE_CLASS_NAME else capitalizeNonEmptyString(packageFQN.shortName().asString()) + PACKAGE_CLASS_NAME_SUFFIX
}

private fun capitalizeNonEmptyString(s: String): String {
    return if (s[0].isUpperCase()) s else s[0].toUpperCase() + s.substring(1)
}
