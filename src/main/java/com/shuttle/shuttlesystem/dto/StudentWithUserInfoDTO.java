package com.shuttle.shuttlesystem.dto;

import java.util.Date;
import java.util.UUID;

public class StudentWithUserInfoDTO {
    public UUID id;
    public UUID userId;
    public String name;
    public String email;
    public String password; // for creation/update
    public String studentId;
    public Integer walletBalance;
    public String profileImageUrl;
    public String phoneNumber;
    public String emergencyContact;
    public Date enrollmentDate;
    public Date graduationDate;
}
