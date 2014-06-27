package ls.tools;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 */
@Target({ElementType.METHOD,ElementType.PARAMETER})
public @interface NotNull {
}
