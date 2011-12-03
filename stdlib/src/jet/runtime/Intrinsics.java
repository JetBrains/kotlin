package jet.runtime;

import java.util.ArrayList;

/**
 * @author alex.tkachman
 */
public class Intrinsics {
    private Intrinsics() {
    }

    public static String stringPlus(String self, Object other) {
        return ((self == null) ? "null" : self) + ((other == null) ? "null" : other.toString());
    }

    public static void throwNpe() {
        throw new JetNullPointerException();
    }

    public static int compare(long thisVal, long anotherVal) {
        return (thisVal<anotherVal ? -1 : (thisVal==anotherVal ? 0 : 1));
    }

    public static int compare(int thisVal, int anotherVal) {
        return (thisVal<anotherVal ? -1 : (thisVal==anotherVal ? 0 : 1));
    }
    
    public static int compare(boolean thisVal, boolean anotherVal) {
        return (thisVal == anotherVal ? 0 : (anotherVal ? 1 : -1));
    }

    private static Throwable sanitizeStackTrace(Throwable throwable) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        ArrayList<StackTraceElement> list = new ArrayList<StackTraceElement>();
        boolean skip = true;
        for(StackTraceElement ste : stackTrace) {
            if(!skip) {
                list.add(ste);
            }
            else {
                if("jet.runtime.Intrinsics".equals(ste.getClassName()) && "throwNpe".equals(ste.getMethodName())) {
                    skip = false;
                }
            }
        }
        throwable.setStackTrace(list.toArray(new StackTraceElement[list.size()]));
        return throwable;
    }

    private static class JetNullPointerException extends NullPointerException {
        @Override
        public synchronized Throwable fillInStackTrace() {
            super.fillInStackTrace();
            return sanitizeStackTrace(this);
        }
    }
}
