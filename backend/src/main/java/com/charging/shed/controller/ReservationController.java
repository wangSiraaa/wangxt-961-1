package com.charging.shed.controller;

import com.charging.shed.dto.ApiResponse;
import com.charging.shed.dto.ReservationCreateDTO;
import com.charging.shed.entity.Reservation;
import com.charging.shed.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservation")
@CrossOrigin(origins = "*")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/create")
    public ApiResponse<Reservation> createReservation(@RequestAttribute("userId") Long userId,
                                                      @Valid @RequestBody ReservationCreateDTO dto) {
        Reservation reservation = reservationService.createReservation(
                userId,
                dto.getVehicleId(),
                dto.getPortId(),
                dto.getReserveStartTime(),
                dto.getReserveEndTime()
        );
        return ApiResponse.success(reservation);
    }

    @PostMapping("/cancel/{reservationId}")
    public ApiResponse<Reservation> cancelReservation(@RequestAttribute("userId") Long userId,
                                                      @PathVariable Long reservationId) {
        Reservation reservation = reservationService.cancelReservation(userId, reservationId);
        return ApiResponse.success(reservation);
    }

    @GetMapping("/list")
    public ApiResponse<List<Reservation>> getReservations(@RequestAttribute("userId") Long userId) {
        List<Reservation> reservations = reservationService.getReservations(userId);
        return ApiResponse.success(reservations);
    }

    @GetMapping("/{reservationId}")
    public ApiResponse<Reservation> getReservationById(@RequestAttribute("userId") Long userId,
                                                       @PathVariable Long reservationId) {
        Reservation reservation = reservationService.getReservationById(userId, reservationId);
        return ApiResponse.success(reservation);
    }

    @GetMapping("/available-ports")
    public ApiResponse<List<Long>> getAvailablePorts(
            @RequestParam("shedId") Long shedId,
            @RequestParam("startTime") java.time.LocalDateTime startTime,
            @RequestParam("endTime") java.time.LocalDateTime endTime) {
        List<Long> portIds = reservationService.getAvailablePorts(shedId, startTime, endTime);
        return ApiResponse.success(portIds);
    }

    @GetMapping("/queued")
    public ApiResponse<List<Reservation>> getQueuedReservations(@RequestParam("shedId") Long shedId) {
        List<Reservation> queued = reservationService.getQueuedReservations(shedId);
        return ApiResponse.success(queued);
    }

    @GetMapping("/shed/{shedId}")
    public ApiResponse<List<Reservation>> getReservationsByShed(@PathVariable Long shedId) {
        List<Reservation> reservations = reservationService.getReservationsByShedId(shedId);
        return ApiResponse.success(reservations);
    }
}
