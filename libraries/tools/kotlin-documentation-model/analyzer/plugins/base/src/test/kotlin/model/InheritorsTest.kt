package model

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.analysis.DokkaAnalysisConfiguration
import org.jetbrains.dokka.analysis.KotlinAnalysis
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.transformers.documentables.InheritorsInfo
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.doc.P
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import utils.AbstractModelTest
import utils.assertNotNull

class InheritorsTest : AbstractModelTest("/src/main/kotlin/inheritors/Test.kt", "inheritors") {

    @Test
    fun simple() {
        inlineModelTest(
            """|interface A{}
               |class B() : A {}
            """.trimMargin(),
        ) {
            with((this / "inheritors" / "A").cast<DInterface>()) {
                val map = extra[InheritorsInfo].assertNotNull("InheritorsInfo").value
                with(map.keys.also { it counts 1 }.find { it.analysisPlatform == Platform.jvm }.assertNotNull("jvm key").let { map[it]!! }
                ) {
                    this counts 1
                    first().classNames equals "B"
                }
            }
        }
    }

    @Test
    fun sealed() {
        inlineModelTest(
            """|sealed class A {}
               |class B() : A() {}
               |class C() : A() {}
               |class D()
            """.trimMargin(),
        ) {
            with((this / "inheritors" / "A").cast<DClass>()) {
                val map = extra[InheritorsInfo].assertNotNull("InheritorsInfo").value
                with(map.keys.also { it counts 1 }.find { it.analysisPlatform == Platform.jvm }.assertNotNull("jvm key").let { map[it]!! }
                ) {
                    this counts 2
                    mapNotNull { it.classNames }.sorted() equals listOf("B", "C")
                }
            }
        }
    }

