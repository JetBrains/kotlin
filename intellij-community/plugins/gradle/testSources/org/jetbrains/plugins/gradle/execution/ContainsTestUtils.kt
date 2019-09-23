// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution

data class Inverted<T>(val value: T)

operator fun <T> T.not() = Inverted(this)

/**
 * @param elements must be a T, Iterable<T>, Inverted<T> or Inverted<Iterable<T>>
 */
inline fun <reified T> assertContains(actual: Iterable<T>, vararg elements: Any?) {
  val (positive, negative) = partition(elements.toList())
  positive.forEach { require(it is T) }
  negative.forEach { require(it is T) }
  val expected = positive.toMutableList()
  val unexpected = actual.toMutableList()
  unexpected.retainAll(negative)
  expected.removeAll(actual)
  when {
    expected.isEmpty() && unexpected.isEmpty() -> return
    expected.isEmpty() -> throw AssertionError("\nunexpected but found: $unexpected")
    unexpected.isEmpty() -> throw AssertionError("\nexpected but not found: $expected")
    else -> throw AssertionError("\nexpected but not found: $expected\nunexpected but found: $unexpected")
  }
}

fun partition(element: Any?): Pair<List<Any?>, List<Any?>> =
  when (element) {
    is Inverted<*> -> partition(element.value).let { it.second to it.first }
    is Iterable<*> -> element.map { partition(it) }
      .let { it.flatMap { p -> p.first } to it.flatMap { p -> p.second } }
    else -> listOf(element) to listOf()
  }