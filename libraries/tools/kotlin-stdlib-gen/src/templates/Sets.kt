/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
            Returns a mutable set containing all distinct ${f.element.pluralize()} from the given ${f.collection}.

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
            """
            val set = LinkedHashSet<T>(mapCapacity(size))
            for (item in this) set.add(item)
            return set
            """
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
    }

    val f_distinctBy = fn("distinctBy(selector: (T) -> K)") {
        includeDefault()
    } builder {
        doc {
            """
                Returns a ${f.mapResult} containing only ${f.element.pluralize()} from the given ${f.collection}
                having distinct keys returned by the given [selector] function.

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
            Returns a set containing all elements that are contained by both this set and the specified collection.

            The returned set preserves the element iteration order of the original ${f.collection}.
            """
        }
        returns("Set<T>")
        body {
            """
            val set = this.toMutableSet()
            set.retainAll(other)
            return set
            """
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
            """
        }
        returns("Set<T>")
        body {
            """
            val set = this.toMutableSet()
            set.removeAll(other)
            return set
            """
        }
    }

}
