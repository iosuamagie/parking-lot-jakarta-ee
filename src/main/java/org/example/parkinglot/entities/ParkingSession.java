package org.example.parkinglot.entities;

import jakarta.persistence.*;
import org.example.parkinglot.parking.CarModel;

import java.time.Instant;

/** One stay of one car in the public lot, from the entry barrier to the exit barrier. */
@Entity
@Table(name = "parking_session")
public class ParkingSession {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "ticket_code", unique = true, nullable = false, length = 22)
    private String ticketCode;

    @Column(name = "license_plate", nullable = false, length = 20)
    private String licensePlate;

    @Column(name = "spot", nullable = false, length = 3)
    private String spot;

    @Enumerated(EnumType.STRING)
    @Column(name = "car_model", nullable = false, length = 10)
    private CarModel carModel;

    @Column(name = "car_color", nullable = false, length = 7)
    private String carColor;

    @Column(name = "accessible_permit", nullable = false)
    private boolean accessiblePermit;

    @Column(name = "entered_at", nullable = false)
    private Instant enteredAt;

    @Column(name = "exited_at")
    private Instant exitedAt;

    @Column(name = "amount_bani")
    private Integer amountBani;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getSpot() {
        return spot;
    }

    public void setSpot(String spot) {
        this.spot = spot;
    }

    public CarModel getCarModel() {
        return carModel;
    }

    public void setCarModel(CarModel carModel) {
        this.carModel = carModel;
    }

    public String getCarColor() {
        return carColor;
    }

    public void setCarColor(String carColor) {
        this.carColor = carColor;
    }

    public boolean isAccessiblePermit() {
        return accessiblePermit;
    }

    public void setAccessiblePermit(boolean accessiblePermit) {
        this.accessiblePermit = accessiblePermit;
    }

    public Instant getEnteredAt() {
        return enteredAt;
    }

    public void setEnteredAt(Instant enteredAt) {
        this.enteredAt = enteredAt;
    }

    public Instant getExitedAt() {
        return exitedAt;
    }

    public void setExitedAt(Instant exitedAt) {
        this.exitedAt = exitedAt;
    }

    public Integer getAmountBani() {
        return amountBani;
    }

    public void setAmountBani(Integer amountBani) {
        this.amountBani = amountBani;
    }
}
