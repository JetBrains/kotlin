// IS_APPLICABLE: false
// ERROR: There's a cycle in the delegation calls chain
// ERROR: There's a cycle in the delegation calls chain

class NonReachableLoop {
    constructor<caret>(x: String)

    constructor(x: Int, y: Int): this(x + y)

    constructor(x: Int): this(x, x)
}