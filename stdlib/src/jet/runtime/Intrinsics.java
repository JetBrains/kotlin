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

    public static Object sure(Object self) {
        if(self == null)
            return throwNpe();
        return self;
    }

    private static Object throwNpe() {
        throw new JetNullPointerException();
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
                if("jet.runtime.Intrinsics".equals(ste.getClassName()) && "sure".equals(ste.getMethodName())) {
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
