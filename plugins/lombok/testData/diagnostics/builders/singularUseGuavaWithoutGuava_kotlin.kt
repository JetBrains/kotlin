// WITH_STDLIB
// FULL_JDK
// ISSUE: KT-89084

// FILE: test.kt

import lombok.Builder
import lombok.Singular

// `lombok.singular.useGuava=true` builds every `@Singular` collection with Guava, which is not on the classpath here.
@Builder
class UseGuavaWithoutGuava(
    @Singular("word") val words: List<String>,
    @Singular("tag") val tags: Set<String>,
    @Singular("sortedItem") val sorted: java.util.SortedSet<String>,
    @Singular("mapping") val mappings: Map<String, Int>,
    @Singular("navigableMapping") val navigableMappings: java.util.NavigableMap<String, Int>,
    val plain: List<String>,
    <!UNSUPPORTED_SINGULAR_TYPE!>@Singular("number")<!> val unsupported: IntArray,
)

object Factory {
    @Builder(builderClassName = "FactoryBuilder")
    fun create(@Singular("word") words: List<String>): String = words.joinToString()
}

// FILE: lombok.config
lombok.singular.useGuava=true
