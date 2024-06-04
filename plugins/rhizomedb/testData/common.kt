package com.jetbrains.rhizomedb

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class GeneratedEntityType(vararg val mixins: KClass<out Mixin<*>>)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class EntityConstructor

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class ValueAttribute(val flags: Indexing = Indexing.NOT_INDEXED)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class TransientAttribute(val flags: Indexing = Indexing.NOT_INDEXED)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class RefAttribute(vararg val flags: RefFlags)

inline fun <T> assertEquals(expected: T, actual: T, report: (String) -> Unit) {
    if (expected != actual) {
        report("$expected != $actual")
    }
}

fun changeBox(body: ChangeScope.() -> String): String {
    var res = "<BODY FAILED>"
    DB.empty().change { res = body() }
    return res
}
