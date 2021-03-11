open class Older { fun upper() {} }
class Younger : Older()
public fun Younger.<caret>outer() { upper() }