package io.github.vikaschaudharyarit_max.smartdbf.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DbfColumn {

    String value();
}
