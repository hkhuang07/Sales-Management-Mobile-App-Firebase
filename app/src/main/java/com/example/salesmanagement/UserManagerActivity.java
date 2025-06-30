package com.example.salesmanagement;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class UserManagerActivity extends AppCompatActivity {

    private static final String TAG = "UserManagementActivity";

    private TextView tvManagedUserName, tvManagedUserEmail;
    private RadioGroup radioGroupRole;
    private RadioButton radioRoleAdmin, radioRoleUser;
    private Switch switchAccountStatus;
    private Button btnSaveChanges;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private String userIdToManage; // UID của người dùng đang được quản lý

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_management);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar_user_management);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Manage User");
        }

        tvManagedUserName = findViewById(R.id.tv_managed_user_name);
        tvManagedUserEmail = findViewById(R.id.tv_managed_user_email);
        radioGroupRole = findViewById(R.id.radio_group_role);
        radioRoleAdmin = findViewById(R.id.radio_role_admin);
        radioRoleUser = findViewById(R.id.radio_role_user);
        switchAccountStatus = findViewById(R.id.switch_account_status);
        btnSaveChanges = findViewById(R.id.btn_save_changes);
        progressBar = findViewById(R.id.progress_bar_management);

        // Lấy dữ liệu người dùng từ Intent
        if (getIntent().getExtras() != null) {
            userIdToManage = getIntent().getStringExtra("userId");
            String userName = getIntent().getStringExtra("userName");
            String userEmail = getIntent().getStringExtra("userEmail");
            String userRole = getIntent().getStringExtra("userRole");
            // String userAccountStatus = getIntent().getStringExtra("userAccountStatus"); // Nếu bạn đã có trường này trong Users model

            tvManagedUserName.setText("User Name: " + userName);
            tvManagedUserEmail.setText("Email: " + userEmail);

            // Đặt trạng thái ban đầu cho RadioGroup và Switch
            if ("admin".equals(userRole)) {
                radioRoleAdmin.setChecked(true);
            } else {
                radioRoleUser.setChecked(true);
            }

            // Tùy chọn: Đặt trạng thái cho Switch nếu có trường accountStatus trong Firestore
            // Giả sử có trường `isActive: boolean` trong Users model
            // if (userAccountStatus != null && "active".equals(userAccountStatus)) {
            //     switchAccountStatus.setChecked(true);
            // } else {
            //     switchAccountStatus.setChecked(false);
            // }

            // Tải trạng thái tài khoản từ Firestore (để đảm bảo dữ liệu mới nhất)
            loadUserAccountStatus(userIdToManage);

        } else {
            Toast.makeText(this, "Error: User ID not provided.", Toast.LENGTH_SHORT).show();
            finish(); // Đóng Activity nếu không có UID
        }

        btnSaveChanges.setOnClickListener(v -> saveChanges());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Tùy chọn: Tải trạng thái tài khoản từ Firestore để đảm bảo cập nhật nhất
    private void loadUserAccountStatus(String uid) {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        Boolean isActive = documentSnapshot.getBoolean("isActive"); // Tên trường trạng thái tài khoản
                        if (isActive != null) {
                            switchAccountStatus.setChecked(isActive);
                        } else {
                            // Mặc định là active nếu không có trường hoặc là null
                            switchAccountStatus.setChecked(true);
                            Log.d(TAG, "isActive field is null for user: " + uid + ". Defaulting to active.");
                        }
                    } else {
                        Log.w(TAG, "User document not found for account status: " + uid);
                        Toast.makeText(UserManagerActivity.this, "User not found to load status.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading user account status: ", e);
                    Toast.makeText(UserManagerActivity.this, "Failed to load account status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void saveChanges() {
        if (userIdToManage == null || userIdToManage.isEmpty()) {
            Toast.makeText(this, "Error: User ID is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSaveChanges.setEnabled(false); // Vô hiệu hóa nút để tránh click nhiều lần

        String newRole = "";
        if (radioGroupRole.getCheckedRadioButtonId() == R.id.radio_role_admin) {
            newRole = "admin";
        } else if (radioGroupRole.getCheckedRadioButtonId() == R.id.radio_role_user) {
            newRole = "user";
        } else {
            Toast.makeText(this, "Please select a role.", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            btnSaveChanges.setEnabled(true);
            return;
        }

        boolean newAccountStatus = switchAccountStatus.isChecked(); // true = active, false = locked

        Map<String, Object> updates = new HashMap<>();
        updates.put("role", newRole);
        updates.put("isActive", newAccountStatus); // Thêm trường isActive vào Firestore

        db.collection("users").document(userIdToManage)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    btnSaveChanges.setEnabled(true);
                    Toast.makeText(UserManagerActivity.this, "User updated successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Quay lại UsersList Activity
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSaveChanges.setEnabled(true);
                    Log.e(TAG, "Error updating user: ", e);
                    Toast.makeText(UserManagerActivity.this, "Error updating user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}