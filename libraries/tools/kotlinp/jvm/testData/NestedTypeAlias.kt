// LANGUAGE: +NestedTypeAliases
// IGNORE_BACKEND_K1: JVM_IR

class C<X> {
    typealias Nested<Y> = C<Y>
}
