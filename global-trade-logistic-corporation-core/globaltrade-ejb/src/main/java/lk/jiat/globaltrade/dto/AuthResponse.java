package lk.jiat.globaltrade.dto;

import java.io.Serializable;

public class AuthResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private String token;
    private String message;
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String roleCode;
    private String roleName;

    public AuthResponse() {}

    public AuthResponse(boolean success, String token, String message, Long userId, String firstName, String lastName, String email, String roleCode, String roleName) {
        this.success = success;
        this.token = token;
        this.message = message;
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.roleCode = roleCode;
        this.roleName = roleName;
    }

    public static AuthResponse failure(String message) {
        return new AuthResponse(false, null, message, null, null, null, null, null, null);
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
}
