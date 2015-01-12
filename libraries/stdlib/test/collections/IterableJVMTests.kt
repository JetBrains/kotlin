package test.collections

import java.util.*

class LinkedSetTest : OrderedIterableTests<Set<String>>(setOf("foo", "bar"), setOf<String>())
class LinkedListTest : OrderedIterableTests<LinkedList<String>>(linkedListOf("foo", "bar"), linkedListOf<String>())

