package lk.jiat.globaltrade.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import lk.jiat.globaltrade.annotation.TradeCompliant;

import java.util.logging.Logger;

@TradeCompliant
@Interceptor
public class TradeComplianceInterceptor {

    private static final Logger LOGGER = Logger.getLogger(TradeComplianceInterceptor.class.getName());

    @AroundInvoke
    public Object checkCompliance(InvocationContext context) throws Exception {
        LOGGER.info("[TRADE COMPLIANCE INTERCEPTOR] Verifying WTO trade rules for " + context.getMethod().getName());
        return context.proceed();
    }
}
