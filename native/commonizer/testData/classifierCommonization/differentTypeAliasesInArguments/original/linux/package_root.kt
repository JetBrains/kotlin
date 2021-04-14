typealias my_long_t = my_linux_long_t
typealias my_linux_long_t = common.stuff.MyLong
typealias MyTypeAlias = common.stuff.Wrapper<my_long_t>

val property: MyTypeAlias = TODO()
