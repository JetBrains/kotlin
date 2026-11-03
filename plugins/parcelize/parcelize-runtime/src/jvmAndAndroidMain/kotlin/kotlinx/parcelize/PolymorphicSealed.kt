/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.parcelize

/**
 * When paired with [Parcelize], instructs the Kotlin compiler to generate a polymorphic
 * `CREATOR` ([android.os.Parcelable.Creator]) on the annotated sealed class or sealed interface,
 * enabling direct deserialization of the sealed hierarchy without reflection.
 *
 * ### Default Behavior of [Parcelize] on Sealed Classes (Without [PolymorphicSealed]):
 * By default, annotating a `sealed class` or `sealed interface` with [Parcelize] acts solely as
 * an annotation shorthand that propagates `@Parcelize` to all concrete subclasses.
 * Under this default behavior:
 * - A `CREATOR` is generated on each concrete subclass, but **not** on the sealed parent class.
 * - Calling `parcelableCreator<SealedClass>()` is not supported.
 * - Polymorphic serialization relies on Android's reflection-based `Parcel.writeParcelable()` / `Parcel.readParcelable()`,
 *   which writes the runtime class name as a string and retrieves subclass `CREATOR`s reflectively.
 *
 * ### Behavior with [PolymorphicSealed]:
 * When `@PolymorphicSealed` is added to a sealed class alongside `@Parcelize`:
 * - **Polymorphic CREATOR**: Generates a static `CREATOR: Parcelable.Creator<SealedClass>` on the sealed base class.
 *    In `createFromParcel(parcel)`, it reads an integer discriminator tag from the parcel and dispatches to the
 *    matching subclass factory without reflection.
 * - **Tagged writeToParcel**: Generates `writeToParcel(parcel, flags)` on all concrete subclasses that writes the
 *    integer discriminator tag immediately before serializing the subclass properties.
 *
 * ### Example Usage:
 * ```kotlin
 * @Parcelize
 * @PolymorphicSealed
 * sealed class ServiceState : Parcelable {
 *     data object Down : ServiceState()
 *     data object Initializing : ServiceState()
 *     data class Active(val key: String) : ServiceState()
 * }
 * ```
 *
 * ### Constraints:
 * - Must be applied to a `sealed class` or `sealed interface` that implements [android.os.Parcelable].
 * - Must be paired with [Parcelize].
 * - **Direct Enclosure**: All subclasses must be declared directly inside the body of the annotated sealed class or interface. External (top-level, in other files, or in other containers) subclasses are not supported.
 * - **Flat Concrete Hierarchy**: Intermediate `sealed`, `abstract`, or `open` subclasses are not supported. Subclasses must be final, concrete types (`class`, `data class`, `object`, `data object`).
 * - **Single Polymorphic Hierarchy**: A class cannot implement or extend multiple `@PolymorphicSealed` classes or interfaces.
 *
 * @see Parcelize
 * @see ParcelTag
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@Experimental
annotation class PolymorphicSealed

/**
 * Explicitly assigns a stable integer discriminator tag to a subclass within a [PolymorphicSealed] hierarchy.
 *
 * This tag is written to the [android.os.Parcel] ahead of the subclass properties and is used by the sealed parent's
 * `CREATOR` to determine which concrete subclass to instantiate during deserialization.
 *
 * ### Example Usage:
 * ```kotlin
 * @Parcelize
 * @PolymorphicSealed
 * sealed class ServiceState : Parcelable {
 *     @ParcelTag(0) data object Down : ServiceState()
 *     @ParcelTag(1) data object Initializing : ServiceState()
 *     @ParcelTag(2) data class Active(val key: String) : ServiceState()
 * }
 * ```
 *
 * ### Tag Assignment Rules:
 * - If `@ParcelTag` is specified on a subclass, the given [tag] value is used (negative values and constant expressions are supported).
 * - **All-or-Nothing Rule**: If any subclass in a [PolymorphicSealed] hierarchy is annotated with `@ParcelTag`,
 *   **all** subclasses in that hierarchy must be annotated with `@ParcelTag`.
 * - If `@ParcelTag` is omitted on all subclasses, the compiler auto-assigns integer tags based on subclass declaration order (0, 1, 2...).
 * - All tags within the same sealed hierarchy must be unique.
 * - `@ParcelTag` is only applicable to direct subclasses declared within the body of a `@PolymorphicSealed` class or interface.
 *
 * @property tag The unique integer discriminator identifying this subclass within its sealed hierarchy.
 * @see PolymorphicSealed
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@Experimental
annotation class ParcelTag(val tag: Int)
