package com.charging.shed.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "temperature_history")
public class TemperatureHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "port_id", nullable = false)
    private Long portId;

    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal temperature;

    @CreationTimestamp
    @Column(name = "recorded_at", updatable = false)
    private LocalDateTime recordedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "port_id", insertable = false, updatable = false)
    private ChargingPort port;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", insertable = false, updatable = false)
    private Reservation reservation;
}
