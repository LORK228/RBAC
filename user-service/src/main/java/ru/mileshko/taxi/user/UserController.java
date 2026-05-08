package ru.mileshko.taxi.user;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class UserController {
    private final UserService service;
    private final JwtService jwtService;

    UserController(UserService service, JwtService jwtService) {
        this.service = service;
        this.jwtService = jwtService;
    }

    @PostMapping("/auth/token")
    TokenResponse token(@Valid @RequestBody TokenRequest request) {
        return new TokenResponse(jwtService.createToken(request.subject(), request.role()));
    }

    @PostMapping("/passengers")
    Passenger createPassenger(@Valid @RequestBody CreatePassengerRequest request) {
        return service.createPassenger(request);
    }

    @GetMapping("/passengers/{id}")
    Passenger getPassenger(@PathVariable long id) {
        return service.getPassenger(id);
    }

    @PostMapping("/drivers")
    Driver createDriver(@Valid @RequestBody CreateDriverRequest request) {
        return service.createDriver(request);
    }

    @GetMapping("/drivers/{id}")
    Driver getDriver(@PathVariable long id) {
        return service.getDriver(id);
    }

    @GetMapping("/drivers/available")
    List<Driver> availableDrivers() {
        return service.availableDrivers();
    }

    @PostMapping("/drivers/allocate")
    Driver allocateDriver() {
        return service.allocateDriver();
    }

    @PatchMapping("/drivers/{id}/status")
    Driver updateDriverStatus(@PathVariable long id, @Valid @RequestBody UpdateDriverStatusRequest request) {
        return service.updateStatus(id, DriverStatus.valueOf(request.status()));
    }

    @PostMapping("/auth/register")
    UserAuthResponse register(@Valid @RequestBody CreateUserAuthRequest request) {
        return service.createUserAuth(request);
    }

    @PostMapping("/auth/login")
    UserAuthResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @GetMapping("/auth/users/{id}")
    UserAuthResponse getUserAuth(@PathVariable long id) {
        return service.getUserAuth(id);
    }

    @GetMapping("/auth/users/by-email/{email}")
    UserAuthResponse getUserAuthByEmail(@PathVariable String email) {
        return service.getUserAuthByEmail(email);
    }
}
