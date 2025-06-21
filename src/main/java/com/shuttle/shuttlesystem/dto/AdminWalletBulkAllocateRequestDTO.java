package com.shuttle.shuttlesystem.dto;

import java.util.List;
import java.util.UUID;

public class AdminWalletBulkAllocateRequestDTO {
    public List<UUID> studentIds;
    public int amount;
    public String type;
    public String description;
    public String reference;
    public UUID processedBy;
}
