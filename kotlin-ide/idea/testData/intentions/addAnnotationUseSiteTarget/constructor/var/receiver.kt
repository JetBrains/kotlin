// CHOOSE_USE_SITE_TARGET: receiver
// IS_APPLICABLE: false

annotation class A

class Constructor(@A<caret> var foo: String)