// "Add type 'Array<String>' to parameter 'value'" "true"
// LANGUAGE_VERSION: 1.2

annotation class CollectionDefault(val value = ["alpha", "beta"]<caret>)