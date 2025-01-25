package com.pacifici.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CacheOperation {
    String operation(); // Possible values: "add", "get", "update", "delete"
    String cacheName() default "defaultCache"; // Name of the cache
}
