package cl.beauty.requestable.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cl.beauty.requestable.enums.EnumRequestableType;



@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestableClass{
	String	identifier();
	EnumRequestableType type();
	String render() default "";
}
