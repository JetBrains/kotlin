// FILE: androidx/annotation/RecentlyNullable.java
package androidx.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
@interface RecentlyNullable {}

// FILE: androidx/annotation/Box.java
package androidx.annotation;

public interface Box {
    @RecentlyNullable
    public String foo();
}

// FILE: test.kt
package app

import androidx.annotation.Box

class KBox(val delegate: Box) : Box by delegate