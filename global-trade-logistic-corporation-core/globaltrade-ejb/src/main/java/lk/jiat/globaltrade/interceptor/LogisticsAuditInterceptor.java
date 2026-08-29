package lk.jiat.globaltrade.interceptor;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import lk.jiat.globaltrade.annotation.LogisticsAudited;
import lk.jiat.globaltrade.service.AuditLogServiceLocal;

import java.lang.reflect.Method;
import java.util.logging.Logger;

@LogisticsAudited
@Interceptor
public class LogisticsAuditInterceptor {

    private static final Logger LOGGER = Logger.getLogger(LogisticsAuditInterceptor.class.getName());

    @Resource
    private SessionContext sessionContext;

    @EJB
    private AuditLogServiceLocal auditLogService;

    @AroundInvoke
    public Object auditExecution(InvocationContext context) throws Exception {
        long startTime = System.currentTimeMillis();
        Method method = context.getMethod();
        String callerPrincipal = "SYSTEM";
        
        try {
            if (sessionContext != null && sessionContext.getCallerPrincipal() != null) {
                callerPrincipal = sessionContext.getCallerPrincipal().getName();
            }
        } catch (Exception e) {
            // Fallback when executed outside active EJB security context
        }

        LogisticsAudited annotation = method.getAnnotation(LogisticsAudited.class);
        if (annotation == null) {
            annotation = method.getDeclaringClass().getAnnotation(LogisticsAudited.class);
        }

        String actionCode = (annotation != null) ? annotation.actionCode() : "METHOD_INVOCATION";
        String targetEntity = (annotation != null) ? annotation.targetEntity() : method.getDeclaringClass().getSimpleName();

        LOGGER.info(String.format("[EJB AUDIT START] Method: %s | User: %s | Action: %s", method.getName(), callerPrincipal, actionCode));

        boolean success = true;
        Object result = null;

        try {
            result = context.proceed();
            return result;
        } catch (Exception ex) {
            success = false;
            LOGGER.warning(String.format("[EJB AUDIT FAILURE] Method: %s | Error: %s", method.getName(), ex.getMessage()));
            throw ex;
        } finally {
            long executionTimeMs = System.currentTimeMillis() - startTime;
            LOGGER.info(String.format("[EJB AUDIT END] Method: %s | Duration: %d ms | Success: %b", method.getName(), executionTimeMs, success));
            
            try {
                if (auditLogService != null) {
                    auditLogService.recordAudit(callerPrincipal, actionCode, targetEntity, null, executionTimeMs, success, "127.0.0.1", "Method: " + method.getName());
                }
            } catch (Exception auditEx) {
                LOGGER.severe("Failed to persist audit log entry: " + auditEx.getMessage());
            }
        }
    }
}
