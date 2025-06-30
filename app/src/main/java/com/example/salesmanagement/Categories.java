package com.example.salesmanagement;

import com.google.firebase.firestore.ServerTimestamp;
import com.google.firebase.firestore.Exclude;
import java.util.Date;

public class Categories {
    private String id; //ID của Document trong Firebase
    private String name;
    private String description;
    @ServerTimestamp //Auto create timestamp khi tạo hoặc cập nhật
    private Date createdAt;

    public Categories(){
        // Public no-argument constructor needed for Firestore
    }

    public Categories(String id, String name, String description){
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public Categories(String name, String description){
        this.name = name;
        this.description = description;
    }

    @Exclude //Không lưu vào Firebase
    public String getId(){
        return id;
    }

    public void setId(String id){
        this.id= id;
    }

    public String getName(){
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription(){
        return description;
    }

    public void setDescription(String description){
        this.description = description;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

}
