// Wasm validation phase won't pass if the function is returns a
//  string but treated as returning int at the call-site. This
//  has to be done for the Wasm test to pass.
fun foo() = 4