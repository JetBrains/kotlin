@file:kotlin.jvm.JvmVersion
package test.collections

import java.util.*

class LinkedListTest : OrderedIterableTests<LinkedList<String>>(LinkedList(listOf("foo", "bar")), LinkedList<String>())

