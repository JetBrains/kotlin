/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * Marks a regular class that has value semantics and will be converted to a `value class` in the future to preventively discourage its referential usages.
 *
 * A class annotated with [WillBecomeValue]:
 * 1. **Must not be relied upon for identity.** Reference equality (`===`), identity hash codes,
 *    and synchronization on instances are considered undefined behavior.
 * 2. **Must be shallow-immutable.** Mutating state through shared references undermines
 *    the value semantics the annotation promises.
 * 3. **Is a candidate for future migration to a `value class`,** so the annotation is expected
 *    to be dropped once the migration is complete.
 *
 * The compiler applies every declaration check of a `value class` to the annotated class,
 * reporting errors just as it would for a real `value class`. Identity-sensitive **usages**,
 * however, are only reported as warnings outside of the annotated class itself,
 * which gives downstream users time to migrate before the class actually becomes a `value class`.
 *
 * A final annotated class also has to override `equals`, `hashCode` and `toString`, because the identity-based
 * implementations inherited from [Any] would silently turn structural once the class becomes a `value class`.
 *
 * The annotation can be applied to final classes, to `abstract`/`sealed` classes intended as base types,
 * and to object declarations. It cannot be applied to `value class`es, interfaces, enums, or `open` classes.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@SinceKotlin("2.5")
public annotation class WillBecomeValue
