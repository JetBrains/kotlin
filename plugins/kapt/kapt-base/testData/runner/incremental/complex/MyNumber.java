package test;

import java.lang.annotation.*;
import java.util.*;
import static java.lang.annotation.ElementType.*;


public class MyNumber extends @TypeUseAnnotation HashSet {
    @FieldAnnotation
    private String value;

    @MethodAnnotation
    private void getPrintedValue(@ParameterAnnotation String format) throws @ThrowTypeUseAnnotation RuntimeException{
    }

    private <@AnotherTypeUseAnnotation T extends Number> void accept(T visitor) {
    }
}

@interface FieldAnnotation {}
@interface MethodAnnotation {}
@interface ParameterAnnotation {}

@Target(value={TYPE_PARAMETER, TYPE_USE})
@interface TypeUseAnnotation {}

@Target(value={TYPE_PARAMETER, TYPE_USE})
@interface AnotherTypeUseAnnotation {}


@Target(value={TYPE_PARAMETER, TYPE_USE})
@interface ThrowTypeUseAnnotation {}