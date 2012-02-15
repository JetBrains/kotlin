/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jet.runtime;

import jet.Function0;

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
    
    public static <T> Class<T> getJavaClass(T self) {
        return (Class<T>) self.getClass();
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

    public static <R> R stupidSync(Object lock, Function0<R> block) {
        synchronized (lock) {
            return block.invoke();
        }
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
