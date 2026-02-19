package test;

@NumberAnnotation
public class NumberHolder<T extends MyNumber> extends java.util.HashSet<T> implements Runnable {

    private NumberManager manager;

    String getStringValue(NumberManager usingManager) {
        return null;
    }

    public void run() throws NumberException {
    }

    class MyInnerClass {}
}