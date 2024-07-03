/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.parcelize

/**
 * When a type is annotated with this annotation, any data classes contained inside it
 * (as well as the type itself, if it is one) will be serialized even if they are not
 * [android.os.Parcelable].
 *
 * For a data class type to be serializable, its constructor and all properties must
 * be accessible from the class that contains this annotation, and all properties must be
 * of supported types. The serialization format is equivalent to what would be generated
 * if the data class itself was annotated with [Parcelize].
 *
 * For technical reasons, this annotation overrides support for [java.io.Serializable]:
 * if a data class implements it, it must still obey the above requirements and will use
 * the data class specific serialization format, not [android.os.Parcel.writeSerializable].
 * Non-data classes are not affected by this annotation.
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
