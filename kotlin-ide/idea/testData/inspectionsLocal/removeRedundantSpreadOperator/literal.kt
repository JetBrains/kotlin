annotation class Ann(vararg val value: String)

@Ann(<caret>*["a", "b"])
class Test