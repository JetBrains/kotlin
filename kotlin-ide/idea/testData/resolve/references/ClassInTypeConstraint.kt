package resolve

open class AA {}
class BB<T : <caret>AA> {}

// REF: (resolve).AA