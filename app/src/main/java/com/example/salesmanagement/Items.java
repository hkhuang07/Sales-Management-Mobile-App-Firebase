package com.example.salesmanagement;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Items {
    @Exclude
    private String id; // ID của Document trong Firestore
    private String name;
    private int quantity;
    private float price;
    private String categoryId;
    private String description;
    private String imageUrl;
    private String userId; // <-- THÊM TRƯỜNG NÀY

    private static final int DEFAULT_IMAGE_RES_ID = R.drawable.ic_photo;

    public Items() {
        // Required no-argument constructor for Firestore
    }

    // Constructor với tất cả các trường, bao gồm cả id, imageUrl, categoryId và userId
    public Items(String id, String name, int quantity, float price, String categoryId, String description, String imageUrl, String userId) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.categoryId = categoryId;
        this.description = description;
        this.imageUrl = imageUrl;
        this.userId = userId; // <-- THÊM userId vào constructor
    }

    // Constructor nếu bạn không muốn truyền ID khi tạo đối tượng mới (ID sẽ được Firestore tạo)
    public Items(String name, int quantity, float price, String categoryId, String description, String imageUrl, String userId) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.categoryId = categoryId;
        this.description = description;
        this.imageUrl = imageUrl;
        this.userId = userId; // <-- THÊM userId vào constructor
    }

    // --- Getters ---
    @Exclude
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public float getPrice() {
        return price;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getUserId() { // <-- GETTER MỚI CHO userId
        return userId;
    }

    // --- Setters ---
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setUserId(String userId) { // <-- SETTER MỚI CHO userId
        this.userId = userId;
    }

    public int getDefaultImageResId() {
        return DEFAULT_IMAGE_RES_ID;
    }

    @Override
    public String toString() {
        return "Items{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                ", categoryId='" + categoryId + '\'' +
                ", description='" + description + '\'' +
                ", imageUrl='" + (imageUrl != null ? imageUrl.substring(0, Math.min(imageUrl.length(), 20)) + "..." : "null") + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}