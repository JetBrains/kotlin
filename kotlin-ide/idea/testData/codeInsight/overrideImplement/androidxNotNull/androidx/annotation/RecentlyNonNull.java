package androidx.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER, ElementType.TYPE})
public @interface RecentlyNonNull {
}
