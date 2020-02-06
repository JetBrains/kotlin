/*
 * Copyright 2016-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package kotlinx.validation.build

import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*

val NamedDomainObjectProvider<SourceSet>.kotlin: SourceDirectorySet
    get() = get().withConvention(KotlinSourceSet::class) { kotlin }

val NamedDomainObjectProvider<SourceSet>.resources: SourceDirectorySet
    get() = get().resources

var SourceDirectorySet.dir: String
    get() = srcDirs().joinToString(",")
    set(value) { setSrcDirs(value.split(",")) }
