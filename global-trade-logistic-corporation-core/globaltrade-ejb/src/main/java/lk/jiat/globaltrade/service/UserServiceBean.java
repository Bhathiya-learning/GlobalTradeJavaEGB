package lk.jiat.globaltrade.service;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lk.jiat.globaltrade.annotation.LogisticsAudited;
import lk.jiat.globaltrade.annotation.PerformanceMonitored;
import lk.jiat.globaltrade.entity.*;
import lk.jiat.globaltrade.exception.GlobalTradeException;
import lk.jiat.globaltrade.interceptor.LogisticsAuditInterceptor;
import lk.jiat.globaltrade.interceptor.PerformanceMonitorInterceptor;
import lk.jiat.globaltrade.security.PasswordSecurityUtil;

import java.util.List;
import java.util.logging.Logger;

@Stateless
@DeclareRoles({"ADMIN", "LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER", "CUSTOMS_OFFICIAL", "VENDOR_REPRESENTATIVE"})
@Interceptors({LogisticsAuditInterceptor.class, PerformanceMonitorInterceptor.class})
public class UserServiceBean implements UserServiceLocal {

    private static final Logger LOGGER = Logger.getLogger(UserServiceBean.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Override
    @RolesAllowed({"ADMIN"})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<User> getAllUsers() {
        return em.createQuery("SELECT u FROM User u JOIN FETCH u.role ORDER BY u.id DESC", User.class).getResultList();
    }

    @Override
    @RolesAllowed({"ADMIN"})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public User getUserById(Long id) {
        return em.find(User.class, id);
    }

    @Override
    @RolesAllowed({"ADMIN"})
    @LogisticsAudited(actionCode = "USER_CREATE", targetEntity = "User")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public User createUser(String firstName, String lastName, String email, String rawPassword, String mobile, Long genderId, Long roleId, Long warehouseId, Long vendorId) throws GlobalTradeException {
        if (firstName == null || firstName.trim().isEmpty() ||
            lastName == null || lastName.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new GlobalTradeException("All required user fields (First Name, Last Name, Email, Password) must be provided.");
        }

        String cleanEmail = email.trim().toLowerCase();
        if (!cleanEmail.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            throw new GlobalTradeException("Invalid email address format (e.g. name@company.com).");
        }

        if (rawPassword.length() < 8 ||
            !rawPassword.matches(".*[A-Z].*") ||
            !rawPassword.matches(".*[a-z].*") ||
            !rawPassword.matches(".*[0-9].*")) {
            throw new GlobalTradeException("Password is too weak. Must be at least 8 characters long and contain uppercase, lowercase, and numeric digits.");
        }

        // Validate unique email
        List<User> existing = em.createQuery("SELECT u FROM User u WHERE LOWER(u.email) = :email", User.class)
                                .setParameter("email", cleanEmail)
                                .getResultList();
        if (!existing.isEmpty()) {
            throw new GlobalTradeException("User email " + cleanEmail + " is already registered in the system.");
        }

        Gender gender = em.find(Gender.class, genderId != null ? genderId : 1L);
        Role role = em.find(Role.class, roleId != null ? roleId : 1L);
        Warehouse warehouse = warehouseId != null ? em.find(Warehouse.class, warehouseId) : null;
        Vendor vendor = vendorId != null ? em.find(Vendor.class, vendorId) : null;

        if (gender == null || role == null) {
            throw new GlobalTradeException("Invalid gender or role specified for user creation.");
        }

        String salt = PasswordSecurityUtil.generateSalt();
        String hashedPassword = PasswordSecurityUtil.hashPassword(rawPassword, salt);

        User user = new User(null, firstName, lastName, email, hashedPassword, mobile != null ? mobile : "+1-555-0199", gender, role, vendor, warehouse, true);
        user.setSalt(salt);

        em.persist(user);
        LOGGER.info(String.format("[EJB USER CREATED] Registered %s (%s) with salted SHA-256 password", email, role.getName()));
        return user;
    }

    @Override
    @RolesAllowed({"ADMIN"})
    @LogisticsAudited(actionCode = "USER_STATUS_TOGGLE", targetEntity = "User")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public User toggleUserStatus(Long userId) throws GlobalTradeException {
        User user = em.find(User.class, userId);
        if (user == null) {
            throw new GlobalTradeException("User not found for ID: " + userId);
        }
        user.setIsActive(!user.getIsActive());
        em.merge(user);
        LOGGER.info(String.format("[EJB USER STATUS TOGGLED] User %s active state set to %b", user.getEmail(), user.getIsActive()));
        return user;
    }

    @Override
    @RolesAllowed({"ADMIN"})
    @LogisticsAudited(actionCode = "USER_ROLE_UPDATE", targetEntity = "User")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public User updateUserRole(Long userId, Long newRoleId) throws GlobalTradeException {
        User user = em.find(User.class, userId);
        Role role = em.find(Role.class, newRoleId);
        if (user == null || role == null) {
            throw new GlobalTradeException("Invalid user or role ID for role update.");
        }
        user.setRole(role);
        em.merge(user);
        LOGGER.info(String.format("[EJB USER ROLE UPDATED] User %s role changed to %s", user.getEmail(), role.getName()));
        return user;
    }
}
