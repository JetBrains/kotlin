fun foo(p: Int) {
    if (p > 0) {

    }
    <caret>
}

// EXIST: else
// EXIST: class
// EXIST: do
// EXIST: false
// EXIST: for
// EXIST: fun
// EXIST: if
// EXIST: null
// EXIST: object
// EXIST: return
// EXIST: super
// EXIST: throw
// EXIST: interface
// EXIST: true
// EXIST: try
// EXIST: val
// EXIST: var
// EXIST: when
// EXIST: while
// EXIST: typealias
// EXIST: as
// NOTHING_ELSE
