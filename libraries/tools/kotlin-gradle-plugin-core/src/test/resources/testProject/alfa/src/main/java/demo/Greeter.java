package demo;

/**
 * Created by Nikita.Skvortsov
 * Date: 3/1/13, 10:53 AM
 */
public class Greeter {
    private final String myGreeting;

    public Greeter(String greeting) {
        myGreeting = greeting;
    }

    public String getGreeting() {
        return myGreeting;
    }
}
