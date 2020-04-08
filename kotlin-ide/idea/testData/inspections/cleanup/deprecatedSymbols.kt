package pack

@Deprecated("", ReplaceWith("bar(p + 1)"))
public fun oldFun1(p: Int): Int = 0

@Deprecated("", ReplaceWith("bar(-1)"))
public fun oldFun1(): Int = 0

@Deprecated("", ReplaceWith("bar(p + 2)"))
public fun oldFun2(p: Int): Int = 0

public fun oldFun2(): Int = 0

@Deprecated("", ReplaceWith("bar(p)"))
public fun oldFun3(p: Int): Int = 0

public fun bar(p: Int): Int = p