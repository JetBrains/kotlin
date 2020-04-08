// IS_APPLICABLE: false
class A {
    constructor() {
        a = 1
    }

    constructor(aa: Int) {
        a = aa
    }

    val a<caret>: Int
}
