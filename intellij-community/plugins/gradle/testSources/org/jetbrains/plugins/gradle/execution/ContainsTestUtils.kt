// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution

data class Inverted<T>(val value: T)

operator fun <T> T.not() = Inverted(this)

/**
 * assertCollection([1, 3, 5], 1, !2, 3, !4, 5) -> success
 * assertCollection([1, 3, 5], 1, !2, 3)        -> success
 * assertCollection([1, 3, 5], 1, 2, 3, 4, 5)   -> fail
 * assertCollection([1, 3, 5], 1, !2, 3, 4)     -> fail
 * assertCollection([1, 3, 5], [1, 3], ![2, 4]) -> success
 *
 * @param elements must be a T, Iterable<T>, Inverted<T> or Inverted<Iterable<T>>
 */
inline fun <reified T> assertCollection(actual: Collection<T>, vararg elements: Any?) {
  val (positive, negative) = partition(elements.toList())
  positive.forEach { require(it is T) }
  negative.forEach { require(it is T) }
  val expected = positive.toMutableList()
  val unexpected = actual.toMutableList()
  unexpected.retainAll(negative)
  expected.removeAll(actual)
  if (expected.isEmpty() && unexpected.isEmpty()) return
  val messageActualPart = "\nactual: $actual"
  val messageExpectedPart = if (expected.isEmpty()) "" else "\nexpected but not found: $expected"
  val messageUnexpectedPart = if (unexpected.isEmpty()) "" else "\nunexpected but found: $unexpected"
  throw AssertionError(messageExpectedPart + messageUnexpectedPart + messageActualPart)
}

fun partition(element: Any?): Pair<List<Any?>, List<Any?>> =
  when (element) {
    is Inverted<*> -> partition(element.value).let { it.second to it.first }
    is Iterable<*> -> element.map { partition(it) }
      .let { it.flatMap { p -> p.first } to it.flatMap { p -> p.second } }
    else -> listOf(element) to listOf()
  }