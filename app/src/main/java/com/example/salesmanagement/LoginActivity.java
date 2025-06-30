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

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private TextView textViewGoToRegister, textViewForgotPassword;
    private SignInButton buttonGoogleSignIn;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Kiểm tra trạng thái đăng nhập của người dùng khi Activity được tạo
        // Đây là phần quan trọng để duy trì phiên đăng nhập
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Nếu có người dùng đã đăng nhập, kiểm tra trạng thái tài khoản Firestore
            // và chuyển hướng đến MainActivity nếu hợp lệ
            checkUserAccountStatus(currentUser);
            // Quan trọng: Return ngay lập tức để không chạy tiếp logic khởi tạo UI nếu đã chuyển hướng
            return;
        }

        editTextEmail = findViewById(R.id.edit_text_email_login);
        editTextPassword = findViewById(R.id.edit_text_password_login);
        buttonLogin = findViewById(R.id.button_login);
        textViewGoToRegister = findViewById(R.id.text_view_go_to_register);
        textViewForgotPassword = findViewById(R.id.text_view_forgot_password);
        buttonGoogleSignIn = findViewById(R.id.button_google_sign_in);

        buttonLogin.setOnClickListener(v -> loginUser());

        textViewGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        textViewForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPassword.class));
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        buttonGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

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

        buttonLogin.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> { // Sử dụng lambda expression cho code ngắn gọn hơn
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        checkUserAccountStatus(user);
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                        buttonLogin.setEnabled(true);
                    }
                });
    }

    private void signInWithGoogle() {
        // Đảm bảo nút không bị nhấn lại nhiều lần
        buttonGoogleSignIn.setEnabled(false);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Google login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                // Re-enable the button on failure
                buttonGoogleSignIn.setEnabled(true);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> { // Sử dụng lambda expression
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        checkUserAccountStatus(user);
                    } else {
                        Log.w(TAG, "signInWithCredential failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                        buttonGoogleSignIn.setEnabled(true);
                    }
                });
    }

    private void checkUserAccountStatus(FirebaseUser user) {
        if (user == null) {
            Log.e(TAG, "FirebaseUser is null after authentication attempt!");
            mAuth.signOut();
            // Đảm bảo client Google Sign-In cũng được đăng xuất để tránh lỗi
            if (mGoogleSignInClient != null) {
                mGoogleSignInClient.signOut();
            }
            Toast.makeText(LoginActivity.this, "Authentication error. Please try again.", Toast.LENGTH_LONG).show();
            // Re-enable buttons if they were disabled
            if (buttonLogin != null) buttonLogin.setEnabled(true);
            if (buttonGoogleSignIn != null) buttonGoogleSignIn.setEnabled(true);
            return;
        }

        db.collection("users").document(user.getUid()).get()
                .addOnCompleteListener(task -> { // Sử dụng lambda expression
                    // Luôn kích hoạt lại nút sau khi kiểm tra xong, bất kể thành công hay thất bại
                    if (buttonLogin != null) buttonLogin.setEnabled(true);
                    if (buttonGoogleSignIn != null) buttonGoogleSignIn.setEnabled(true);

                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Boolean isActive = document.getBoolean("isActive");
                            // String role = document.getString("role"); // Giữ lại nếu bạn cần sử dụng role

                            if (isActive == null || !isActive) {
                                mAuth.signOut();
                                if (mGoogleSignInClient != null) {
                                    mGoogleSignInClient.signOut();
                                }
                                Toast.makeText(LoginActivity.this, "Your account is currently locked or inactive. Please contact support.", Toast.LENGTH_LONG).show();
                                Log.w(TAG, "Account " + user.getUid() + " is inactive or locked.");
                            } else {
                                Log.d(TAG, "User " + user.getUid() + " is active. Proceeding to MainActivity.");
                                Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            }
                        } else {
                            Log.w(TAG, "User document for " + user.getUid() + " does not exist. Creating default document.");
                            createDefaultFirestoreUser(user);
                        }
                    } else {
                        Log.e(TAG, "Failed to check user account status in Firestore: " + task.getException().getMessage(), task.getException());
                        mAuth.signOut();
                        if (mGoogleSignInClient != null) {
                            mGoogleSignInClient.signOut();
                        }
                        Toast.makeText(LoginActivity.this, "Error checking account status. Please try again.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void createDefaultFirestoreUser(FirebaseUser user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", user.getUid());
        // Sử dụng user.getDisplayName() và user.getEmail() từ FirebaseUser
        // để đảm bảo dữ liệu nhất quán, đặc biệt với Google Login
        userData.put("name", user.getDisplayName() != null ? user.getDisplayName() : "");
        userData.put("email", user.getEmail() != null ? user.getEmail() : "");
        userData.put("address", "");
        userData.put("phoneNumber", "");
        userData.put("createdAt", System.currentTimeMillis());
        userData.put("role", "user");
        userData.put("isActive", true);

        db.collection("users").document(user.getUid()).set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "New user data added to Firestore with default role 'user' and isActive: true.");
                    Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding new user document to Firestore.", e);
                    mAuth.signOut();
                    if (mGoogleSignInClient != null) {
                        mGoogleSignInClient.signOut();
                    }
                    Toast.makeText(LoginActivity.this, "Error setting up user data. Please try again.", Toast.LENGTH_LONG).show();
                });
    }
}