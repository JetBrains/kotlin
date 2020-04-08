typealias testGlobal = Any

fun some() {
    typealias testInFun = Any
}

interface SomeTrait {
    typealias testInTrait = Any
}

class Some() {
    typealias testInClass = Any

    companion object {
        typealias testInClassObject = Any
    }
}

// SEARCH_TEXT: test
// REF: (<root>).testGlobal
// REF: (in Some).testInClass
// REF: (in Some.Companion).testInClassObject
// REF: (in SomeTrait).testInTrait