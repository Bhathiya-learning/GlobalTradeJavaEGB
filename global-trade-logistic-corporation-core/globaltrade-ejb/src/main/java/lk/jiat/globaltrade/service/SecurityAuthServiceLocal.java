package lk.jiat.globaltrade.service;

import jakarta.ejb.Local;
import lk.jiat.globaltrade.dto.AuthResponse;
import lk.jiat.globaltrade.entity.User;
import lk.jiat.globaltrade.exception.GlobalTradeException;

@Local
public interface SecurityAuthServiceLocal {
    AuthResponse login(String email, String rawPassword);
    void logout(String token);
    User authenticate(String email, String rawPassword) throws GlobalTradeException;
    String getCurrentCallerName();
    boolean isCallerInRole(String roleName);
}
