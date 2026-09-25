/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.gradle

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * The `dataframe { }` block, used to configure the Kotlin DataFrame compiler plugin.
 */
abstract class DataFrameExtension @Inject constructor(
    objectFactory: ObjectFactory,
) {
    /**
     * Experimental: adds every `@DataSchema` interface of the module that a data frame's inferred schema is compatible
     * with as a supertype of the schema marker the plugin generates for it. `DataFrame` and `DataRow` are covariant in
     * their schema parameter, so an extension declared on such an interface becomes applicable without an explicit
     * `cast`:
     *
     * ```kotlin
     * @DataSchema
     * interface UserLike {
     *     val name: String
     *     val age: Int
     * }
     *
     * fun DataFrame<UserLike>.nameAndAge(): String = rows().joinToString { "${it.name} is ${it.age} years old" }
     *
     * // `df` has an extra `favoriteColor` column but is still `UserLike`
     * val df = dataFrameOf(
     *     "name" to columnOf("Alice"),
     *     "age" to columnOf(12),
     *     "favoriteColor" to columnOf("blue"),
     * )
     * df.nameAndAge()
     * ```
     *
     * Only interfaces of the module being compiled are considered, and only the final marker of a call gets the
     * supertypes. Disabled by default: it affects call resolution, so it is opt-in until the design settles.
     */
    val polymorphicDataSchemas: Property<Boolean> =
        objectFactory.property(Boolean::class.java).convention(false)
}
