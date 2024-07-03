/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.parcelize

/**
 * When a type is annotated with this, any data classes contained inside it will be
 * serialized even if they are not [android.os.Parcelable].
 *
 * For a data class type to be serializable, its constructor and all properties must
 * be accessible from the class with this annotation. Data class serialization logic
 * has the lowest priority and will only be used if all other methods, except class loader
 * and [java.io.Serializable] based serialization, are not applicable. The serialization
 * format is equivalent to what would be generated if the data class itself was annotated
 * with [Parcelize].
 *
 * For example:
 *
 * ```
 * data class C(val a: Int, val b: String)
 *
 * @Parcelize
 * class P(val c: @DataClass C) : Parcelable
 * ```
 *
 * The produced parcels will be exactly the same as if `C` was declared as
 * `@Parcelize data class C(val a: Int, val b: String) : Parcelable`.
 */
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class DataClass
