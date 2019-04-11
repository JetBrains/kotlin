@interface Ann {
    Class<?> value();
    Class<?> other();
}

@Ann(other = String.class, value = Object.class)
class C {
}