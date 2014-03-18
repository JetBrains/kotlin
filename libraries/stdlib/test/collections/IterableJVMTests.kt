package test.collections

import org.junit.Test
import kotlin.test.*
import java.util.*

class LinkedSetTest : OrderedIterableTests<Set<String>>(setOf("foo", "bar"), setOf<String>())
class LinkedListTest : OrderedIterableTests<LinkedList<String>>(linkedListOf("foo", "bar"), linkedListOf<String>())

