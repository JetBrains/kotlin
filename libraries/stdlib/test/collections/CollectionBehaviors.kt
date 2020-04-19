/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections.behaviors

import test.collections.CompareContext
import kotlin.test.assertEquals

public fun <T> CompareContext<List<T>>.listBehavior() {
    equalityBehavior(withToString = true)
    collectionBehavior()
    compareProperty({ listIterator() }, { listIteratorBehavior() })
    compareProperty({ listIterator(0) }, { listIteratorBehavior() })

    propertyFails { listIterator(-1) }
    propertyFails { listIterator(size + 1) }

    for (index in expected.indices)
        propertyEquals { this[index] }

    propertyFailsWith<IndexOutOfBoundsException> { this[size] }

    propertyEquals { indexOf(elementAtOrNull(0)) }
    propertyEquals { lastIndexOf(elementAtOrNull(0)) }

    propertyFails { subList(0, size + 1) }
    propertyFails { subList(-1, 0) }
    propertyEquals { subList(0, size) }
}

public fun <T> CompareContext<ListIterator<T>>.listIteratorBehavior() {
    listIteratorProperties()

    while (expected.hasNext()) {
        propertyEquals { next() }
        listIteratorProperties()
    }
    propertyFails { next() }

    while (expected.hasPrevious()) {
        propertyEquals { previous() }
        listIteratorProperties()
    }
    propertyFails { previous() }
}

public fun CompareContext<ListIterator<*>>.listIteratorProperties() {
    propertyEquals { hasNext() }
    propertyEquals { hasPrevious() }
    propertyEquals { nextIndex() }
    propertyEquals { previousIndex() }
}

public fun <T> CompareContext<Iterator<T>>.iteratorBehavior() {
    propertyEquals { hasNext() }

    while (expected.hasNext()) {
        propertyEquals { next() }
        propertyEquals { hasNext() }
    }
    propertyFails { next() }
}

public fun <T> CompareContext<Iterator<T>>.unorderedIteratorBehavior() {
    propertyEquals { hasNext() }
    val expectedValues = mutableListOf<T>()
    val actualValues = mutableListOf<T>()
    while (expected.hasNext()) {
        expectedValues.add(expected.next())
        actualValues.add(actual.next())
        propertyEquals { hasNext() }
    }
    propertyFails { next() }
    assertEquals(expectedValues.groupingBy { it }.eachCount(), actualValues.groupingBy { it }.eachCount())
}

public fun <T> CompareContext<Set<T>>.setBehavior(objectName: String = "", ordered: Boolean = false) {
    equalityBehavior(objectName, withToString = ordered)
    collectionBehavior(objectName)
    compareProperty({ iterator() }, { if (ordered) iteratorBehavior() else unorderedIteratorBehavior() })
}

public fun <K, V> CompareContext<Map<K, V>>.mapBehavior(ordered: Boolean = false) {
    equalityBehavior(withToString = ordered)
    propertyEquals { size }
    propertyEquals { isEmpty() }

    (object {}).let { propertyEquals { containsKey(it as Any?) } }

    if (expected.isEmpty().not())
        propertyEquals { contains(keys.first()) }

    propertyEquals { containsKey(keys.firstOrNull()) }
    propertyEquals { containsValue(values.firstOrNull()) }
    propertyEquals { get(null as Any?) }

    compareProperty({ keys }, { setBehavior("keySet", ordered) })
    compareProperty({ entries }, { setBehavior("entrySet", ordered) })
    compareProperty({ values }, { collectionBehavior("values") })
}

public fun <T> CompareContext<T>.equalityBehavior(objectName: String = "", withToString: Boolean = true) {
    val prefix = objectName + if (objectName.isNotEmpty()) "." else ""
    equals(objectName)
    propertyEquals(prefix + "hashCode") { hashCode() }
    if (withToString) {
        propertyEquals(prefix + "toString") { toString() }
    }
}


public fun <T> CompareContext<Collection<T>>.collectionBehavior(objectName: String = "") {
    val prefix = objectName + if (objectName.isNotEmpty()) "." else ""
    propertyEquals(prefix + "size") { size }
    propertyEquals(prefix + "isEmpty") { isEmpty() }

    (object {}).let { propertyEquals { contains(it as Any?) } }
    propertyEquals { contains(firstOrNull()) }
    propertyEquals { containsAll(this) }
    (object {}).let { propertyEquals { containsAll(listOf<Any?>(it)) } }
    propertyEquals { containsAll(listOf<Any?>(null)) }
}


