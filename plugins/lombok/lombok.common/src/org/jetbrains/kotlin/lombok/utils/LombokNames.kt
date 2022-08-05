/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.utils

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

object LombokNames {

    val ACCESSORS = FqName("lombok.experimental.Accessors")
    val GETTER = FqName("lombok.Getter")
    val SETTER = FqName("lombok.Setter")
    val WITH = FqName("lombok.With")
    val DATA = FqName("lombok.Data")
    val VALUE = FqName("lombok.Value")
    val NO_ARGS_CONSTRUCTOR = FqName("lombok.NoArgsConstructor")
    val ALL_ARGS_CONSTRUCTOR = FqName("lombok.AllArgsConstructor")
    val REQUIRED_ARGS_CONSTRUCTOR = FqName("lombok.RequiredArgsConstructor")
    val BUILDER = FqName("lombok.Builder")
    val SINGULAR = FqName("lombok.Singular")


    val ACCESSORS_ID = ClassId.topLevel(ACCESSORS)
    val GETTER_ID = ClassId.topLevel(GETTER)
    val SETTER_ID = ClassId.topLevel(SETTER)
    val WITH_ID = ClassId.topLevel(WITH)
    val DATA_ID = ClassId.topLevel(DATA)
    val VALUE_ID = ClassId.topLevel(VALUE)
    val NO_ARGS_CONSTRUCTOR_ID = ClassId.topLevel(NO_ARGS_CONSTRUCTOR)
    val ALL_ARGS_CONSTRUCTOR_ID = ClassId.topLevel(ALL_ARGS_CONSTRUCTOR)
    val REQUIRED_ARGS_CONSTRUCTOR_ID = ClassId.topLevel(REQUIRED_ARGS_CONSTRUCTOR)

    //taken from idea lombok plugin
    val NON_NULL_ANNOTATIONS = listOf(
        "androidx.annotation.NonNull",
        "android.support.annotation.NonNull",
        "com.sun.istack.internal.NotNull",
        "edu.umd.cs.findbugs.annotations.NonNull",
        "javax.annotation.Nonnull",
        "lombok.NonNull",
        "org.checkerframework.checker.nullness.qual.NonNull",
        "org.eclipse.jdt.annotation.NonNull",
        "org.eclipse.jgit.annotations.NonNull",
        "org.jetbrains.annotations.NotNull",
        "org.jmlspecs.annotation.NonNull",
        "org.netbeans.api.annotations.common.NonNull",
        "org.springframework.lang.NonNull"
    ).map { FqName(it) }.toSet()

    private val SUPPORTED_JAVA_COLLECTIONS = setOf(
        "java.util.Iterable",
        "java.util.Collection",
        "java.util.List",
        "java.util.Set",
        "java.util.SortedSet",
        "java.util.NavigableSet",
    )

    private val SUPPORTED_JAVA_MAPS = setOf(
        "java.util.Map",
        "java.util.SortedMap",
        "java.util.NavigableMap",
    )

    private val SUPPORTED_KOTLIN_COLLECTIONS = setOf(
        "kotlin.collections.Iterable",
        "kotlin.collections.MutableIterable",
        "kotlin.collections.Collection",
        "kotlin.collections.MutableCollection",
        "kotlin.collections.List",
        "kotlin.collections.MutableList",
        "kotlin.collections.Set",
        "kotlin.collections.MutableSet",
    )

    private val SUPPORTED_KOTLIN_MAPS = setOf(
        "kotlin.collections.Map",
        "kotlin.collections.MutableMap",
    )

    private val SUPPORTED_COLLECTIONS = SUPPORTED_JAVA_COLLECTIONS + SUPPORTED_KOTLIN_COLLECTIONS
    private val SUPPORTED_MAPS = SUPPORTED_JAVA_MAPS + SUPPORTED_KOTLIN_MAPS

    private val SUPPORTED_COLLECTIONS_WITH_GUAVA = SUPPORTED_COLLECTIONS + setOf(
        "com.google.common.collect.ImmutableCollection",
        "com.google.common.collect.ImmutableList",
        "com.google.common.collect.ImmutableSet",
        "com.google.common.collect.ImmutableSortedSet",
    )

    private val SUPPORTED_MAPS_WITH_GUAVA = SUPPORTED_MAPS + setOf(
        "com.google.common.collect.ImmutableMap",
        "com.google.common.collect.ImmutableBiMap",
        "com.google.common.collect.ImmutableSortedMap",
        "com.google.common.collect.ImmutableTable",
    )

    fun getSupportedCollectionsForSingular(includeGuava: Boolean): Set<String> {
        return if (includeGuava) {
            SUPPORTED_COLLECTIONS_WITH_GUAVA
        } else {
            SUPPORTED_COLLECTIONS
        }
    }

    fun getSupportedMapsForSingular(includeGuava: Boolean): Set<String> {
        return if (includeGuava) {
            SUPPORTED_MAPS_WITH_GUAVA
        } else {
            SUPPORTED_MAPS
        }
    }
}
