package com.shuttle.shuttlesystem.dto;

public class AdminWalletAllocateRequestDTO {
    public String studentCode; // changed from UUID studentId
    public int amount;
    public String type;
    public String description;
    public String reference;
    public String processedBy; // keep as String for consistency
}
