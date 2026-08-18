/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package transformers

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.dfs
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmMappedMethodsFilterTest : BaseAbstractTest() {
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src")
                analysisPlatform = "jvm"
                classpath = listOfNotNull(jvmStdlibPath)
            }
        }
    }

    fun List<DModule>.getMethodNamesFrom(classname: String): Set<String> {
        val cl = this.mapNotNull { it.dfs { it.name == classname && it is DClasslike } }.first() as DClasslike
        return cl.functions.map { it.name }.toSet()
    }

    fun List<DModule>.getPropertyNamesFrom(classname: String): Set<String> {
        val cl = this.mapNotNull { it.dfs { it.name == classname && it is DClasslike } }.first() as DClasslike
        return cl.properties.map { it.name }.toSet()
    }

    @Test
    fun `should filter out JVM synthetic properties in RuntimeException`() {
        testInline(
            """
            |/src/MyRuntimeException.kt
            |class MyRuntimeException: RuntimeException()
            """.trimIndent(), configuration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                assertEquals(
                    setOf("cause", "message"),
                    modules.getPropertyNamesFrom("MyRuntimeException")
                )
            }
        }
    }

    @Test
    fun `should filter out JVM mapped methods in CharSequence`() {
        testInline(
            """
            |/src/MyCharSequence.kt
            |interface MyCharSequence: CharSequence
            """.trimIndent(), configuration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                assertEquals(
                    setOf("subSequence", "get"),
                    modules.getMethodNamesFrom("MyCharSequence")
                )
            }
        }
    }

    @Test
    fun `should filter out JVM mapped methods in Collection and MutableCollection`() {
        testInline(
            """
            |/src/MyList.kt
            |interface MyCollection<T>: Collection<T>
            |interface MyMutableCollection<T>: MutableCollection<T>
            """.trimIndent(), configuration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->

                assertEquals(
                    setOf(
                        "contains",
                        "containsAll",
                        "isEmpty",
                        "iterator",
                    ),
                    modules.getMethodNamesFrom("MyCollection")
                )
                assertEquals(
                    setOf(
                        "add",
                        "addAll",
                        "clear",
                        "iterator",
                        "remove",
                        "removeAll",
                        "retainAll",
                        "isEmpty",
                        "contains",
                        "containsAll",
                    ),
                    modules.getMethodNamesFrom("MyMutableCollection")
                )
            }
        }
    }

    @Test
    fun `should filter out JVM mapped methods in Set and MutableSet`() {
        testInline(
            """
            |/src/MyList.kt
            |interface MySet<T>: Set<T>
            |interface MyMutableSet<T>: MutableSet<T>
            """.trimIndent(), configuration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->

                assertEquals(
                    setOf(
                        "contains",
                        "containsAll",
                        "isEmpty",
                        "iterator"
                    ),
                    modules.getMethodNamesFrom("MySet")
                )
                assertEquals(
                    setOf(
                        "add",
                        "addAll",
                        "clear",
                        "contains",
                        "containsAll",
                        "isEmpty",
                        "iterator",
                        "remove",
                        "removeAll",
                        "retainAll",
                    ),
                    modules.getMethodNamesFrom("MyMutableSet")
                )
            }
        }
    }

    @Test
    fun `should filter out JVM mapped methods in ListIterator and MutableListIterator`() {
        testInline(
            """
            |/src/MyList.kt
            |interface MyListIterator<T>: ListIterator<T>
            |interface MyMutableListIterator<T>: MutableListIterator<T>
            """.trimIndent(), configuration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->

                assertEquals(
                    setOf(
                        "hasNext",
                        "hasPrevious",
                        "next",
                        "nextIndex",
                        "previous",
                        "previousIndex",
                    ),
                    modules.getMethodNamesFrom("MyListIterator")
                )
                assertEquals(
                    setOf(
                        "add",
                        "hasNext",
                        "hasPrevious",
                        "next",
                        "nextIndex",
                        "previous",
                        "previousIndex",
                        "remove",
                        "set",
                    ),
                    modules.getMethodNamesFrom("MyMutableListIterator")
                )
            }
        }
    }

    @Test
    fun `should filter out JVM mapped methods in MutableIterator`() {
        testInline(
            """
            |/src/MyList.kt
            |interface MyIterator<T>: MutableIterator<T>
            """.trimIndent(), configuration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                assertEquals(
                    setOf(
                        "hasNext",
                        "next",
                        "remove",
                    ),
                    modules.getMethodNamesFrom("MyIterator")
                )
            }
        }
    }

    @Test
    fun `should filter out JVM mapped methods in List and MutableList`() {
        testInline(
            """
            |/src/MyList.kt
            |interface MyList<T>: List<T>
            |interface MyMutableList<T>: MutableList<T>
            """.trimIndent(), configuration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                assertEquals(
                    setOf(
                        "contains",
                        "containsAll",
                        "get",
                        "indexOf",
                        "isEmpty",
                        "iterator",
                        "lastIndexOf",
                        "listIterator",
                        "subList"
                    ),
                    modules.getMethodNamesFrom("MyList")
                )
                assertEquals(
                    setOf(
                        "add",
                        "addAll",
                        "clear",
                        "contains",
                        "containsAll",
                        "get",
                        "indexOf",
                        "isEmpty",
                        "iterator",
                        "lastIndexOf",
                        "listIterator",
                        "remove",
                        "removeAll",
                        "removeAt",
                        "retainAll",
                        "set",
                        "subList",
                    ),
                    modules.getMethodNamesFrom("MyMutableList")
                )
            }
        }
    }

    @Test
    fun `should filter out JVM mapped methods in Throwable`() {
        testInline(
            """
            |/src/MyThrowable.kt
            |class MyThrowable: Throwable()
            """.trimIndent(), configuration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                assertEquals(
                    emptySet(),
                    modules.getMethodNamesFrom("MyThrowable")
                )
            }
        }
    }

    @Test
    fun `should filter out JVM mapped methods in MutableMap`() {
        testInline(
            """
            |/src/MyMap.kt
            |interface MyMap<K, V> : MutableMap<K, V>
            """.trimIndent(), configuration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                assertEquals(
                    setOf("clear", "containsKey", "containsValue", "get", "isEmpty", "put", "putAll"),
                    modules.getMethodNamesFrom("MyMap"),
                )
            }
        }
    }

    @Test
    fun `should filter out JVM mapped methods in Iterable and MutableIterable`() {
        testInline(
            """
            |/src/MyIterable.kt
            |interface MyIterable<T> : Iterable<T>
            |interface MyMutableIterable<T> : MutableIterable<T>
            """.trimIndent(), configuration
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                assertEquals(
                    setOf("iterator"),
                    modules.getMethodNamesFrom("MyIterable"),
                )
                assertEquals(
                    setOf("iterator"),
                    modules.getMethodNamesFrom("MyMutableIterable"),
                )
            }
        }
    }
}
