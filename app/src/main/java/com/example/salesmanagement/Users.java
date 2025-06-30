package com.example.salesmanagement;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Users {
    private String userId; // Đây là document ID
    private String name;
    private String email;
    private String address;
    private String phoneNumber;
    private Long createdAt;
    private String role;
    private Boolean isActive;
    private double balance; // <--- THÊM TRƯỜNG NÀY

    public Users() {
        // Public no-argument constructor needed for Firestore
    }

    public Users(String userId, String name, String email, String address, String phoneNumber, Long createdAt, String role, Boolean isActive, double balance) { // <--- CẬP NHẬT CONSTRUCTOR
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.createdAt = createdAt;
        this.role = role;
        this.isActive = isActive;
        this.balance = balance; // <--- KHỞI TẠO TRƯỜNG BALANCE
    }

    @Exclude // Thêm annotation này
    public String getUserId() {
        return userId;
    }

    @Exclude // Thêm annotation này
    public void setUserId(String userId) {
        this.userId = userId;
    }

    // Các Getters và Setters khác (không có @Exclude)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }

    // --- GETTER VÀ SETTER MỚI CHO BALANCE ---
    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}