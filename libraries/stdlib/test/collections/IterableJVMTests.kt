package test.collections

import java.util.*

class LinkedListTest : OrderedIterableTests<LinkedList<String>>(linkedListOf("foo", "bar"), linkedListOf<String>())

