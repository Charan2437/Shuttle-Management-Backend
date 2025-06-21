package com.shuttle.shuttlesystem.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "transfer_bookings")
public class TransferBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_booking_id", nullable = false)
    private Booking mainBooking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_stop_id", nullable = false)
    private Stop fromStop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_stop_id", nullable = false)
    private Stop toStop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_stop_id", nullable = false)
    private Stop transferStop;

    private Integer estimatedWaitTime;
    private int transferOrder;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Booking getMainBooking() { return mainBooking; }
    public void setMainBooking(Booking mainBooking) { this.mainBooking = mainBooking; }
    public Stop getFromStop() { return fromStop; }
    public void setFromStop(Stop fromStop) { this.fromStop = fromStop; }
    public Stop getToStop() { return toStop; }
    public void setToStop(Stop toStop) { this.toStop = toStop; }
    public Stop getTransferStop() { return transferStop; }
    public void setTransferStop(Stop transferStop) { this.transferStop = transferStop; }
    public Integer getEstimatedWaitTime() { return estimatedWaitTime; }
    public void setEstimatedWaitTime(Integer estimatedWaitTime) { this.estimatedWaitTime = estimatedWaitTime; }
    public int getTransferOrder() { return transferOrder; }
    public void setTransferOrder(int transferOrder) { this.transferOrder = transferOrder; }
}
