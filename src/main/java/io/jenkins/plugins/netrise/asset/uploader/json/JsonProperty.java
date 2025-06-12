package io.jenkins.plugins.netrise.asset.uploader.json;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Map the names of the fields to represent in JSON
 * */
@Documented
@Target(FIELD)
@Retention(RUNTIME)
public @interface JsonProperty {
    /** JSON name of the field */
    String value();

    /** Option to exclude the field from the JSON */
    boolean enabled() default true;
}
