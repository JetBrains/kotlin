// "class org.jetbrains.kotlin.idea.quickfix.SuperClassNotInitialized$AddParenthesisFix" "false"
// ERROR: This type has a constructor, and thus must be initialized here
open class Base private constructor(p: Int)

class C : Base<caret>
