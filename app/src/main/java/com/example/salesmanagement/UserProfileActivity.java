package com.example.salesmanagement; // Đảm bảo đúng package của bạn

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat; // THÊM DÒNG NÀY
import java.util.Locale;      // THÊM DÒNG NÀY

public class UserProfileActivity extends AppCompatActivity {

    private static final String TAG = "UserProfile";
    private EditText editTextName, editTextEmail, editTextBalance, editTextPhone, editTextAddress;
    private Button buttonEditProfile, buttonSaveProfile, buttonLogout;
    private Toolbar toolbar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser loggedInUser;
    private String profileUserId;
    private DocumentReference userDocRef;
    private GoogleSignInClient mGoogleSignInClient;

    private boolean isEditing = false;
    private boolean isViewingOwnProfile = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        loggedInUser = mAuth.getCurrentUser();

        toolbar = findViewById(R.id.toolbarprofile);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("User Profile");
        }

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("userId")) {
            profileUserId = intent.getStringExtra("userId");
            if (loggedInUser != null && loggedInUser.getUid().equals(profileUserId)) {
                isViewingOwnProfile = true;
                Log.d(TAG, "Viewing own profile.");
            } else {
                isViewingOwnProfile = false;
                Log.d(TAG, "Viewing profile of another user: " + profileUserId);
            }
        } else if (loggedInUser != null) {
            profileUserId = loggedInUser.getUid();
            isViewingOwnProfile = true;
            Log.d(TAG, "No userId in Intent, defaulting to own profile.");
        } else {
            Log.e(TAG, "No user logged in and no userId provided. Redirecting to Login.");
            startActivity(new Intent(UserProfileActivity.this, LoginActivity.class));
            finish();
            return;
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        editTextName = findViewById(R.id.edit_text_profile_name);
        editTextEmail = findViewById(R.id.edit_text_profile_email);
        editTextBalance = findViewById(R.id.edit_text_profile_balance);
        editTextPhone = findViewById(R.id.edit_text_profile_phone);
        editTextAddress = findViewById(R.id.edit_text_profile_address);
        buttonEditProfile = findViewById(R.id.button_edit_profile);
        buttonSaveProfile = findViewById(R.id.button_save_profile);
        buttonLogout = findViewById(R.id.button_logout);

        userDocRef = db.collection("users").document(profileUserId);

        loadUserProfile();

        if (isViewingOwnProfile) {
            buttonEditProfile.setVisibility(View.VISIBLE);
            buttonLogout.setVisibility(View.VISIBLE);
            buttonEditProfile.setOnClickListener(v -> toggleEditMode());
            buttonSaveProfile.setOnClickListener(v -> saveProfileChanges());
            buttonLogout.setOnClickListener(v -> signOut());
        } else {
            buttonEditProfile.setVisibility(View.GONE);
            buttonSaveProfile.setVisibility(View.GONE);
            buttonLogout.setVisibility(View.GONE);
            editTextName.setEnabled(false);
            editTextEmail.setEnabled(false);
            editTextBalance.setEnabled(false); // Make balance not editable for other profiles
            editTextPhone.setEnabled(false);
            editTextAddress.setEnabled(false);
            Toast.makeText(this, "You are viewing another user's profile.", Toast.LENGTH_SHORT).show();
        }

        editTextEmail.setEnabled(false);
        editTextBalance.setEnabled(false); // Balance should generally not be directly editable by user
    }

    private void loadUserProfile() {
        userDocRef.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            Users user = documentSnapshot.toObject(Users.class);
                            if (user != null) {
                                editTextName.setText(user.getName());
                                editTextEmail.setText(user.getEmail());

                                // Sửa đổi tại đây: Định dạng balance
                                NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                                currencyFormatter.setMinimumFractionDigits(0);
                                currencyFormatter.setMaximumFractionDigits(2);
                                editTextBalance.setText(currencyFormatter.format(user.getBalance()));

                                editTextPhone.setText(user.getPhoneNumber());
                                editTextAddress.setText(user.getAddress());
                                if (getSupportActionBar() != null) {
                                    getSupportActionBar().setTitle(user.getName() + "'s Profile");
                                }
                            }
                        } else {
                            Log.d(TAG, "No such document for user profile ID: " + profileUserId);
                            Toast.makeText(UserProfileActivity.this, "User profile not found in database.", Toast.LENGTH_LONG).show();
                            if (isViewingOwnProfile && loggedInUser != null) {
                                editTextEmail.setText(loggedInUser.getEmail());
                            } else {
                                editTextEmail.setText("N/A");
                            }
                            editTextBalance.setText("0 "); // Hiển thị 0 hoặc N/A nếu không tìm thấy balance
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error getting user profile for ID: " + profileUserId, e);
                        Toast.makeText(UserProfileActivity.this, "Unable to load user profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        if (isViewingOwnProfile && loggedInUser != null) {
                            editTextEmail.setText(loggedInUser.getEmail());
                        } else {
                            editTextEmail.setText("N/A");
                        }
                        editTextBalance.setText("Error"); // Hiển thị lỗi
                    }
                });
    }

    private void toggleEditMode() {
        isEditing = !isEditing;
        editTextName.setEnabled(isEditing);
        editTextPhone.setEnabled(isEditing);
        editTextAddress.setEnabled(isEditing);
        // editTextBalance.setEnabled(false); // Balance should never be editable

        if (isEditing) {
            buttonEditProfile.setText("Cancel");
            buttonSaveProfile.setVisibility(View.VISIBLE);
            Toast.makeText(this, "In edit mode", Toast.LENGTH_SHORT).show();
        } else {
            buttonEditProfile.setText("Edit");
            buttonSaveProfile.setVisibility(View.GONE);
            loadUserProfile(); // Tải lại dữ liệu gốc nếu hủy chỉnh sửa
        }
    }

    private void saveProfileChanges() {
        String newName = editTextName.getText().toString().trim();
        String newPhone = editTextPhone.getText().toString().trim();
        String newAddress = editTextAddress.getText().toString().trim();

        if (isViewingOwnProfile && loggedInUser != null && loggedInUser.getUid().equals(profileUserId)) {
            userDocRef.update(
                            "name", newName,
                            "phoneNumber", newPhone,
                            "address", newAddress
                    )
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(UserProfileActivity.this, "Changes saved!", Toast.LENGTH_SHORT).show();
                            toggleEditMode();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error updating user profile", e);
                            Toast.makeText(UserProfileActivity.this, "Error saving changes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "You do not have permission to edit this profile.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Attempted to save profile changes for non-own profile or without permission.");
        }
    }

    private void signOut() {
        mAuth.signOut();
        if (mGoogleSignInClient != null) {
            mGoogleSignInClient.signOut().addOnCompleteListener(this,
                    task -> {
                        Log.d(TAG, "Google Sign-out complete.");
                    });
        }
        Toast.makeText(this, "Logged out.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(UserProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
}