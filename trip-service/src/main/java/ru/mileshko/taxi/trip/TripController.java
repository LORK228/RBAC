package ru.mileshko.taxi.trip;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
class TripController {
    private final TripService service;

    TripController(TripService service) {
        this.service = service;
    }

    @PostMapping("/trips")
    Trip create(@Valid @RequestBody CreateTripRequest request) {
        return service.create(request);
    }

    @GetMapping("/trips/{id}")
    Trip get(@PathVariable long id) {
        return service.get(id);
    }

    @GetMapping(value = "/trips", params = "passenger_id")
    List<Trip> history(@RequestParam("passenger_id") long passengerId) {
        return service.history(passengerId);
    }

    @PatchMapping("/trips/{id}/status")
    Trip updateStatus(@PathVariable long id, @Valid @RequestBody UpdateTripStatusRequest request) {
        return service.updateStatus(id, TripStatus.valueOf(request.status()));
    }

    @PatchMapping("/trips/{id}/rating")
    Trip rate(@PathVariable long id, @Valid @RequestBody RateTripRequest request) {
        return service.rate(id, request.rating());
    }

    @GetMapping("/trips/statistics")
    TripStatistics statistics(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.statistics(date);
    }
}