    @Test
    fun multiplatform() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("common/src/", "jvm/src/")
                    analysisPlatform = "jvm"
                }
                sourceSet {
                    sourceRoots = listOf("common/src/", "js/src/")
                    analysisPlatform = "js"
                }
            }
        }

        testInline(
            """
            |/common/src/main/kotlin/inheritors/Test.kt
            |package inheritors
            |interface A{}
            |/jvm/src/main/kotlin/inheritors/Test.kt
            |package inheritors
            |class B() : A {}
            |/js/src/main/kotlin/inheritors/Test.kt
            |package inheritors
            |class B() : A {}
            |class C() : A {}
        """.trimMargin(),
            configuration,
            cleanupOutput = false,
        ) {
            documentablesTransformationStage = { m ->
                with((m / "inheritors" / "A").cast<DInterface>()) {
                    val map = extra[InheritorsInfo].assertNotNull("InheritorsInfo").value
                    with(map.keys.also { it counts 2 }) {
                        with(find { it.analysisPlatform == Platform.jvm }.assertNotNull("jvm key").let { map[it]!! }) {
                            this counts 1
                            first().classNames equals "B"
                        }
                        with(find { it.analysisPlatform == Platform.js }.assertNotNull("js key").let { map[it]!! }) {
                            this counts 2
                            val classes = listOf("B", "C")
                            assertTrue(all { classes.contains(it.classNames) }, "One of subclasses missing in js" )
                        }
                    }

                }
            }
        }
    }

    @Test
    fun `should inherit docs`() {
        val expectedDoc = listOf(P(listOf(Text("some text"))))
        inlineModelTest(
            """|interface A<out E> {
               | /**
               | * some text
               | */
               | val a: Int
               | 
               | /**
               | * some text
               | */
               | fun b(): E
               |}
               |open class C
               |class B<out E>() : C(), A<out E> {
               | val a = 0
               | override fun b(): E {}
               |}
            """.trimMargin(),
            platform = Platform.common.toString()
        ) {
            with((this / "inheritors" / "A").cast<DInterface>()) {
                with(this / "a") {
                    val propDoc = this?.documentation?.values?.single()?.children?.first()?.children
                    propDoc equals expectedDoc
                }
                with(this / "b") {
                    val funDoc = this?.documentation?.values?.single()?.children?.first()?.children
                    funDoc equals expectedDoc
                }

            }

            with((this / "inheritors" / "B").cast<DClass>()) {
                with(this / "a") {
                    val propDoc = this?.documentation?.values?.single()?.children?.first()?.children
                    propDoc equals expectedDoc
                }
            }
        }
    }

    class IgnoreCommonBuiltInsPlugin : DokkaPlugin() {
        private val dokkaBase by lazy { plugin<DokkaBase>() }
        @Suppress("unused")
        val stdLibKotlinAnalysis by extending {
            dokkaBase.kotlinAnalysis providing { ctx ->
                KotlinAnalysis(
                    sourceSets = ctx.configuration.sourceSets,
                    logger = ctx.logger,
                    analysisConfiguration = DokkaAnalysisConfiguration(ignoreCommonBuiltIns = true)
                )
            } override dokkaBase.defaultKotlinAnalysis
        }
    }
    @Test
    fun `should inherit docs for stdLib #2638`() {
        val testConfiguration = dokkaConfiguration {
            suppressObviousFunctions = false
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                    analysisPlatform = "common"
                    languageVersion = "1.4"
                }
            }
        }

        inlineModelTest(
            """
            package kotlin.collections
            
            import kotlin.internal.PlatformDependent
            
            /**
             * Classes that inherit from this interface can be represented as a sequence of elements that can
             * be iterated over.
             * @param T the type of element being iterated over. The iterator is covariant in its element type.
             */
            public interface Iterable<out T> {
                /**
                 * Returns an iterator over the elements of this object.
                 */
                public operator fun iterator(): Iterator<T>
            }
            
            /**
             * Classes that inherit from this interface can be represented as a sequence of elements that can
             * be iterated over and that supports removing elements during iteration.
             * @param T the type of element being iterated over. The mutable iterator is invariant in its element type.
             */
            public interface MutableIterable<out T> : Iterable<T> {
                /**
                 * Returns an iterator over the elements of this sequence that supports removing elements during iteration.
                 */
                override fun iterator(): MutableIterator<T>
            }
            
            /**
             * A generic collection of elements. Methods in this interface support only read-only access to the collection;
             * read/write access is supported through the [MutableCollection] interface.
             * @param E the type of elements contained in the collection. The collection is covariant in its element type.
             */
            public interface Collection<out E> : Iterable<E> {
                // Query Operations
                /**
                 * Returns the size of the collection.
                 */
                public val size: Int
            
                /**
                 * Returns `true` if the collection is empty (contains no elements), `false` otherwise.
                 */
                public fun isEmpty(): Boolean
            
                /**
                 * Checks if the specified element is contained in this collection.
                 */
                public operator fun contains(element: @UnsafeVariance E): Boolean
            
                override fun iterator(): Iterator<E>
            
                // Bulk Operations
                /**
                 * Checks if all elements in the specified collection are contained in this collection.
                 */
                public fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean
            }
            
            /**
             * A generic collection of elements that supports adding and removing elements.
             *
             * @param E the type of elements contained in the collection. The mutable collection is invariant in its element type.
             */
            public interface MutableCollection<E> : Collection<E>, MutableIterable<E> {
                // Query Operations
                override fun iterator(): MutableIterator<E>
            
                // Modification Operations
                /**
                 * Adds the specified element to the collection.
                 *
                 * @return `true` if the element has been added, `false` if the collection does not support duplicates
                 * and the element is already contained in the collection.
                 */
                public fun add(element: E): Boolean
            
                /**
                 * Removes a single instance of the specified element from this
                 * collection, if it is present.
                 *
                 * @return `true` if the element has been successfully removed; `false` if it was not present in the collection.
                 */
                public fun remove(element: E): Boolean
            
                // Bulk Modification Operations
                /**
                 * Adds all of the elements of the specified collection to this collection.
                 *
                 * @return `true` if any of the specified elements was added to the collection, `false` if the collection was not modified.
                 */
                public fun addAll(elements: Collection<E>): Boolean
            
                /**
                 * Removes all of this collection's elements that are also contained in the specified collection.
                 *
                 * @return `true` if any of the specified elements was removed from the collection, `false` if the collection was not modified.
                 */
                public fun removeAll(elements: Collection<E>): Boolean
            
                /**
                 * Retains only the elements in this collection that are contained in the specified collection.
                 *
                 * @return `true` if any element was removed from the collection, `false` if the collection was not modified.
                 */
                public fun retainAll(elements: Collection<E>): Boolean
            
                /**
                 * Removes all elements from this collection.
                 */
                public fun clear(): Unit
            }
            
            /**
             * A generic ordered collection of elements. Methods in this interface support only read-only access to the list;
             * read/write access is supported through the [MutableList] interface.
             * @param E the type of elements contained in the list. The list is covariant in its element type.
             */
            public interface List<out E> : Collection<E> {
                // Query Operations
            
                override val size: Int
                override fun isEmpty(): Boolean
                override fun contains(element: @UnsafeVariance E): Boolean
                override fun iterator(): Iterator<E>
            
                // Bulk Operations
                override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean
            
                // Positional Access Operations
                /**
                 * Returns the element at the specified index in the list.
                 */
                public operator fun get(index: Int): E
            
                // Search Operations
                /**
                 * Returns the index of the first occurrence of the specified element in the list, or -1 if the specified
                 * element is not contained in the list.
                 */
                public fun indexOf(element: @UnsafeVariance E): Int
            
                /**
                 * Returns the index of the last occurrence of the specified element in the list, or -1 if the specified
                 * element is not contained in the list.
                 */
                public fun lastIndexOf(element: @UnsafeVariance E): Int
            
                // List Iterators
                /**
                 * Returns a list iterator over the elements in this list (in proper sequence).
                 */
                public fun listIterator(): ListIterator<E>
            
                /**
                 * Returns a list iterator over the elements in this list (in proper sequence), starting at the specified [index].
                 */
                public fun listIterator(index: Int): ListIterator<E>
            
                // View
                /**
                 * Returns a view of the portion of this list between the specified [fromIndex] (inclusive) and [toIndex] (exclusive).
                 * The returned list is backed by this list, so non-structural changes in the returned list are reflected in this list, and vice-versa.
                 *
                 * Structural changes in the base list make the behavior of the view undefined.
                 */
                public fun subList(fromIndex: Int, toIndex: Int): List<E>
            }
            
            // etc
            """.trimMargin(),
            platform = Platform.common.toString(),
            configuration = testConfiguration,
            prependPackage = false,
            pluginsOverrides = listOf(IgnoreCommonBuiltInsPlugin())
        ) {
            with((this / "kotlin.collections" / "List" / "contains").cast<DFunction>()) {
                documentation.size equals 1

            }
        }
    }

    @Test
    fun `should inherit docs in case of diamond inheritance`() {
        inlineModelTest(
            """
            public interface Collection2<out E>  {
                /**
                 * Returns `true` if the collection is empty (contains no elements), `false` otherwise.
                 */
                public fun isEmpty(): Boolean
            
                /**
                 * Checks if the specified element is contained in this collection.
                 */
                public operator fun contains(element: @UnsafeVariance E): Boolean
            }
            
            public interface MutableCollection2<E> : Collection2<E>, MutableIterable2<E> 
            

            public interface List2<out E> : Collection2<E> {
                override fun isEmpty(): Boolean
                override fun contains(element: @UnsafeVariance E): Boolean
            }
            
            public interface MutableList2<E> : List2<E>, MutableCollection2<E>
            
            public class AbstractMutableList2<E> : MutableList2<E> {
                protected constructor()
            
                // From List
            
                override fun isEmpty(): Boolean = size == 0
                public override fun contains(element: E): Boolean = indexOf(element) != -1
            }
            public class ArrayDeque2<E> : AbstractMutableList2<E> {
                override fun isEmpty(): Boolean = size == 0
                public override fun contains(element: E): Boolean = indexOf(element) != -1
            
            }
            """.trimMargin()
        ) {
            with((this / "inheritors" / "ArrayDeque2" / "isEmpty").cast<DFunction>()) {
                documentation.size equals 1
            }
            with((this / "inheritors" / "ArrayDeque2" / "contains").cast<DFunction>()) {
                documentation.size equals 1
            }
        }
    }
}
