package org.example.app;

public class Main {
    public static void main(String[] args) {
        System.out.println(new org.example.k.Greeter().greet() + " from " + Main.class.getModule().getName());
    }
}
