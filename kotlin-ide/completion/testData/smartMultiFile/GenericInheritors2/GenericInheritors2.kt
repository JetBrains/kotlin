import p.*

fun foo(): KotlinTrait<I1, I1> {
    return <caret>
}

// EXIST: { lookupString: "object", itemText: "object : KotlinTrait<I1, I1>{...}" }
// EXIST: { lookupString: "KotlinInheritor", itemText: "KotlinInheritor", tailText: "() (p)" }
