// simple
@available(*, deprecated, message: "simple")
public struct test {
}
// 
@available(*, deprecated, message: "")
public struct test {
}
// ∞
@available(*, deprecated, message: "∞")
public struct test {
}
// ∞
@available(*, deprecated, message: "∞")
public struct test {
}
// unicode∞symbol221e
@available(*, deprecated, message: "unicode∞symbol221e")
public struct test {
}
// with space
@available(*, deprecated, message: "with space")
public struct test {
}
// with	extensive
// white spacing
@available(*, deprecated, message: """
with	extensive
white spacing
""")
public struct test {
}
// 	
// 
@available(*, deprecated, message: """


""")
public struct test {
}
// \$
@available(*, deprecated, message: #"\$"#)
public struct test {
}
// "doubly-quoted"
@available(*, deprecated, message: #""doubly-quoted""#)
public struct test {
}
// 'singly-quoted'
@available(*, deprecated, message: "'singly-quoted'")
public struct test {
}
// `backticked`
@available(*, deprecated, message: "`backticked`")
public struct test {
}
// "#unescaped
@available(*, deprecated, message: ##""#unescaped"##)
public struct test {
}