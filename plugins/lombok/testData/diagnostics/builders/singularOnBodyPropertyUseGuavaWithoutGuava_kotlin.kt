// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// FULL_JDK
// ISSUE: KT-90709

// FILE: test.kt

import lombok.Builder
import lombok.Singular

// `lombok.singular.useGuava=true` builds every `@Singular` collection with Guava, which is not on the classpath here.
// A regular property is a builder field just like a constructor parameter (see `singularUseGuavaWithoutGuava_kotlin.kt`),
// so the same error is expected for it.
@Builder
class UseGuavaWithoutGuavaProperties {
    <!SINGULAR_REQUIRES_GUAVA!>@Singular("word")<!> var words: List<String> = emptyList()
    <!SINGULAR_REQUIRES_GUAVA!>@Singular("tag")<!> var tags: Set<String> = emptySet()
    <!SINGULAR_REQUIRES_GUAVA!>@Singular("sortedItem")<!> var sorted: java.util.SortedSet<String> = java.util.TreeSet()
    <!SINGULAR_REQUIRES_GUAVA!>@Singular("mapping")<!> var mappings: Map<String, Int> = emptyMap()
    var plain: List<String> = emptyList()
}

// FILE: lombok.config
lombok.singular.useGuava=true

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, javaFunction, propertyDeclaration, stringLiteral */
