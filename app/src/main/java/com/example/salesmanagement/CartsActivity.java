// File: app/src/main/java/com/example/salesmanagement/CartsActivity.java
package com.example.salesmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
// Không cần FirebaseFirestore trực tiếp ở đây để thao tác cart,
// vì ShoppingCartManager đã xử lý điều đó.
// import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CartsActivity extends AppCompatActivity implements ShoppingCartManager.OnCartChangeListener {

    private static final String TAG = "CartActivity";

    private RecyclerView recyclerView;
    private CartsAdapter cartAdapter; // Bây giờ sẽ làm việc với CartItem
    private TextView textViewTotal;
    private MaterialButton btnCheckout;
    private TextView emptyCartTextView;

    // Không cần FirebaseFirestore db trực tiếp ở đây để thao tác cart,
    // vì ShoppingCartManager đã xử lý điều đó.
    // private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.carts);

        // db = FirebaseFirestore.getInstance(); // Không cần thiết ở đây nếu dùng ShoppingCartManager

        Toolbar toolbar = findViewById(R.id.toolbar_cart);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Your Cart"); // Đặt tiêu đề rõ ràng
        }

        recyclerView = findViewById(R.id.recycler_view_cart_items);
        textViewTotal = findViewById(R.id.text_view_cart_total);
        btnCheckout = findViewById(R.id.btn_checkout);
        emptyCartTextView = findViewById(R.id.text_view_empty_cart);

        setupCartRecyclerView();
        // Không cần updateCartDisplay() ở đây vì onResume sẽ gọi và trigger onCartChanged

        btnCheckout.setOnClickListener(v -> {
            if (ShoppingCartManager.getInstance().getCartItemCount() > 0) {
                // Chuyển sang CheckoutActivity.
                // Lưu ý: Nếu CheckoutActivity của bạn cần danh sách các mặt hàng,
                // bạn có thể lấy nó từ ShoppingCartManager.getInstance().getCartItems()
                // trong CheckoutActivity's onCreate, hoặc truyền một cách serialize (không khuyến khích cho danh sách lớn).
                Intent intent = new Intent(CartsActivity.this, CheckoutActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(CartsActivity.this, "Your cart is empty!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupCartRecyclerView() {
        // Khởi tạo adapter với danh sách hiện tại từ ShoppingCartManager (sẽ là CartItem)
        cartAdapter = new CartsAdapter(ShoppingCartManager.getInstance().getCartItems());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(cartAdapter);

        // Đặt listener cho các hành động trên mỗi mục giỏ hàng
        cartAdapter.setOnCartItemActionListener(new CartsAdapter.OnCartItemActionListener() {
            @Override
            public void onUpdateQuantity(OrderItem item, int newQuantity) { // item là CartItem
                // Gọi ShoppingCartManager để xử lý cập nhật số lượng trên Firestore
                ShoppingCartManager.getInstance().updateCartItemQuantity(item.getItemId(), newQuantity, new ShoppingCartManager.OnOperationCompleteListener() {
                    @Override
                    public void onSuccess(String message) {
                        Toast.makeText(CartsActivity.this, message, Toast.LENGTH_SHORT).show();
                        // updateCartDisplay() sẽ được gọi tự động qua listener khi Firestore thay đổi
                    }

                    @Override
                    public void onFailure(Exception e) {
                        String errorMessage = e.getMessage();
                        if (errorMessage == null) errorMessage = "Error updating quantity.";
                        Toast.makeText(CartsActivity.this, "Failed to update: " + errorMessage, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Failed to update quantity for " + item.getName() + ": " + errorMessage);
                    }
                });
            }

            @Override
            public void onDeleteItem(OrderItem item) { // item là CartItem
                // Gọi ShoppingCartManager để xử lý xóa mục trên Firestore
                ShoppingCartManager.getInstance().removeItemFromCart(item.getItemId(), new ShoppingCartManager.OnOperationCompleteListener() {
                    @Override
                    public void onSuccess(String message) {
                        Toast.makeText(CartsActivity.this, message, Toast.LENGTH_SHORT).show();
                        // updateCartDisplay() sẽ được gọi tự động qua listener khi Firestore thay đổi
                    }

                    @Override
                    public void onFailure(Exception e) {
                        String errorMessage = e.getMessage();
                        if (errorMessage == null) errorMessage = "Error deleting item.";
                        Toast.makeText(CartsActivity.this, "Failed to delete: " + errorMessage, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Failed to delete item " + item.getName() + ": " + errorMessage);
                    }
                });
            }
        });
    }

    // Cập nhật giao diện giỏ hàng
    private void updateCartDisplay() {
        // Lấy danh sách mới nhất từ ShoppingCartManager (đã được đồng bộ với Firestore)
        List<OrderItem> currentOrderItems = ShoppingCartManager.getInstance().getCartItems();
        cartAdapter.updateCartItems(currentOrderItems); // Cập nhật adapter với CartItem

        double total = ShoppingCartManager.getInstance().getTotalAmount();
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        textViewTotal.setText("Total: " + currencyFormatter.format(total));

        // Hiển thị/ẩn TextView "Giỏ hàng trống"
        if (currentOrderItems.isEmpty()) {
            emptyCartTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            btnCheckout.setEnabled(false); // Vô hiệu hóa nút thanh toán
        } else {
            emptyCartTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            btnCheckout.setEnabled(true); // Kích hoạt nút thanh toán
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Đăng ký listener khi Activity hoạt động trở lại
        ShoppingCartManager.getInstance().setOnCartChangeListener(this);
        // Bắt đầu lắng nghe Firestore cho giỏ hàng của người dùng khi Activity hoạt động trở lại
        ShoppingCartManager.getInstance().setupUserCartListener();
        // Không cần updateCartDisplay() ở đây vì onCartChanged() sẽ được gọi tự động
        // ngay sau khi setupUserCartListener() fetch dữ liệu lần đầu hoặc khi có thay đổi.
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Hủy đăng ký listener khi Activity tạm dừng để tránh rò rỉ bộ nhớ
        ShoppingCartManager.getInstance().removeOnCartChangeListener(this); // Gỡ bỏ listener cụ thể này
        // Dừng lắng nghe Firestore khi Activity tạm dừng để tiết kiệm tài nguyên
        ShoppingCartManager.getInstance().stopListeningToCart();
    }

    // Triển khai phương thức của ShoppingCartManager.OnCartChangeListener
    @Override
    public void onCartChanged() {
        Log.d(TAG, "Cart changed, updating display via onCartChanged callback.");
        updateCartDisplay(); // Gọi cập nhật giao diện khi giỏ hàng thay đổi
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Xử lý nút quay lại trên Toolbar
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}