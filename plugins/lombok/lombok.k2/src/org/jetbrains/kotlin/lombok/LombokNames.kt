/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

object LombokNames {
    val LOMBOK = FqName("lombok")
    val GUAVA_COLLECT_PACKAGE = FqName("com.google.common.collect")

    val ACCESSORS = FqName("lombok.experimental.Accessors")
    val GETTER = FqName("lombok.Getter")
    val SETTER = FqName("lombok.Setter")
    val WITH = FqName("lombok.With")
    val DATA = FqName("lombok.Data")
    val VALUE = FqName("lombok.Value")
    val PACKAGE_PRIVATE = FqName("lombok.PackagePrivate")
    val NO_ARGS_CONSTRUCTOR = FqName("lombok.NoArgsConstructor")
    val ALL_ARGS_CONSTRUCTOR = FqName("lombok.AllArgsConstructor")
    val REQUIRED_ARGS_CONSTRUCTOR = FqName("lombok.RequiredArgsConstructor")
    val BUILDER = FqName("lombok.Builder")
    val SUPER_BUILDER = FqName("lombok.experimental.SuperBuilder")
    val SINGULAR = FqName("lombok.Singular")
    val LOG = FqName("lombok.extern.java.Log")
    val SLF4J = FqName("lombok.extern.slf4j.Slf4j")
    val LOG4J = FqName("lombok.extern.log4j.Log4j")
    val COMMONS_LOG = FqName("lombok.extern.apachecommons.CommonsLog")
    val FLOGGER = FqName("lombok.extern.flogger.Flogger")
    val JBOSS_LOG = FqName("lombok.extern.jbosslog.JBossLog")
    val LOG4J2 = FqName("lombok.extern.log4j.Log4j2")
    val XSLF4J = FqName("lombok.extern.slf4j.XSlf4j")
    val TO_STRING = FqName("lombok.ToString")
    val EQUALS_AND_HASH_CODE = FqName("lombok.EqualsAndHashCode")

    val ACCESSORS_ID = ClassId.topLevel(ACCESSORS)
    val GETTER_ID = ClassId.topLevel(GETTER)
    val SETTER_ID = ClassId.topLevel(SETTER)
    val WITH_ID = ClassId.topLevel(WITH)
    val DATA_ID = ClassId.topLevel(DATA)
    val VALUE_ID = ClassId.topLevel(VALUE)
    val BUILDER_ID = ClassId.topLevel(BUILDER)
    val BUILDER_DEFAULT_ID = BUILDER_ID.createNestedClassId(Name.identifier("Default"))
    val SUPER_BUILDER_ID = ClassId.topLevel(SUPER_BUILDER)
    val SINGULAR_ID = ClassId.topLevel(SINGULAR)
    val NO_ARGS_CONSTRUCTOR_ID = ClassId.topLevel(NO_ARGS_CONSTRUCTOR)
    val ALL_ARGS_CONSTRUCTOR_ID = ClassId.topLevel(ALL_ARGS_CONSTRUCTOR)
    val REQUIRED_ARGS_CONSTRUCTOR_ID = ClassId.topLevel(REQUIRED_ARGS_CONSTRUCTOR)

