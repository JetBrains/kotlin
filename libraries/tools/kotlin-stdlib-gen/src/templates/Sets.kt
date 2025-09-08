/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package templates

import templates.Family.*
import templates.SequenceClass.*

object SetOps : TemplateGroupBase() {

    val f_toMutableSet = fn("toMutableSet()") {
        includeDefault()
    } builder {
        doc {
            """
            Returns a new [MutableSet] containing all distinct ${f.element.pluralize()} from the given ${f.collection}.

            The returned set preserves the element iteration order of the original ${f.collection}.
            """
        }
        sequenceClassification(terminal)
        returns("MutableSet<T>")
        body {
            """
            return when (this) {
                is Collection<T> -> LinkedHashSet(this)
                else -> toCollection(LinkedHashSet<T>())
            }
            """
        }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            val capacity = "size" + if (primitive == PrimitiveType.Char) ".coerceAtMost(128)" else ""
            "return toCollection(LinkedHashSet<T>(mapCapacity($capacity)))"
        }
        body(Sequences) {
            """
            val set = LinkedHashSet<T>()
            for (item in this) set.add(item)
            return set
            """
        }
    }

    val f_distinct = fn("distinct()") {
        includeDefault()
    } builder {
        doc {
            """
                Returns a ${f.mapResult} containing only distinct ${f.element.pluralize()} from the given ${f.collection}.
                ${if (f.isPrimitiveSpecialization) "" else "\n" +
                "Among equal ${f.element.pluralize()} of the given ${f.collection}, only the first one will be present in the resulting ${f.mapResult}."}
                The ${f.element.pluralize()} in the resulting ${f.mapResult} are in the same order as they were in the source ${f.collection}.
                """
        }

        returns("List<T>")
        body { "return this.toMutableSet().toList()" }
        specialFor(Sequences) {
            sequenceClassification(intermediate, stateful)
            returns("Sequence<T>")
            body { "return this.distinctBy { it }" }
        }
        sample("samples.collections.Collections.Transformations.distinctAndDistinctBy")
    }

    val f_distinctBy = fn("distinctBy(selector: (T) -> K)") {
        includeDefault()
    } builder {
        doc {
            """
                Returns a ${f.mapResult} containing only ${f.element.pluralize()} from the given ${f.collection}
                having distinct keys returned by the given [selector] function.
                ${if (f.isPrimitiveSpecialization) "" else "\n" +
                "Among ${f.element.pluralize()} of the given ${f.collection} with equal keys, only the first one will be present in the resulting ${f.mapResult}."}
                The ${f.element.pluralize()} in the resulting ${f.mapResult} are in the same order as they were in the source ${f.collection}.
                """
        }

        inline()
        typeParam("K")
        returns("List<T>")
        body {
            """
            val set = HashSet<K>()
            val list = ArrayList<T>()
            for (e in this) {
                val key = selector(e)
                if (set.add(key))
                    list.add(e)
            }
            return list
            """
        }

        specialFor(Sequences) {
            inline(Inline.No)
            returns("Sequence<T>")
            sequenceClassification(intermediate, stateful)
            body { """return DistinctSequence(this, selector)""" }
        }

        sample("samples.collections.Collections.Transformations.distinctAndDistinctBy")
    }

    val f_union = fn("union(other: Iterable<T>)") {
        include(Family.defaultFamilies - Sequences)
    } builder {
        infix(true)
        doc {
            """
            Returns a set containing all distinct elements from both collections.

            The returned set preserves the element iteration order of the original ${f.collection}.
            Those elements of the [other] collection that are unique are iterated in the end
            in the order of the [other] collection.
            
            The returned set uses structural equality (`==`) to distinguish elements, meaning there will be no two
            structurally equal, but otherwise different elements in it.
            
            To get a set containing all elements that are contained in both collections use [intersect].
            """
        }
        returns("Set<T>")
        body {
            """
            val set = this.toMutableSet()
            set.addAll(other)
            return set
            """
        }
    }

    val f_intersect = fn("intersect(other: Iterable<T>)") {
        include(Family.defaultFamilies - Sequences)
    } builder {
        infix()

        doc {
            """
            Returns a set containing elements of this ${f.collection} that are also contained in the specified [other] ${f.collection}.

            The returned set preserves the element iteration order of the original ${f.collection}.
            
            The returned set uses structural equality (`==`) to distinguish elements, meaning there will be no two
            structurally equal, but otherwise different elements in it.

            To get a set containing all elements that are contained at least in one of these collections use [union].
            """
        }
        returns("Set<T>")
        body {
            """
            val otherCollection = other.convertToListIfNotCollection()
            val set = mutableSetOf<T>()
            for (e in this) {
                if (otherCollection.contains(e)) {
                    set.add(e)
                }
            }
            return set
            """
        }
        specialFor(ArraysOfPrimitives) {
            // In general, converting receiver to a set may lead to function's contract not being obeyed.
            // However, primitive values will end up being auto-boxed no matter what.
            // On some platforms (namely, on JVM), some auto-boxed instances could be taken from a cache,
            // others will be allocated. Either way, the order of operations won't make any difference even
            // if `other.contains` use referential equality (newly allocated instance won't be contained by `other`,
            // for pooled instances the order of operation won't matter).
            body {
                """
                val set = this.toMutableSet()
                set.retainAll(other)
                return set
                """
            }
        }
    }

    val f_subtract = fn("subtract(other: Iterable<T>)") {
        include(Family.defaultFamilies - Sequences)
    } builder {
        infix()
        doc {
            """
            Returns a set containing all elements that are contained by this ${f.collection} and not contained by the specified collection.

            The returned set preserves the element iteration order of the original ${f.collection}.
            
            The returned set uses structural equality (`==`) to distinguish elements, meaning there will be no two
            structurally equal, but otherwise different elements in it.
            """
        }
        returns("Set<T>")
        body {
            """
            val otherCollection = other.convertToListIfNotCollection()
            val result = mutableSetOf<T>()
            for (e in this) {
                if (!otherCollection.contains(e)) {
                    result.add(e)
                }
            }
            return result
            """
        }
        specialFor(ArraysOfPrimitives) {
            // In general, converting receiver to a set may lead to function's contract not being obeyed.
            // However, primitive values will end up being auto-boxed no matter what.
            // On some platforms (namely, on JVM), some auto-boxed instances could be taken from a cache,
            // others will be allocated. Either way, the order of operations won't make any difference even
            // if `other.contains` use referential equality (newly allocated instance won't be contained by `other`,
            // for pooled instances the order of operation won't matter).
            body {
                """
                val set = this.toMutableSet()
                set.removeAll(other)
                return set
                """
            }
        }
    }

}
