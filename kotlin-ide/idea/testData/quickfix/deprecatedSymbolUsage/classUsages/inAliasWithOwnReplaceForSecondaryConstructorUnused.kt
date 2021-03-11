// "Replace with 'NewClass'" "true"

@Deprecated("", ReplaceWith("NewClass"))
class OldClass constructor()  {
    @Deprecated("", ReplaceWith("NewClass(12)")) constructor(i: Int): this()
}

class NewClass(p: Int = 0)

typealias Old = <caret>OldClass

val a: Old = Old()