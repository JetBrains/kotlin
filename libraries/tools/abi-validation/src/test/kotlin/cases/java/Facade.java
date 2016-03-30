package cases.java;

class Part1 {
    public static void publicMethod(int param) { }

    public static class Part2 extends Part1 {
        public static void publicMethod(String param) { }
    }
}


public class Facade extends Part1.Part2 { }
