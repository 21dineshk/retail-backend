package com.example.retail.web;

import com.example.retail.repository.UserRepository;
import com.example.retail.web.dto.UserDto;
import com.example.retail.web.error.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User profiles. There is no real authentication — user_id is passed in the path.")
public class UserController {

    private final UserRepository repo;

    public UserController(UserRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    @Operation(summary = "List all users",
        description = "Helpful for tests: returns the seeded test users so you can pick a user_id to act as.")
    public List<UserDto> list() {
        return repo.findAll().stream().map(UserDto::of).toList();
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get one user by id")
    public UserDto get(@PathVariable Long userId) {
        return repo.findById(userId).map(UserDto::of)
            .orElseThrow(() -> ApiException.notFound("User " + userId));
    }

    @GetMapping("/by-email/{email}")
    @Operation(summary = "Look up a user by email")
    public UserDto getByEmail(@PathVariable String email) {
        return repo.findByEmail(email).map(UserDto::of)
            .orElseThrow(() -> ApiException.notFound("User " + email));
    }
}
