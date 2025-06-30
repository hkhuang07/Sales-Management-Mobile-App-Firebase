package com.example.salesmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private EditText editTextName, editTextEmail, editTextPassword, editTextConfirmPassword;
    private Button buttonRegister;
    private TextView textViewGoToLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextName = findViewById(R.id.edit_text_name_register);
        editTextEmail = findViewById(R.id.edit_text_email_register);
        editTextPassword = findViewById(R.id.edit_text_password_register);
        editTextConfirmPassword = findViewById(R.id.edit_text_confirm_password_register);

        buttonRegister = findViewById(R.id.button_register);
        textViewGoToLogin = findViewById(R.id.text_view_go_to_login);

        buttonRegister.setOnClickListener(v -> registerUser());

        textViewGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            editTextName.setError("Please enter name.");
            editTextName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Please enter email.");
            editTextEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Please enter password.");
            editTextPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            editTextPassword.setError("Password must be at least 6 characters.");
            editTextPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            editTextConfirmPassword.setError("Confirmation password does not match.");
            editTextConfirmPassword.requestFocus();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Truyền chuỗi rỗng cho address và phoneNumber
                                // VÀ THÊM 0.0 CHO BALANCE
                                saveUserDataToFirestore(user.getUid(), name, email, "", "", 0.0);
                            }
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Cập nhật phương thức này để nhận thêm tham số 'balance'
    private void saveUserDataToFirestore(String uid, String name, String email, String address, String phoneNumber, double balance) {
        // Tạo một đối tượng Users sử dụng constructor đã cập nhật
        Users newUser = new Users(
                uid,
                name,
                email,
                address,
                phoneNumber,
                new Date().getTime(),
                "user",        // Gán vai trò mặc định là "user"
                true,          // Gán isActive mặc định là true
                balance        // Ví tiền mặc định là 0.0
        );

        // Lưu đối tượng Users vào collection "users" với UID làm ID tài liệu
        db.collection("users").document(uid)
                .set(newUser)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "User data successfully written to Firestore!");
                        Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                        // Chuyển hướng đến màn hình chính hoặc màn hình danh sách sản phẩm
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing user document", e);
                        Toast.makeText(RegisterActivity.this, "Error saving user information.", Toast.LENGTH_SHORT).show();
                        // Nếu lưu Firestore thất bại, bạn có thể cân nhắc xóa tài khoản Firebase Auth vừa tạo
                        if (mAuth.getCurrentUser() != null) {
                            mAuth.getCurrentUser().delete().addOnCompleteListener(deleteTask -> {
                                if (deleteTask.isSuccessful()) {
                                    Log.d(TAG, "Firebase Auth user deleted due to Firestore save failure.");
                                } else {
                                    Log.e(TAG, "Failed to delete Firebase Auth user after Firestore save failure.", deleteTask.getException());
                                }
                            });
                        }
                    }
                });
    }
}