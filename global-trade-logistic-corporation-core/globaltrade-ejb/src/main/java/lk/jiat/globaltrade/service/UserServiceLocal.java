package lk.jiat.globaltrade.service;

import jakarta.ejb.Local;
import lk.jiat.globaltrade.entity.User;
import lk.jiat.globaltrade.exception.GlobalTradeException;

import java.util.List;

@Local
public interface UserServiceLocal {
    List<User> getAllUsers();
    User getUserById(Long id);
    User createUser(String firstName, String lastName, String email, String rawPassword, String mobile, Long genderId, Long roleId, Long warehouseId, Long vendorId) throws GlobalTradeException;
    User toggleUserStatus(Long userId) throws GlobalTradeException;
    User updateUserRole(Long userId, Long newRoleId) throws GlobalTradeException;
}
