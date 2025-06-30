// File: app/src/main/java/com/example/salesmanagement/Order.java
package com.example.salesmanagement;

import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Orders implements Serializable {
    private String orderId;              // ID của đơn hàng (Firestore Document ID)
    private String userId;               // ID của người dùng đặt hàng (từ Firebase Auth UID)
    private String userName;             // Tên khách hàng (có thể lấy từ User model, tương ứng với userName trong hướng dẫn trước)
    private String userEmail;            // Email của người dùng đặt hàng (thêm để dễ hiển thị trong OrdersList Admin)
    private String deliveryAddress;      // Địa chỉ giao hàng
    private String phoneNumber;          // Số điện thoại liên hệ
    private String paymentMethod;        // Phương thức thanh toán (e.g., "COD", "Credit Card", "Bank Transfer")
    private double totalAmount;          // Tổng số tiền của đơn hàng
    private String status;               // Trạng thái đơn hàng (e.g., "Pending", "Confirmed", "Shipped", "Delivered", "Cancelled")
    private String notes;                // Ghi chú của khách hàng (tùy chọn)

    @ServerTimestamp
    private Date orderDate;              // Thời gian đặt hàng (Firebase sẽ tự động thêm)

    // Thay đổi từ List<OrderItem> sang List<CartItem>
    private List<OrderItem> items;        // Danh sách các sản phẩm trong đơn hàng

    public Orders() {
        // Constructor rỗng cần thiết cho Firebase Firestore
        this.items = new ArrayList<>(); // Khởi tạo danh sách item để tránh NullPointerException
    }

    // Cập nhật constructor để chấp nhận List<CartItem> và thêm orderDate
    public Orders(String orderId, String userId, String userName, String userEmail, String deliveryAddress,
                  String phoneNumber, String paymentMethod, double totalAmount, String status, String notes,
                  Date orderDate, List<OrderItem> items) { // Thêm Date orderDate
        this.orderId = orderId;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.deliveryAddress = deliveryAddress;
        this.phoneNumber = phoneNumber;
        this.paymentMethod = paymentMethod;
        this.totalAmount = totalAmount;
        this.status = status;
        this.notes = notes;
        this.orderDate = orderDate; // Gán orderDate
        this.items = items;
    }

    // --- Getters and Setters ---

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", deliveryAddress='" + deliveryAddress + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", totalAmount=" + totalAmount +
                ", status='" + status + '\'' +
                ", notes='" + notes + '\'' +
                ", orderDate=" + orderDate +
                ", items=" + items +
                '}';
    }
}