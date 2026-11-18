package kz.kimep.ticketing.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() { }
    public record Register(@NotBlank @Email String email,
                           @NotBlank @Size(min = 8) String password,
                           @NotBlank String displayName) { }
    public record Login(@NotBlank String email, @NotBlank String password) { }
    public record Refresh(@NotBlank String refreshToken) { }
    public record Tokens(String accessToken, String refreshToken, String tokenType) { }
}
