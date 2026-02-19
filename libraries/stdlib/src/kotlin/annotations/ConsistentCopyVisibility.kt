/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * In previous versions of Kotlin, the generated 'copy' method of a data class always had public visibility,
 * even if the primary constructor was non-public. That exposed the non-public constructor of the data class.
 *
 * In future versions of Kotlin,
 * the generated 'copy' method of a data class will have the same visibility as the primary constructor.
 *
 * When you apply the [ConsistentCopyVisibility] annotation to a data class with non-public constructor:
 * 1. The generated 'copy' method will have the same visibility as the primary constructor.
 *    You enroll into the new behavior right away.
 * 2. You disable all the warnings/errors about the behavior change because they become unnecessary.
 *
 * The effect of '-Xconsistent-data-class-copy-visibility' flag is the same as applying [ConsistentCopyVisibility] to all data classes in the module.
 *
 * ## Deprecation timeline
 *
 * - **Phase 1.** Kotlin 2.0.20.
 *   The compiler warns about the behavior change on the data class declaration and on illegal 'copy' method usages (illegal usages are those that will become invisible by the end of the migration).
 *   It's possible to suppress the warning on the declaration with [ConsistentCopyVisibility]/[ExposedCopyVisibility] annotations or the '-Xconsistent-data-class-copy-visibility' flag.
 *   Illegal usages should start the migration.
 * - **Phase 2.** (Supposedly Kotlin 2.1 or Kotlin 2.2). The warnings turn into errors.
 *   Keep in mind that the compiler still generates public 'copy' under the hood. The binary signature is preserved.
 *   It's possible to suppress the error on the declaration with [ConsistentCopyVisibility]/[ExposedCopyVisibility] annotations or the '-Xconsistent-data-class-copy-visibility' flag.
 *   It's impossible to suppress the error on illegal 'copy' method usages.
 *   Illegal usages should migrate.
 * - **Phase 3.** (Supposedly Kotlin 2.2 or Kotlin 2.3). The default changes.
 *   Unless [ExposedCopyVisibility] is used, the generated 'copy' method has the same visibility as the primary constructor.
 *   The binary signature changes.
 *   The error on the declaration is no longer reported.
 *   '-Xconsistent-data-class-copy-visibility' compiler flag and [ConsistentCopyVisibility] annotation are now unnecessary.
 *
 * **Notes:**
 * - For the exact mapping of deprecation phases and Kotlin versions follow [KT-11914](https://youtrack.jetbrains.com/issue/KT-11914).
 * - You can turn the warning into an error by using the '-progressive'/'-Werror' compiler flag.
 *
 * ## Recommendation and alternatives
 *
 * - If you write new code or don't care about binary compatibility,
 *   it's recommended to use [ConsistentCopyVisibility] (or '-Xconsistent-data-class-copy-visibility' compiler flag) instead of the [ExposedCopyVisibility].
 * - When you use [ExposedCopyVisibility], it's also recommended to use '-Xconsistent-data-class-copy-visibility' in the same module.
 *   This way, old classes won't change their behavior, but new classes will have the correct visibility of the 'copy' method.
 * - Once all the illegal 'copy' method usages are migrated, please drop the [ExposedCopyVisibility] annotation.
 * - You can introduce your own 'copy'-like method alongside the generated 'copy',
 *   and migrate all the usages to the introduced method.
 * - You can rewrite your data class to regular Kotlin class.
 *   You need to manually implement all the data class generated methods.
 *
 * @see [ExposedCopyVisibility]
 * @see [KT-11914](https://youtrack.jetbrains.com/issue/KT-11914)
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@SinceKotlin("2.0")
public annotation class ConsistentCopyVisibility

/**
 * In previous versions of Kotlin, the generated 'copy' method of a data class always had public visibility,
 * even if the primary constructor was non-public. That exposed the non-public constructor of the data class.
 *
 * In future versions of Kotlin,
 * the generated 'copy' method of a data class will have the same visibility as the primary constructor.
 *
 * When you apply [ExposedCopyVisibility] annotation to your data class:
 * 1. You choose to keep the public **binary** visibility of the generated 'copy' method.
 *    But illegal usages of the 'copy' method will become inaccessible anyway.
 * 2. You suppress the warning/error about the behavior change only on the declaration.
 *    **Please note** that the warning/error on all the illegal **usages** of the 'copy' method **stays even if you use [ExposedCopyVisibility]!**
 *
 * ## Deprecation timeline
 *
 * - **Phase 1.** Kotlin 2.0.20.
 *   The compiler warns about the behavior change on the data class declaration and on illegal 'copy' method usages (illegal usages are those that will become invisible by the end of the migration).
 *   It's possible to suppress the warning on the declaration with [ConsistentCopyVisibility]/[ExposedCopyVisibility] annotations or the '-Xconsistent-data-class-copy-visibility' flag.
 *   Illegal usages should start the migration.
 * - **Phase 2.** (Supposedly Kotlin 2.1 or Kotlin 2.2). The warnings turn into errors.
 *   Keep in mind that the compiler still generates public 'copy' under the hood. The binary signature is preserved.
 *   It's possible to suppress the error on the declaration with [ConsistentCopyVisibility]/[ExposedCopyVisibility] annotations or the '-Xconsistent-data-class-copy-visibility' flag.
 *   It's impossible to suppress the error on illegal 'copy' method usages.
 *   Illegal usages should migrate.
 * - **Phase 3.** (Supposedly Kotlin 2.2 or Kotlin 2.3). The default changes.
 *   Unless [ExposedCopyVisibility] is used, the generated 'copy' method has the same visibility as the primary constructor.
 *   The binary signature changes.
 *   The error on the declaration is no longer reported.
 *   '-Xconsistent-data-class-copy-visibility' compiler flag and [ConsistentCopyVisibility] annotation are now unnecessary.
 *
 * **Notes:**
 * - For the exact mapping of deprecation phases and Kotlin versions follow [KT-11914](https://youtrack.jetbrains.com/issue/KT-11914).
 * - You can turn the warning into an error by using the '-progressive'/'-Werror' compiler flag.
 *
 * ## Recommendation and alternatives
 *
 * - If you write new code or don't care about binary compatibility,
 *   it's recommended to use [ConsistentCopyVisibility] (or '-Xconsistent-data-class-copy-visibility' compiler flag) instead of the [ExposedCopyVisibility].
 * - When you use [ExposedCopyVisibility], it's also recommended to use '-Xconsistent-data-class-copy-visibility' in the same module.
 *   This way, old classes won't change their behavior, but new classes will have the correct visibility of the 'copy' method.
 * - Once all the illegal 'copy' method usages are migrated, please drop the [ExposedCopyVisibility] annotation.
 * - You can introduce your own 'copy'-like method alongside the generated 'copy',
 *   and migrate all the usages to the introduced method.
 * - You can rewrite your data class to regular Kotlin class.
 *   You need to manually implement all the data class generated methods.
 *
 * @see [ConsistentCopyVisibility]
 * @see [KT-11914](https://youtrack.jetbrains.com/issue/KT-11914)
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@SinceKotlin("2.0")
public annotation class ExposedCopyVisibility
