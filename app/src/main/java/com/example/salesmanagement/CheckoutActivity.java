package com.example.salesmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.NumberFormat;
import java.util.ArrayList; // <--- THÊM DÒNG NÀY
import java.util.Date;
import java.util.Locale;

public class CheckoutActivity extends AppCompatActivity {

    private static final String TAG = "CheckoutActivity";

    private TextInputEditText editTextCustomerName, editTextDeliveryAddress, editTextPhoneNumber, editTextOrderNotes;
    private RadioGroup radioGroupPaymentMethod;
    private TextView textViewCheckoutTotal;
    private RecyclerView recyclerViewCheckoutItems;
    private MaterialButton btnPlaceOrder;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private CheckoutAdapter checkoutItemsAdapter;

    // Thay đổi từ List<CartItem> currentCartItems sang ArrayList để linh hoạt hơn
    private ArrayList<OrderItem> itemsToProcess; // Sẽ chứa item duy nhất nếu là Buy Now, hoặc toàn bộ giỏ hàng

    private String currentUserDisplayName = "";
    private String currentUserEmail = "";
    private boolean isBuyNowFlow = false; // Cờ để phân biệt luồng Buy Now và Cart Checkout

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.checkout);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar_checkout);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Checkout");
        }

        editTextCustomerName = findViewById(R.id.edit_text_customer_name);
        editTextDeliveryAddress = findViewById(R.id.edit_text_delivery_address);
        editTextPhoneNumber = findViewById(R.id.edit_text_phone_number);
        editTextOrderNotes = findViewById(R.id.edit_text_order_notes);
        radioGroupPaymentMethod = findViewById(R.id.radio_group_payment_method);
        textViewCheckoutTotal = findViewById(R.id.text_view_checkout_total);
        recyclerViewCheckoutItems = findViewById(R.id.recycler_view_checkout_items);
        btnPlaceOrder = findViewById(R.id.btn_place_order);

        // --- Bắt đầu logic xử lý Buy Now vs Cart Checkout ---
        Intent intent = getIntent();
        isBuyNowFlow = intent.getBooleanExtra("isBuyNow", false);

        if (isBuyNowFlow) {
            // Trường hợp "Buy Now"
            OrderItem buyNowItem = (OrderItem) intent.getSerializableExtra("buyNowItem");
            if (buyNowItem != null) {
                itemsToProcess = new ArrayList<>();
                itemsToProcess.add(buyNowItem); // Chỉ thêm sản phẩm được mua ngay
                Log.d(TAG, "Checkout in Buy Now mode for item: " + buyNowItem.getName() + ", Quantity: " + buyNowItem.getQuantity());
            } else {
                Toast.makeText(this, "Error: Buy Now item not found. Returning to previous screen.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Buy Now item was null in intent.");
                finish(); // Đóng activity nếu không có dữ liệu
                return;
            }
        } else {
            // Trường hợp "Checkout from Cart" (logic cũ)
            itemsToProcess = new ArrayList<>(ShoppingCartManager.getInstance().getCartItems()); // Lấy toàn bộ giỏ hàng
            Log.d(TAG, "Checkout in Cart mode. Total items: " + itemsToProcess.size());
            if (itemsToProcess.isEmpty()) {
                Toast.makeText(this, "Your cart is empty. Please add items to checkout.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        // --- Kết thúc logic xử lý Buy Now vs Cart Checkout ---


        setupCheckoutRecyclerView(); // Sử dụng itemsToProcess đã được xác định
        updateCheckoutSummary();     // Sử dụng itemsToProcess đã được xác định

        populateUserData();

        btnPlaceOrder.setOnClickListener(v -> placeOrder());
    }

    private void setupCheckoutRecyclerView() {
        checkoutItemsAdapter = new CheckoutAdapter(itemsToProcess); // Sử dụng itemsToProcess
        recyclerViewCheckoutItems.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCheckoutItems.setAdapter(checkoutItemsAdapter);
        recyclerViewCheckoutItems.setNestedScrollingEnabled(false);
    }

    private void updateCheckoutSummary() {
        double total = 0;
        for (OrderItem item : itemsToProcess) { // Tính tổng từ itemsToProcess
            total += item.getSubtotal();
        }
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        textViewCheckoutTotal.setText("Total price: " + currencyFormatter.format(total));
    }

    private void populateUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserEmail = currentUser.getEmail();
            currentUserDisplayName = currentUser.getDisplayName();

            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Users user = documentSnapshot.toObject(Users.class);
                            if (user != null) {
                                if (user.getName() != null && !user.getName().isEmpty()) {
                                    editTextCustomerName.setText(user.getName());
                                    currentUserDisplayName = user.getName();
                                } else if (currentUserDisplayName != null && !currentUserDisplayName.isEmpty()) {
                                    editTextCustomerName.setText(currentUserDisplayName);
                                }

                                if (user.getAddress() != null && !user.getAddress().isEmpty()) {
                                    editTextDeliveryAddress.setText(user.getAddress());
                                }
                                if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                                    editTextPhoneNumber.setText(user.getPhoneNumber());
                                }
                                Log.d(TAG, "User data populated from Firestore.");
                            }
                        } else {
                            Log.d(TAG, "No user document found in Firestore for UID: " + currentUser.getUid());
                            if (currentUserDisplayName != null && !currentUserDisplayName.isEmpty()) {
                                editTextCustomerName.setText(currentUserDisplayName);
                            } else if (currentUserEmail != null && !currentUserEmail.isEmpty()) {
                                // editTextCustomerName.setText(currentUserEmail.split("@")[0]);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching user data for auto-population: " + e.getMessage());
                        Toast.makeText(CheckoutActivity.this, "Error loading user information.", Toast.LENGTH_SHORT).show();
                        if (currentUserDisplayName != null && !currentUserDisplayName.isEmpty()) {
                            editTextCustomerName.setText(currentUserDisplayName);
                        } else if (currentUserEmail != null && !currentUserEmail.isEmpty()) {
                            // editTextCustomerName.setText(currentUserEmail.split("@")[0]);
                        }
                    });
        } else {
            Log.d(TAG, "No current user logged in. Cannot auto-populate user data.");
        }
    }


    private void placeOrder() {
        String customerName = editTextCustomerName.getText().toString().trim();
        String deliveryAddress = editTextDeliveryAddress.getText().toString().trim();
        String phoneNumber = editTextPhoneNumber.getText().toString().trim();
        String notes = editTextOrderNotes.getText().toString().trim();

        if (customerName.isEmpty() || deliveryAddress.isEmpty() || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Please fill in complete shipping information.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra itemsToProcess thay vì currentCartItems
        if (itemsToProcess == null || itemsToProcess.isEmpty()) {
            Toast.makeText(this, "No items to order. Cannot place order.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You need to login to order.", Toast.LENGTH_SHORT).show();
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
            return;
        }

        String userId = currentUser.getUid();
        String orderUserName = currentUserDisplayName;
        if (orderUserName == null || orderUserName.isEmpty()) {
            orderUserName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : (customerName.isEmpty() ? "Guest" : customerName);
        }

        String orderUserEmail = currentUserEmail;
        if (orderUserEmail == null || orderUserEmail.isEmpty()) {
            orderUserEmail = currentUser.getEmail() != null ? currentUser.getEmail() : "N/A";
        }

        String paymentMethod = "";
        int selectedId = radioGroupPaymentMethod.getCheckedRadioButtonId();
        if (selectedId == R.id.radio_cash_on_delivery) {
            paymentMethod = "Cash on Delivery (COD)";
        } else if (selectedId == R.id.radio_bank_transfer) {
            paymentMethod = "Bank transfer";
        } else if (selectedId == R.id.radio_credit_card) {
            paymentMethod = "Credit card";
        } else {
            Toast.makeText(this, "Please select payment method.", Toast.LENGTH_SHORT).show();
            return;
        }

        double totalAmount = 0;
        for (OrderItem item : itemsToProcess) { // Tính tổng từ itemsToProcess
            totalAmount += item.getSubtotal();
        }

        String status = "Pending"; // Trạng thái ban đầu của đơn hàng

        Orders newOrder = new Orders(
                null, // orderId sẽ được Firestore tự tạo
                userId,
                orderUserName,
                orderUserEmail,
                deliveryAddress,
                phoneNumber,
                paymentMethod,
                totalAmount,
                status,
                notes,
                new Date(),
                itemsToProcess // Truyền itemsToProcess (có thể là 1 hoặc nhiều item)
        );

        WriteBatch batch = db.batch();
        DocumentReference newOrderRef = db.collection("orders").document();

        newOrder.setOrderId(newOrderRef.getId());

        batch.set(newOrderRef, newOrder);

        /*/ Cập nhật số lượng sản phẩm trong collection "Items"
        for (CartItem itemInOrder : itemsToProcess) { // Lặp qua itemsToProcess
            DocumentReference itemRef = db.collection("Items").document(itemInOrder.getItemId());
            batch.update(itemRef, "quantity", FieldValue.increment(-itemInOrder.getQuantity()));
        }*/

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(CheckoutActivity.this, "Order placed successful!", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Order placed successfully: " + newOrder.getOrderId());

                    // Nếu là Buy Now, không cần xóa giỏ hàng
                    if (!isBuyNowFlow) {
                        ShoppingCartManager.getInstance().clearAllCartItems(new ShoppingCartManager.OnOperationCompleteListener() {
                            @Override
                            public void onSuccess(String message) {
                                Log.d(TAG, "Cart cleared after successful order: " + message);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Log.e(TAG, "Failed to clear cart after order: " + e.getMessage());
                            }
                        });
                    }

                    Intent intent = new Intent(CheckoutActivity.this, OrderHistoryActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CheckoutActivity.this, "Error placing order: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error placing order: " + e.getMessage(), e);
                });
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