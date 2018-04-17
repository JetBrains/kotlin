/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.*
import kotlin.reflect.KClass

/**
 * Signals that the annotated annotation class is a marker of an experimental API. Any declaration annotated with that marker is thus
 * considered an experimental declaration and its call sites must accept the experimental aspect of it either by using [UseExperimental],
 * or by being annotated with that marker themselves, effectively causing further propagation of that experimental aspect.
 */
@Target(ANNOTATION_CLASS)
@Retention(BINARY)
@SinceKotlin("1.3")
@Suppress("ANNOTATION_CLASS_WITH_BODY", "ANNOTATION_CLASS_MEMBER", "ReplaceArrayOfWithLiteral")
annotation class Experimental(
    val level: Level = Level.ERROR,
    val changesMayBreak: Array<Impact> = arrayOf(Impact.COMPILATION, Impact.LINKAGE, Impact.RUNTIME) // arrayOf, not [] because of KT-22578
) {
    /**
     * Severity of the diagnostic that should be reported on usages of experimental API which did not explicitly accept the experimental aspect
     * of that API either by using [UseExperimental] or by being annotated with the corresponding marker annotation.
     */
    enum class Level {
        /** Specifies that a warning should be reported on incorrect usages of this experimental API. */
        WARNING,
        /** Specifies that an error should be reported on incorrect usages of this experimental API. */
        ERROR,
    }

    /**
     * Impact of the experimental API specifies what aspects may break after that API is changed.
     */
    enum class Impact {
        /**
         * Signifies that changes in this experimental API can cause compilation errors or warnings in the client code.
         *
         * Non-signature usages (inside a function body, variable initializer, default argument value, etc.) of compilation-affecting
         * experimental API are allowed either if the containing declaration is annotated with the experimental annotation marker
         * and thus propagates the experimental aspect to its clients, or if there's a [UseExperimental] annotation entry with
         * the corresponding annotation marker somewhere above that usage in the parse tree. Signature usages of compilation-affecting
         * experimental API always require propagation (as long as the experimental API is declared in another module).
         */
        COMPILATION,
        /**
         * Signifies that changes in this experimental API can cause linkage errors, i.e. exceptions at runtime
         * if the client code was not recompiled after the change.
         *
         * Any usage of a linkage-affecting experimental API requires its containing declaration to be annotated with
         * the corresponding annotation marker (except non-signature usages in annotation marker's module).
         */
        LINKAGE,
        /**
         * Signifies that changes in this experimental API can cause changes in runtime behavior, including exceptions at runtime.
         *
         * Any usage of a runtime-affecting experimental API requires its containing declaration to be annotated with
         * the corresponding annotation marker (except non-signature usages in the annotation marker's module).
         */
        RUNTIME,
    }
}

/**
 * Allows to use experimental API denoted by the given markers in the annotated file, declaration, or expression. Each of the given markers
 * must be an annotation class, whose impact ([Experimental.changesMayBreak]) is [Experimental.Impact.COMPILATION].
 * Any other given annotation classes have no effect and are ignored.
 *
 * Only allows non-signature usages of the experimental API, i.e. inside a function body, variable initializer, default argument value, etc.
 * (Usages in declaration signatures must be propagated by annotating the affected signature with the marker annotation itself.)
 */
@Target(CLASS, PROPERTY, LOCAL_VARIABLE, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, EXPRESSION, FILE)
@Retention(SOURCE)
@SinceKotlin("1.3")
annotation class UseExperimental(
    vararg val markerClass: KClass<out Annotation>
)
