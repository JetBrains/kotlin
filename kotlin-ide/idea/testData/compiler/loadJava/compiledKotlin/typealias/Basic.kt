//ALLOW_AST_ACCESS
package test

typealias S = String
typealias SS = S
typealias SSS = SS

val x1: S = { "" }()
val x2: SS = { "" }()
val x3: SSS = { "" }()

val x4: S? = { "" }()
val x5: SS? = { "" }()
val x6: SSS? = { "" }()
