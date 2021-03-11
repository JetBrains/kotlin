// "Remove 'in' modifier" "true"
interface A<T> {}

class B : A<<caret>in Int> {}
