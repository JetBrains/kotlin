package usage

class Usage(val x: Int) {
    constructor() : this(inline.f())
}