package com.shuttle.shuttlesystem.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    private int pointsDeducted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_stop_id", nullable = false)
    private Stop fromStop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_stop_id", nullable = false)
    private Stop toStop;

    @Column(nullable = false)
    private java.time.Instant scheduledTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private BookingStatusType status;

    @Column(unique = true, nullable = false)
    private String bookingReference;

    private String notes;

    private java.time.Instant cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    private String cancellationReason;

    @Column(nullable = false)
    private java.time.Instant createdAt = java.time.Instant.now();
    @Column(nullable = false)
    private java.time.Instant updatedAt = java.time.Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public int getPointsDeducted() { return pointsDeducted; }
    public void setPointsDeducted(int pointsDeducted) { this.pointsDeducted = pointsDeducted; }
    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }
    public Stop getFromStop() { return fromStop; }
    public void setFromStop(Stop fromStop) { this.fromStop = fromStop; }
    public Stop getToStop() { return toStop; }
    public void setToStop(Stop toStop) { this.toStop = toStop; }
    public java.time.Instant getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(java.time.Instant scheduledTime) { this.scheduledTime = scheduledTime; }
    public BookingStatusType getStatus() { return status; }
    public void setStatus(BookingStatusType status) { this.status = status; }
    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public java.time.Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(java.time.Instant cancelledAt) { this.cancelledAt = cancelledAt; }
    public User getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(User cancelledBy) { this.cancelledBy = cancelledBy; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public java.time.Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.Instant createdAt) { this.createdAt = createdAt; }
    public java.time.Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.Instant updatedAt) { this.updatedAt = updatedAt; }
}
