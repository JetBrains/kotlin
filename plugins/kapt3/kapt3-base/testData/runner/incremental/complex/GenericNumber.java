package test;

import java.util.HashSet;

public class GenericNumber {
    <T extends Runnable&Cloneable> java.util.HashSet<? super java.lang.Number> usingGenerics(HashSet<? extends CharSequence> set) {
        return null;
    }
}