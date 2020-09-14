fun foo() {
    10 to nu<caret> // stdlib Tuples.kt function producing a Pair
}

// ORDER: null
// ORDER: arrayOfNulls
// ORDER: enumValueOf
// ORDER: enumValues