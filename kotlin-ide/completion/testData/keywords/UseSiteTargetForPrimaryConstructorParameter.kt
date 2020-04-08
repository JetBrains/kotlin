annotation class Ann

class Completion(@get:Ann val p1: String, @<caret>)

// EXIST: get
// EXIST: set
// EXIST: field
// EXIST: param
// EXIST: setparam
// EXIST: property

/*TODO: in fact is not applicable */
// EXIST: receiver
// EXIST: delegate
// EXIST: file
// NOTHING_ELSE
