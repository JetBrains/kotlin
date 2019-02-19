/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.collections.behaviors

import test.collections.CompareContext

public fun <T> CompareContext<List<T>>.listBehavior() {
    equalityBehavior()
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

public fun <T> CompareContext<Set<T>>.setBehavior(objectName: String = "") {
    equalityBehavior(objectName)
    collectionBehavior(objectName)
    compareProperty({ iterator() }, { iteratorBehavior() })
}

public fun <K, V> CompareContext<Map<K, V>>.mapBehavior() {
    equalityBehavior()
    propertyEquals { size }
    propertyEquals { isEmpty() }

    (object {}).let { propertyEquals { containsKey(it as Any?) } }

    if (expected.isEmpty().not())
        propertyEquals { contains(keys.first()) }

    propertyEquals { containsKey(keys.firstOrNull()) }
    propertyEquals { containsValue(values.firstOrNull()) }
    propertyEquals { get(null as Any?) }

    compareProperty({ keys }, { setBehavior("keySet") })
    compareProperty({ entries }, { setBehavior("entrySet") })
    compareProperty({ values }, { collectionBehavior("values") })
}

public fun <T> CompareContext<T>.equalityBehavior(objectName: String = "") {
    val prefix = objectName + if (objectName.isNotEmpty()) "." else ""
    equals(objectName)
    propertyEquals(prefix + "hashCode") { hashCode() }
    propertyEquals(prefix + "toString") { toString() }
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


