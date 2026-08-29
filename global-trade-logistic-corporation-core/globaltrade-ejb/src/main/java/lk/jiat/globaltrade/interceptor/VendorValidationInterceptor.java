package lk.jiat.globaltrade.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import lk.jiat.globaltrade.annotation.VendorValidated;
import lk.jiat.globaltrade.exception.VendorNonCompliantException;

import java.util.logging.Logger;

@VendorValidated
@Interceptor
public class VendorValidationInterceptor {

    private static final Logger LOGGER = Logger.getLogger(VendorValidationInterceptor.class.getName());

    @AroundInvoke
    public Object validateVendorState(InvocationContext context) throws Exception {
        Object[] parameters = context.getParameters();
        
        LOGGER.info("[VENDOR INTERCEPTOR] Validating vendor parameter state for " + context.getMethod().getName());

        for (Object param : parameters) {
            if (param instanceof String && ((String) param).startsWith("NON_COMPLIANT")) {
                LOGGER.warning("[VENDOR INTERCEPTOR REJECTED] Vendor parameter indicated non-compliant state: " + param);
                throw new VendorNonCompliantException((String) param, "NON_COMPLIANT");
            }
        }

        return context.proceed();
    }
}