    val LOG_ID = ClassId.topLevel(LOG)
    val SLF4J_ID = ClassId.topLevel(SLF4J)
    val LOG4J_ID = ClassId.topLevel(LOG4J)
    val COMMONS_LOG_ID = ClassId.topLevel(COMMONS_LOG)
    val FLOGGER_ID = ClassId.topLevel(FLOGGER)
    val JBOSS_LOG_ID = ClassId.topLevel(JBOSS_LOG)
    val LOG4J2_ID = ClassId.topLevel(LOG4J2)
    val XSLF4J_ID = ClassId.topLevel(XSLF4J)
    val TO_STRING_ID = ClassId.topLevel(TO_STRING)
    val INCLUDE_NAME = Name.identifier("Include")
    val EXCLUDE_NAME = Name.identifier("Exclude")
    val TO_STRING_INCLUDE_ID = TO_STRING_ID.createNestedClassId(INCLUDE_NAME)
    val TO_STRING_EXCLUDE_ID = TO_STRING_ID.createNestedClassId(EXCLUDE_NAME)
    val EQUALS_AND_HASH_CODE_ID = ClassId.topLevel(EQUALS_AND_HASH_CODE)
    val EQUALS_AND_HASH_CODE_INCLUDE_ID = EQUALS_AND_HASH_CODE_ID.createNestedClassId(INCLUDE_NAME)
    val EQUALS_AND_HASH_CODE_EXCLUDE_ID = EQUALS_AND_HASH_CODE_ID.createNestedClassId(EXCLUDE_NAME)

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

    val JAVA_OBJECT_ID = ClassId.fromString("java.lang/Object")
    val JAVA_ITERABLE_ID = ClassId.fromString("java.lang/Iterable")
    val JAVA_COLLECTION_ID = ClassId.fromString("java.util/Collection")
    val JAVA_MAP_ID = ClassId.fromString("java.util/Map")

    val IMMUTABLE_COLLECTION_ID = "ImmutableCollection".guavaCollectType()
    val IMMUTABLE_LIST_ID = "ImmutableList".guavaCollectType()
    val IMMUTABLE_SET_ID = "ImmutableSet".guavaCollectType()
    val IMMUTABLE_SORTED_SET_ID = "ImmutableSortedSet".guavaCollectType()

    val IMMUTABLE_MAP_ID = "ImmutableMap".guavaCollectType()
    val IMMUTABLE_BI_MAP_ID = "ImmutableBiMap".guavaCollectType()
    val IMMUTABLE_SORTED_MAP_ID = "ImmutableSortedMap".guavaCollectType()

    val TABLE_ID = "Table".guavaCollectType()

    val SUPPORTED_GUAVA_COLLECTION_IDS = setOf(
        IMMUTABLE_COLLECTION_ID,
        IMMUTABLE_LIST_ID,
        IMMUTABLE_SET_ID,
        IMMUTABLE_SORTED_SET_ID,
    )

    val SUPPORTED_COLLECTION_IDS = setOf(
        // Java collections
        JAVA_ITERABLE_ID,
        JAVA_COLLECTION_ID,
        ClassId.fromString("java.util/List"),
        ClassId.fromString("java.util/Set"),
        ClassId.fromString("java.util/SortedSet"),
        ClassId.fromString("java.util/NavigableSet"),

        // Kotlin collections
        StandardClassIds.Iterable,
        StandardClassIds.MutableIterable,
        StandardClassIds.Collection,
        StandardClassIds.MutableCollection,
        StandardClassIds.List,
        StandardClassIds.MutableList,
        StandardClassIds.Set,
        StandardClassIds.MutableSet,

        *SUPPORTED_GUAVA_COLLECTION_IDS.toTypedArray()
    )

    val SUPPORTED_MAP_IDS = setOf(
        // Java maps
        JAVA_MAP_ID,
        ClassId.fromString("java.util/SortedMap"),
        ClassId.fromString("java.util/NavigableMap"),

        // Kotlin maps
        StandardClassIds.Map,
        StandardClassIds.MutableMap,

        // Guava maps
        IMMUTABLE_MAP_ID,
        IMMUTABLE_BI_MAP_ID,
        IMMUTABLE_SORTED_MAP_ID,
    )

    val SUPPORTED_TABLE_IDS = setOf(
        "ImmutableTable".guavaCollectType(),
    )

    private fun String.guavaCollectType(): ClassId {
        return ClassId(GUAVA_COLLECT_PACKAGE, Name.identifier(this))
    }
}

object AccessorNames {
    const val IS = "is"
    const val GET = "get"
    const val SET = "set"
}
