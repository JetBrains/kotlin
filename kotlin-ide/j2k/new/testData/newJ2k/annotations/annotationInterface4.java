@interface Anon {
    String[] value();
    int x() default 1;
}

@Anon("a", "b")
interface I1 {

}

@Anon("c", "d", x = 1)
interface I2 {

}

@Anon({"c", "d"}, x = 1)
interface I3 {

}

@Anon(value = {"c", "d"})
interface I4 {

}
