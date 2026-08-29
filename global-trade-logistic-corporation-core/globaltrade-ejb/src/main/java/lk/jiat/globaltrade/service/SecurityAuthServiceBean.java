package lk.jiat.globaltrade.service;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.PermitAll;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lk.jiat.globaltrade.dto.AuthResponse;
import lk.jiat.globaltrade.entity.User;
import lk.jiat.globaltrade.exception.GlobalTradeException;
import lk.jiat.globaltrade.security.PasswordSecurityUtil;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Stateless
@DeclareRoles({"ADMIN", "CUSTOMS_INSPECTOR", "WAREHOUSE_MANAGER", "VENDOR_ANALYST", "CUSTOMER"})
@PermitAll
public class SecurityAuthServiceBean implements SecurityAuthServiceLocal {

    private static final Logger LOGGER = Logger.getLogger(SecurityAuthServiceBean.class.getName());

    @Resource
    private SessionContext sessionContext;

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public AuthResponse login(String email, String rawPassword) {
        if (email == null || rawPassword == null || email.trim().isEmpty() || rawPassword.trim().isEmpty()) {
            return AuthResponse.failure("Email and password are required.");
        }

        try {
            User user = authenticate(email, rawPassword);
            String token = "GTL-SESSION-" + UUID.randomUUID().toString().toUpperCase();

            LOGGER.info(String.format("[EJB SECURITY AUTH SUCCESS] User %s (%s) logged in with role %s",
                    user.getEmail(), user.getFirstName() + " " + user.getLastName(), user.getRole().getName()));

            return new AuthResponse(
                    true,
                    token,
                    "Authentication successful.",
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getRole().getName(),
                    user.getRole().getName()
            );
        } catch (GlobalTradeException e) {
            LOGGER.warning("[EJB SECURITY AUTH FAILED] Login attempt failed for " + email + ": " + e.getMessage());
            return AuthResponse.failure(e.getMessage());
        }
    }

    @Override
    public void logout(String token) {
        String caller = getCurrentCallerName();
        LOGGER.info(String.format("[EJB SECURITY LOGOUT SUCCESS] Session token %s terminated for user %s", token, caller));
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public User authenticate(String email, String rawPassword) throws GlobalTradeException {
        List<User> users = em.createQuery("SELECT u FROM User u JOIN FETCH u.role WHERE u.email = :email", User.class)
                             .setParameter("email", email)
                             .getResultList();

        if (users.isEmpty()) {
            throw new GlobalTradeException("Invalid user credentials.");
        }

        User user = users.get(0);
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new GlobalTradeException("⚠️ Your account has been suspended or set to INACTIVE by system administration. Access denied.");
        }

        boolean passwordValid = PasswordSecurityUtil.verifyPassword(rawPassword, user.getSalt(), user.getPassword());

        if (!passwordValid) {
            throw new GlobalTradeException("Invalid credentials provided for user: " + email);
        }

        return user;
    }

    @Override
    public String getCurrentCallerName() {
        if (sessionContext != null && sessionContext.getCallerPrincipal() != null) {
            return sessionContext.getCallerPrincipal().getName();
        }
        return "ANONYMOUS";
    }

    @Override
    public boolean isCallerInRole(String roleName) {
        if (sessionContext != null) {
            return sessionContext.isCallerInRole(roleName);
        }
        return false;
    }
}
