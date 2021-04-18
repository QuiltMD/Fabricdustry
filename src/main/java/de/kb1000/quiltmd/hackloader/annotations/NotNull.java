package de.kb1000.quiltmd.hackloader.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({
        ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.METHOD, ElementType.PACKAGE, ElementType.PARAMETER,
        ElementType.TYPE_USE
})
@Retention(RetentionPolicy.SOURCE)
public @interface NotNull {
}
