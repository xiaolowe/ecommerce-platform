package com.example.commerce.auth.user;

import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface UserAccountMapper {

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findById(String id);

    int countByEmail(String email);

    int insert(UserAccount user);

    default boolean existsByEmail(String email) {
        return countByEmail(email) > 0;
    }
}
