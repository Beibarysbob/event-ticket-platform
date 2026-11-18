package kz.kimep.ticketing;

import java.util.UUID;
import kz.kimep.ticketing.identity.api.AuthDtos;
import kz.kimep.ticketing.identity.service.IdentityService;
import kz.kimep.ticketing.shared.repo.VenueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationIT extends IntegrationContainers {
    @Autowired private MockMvc mvc;
    @Autowired private VenueRepository venues;
    @Autowired private IdentityService identity;

    private String uniqueEmail() {
        return UUID.randomUUID() + "@example.org";
    }
    @Test
    void venuesListIsReadable() throws Exception {
        mvc.perform(get("/api/v1/venues")).andExpect(status().isOk()).andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void venueMissingReturns404() throws Exception {
        mvc.perform(get("/api/v1/venues/999999")).andExpect(status().isNotFound());
    }

    @Test
    void venuesSurviveRepositoryReload() throws Exception {
        assertTrue(venues.count() >= 3);
        assertTrue(venues.findById(1L).isPresent());
    }

    @Test
    void newCustomerRegistrationReturns201() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + uniqueEmail() + "\",\"password\":\"12345678\",\"displayName\":\"Test\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void invalidEmailReturnsNamedField() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"not-email\",\"password\":\"12345678\",\"displayName\":\"Test\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    void shortPasswordReturns400() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + uniqueEmail() + "\",\"password\":\"123\",\"displayName\":\"Test\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    void wrongLoginReturns401() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"missing@demo.local\",\"password\":\"wrong\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void eventListIsValidEmptyPage() throws Exception {
        mvc.perform(get("/api/v1/events?status=DOES_NOT_EXIST")).andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void eventMissingReturns404() throws Exception {
        mvc.perform(get("/api/v1/events/999999")).andExpect(status().isNotFound());
    }

    @Test
    void venueCreationRequiresLogin() throws Exception {
        mvc.perform(post("/api/v1/venues").contentType(MediaType.APPLICATION_JSON)
            .content("{}")) .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedCustomerCannotCreateVenue() throws Exception {
        String email = uniqueEmail();
        identity.register(new AuthDtos.Register(email, "12345678", "Test"));
        String token = identity.login(new AuthDtos.Login(email, "12345678")).accessToken();
        mvc.perform(post("/api/v1/venues").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void largePageSizeIsCapped() throws Exception {
        mvc.perform(get("/api/v1/venues?size=10000")).andExpect(status().isOk()).andExpect(jsonPath("$.size").value(50));
    }

}
