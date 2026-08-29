package lk.jiat.globaltrade.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import lk.jiat.globaltrade.annotation.PerformanceMonitored;

import java.lang.reflect.Method;
import java.util.logging.Logger;

@PerformanceMonitored
@Interceptor
public class PerformanceMonitorInterceptor {

    private static final Logger LOGGER = Logger.getLogger(PerformanceMonitorInterceptor.class.getName());

    @AroundInvoke
    public Object monitorPerformance(InvocationContext context) throws Exception {
        long start = System.currentTimeMillis();
        Method method = context.getMethod();

        PerformanceMonitored annotation = method.getAnnotation(PerformanceMonitored.class);
        long threshold = (annotation != null) ? annotation.thresholdMs() : 500L;

        try {
            return context.proceed();
        } finally {
            long duration = System.currentTimeMillis() - start;
            if (duration > threshold) {
                LOGGER.warning(String.format("[SLA PERFORMANCE BREACH] Method %s took %d ms (Threshold: %d ms)", method.getName(), duration, threshold));
            } else {
                LOGGER.info(String.format("[PERFORMANCE OK] Method %s took %d ms", method.getName(), duration));
            }
        }
    }
}
