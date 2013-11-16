package demo
open class Test() : java.lang.Iterable<String> {
public override fun iterator() : java.util.Iterator<String> {
return null
}
public open fun push(i : java.util.Iterator<String>) : java.util.Iterator<String> {
val j = i
return j
}
}
open class FullTest() : java.lang.Iterable<String> {
public override fun iterator() : java.util.Iterator<String> {
return null
}
public open fun push(i : java.util.Iterator<String>) : java.util.Iterator<String> {
val j = i
return j
}
}