typealias my_long_t = common.stuff.MyLong
typealias MyTypeAlias = common.stuff.Wrapper<my_long_t>

expect val property: MyTypeAlias
