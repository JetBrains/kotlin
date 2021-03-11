interface A<T> {}
interface B<T> {}
interface C<T> {}
interface D<T> {}

interface Test : A<<error>in</error> Int>, B<<error>out</error> Int>, C<<error>*</error>><error>?</error><warning>?</warning><warning>?</warning>, D<Int> {}