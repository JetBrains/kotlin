package cases.java;

class Part1 {
    public static void publicMethod(String param) { }
}

class Part2 extends Part1 {
    public static void publicMethod(int param) { }
}

public class Facade extends Part2 { }
