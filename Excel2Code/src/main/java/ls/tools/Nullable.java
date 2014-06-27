package ls.tools;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Indicates an expression that may be null
 */
@Target({ElementType.PARAMETER,ElementType.METHOD})
public @interface Nullable {
}
