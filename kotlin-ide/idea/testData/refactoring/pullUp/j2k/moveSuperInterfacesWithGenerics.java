// INFO: {"checked": "true"}
interface X<T, U> {

}

// INFO: {"checked": "false"}
interface Y<V> {

}

// INFO: {"checked": "true"}
interface Z<T> {

}

class P {

}

class <caret>B<S, U, V> extends A<S> implements X<P, S>, Y<U>, Z<V> {

}