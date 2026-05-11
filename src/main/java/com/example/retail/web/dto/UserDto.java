package com.example.retail.web.dto;

import com.example.retail.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "User profile")
public record UserDto(
    @Schema(example = "1") Long id,
    @Schema(example = "alice@example.com") String email,
    @Schema(example = "Alice Anderson") String fullName,
    @Schema(example = "742 Evergreen Terrace, Springfield, OR 97477") String shippingAddress,
    Instant createdAt
) {
    public static UserDto of(User u) {
        return new UserDto(u.getId(), u.getEmail(), u.getFullName(), u.getShippingAddress(), u.getCreatedAt());
    }
}
