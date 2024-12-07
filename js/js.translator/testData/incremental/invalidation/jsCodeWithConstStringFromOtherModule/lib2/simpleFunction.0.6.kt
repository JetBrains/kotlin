fun simpleFunction(): String {
    return js("var testObj = { $CONST_KEY: '$CONST_VALUE' }; testObj.$CONST_KEY") as String
}
