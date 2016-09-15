// FQNAME: MetaAnnotation

// FILE: MetaAnnotation.java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Deprecated
@Target({ ElementType.CONSTRUCTOR, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface MetaAnnotation {
    String strValue();
}

// FILE: Anno.kt
annotation class Anno