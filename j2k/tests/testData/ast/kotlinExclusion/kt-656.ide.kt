package demo
class Test() : java.lang.Iterable<String> {
override fun iterator() : java.util.Iterator<String> {
return null
}
public fun push(i : java.util.Iterator<String>) : java.util.Iterator<String> {
val j = i
return j
}
}
class FullTest() : java.lang.Iterable<String> {
override fun iterator() : java.util.Iterator<String> {
return null
}
public fun push(i : java.util.Iterator<String>) : java.util.Iterator<String> {
val j = i
return j
}
}