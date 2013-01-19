package org.test
import java.util.LinkedList
open class Member() {
}
open class User() {
open fun main() : Unit {
var members : List<Member?>? = LinkedList<Member?>()
members?.add(Member())
}
}