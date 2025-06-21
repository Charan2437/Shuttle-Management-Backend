package com.shuttle.shuttlesystem.dto;

import java.util.Date;
import java.util.UUID;

public class AdminWalletTransactionDTO {
    public UUID id;
    public String studentId;
    public String studentName;
    public String type;
    public int amount;
    public String description;
    public String reference;
    public UUID bookingId;
    public UUID processedBy;
    public String processedByName;
    public String status;
    public Date createdAt;
}
