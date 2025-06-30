package com.example.salesmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity"; // Đồng bộ TAG

    private GestureDetector gestureDetector;
    private View mainRootLayout;

    private MaterialCardView cardItemsList;
    private MaterialCardView cardCategoriesList;
    private MaterialCardView cardProfile;
    private MaterialCardView cardUsers;
    private MaterialCardView cardOrders;
    private MaterialCardView cardShopping;


    private FloatingActionButton btnCart;
    private MaterialButton btnLogin;
    private MaterialButton btnLogout;
    private MaterialButton btnSettings;

    private TextView textViewWelcome;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private GoogleSignInClient mGoogleSignInClient;

    private boolean isAdminUser = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.main_activity);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        // Sử dụng ID mà bạn đã định nghĩa trong XML, ví dụ: R.id.main_activity_layout
        mainRootLayout = findViewById(R.id.main_activity_layout); // Đảm bảo ID này khớp với file XML của bạn!
        if (mainRootLayout == null) {
            Log.e(TAG, "Root layout ID 'main_activity_layout' not found. Swipe gesture might not work. Using android.R.id.content.");
            mainRootLayout = findViewById(android.R.id.content);
        }

        cardItemsList = findViewById(R.id.card_items_list);
        cardCategoriesList = findViewById(R.id.card_categories_list);
        cardProfile = findViewById(R.id.card_profile);
        cardUsers = findViewById(R.id.card_users);
        cardOrders = findViewById(R.id.card_order_list);
        cardShopping = findViewById(R.id.card_shopping);

        btnCart = findViewById(R.id.fab_cart);
        btnLogin = findViewById(R.id.btn_login);
        btnLogout = findViewById(R.id.btn_logout);
        btnSettings = findViewById(R.id.btn_settings);

        textViewWelcome = findViewById(R.id.text_view_welcome);

        // --- Khởi tạo GestureDetector và gắn OnTouchListener ---
        gestureDetector = new GestureDetector(this, new MyGestureListener());
        mainRootLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Đảm bảo GestureDetector xử lý sự kiện chạm
                // Trả về true để tiêu thụ sự kiện chạm, ngăn chặn các View con nhận sự kiện này
                return gestureDetector.onTouchEvent(event);
            }
        });
        // --- END: Khởi tạo GestureDetector ---


        // --- Thiết lập OnClickListener cho các MaterialCardView ---
        cardItemsList.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ItemsListActivity.class);
            startActivity(intent);
        });

        cardCategoriesList.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CategoriesListActivity.class);
            startActivity(intent);
        });

        cardShopping.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SalesActivity.class);
            startActivity(intent);
        });

        cardOrders.setOnClickListener(v -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {

                if (isAdminUser) {
                    Intent intent = new Intent(MainActivity.this, OrdersListActivity.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(MainActivity.this, OrderHistoryActivity.class);
                    startActivity(intent);
                }
            } else {
                Toast.makeText(MainActivity.this, "Please login to view orders.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            }
        });

        cardUsers.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, UsersListActivity.class);
            startActivity(intent);
        });

        cardProfile.setOnClickListener(v -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                Intent intent = new Intent(MainActivity.this, UserProfileActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Please login to manage personal information.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            }
        });

        btnCart.setOnClickListener(v -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                if (isAdminUser) {
                    Toast.makeText(MainActivity.this, "Cart is not available for admin.", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(MainActivity.this, CartsActivity.class);
                    startActivity(intent);
                }
            } else {
                Toast.makeText(MainActivity.this, "Please login to view cart.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            }
        });

        // --- Thiết lập OnClickListener cho các MaterialButton Authentication/Utility ---
        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            signOut();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Opening Settings...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // Quan sát trạng thái đăng nhập để cập nhật UI
        mAuth.addAuthStateListener(firebaseAuth -> {
            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            if (currentUser != null) {
                fetchUserRoleAndUpdateUI(currentUser);
            } else {
                updateUIForLoggedOutState();
            }
        });

        Log.d(TAG, "MainActivity created and listeners set.");
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            fetchUserRoleAndUpdateUI(currentUser);
        } else {
            updateUIForLoggedOutState();
        }
    }

    /**
     * Lấy vai trò của người dùng hiện tại từ Firestore và sau đó cập nhật UI.
     * @param user FirebaseUser hiện tại.
     */
    private void fetchUserRoleAndUpdateUI(FirebaseUser user) {
        db.collection("users").document(user.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String role = document.getString("role");
                            isAdminUser = "admin".equals(role);
                            updateUIAccess(user);
                        } else {
                            Log.w(TAG, "User document not found for UID: " + user.getUid());
                            isAdminUser = false;
                            updateUIAccess(user);
                            Toast.makeText(MainActivity.this, "User data missing, please contact support.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.e(TAG, "Error fetching user role: ", task.getException());
                        isAdminUser = false;
                        updateUIAccess(user);
                        Toast.makeText(MainActivity.this, "Failed to fetch user role. " + (task.getException() != null ? task.getException().getMessage() : ""), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Cập nhật hiển thị UI dựa trên vai trò của người dùng (admin hay user).
     * @param user FirebaseUser hiện tại.
     */
    private void updateUIAccess(FirebaseUser user) {
        btnLogin.setVisibility(View.GONE);
        btnLogout.setVisibility(View.VISIBLE);
        String welcomeMessage = "Welcome, " + (user.getDisplayName() != null && !user.getDisplayName().isEmpty() ? user.getDisplayName() : user.getEmail()) + "!\nThe place to sell more conveniently than ever";
        textViewWelcome.setText(welcomeMessage);

        if (isAdminUser) {
            cardCategoriesList.setVisibility(View.VISIBLE);
            cardUsers.setVisibility(View.VISIBLE);
            btnCart.setVisibility(View.GONE);

            Log.d(TAG, "UI updated for Admin user.");
        } else {
            cardCategoriesList.setVisibility(View.GONE);
            cardUsers.setVisibility(View.GONE);
            btnCart.setVisibility(View.VISIBLE); // HIỂN THỊ GIỎ HÀNG CHO USER THÔNG THƯỜNG

            Log.d(TAG, "UI updated for Regular user.");
        }
        cardProfile.setVisibility(View.VISIBLE);
        cardShopping.setVisibility(View.VISIBLE);
        cardItemsList.setVisibility(View.VISIBLE);
        cardOrders.setVisibility(View.VISIBLE);



    }

    /**
     * Cập nhật hiển thị UI khi không có người dùng nào đăng nhập.
     */
    private void updateUIForLoggedOutState() {
        btnLogin.setVisibility(View.VISIBLE);
        btnLogout.setVisibility(View.GONE);
        textViewWelcome.setText("Welcome to Sale Management! \nThe place to sell more conveniently than ever");

        cardItemsList.setVisibility(View.GONE);
        cardCategoriesList.setVisibility(View.GONE);
        cardUsers.setVisibility(View.GONE);
        cardOrders.setVisibility(View.GONE);
        cardShopping.setVisibility(View.GONE);
        cardProfile.setVisibility(View.GONE);
        btnCart.setVisibility(View.GONE);

        Log.d(TAG, "UI updated for Logged out state.");
    }

    /**
     * Xử lý đăng xuất người dùng khỏi Firebase và Google (nếu có).
     */
    private void signOut() {
        mAuth.signOut();

        if (mGoogleSignInClient != null) {
            mGoogleSignInClient.signOut().addOnCompleteListener(this,
                    task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Google Sign-out complete.");
                        } else {
                            Log.e(TAG, "Google Sign-out failed.", task.getException());
                        }
                    });
        }
        Toast.makeText(this, "Logged out.", Toast.LENGTH_SHORT).show();
        updateUIForLoggedOutState();
    }

    // Lớp nội bộ để xử lý các cử chỉ vuốt
    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) { // Vuốt sang phải
                            // Hiện tại không có hành động cho vuốt phải, có thể thêm Activity khác nếu cần
                            // if (!isAdminUser) { onSwipeRightToSales(); } // Logic này có thể gây nhầm lẫn nếu bạn muốn vuốt trái
                            Log.d(TAG, "Swiped right. No specific action defined.");
                        } else { // Vuốt sang trái (diffX < 0)
                            // Chỉ cho phép vuốt sang Sales khi không phải admin
                            if (!isAdminUser) {
                                onSwipeLeftToSales(); // ĐÃ THÊM LOGIC VUỐT TRÁI
                            } else {
                                Toast.makeText(MainActivity.this, "Sales view is for regular users.", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "Admin user tried to swipe to Sales, but it's restricted.");
                            }
                        }
                        result = true;
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return result;
        }
    }

    // Phương thức xử lý khi vuốt sang TRÁI để mở Sales Activity
    private void onSwipeLeftToSales() { // ĐÃ ĐỔI TÊN PHƯƠNG THỨC
        Toast.makeText(MainActivity.this, "Swiped Left to Products for Sale!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivity.this, SalesActivity.class);
        startActivity(intent);
    }
}