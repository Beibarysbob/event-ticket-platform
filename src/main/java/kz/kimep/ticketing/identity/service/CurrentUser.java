package kz.kimep.ticketing.identity.service;

import kz.kimep.ticketing.shared.domain.UserAccount;
import kz.kimep.ticketing.shared.error.DomainException;
import kz.kimep.ticketing.shared.repo.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUser {
    private final UserAccountRepository users;

    public UserAccount require() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return users.findByEmail(email).orElseThrow(() ->
            new DomainException(HttpStatus.UNAUTHORIZED, "Please login again"));
    }
}
