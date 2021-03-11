fun foo(o: Any) {
<caret>    somethingElse(o as String/* we should not remove this cast because it's not in pasted range*/)
}