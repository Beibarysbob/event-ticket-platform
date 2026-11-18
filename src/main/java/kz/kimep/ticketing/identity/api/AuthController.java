package kz.kimep.ticketing.identity.api;

import jakarta.validation.Valid;
import kz.kimep.ticketing.identity.service.CurrentUser;
import kz.kimep.ticketing.identity.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final IdentityService service;
    private final CurrentUser current;

    @PostMapping("/register")
    public ResponseEntity<AuthDtos.Tokens> register(@Valid @RequestBody AuthDtos.Register request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request));
    }

    @PostMapping("/login")
    public AuthDtos.Tokens login(@Valid @RequestBody AuthDtos.Login request) {
        return service.login(request);
    }

    @PostMapping("/refresh")
    public AuthDtos.Tokens refresh(@Valid @RequestBody AuthDtos.Refresh request) {
        return service.refresh(request);
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout(@Valid @RequestBody AuthDtos.Refresh request) {
        service.logout(request, current.require().getId());
        return ResponseEntity.noContent().build();
    }
}
