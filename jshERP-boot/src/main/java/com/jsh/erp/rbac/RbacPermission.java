package com.jsh.erp.rbac;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RbacPermission {
    String resource() default "";

    RbacMode mode() default RbacMode.AUTO;

    String statusField() default "status";

    boolean publicAccess() default false;

    boolean loginOnly() default false;

    boolean adminOnly() default false;
}
