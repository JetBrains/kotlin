package kotlin

import kotlin.annotation.AnnotationTarget.*

/**
 * Kotlin reports a compilation error for cases when non-final `expect class` and its counterpart `actual class` have different set of
 * non-private members (e.g. when a new member declared in `actual class` that wasn't presented in its `expect class`, or when
 * modality is different).
 *
 * By marking `actual class` with this annotation, you suppress the compilation error mentioned above.
 *
 * ## Safety Risks
 *
 * Using this annotation causes undefined behaviour, it's not known what can break because of that. Be prepared that at some point this
 * annotation might become deprecated, and you'd need to migrate your code.
 *
 * If you use this annotation and can't migrate, consider describing your use cases in
 * [KT-22841](https://youtrack.jetbrains.com/issue/KT-22841) comments.
 *
 * ## Migration
 *
 * Please consider the following alternatives:
 * - Make members in `expect class` and `actual class` the same
 *     - If the member was declared only in `actual class` then you'd need to declare it in the `expect class` as well
 *     - If the member is already declared in the `expect class` then make sure that the member is the same in the `actual class` (modality is
 *       the same, visibility is the same, return type is the same, etc.)
 * - Make the `expect class` `final`. You might need to rewrite your code to use composition instead of inheritance.
 * - Mark the members under the question as `private`
 *
 * [AllowDifferentMembersInActual] is supposed to be used for the transition period.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(CLASS, TYPEALIAS)
@MustBeDocumented
@ExperimentalMultiplatform
@SinceKotlin("1.9")
public annotation class AllowDifferentMembersInActual
