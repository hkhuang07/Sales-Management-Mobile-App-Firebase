package com.example.salesmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

public class CategoriesAddEditActivity extends AppCompatActivity { // Đổi tên class

    private static final String TAG = "AddEditCategoryActivity"; // Đổi TAG

    private EditText edtCategoryName, edtCategoryDescription; // Đổi tên EditTexts
    private MaterialButton btnSubmitCategory; // Đổi tên nút
    private Toolbar toolbar;

    private FirebaseFirestore db;
    private String currentCategoryId; // Đổi tên ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.category_add_edit); // Đảm bảo layout này tồn tại và đúng

        db = FirebaseFirestore.getInstance();

        toolbar = findViewById(R.id.toolbar_add_edit_category); // ID của Toolbar trong activity_add_edit_category.xml
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        edtCategoryName = findViewById(R.id.edtCateName); // ID mới
        edtCategoryDescription = findViewById(R.id.edtCateDes); // ID mới
        btnSubmitCategory = findViewById(R.id.btnSubmitCate); // ID mới

        Intent intent = getIntent();
        if (intent.hasExtra("category_id")) { // Kiểm tra "category_id"
            currentCategoryId = intent.getStringExtra("category_id");
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Edit Category"); // Tiêu đề
            }
            loadCategoryData(currentCategoryId); // Tải dữ liệu danh mục
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Add new category"); // Tiêu đề
            }
            // Không cần edtID cho Category vì ID được Firestore tự động tạo
            // edtID.setText("ID will be auto-generated");
            // edtID.setEnabled(false);
        }

        // Không cần xử lý hình ảnh cho Category
        // imgItem.setOnClickListener(v -> checkPermissionAndOpenGallery());

        btnSubmitCategory.setOnClickListener(v -> saveCategory()); // Gọi saveCategory

        Log.d(TAG, "AddEditCategoryActivity created.");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadCategoryData(String categoryId) { // Tải dữ liệu danh mục
        db.collection("Categories").document(categoryId) // Collection "Categories"
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Categories category = documentSnapshot.toObject(Categories.class); // Dùng Category.class
                        if (category != null) {
                            category.setId(documentSnapshot.getId()); // Đặt ID cho đối tượng Category

                            edtCategoryName.setText(category.getName());
                            edtCategoryDescription.setText(category.getDescription());

                            Log.d(TAG, "Category data loaded for ID: " + categoryId);
                        }
                    } else {
                        Toast.makeText(CategoriesAddEditActivity.this, "Cannot find category!", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Category document does not exist for ID: " + categoryId);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CategoriesAddEditActivity.this, "Error loading category: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error loading category data for ID: " + categoryId, e);
                    finish();
                });
    }

    // Các hàm liên quan đến hình ảnh không cần thiết cho Category
    // private void checkPermissionAndOpenGallery() { ... }
    // private void openGallery() { ... }
    // @Override public void onRequestPermissionsResult(...) { ... }
    // @Override protected void onActivityResult(...) { ... }
    // public byte[] getCompressedBytes(Uri imageUri) throws IOException { ... }

    private void saveCategory() { // Lưu danh mục
        String name = edtCategoryName.getText().toString().trim();
        String description = edtCategoryDescription.getText().toString().trim();

        if (name.isEmpty()) { // Chỉ kiểm tra tên danh mục
            Toast.makeText(this, "Enter category name, please!", Toast.LENGTH_SHORT).show();
            return;
        }

        saveCategoryToFirestore(currentCategoryId, name, description);
    }

    private void saveCategoryToFirestore(String categoryId, String name, String description) {
        Categories category;
        if (categoryId == null) {
            category = new Categories(name, description); // Constructor cho thêm mới
        } else {
            category = new Categories(categoryId, name, description); // Constructor cho chỉnh sửa (có ID)
        }

        if (categoryId == null) {
            // Thêm mới
            db.collection("Categories") // Collection "Categories"
                    .add(category)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(CategoriesAddEditActivity.this, "Category Added successfully!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Category added with ID: " + documentReference.getId());
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(CategoriesAddEditActivity.this, "Error adding category: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error adding category", e);
                    });
        } else {
            // Cập nhật
            db.collection("Categories").document(categoryId) // Collection "Categories"
                    .set(category) // set() sẽ ghi đè toàn bộ document, phù hợp với Category model đơn giản
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(CategoriesAddEditActivity.this, "Category updating successfully!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Category updated: " + categoryId);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(CategoriesAddEditActivity.this, "Category uppdating error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error updating category", e);
                    });
        }
    }
}