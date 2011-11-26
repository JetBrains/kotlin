package jet.runtime;

import java.util.ArrayList;

/**
* @author alex.tkachman
*/
class JetNullPointerException extends NullPointerException {
    @Override
    public synchronized Throwable fillInStackTrace() {
        super.fillInStackTrace();
        StackTraceElement[] stackTrace = getStackTrace();
        ArrayList<StackTraceElement> list = new ArrayList<StackTraceElement>();
        boolean skip = true;
        for(StackTraceElement ste : stackTrace) {
            if(!skip) {
                list.add(ste);
            }
            else {
                if("jet.runtime.Intrinsics".equals(ste.getClassName()) && "npe".equals(ste.getMethodName())) {
                    skip = false;
                }
            }
        }
        setStackTrace(list.toArray(new StackTraceElement[list.size()]));
        return this;
    }

    public static void main(String[] args) {
        Intrinsics.npe(null);
    }
}
