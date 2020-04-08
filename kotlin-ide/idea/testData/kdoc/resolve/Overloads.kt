/**
 * @see ContainingClass.classOv<caret>erload
 */
class DocReferrer

class ContainingClass {
    fun classOverload() {}
    fun classOverload(p: String) {}
}

// REF: (in ContainingClass).classOverload()