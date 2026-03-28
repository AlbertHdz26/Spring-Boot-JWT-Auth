package com.example.jwtauth.token;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    @Query("select rt from RefreshToken rt where rt.user.id = :userId")
    List<RefreshToken> findAllByUserId(@Param("userId") UUID userId);
}
