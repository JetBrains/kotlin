// LANGUAGE: +NestedTypeAliases

class C<X> {
    typealias Nested<Y> = C<Y>
}
