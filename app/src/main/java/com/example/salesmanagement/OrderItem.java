// File: app/src/main/java/com/example/salesmanagement/CartItem.java
package com.example.salesmanagement;

import java.io.Serializable; // <--- THÊM DÒNG NÀY

public class OrderItem implements Serializable { // <--- THÊM "implements Serializable"
    // ID của document trong subcollection "cartItems".
    // Firestore sẽ tự động set giá trị này khi bạn sử dụng document.getId()
    // Nó trùng với itemId của sản phẩm gốc để dễ dàng truy vấn.
    private String id;
    private String itemId;       // ID của sản phẩm gốc (từ collection "Items")
    private String name;         // Tên sản phẩm
    private float price;         // Giá sản phẩm
    private String imageUrl;     // URL ảnh (hoặc Base64 string)
    private int quantity;        // Số lượng trong giỏ hàng
    private long addedAt;        // Thời gian thêm vào giỏ (milliseconds since epoch)

    public OrderItem() {
        // Constructor rỗng cần thiết cho Firestore deserialization
    }

    public OrderItem(String itemId, String name, float price, String imageUrl, int quantity, long addedAt) {
        this.itemId = itemId;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.quantity = quantity;
        this.addedAt = addedAt;
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getItemId() { return itemId; }
    public String getName() { return name; }
    public float getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
    public int getQuantity() { return quantity; }
    public long getAddedAt() { return addedAt; }

    // --- Setters --- (Quan trọng cho Firestore, đặc biệt setId)
    public void setId(String id) { this.id = id; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public void setName(String name) { this.name = name; }
    public void setPrice(float price) { this.price = price; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setAddedAt(long addedAt) { this.addedAt = addedAt; }

    // Phương thức tiện ích để tính tổng phụ cho một mục
    public double getSubtotal() {
        return price * quantity;
    }
}