// LANGUAGE_VERSION: 1.4
// PROBLEM: none
// DISABLE-ERRORS

interface Foo

annotation class Ann

class E : @field:Ann @get:Ann @set:Ann @setparam:Ann Foo

interface G : <caret>@Ann Foo