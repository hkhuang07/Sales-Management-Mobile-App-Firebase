package com.example.salesmanagement;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView; // Đã thêm import này
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth; // Import FirebaseAuth
import com.google.firebase.auth.FirebaseUser; // Import FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ItemsAddEditActivity extends AppCompatActivity {

    private static final String TAG = "ItemsAddEdit";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private EditText edtID, edtName, edtQuantity, edtPrice, edtDescription;
    private AutoCompleteTextView spinnerCategory; // Đã đổi kiểu từ Spinner thành AutoCompleteTextView
    private ImageView imgItem;
    private MaterialButton btnSubmit;
    private MaterialButton btnSelectImage; // Thêm khai báo cho nút chọn ảnh
    private Toolbar toolbar;

    private Uri imageUri;
    private byte[] imageData;

    private FirebaseFirestore db;
    private String currentItemId;
    private String currentImageBase64;
    private String currentUserId;

    private List<Categories> categoryList;
    private ArrayAdapter<String> categoryAdapter;
    private String selectedCategoryId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.item_add_edit);

        db = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
            Log.d(TAG, "Current User ID: " + currentUserId);
        } else {
            Toast.makeText(this, "User not logged in. Cannot add/edit item.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "User not logged in.");
            finish();
            return;
        }

        toolbar = findViewById(R.id.tolMain);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        edtID = findViewById(R.id.edtID);
        edtName = findViewById(R.id.edtName);
        edtQuantity = findViewById(R.id.edtQuantity);
        edtPrice = findViewById(R.id.edtPrice);
        spinnerCategory = findViewById(R.id.spinner_category); // Đã gán đúng kiểu AutoCompleteTextView
        edtDescription = findViewById(R.id.edtDescription);
        imgItem = findViewById(R.id.imgItem);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnSelectImage = findViewById(R.id.btnSelectImage); // Gán nút chọn ảnh

        categoryList = new ArrayList<>();
        List<String> categoryNames = new ArrayList<>();
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryNames);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Thiết lập adapter cho AutoCompleteTextView
        spinnerCategory.setAdapter(categoryAdapter);

        loadCategories();

        Intent intent = getIntent();
        if (intent.hasExtra("item_id")) {
            currentItemId = intent.getStringExtra("item_id");
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Edit Item");
            }
            edtID.setText(currentItemId);
            edtID.setEnabled(false);
            loadItemData(currentItemId);
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Add New Item");
            }
            edtID.setText("Auto-generated");
            edtID.setEnabled(false);
        }

        // Cả ImageView và nút "Select Image" đều gọi cùng một phương thức
        imgItem.setOnClickListener(v -> checkPermissionAndOpenGallery());
        btnSelectImage.setOnClickListener(v -> checkPermissionAndOpenGallery()); // Đã thêm Listener cho nút

        btnSubmit.setOnClickListener(v -> saveItem());

        // Thay setOnItemSelectedListener bằng setOnItemClickListener cho AutoCompleteTextView
        spinnerCategory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < categoryList.size()) {
                    selectedCategoryId = categoryList.get(position).getId();
                    Log.d(TAG, "Selected category ID: " + selectedCategoryId);
                }
            }
        });
        // Không cần setOnNothingSelected cho AutoCompleteTextView khi dùng ExposedDropdownMenu
        // vì nó luôn có một giá trị hiển thị (có thể là hint hoặc giá trị đã chọn).


        Log.d(TAG, "ItemsAddEdit Activity created.");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadCategories() {
        db.collection("Categories")
                .orderBy("name")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    categoryList.clear();
                    List<String> names = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Categories category = document.toObject(Categories.class);
                        if (category != null) {
                            category.setId(document.getId());
                            categoryList.add(category);
                            names.add(category.getName());
                        }
                    }
                    categoryAdapter.clear();
                    categoryAdapter.addAll(names);
                    categoryAdapter.notifyDataSetChanged();

                    // Cần gọi setSpinnerSelectionForEdit() SAU KHI categories đã được tải xong
                    // và chỉ khi đang ở chế độ chỉnh sửa. Điều này đã được xử lý đúng trong onCreate.
                    if (currentItemId != null) {
                        // Nếu loadItemData gọi loadCategories, thì loadItemData sẽ gọi lại setSpinnerSelectionForEdit sau khi có item.categoryId
                        // Nếu không, cần gọi ở đây nếu ItemId đã có từ Intent trước khi loadCategories xong.
                        // Hiện tại, loadItemData sẽ đảm bảo spinner được chọn đúng.
                    }
                    Log.d(TAG, "Categories loaded: " + categoryList.size());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading categories", e);
                    Toast.makeText(ItemsAddEditActivity.this, "Error loading categories: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadItemData(String itemId) {
        db.collection("Items").document(itemId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Items item = documentSnapshot.toObject(Items.class);
                        if (item != null) {
                            item.setId(documentSnapshot.getId());

                            edtID.setText(item.getId());
                            edtName.setText(item.getName());
                            edtQuantity.setText(String.valueOf(item.getQuantity()));
                            edtPrice.setText(String.valueOf(item.getPrice()));
                            edtDescription.setText(item.getDescription());

                            selectedCategoryId = item.getCategoryId();
                            // Đảm bảo categories đã được tải trước khi gọi hàm này
                            // loadCategories() được gọi trước trong onCreate, nên khi loadItemData chạy, categories đã có.
                            setSpinnerSelectionForEdit(); // Đã cập nhật để hoạt động với AutoCompleteTextView

                            currentImageBase64 = item.getImageUrl();

                            if (currentImageBase64 != null && !currentImageBase64.isEmpty()) {
                                try {
                                    byte[] decodedBytes = Base64.decode(currentImageBase64, Base64.DEFAULT);
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                    imgItem.setImageBitmap(bitmap);
                                } catch (IllegalArgumentException e) {
                                    Log.e(TAG, "Base64 decoding error, might not be a valid Base64 string: " + e.getMessage());
                                    imgItem.setImageResource(R.drawable.ic_photo);
                                }
                            } else {
                                imgItem.setImageResource(R.drawable.ic_photo);
                            }
                            Log.d(TAG, "Item data loaded for ID: " + itemId);
                        }
                    } else {
                        Toast.makeText(ItemsAddEditActivity.this, "Item not found!", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Item document does not exist for ID: " + itemId);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ItemsAddEditActivity.this, "Error loading item: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error loading item data for ID: " + itemId, e);
                    finish();
                });
    }

    private void setSpinnerSelectionForEdit() {
        if (selectedCategoryId != null && !categoryList.isEmpty()) {
            for (int i = 0; i < categoryList.size(); i++) {
                if (categoryList.get(i).getId().equals(selectedCategoryId)) {
                    // Thay thế setSelection bằng setText cho AutoCompleteTextView
                    spinnerCategory.setText(categoryList.get(i).getName(), false); // false để không kích hoạt bộ lọc
                    // Cập nhật selectedCategoryId một lần nữa để đảm bảo đồng bộ
                    selectedCategoryId = categoryList.get(i).getId();
                    Log.d(TAG, "Spinner category set to: " + categoryList.get(i).getName() + " (ID: " + selectedCategoryId + ")");
                    return;
                }
            }
            Log.w(TAG, "Category ID " + selectedCategoryId + " not found in loaded categories for selection.");
            // Nếu category không tìm thấy, có thể xóa lựa chọn hoặc đặt lại về giá trị mặc định
            spinnerCategory.setText("", false); // Xóa văn bản nếu không tìm thấy
            selectedCategoryId = null;
        } else if (selectedCategoryId == null && !categoryList.isEmpty()) {
            // Nếu không có category được chọn nhưng có danh sách category, có thể chọn mục đầu tiên
            // hoặc để trống và hiển thị hint. Ở đây, tôi để trống và hint sẽ hiển thị.
            spinnerCategory.setText("", false);
        }
    }


    private void checkPermissionAndOpenGallery() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST_CODE);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Permission denied to read storage. Cannot select image.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            imgItem.setImageURI(imageUri);

            try {
                imageData = getCompressedBytes(imageUri);
            } catch (IOException e) {
                Log.e(TAG, "Error compressing and converting image to byte array", e);
                Toast.makeText(this, "Error processing image.", Toast.LENGTH_SHORT).show();
                imageData = null;
            }
            Log.d(TAG, "Image selected: " + imageUri.toString() + ", Compressed data size: " + (imageData != null ? imageData.length : 0) + " bytes");
        }
    }

    public byte[] getCompressedBytes(Uri imageUri) throws IOException {
        InputStream iStream = getContentResolver().openInputStream(imageUri);
        Bitmap originalBitmap = BitmapFactory.decodeStream(iStream);
        if (iStream != null) iStream.close();

        if (originalBitmap == null) {
            throw new IOException("Could not decode bitmap from URI");
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        originalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);

        originalBitmap.recycle();

        return byteArrayOutputStream.toByteArray();
    }

    private void saveItem() {
        String name = edtName.getText().toString().trim();
        String quantityStr = edtQuantity.getText().toString().trim();
        String priceStr = edtPrice.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();

        if (selectedCategoryId == null || selectedCategoryId.isEmpty()) {
            Toast.makeText(this, "Please select a category.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (name.isEmpty() || quantityStr.isEmpty() || priceStr.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        int quantity = 0;
        float price = 0.0f;
        try {
            quantity = Integer.parseInt(quantityStr);
            price = Float.parseFloat(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers for quantity and price.", Toast.LENGTH_SHORT).show();
            return;
        }


        String imageBase64 = null;
        if (imageData != null) {
            imageBase64 = Base64.encodeToString(imageData, Base64.DEFAULT);
            if (imageData.length > 800 * 1024) { // Giới hạn 800KB
                Toast.makeText(this, "Image is still too large after compression. Please choose a smaller image or reduce quality.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Compressed image data (bytes) is too large: " + imageData.length + " bytes.");
                return;
            }
            Log.d(TAG, "Image converted to Base64 (length: " + imageBase64.length() + ")");
        } else {
            // Nếu không có ảnh mới được chọn, giữ lại ảnh cũ (nếu có)
            imageBase64 = currentImageBase64;
        }

        if (currentUserId == null) {
            Toast.makeText(this, "User ID not available. Please log in again.", Toast.LENGTH_LONG).show();
            return;
        }

        saveItemToFirestore(currentItemId, name, quantity, price, selectedCategoryId, description, imageBase64, currentUserId);
    }

    private void saveItemToFirestore(String itemId, String name, int quantity, float price, String categoryId, String description, String imageBase64, String userId) {
        Items item;
        if (itemId == null) {
            item = new Items(name, quantity, price, categoryId, description, imageBase64, userId);
        } else {
            item = new Items(itemId, name, quantity, price, categoryId, description, imageBase64, userId);
        }

        if (itemId == null) {
            db.collection("Items")
                    .add(item)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(ItemsAddEditActivity.this, "Item added successfully!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Item added with ID: " + documentReference.getId());
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ItemsAddEditActivity.this, "Error adding item: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error adding item", e);
                    });
        } else {
            db.collection("Items").document(itemId)
                    .set(item)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(ItemsAddEditActivity.this, "Item updated successfully!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Item updated: " + itemId);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ItemsAddEditActivity.this, "Error updating item: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error updating item", e);
                    });
        }
    }
}