package test

class InClassObject {
    companion object {
        class ClassObjectClass {}

        interface ClassObjectTrait {}

        interface ClassObjectTraitWithImpl {
            fun foo() {}
        }

        object ClassObjectObject() {}
    }
}

// SEARCH_TEXT: ClassObject
// REF: (in test.InClassObject.Companion).ClassObjectClass
// REF: (in test.InClassObject.Companion).ClassObjectObject
// REF: (in test.InClassObject.Companion).ClassObjectTrait
// REF: (in test.InClassObject.Companion).ClassObjectTraitWithImpl