package com.example.salesmanagement;

import android.content.DialogInterface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreException.Code;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap; // Thêm import này
import java.util.List;
import java.util.Locale;
import java.util.Map; // Thêm import này

public class OrderDetailsActivity extends AppCompatActivity {

    private static final String TAG = "OrderDetails";

    private TextView tvOrderId, tvOrderDate, tvOrderStatus, tvCustomerName, tvPhoneNumber,
            tvDeliveryAddress, tvPaymentMethod, tvNotes, tvTotalAmount;
    private RecyclerView recyclerViewDetailItems;
    private Button buttonUpdateStatus;

    private CheckoutAdapter detailItemsAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private Orders currentOrder;
    private String previousOrderStatus; // Để lưu trạng thái trước khi cập nhật

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.order_details);

        Toolbar toolbar = findViewById(R.id.toolbar_order_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Order Details");
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvOrderId = findViewById(R.id.tv_detail_order_id);
        tvOrderDate = findViewById(R.id.tv_detail_order_date);
        tvOrderStatus = findViewById(R.id.tv_detail_order_status);
        tvCustomerName = findViewById(R.id.tv_detail_customer_name);
        tvPhoneNumber = findViewById(R.id.tv_detail_phone_number);
        tvDeliveryAddress = findViewById(R.id.tv_detail_delivery_address);
        tvPaymentMethod = findViewById(R.id.tv_detail_payment_method);
        tvNotes = findViewById(R.id.tv_detail_notes);
        tvTotalAmount = findViewById(R.id.tv_detail_total_amount);
        recyclerViewDetailItems = findViewById(R.id.recycler_view_order_detail_items);
        buttonUpdateStatus = findViewById(R.id.button_update_status);

        currentOrder = (Orders) getIntent().getSerializableExtra("order");

        if (currentOrder != null) {
            previousOrderStatus = currentOrder.getStatus(); // Lưu trạng thái hiện tại của đơn hàng
            displayOrderDetails(currentOrder);
            checkUserRoleAndShowUpdateButton();

            buttonUpdateStatus.setOnClickListener(v -> showStatusUpdateDialog());
        } else {
            Toast.makeText(this, "Order information not found.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void displayOrderDetails(Orders order) {
        tvOrderId.setText("NO: " + order.getOrderId());

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        if (order.getOrderDate() != null) {
            tvOrderDate.setText("Order date: " + sdf.format(order.getOrderDate()));
        } else {
            tvOrderDate.setText("Order date: N/A");
        }

        tvOrderStatus.setText("Status: " + order.getStatus());
        setColorForStatus(tvOrderStatus, order.getStatus());

        tvCustomerName.setText("Customer name: " + (order.getUserName() != null ? order.getUserName() : "N/A"));
        tvPhoneNumber.setText("Phone number: " + (order.getPhoneNumber() != null ? order.getPhoneNumber() : "N/A"));

        tvDeliveryAddress.setText("Delivery address: " + order.getDeliveryAddress());
        tvPaymentMethod.setText("Payment method: " + order.getPaymentMethod());

        if (order.getNotes() != null && !order.getNotes().isEmpty()) {
            tvNotes.setText("Order note: " + order.getNotes());
            tvNotes.setVisibility(View.VISIBLE);
        } else {
            tvNotes.setVisibility(View.GONE);
        }

        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        tvTotalAmount.setText("Total Price: " + currencyFormatter.format(order.getTotalAmount()));

        List<OrderItem> orderItems = order.getItems();

        if (orderItems != null && !orderItems.isEmpty()) {
            detailItemsAdapter = new CheckoutAdapter(orderItems);
            recyclerViewDetailItems.setLayoutManager(new LinearLayoutManager(this));
            recyclerViewDetailItems.setAdapter(detailItemsAdapter);
            recyclerViewDetailItems.setNestedScrollingEnabled(false);
        } else {
            Log.d(TAG, "No items found in this order.");
        }
    }

    private void checkUserRoleAndShowUpdateButton() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String role = documentSnapshot.getString("role");
                            if (role != null && role.equals("admin")) {
                                buttonUpdateStatus.setVisibility(View.VISIBLE);
                            } else {
                                buttonUpdateStatus.setVisibility(View.GONE);
                            }
                        } else {
                            Log.e(TAG, "User document not found for UID: " + currentUser.getUid());
                            buttonUpdateStatus.setVisibility(View.GONE);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error checking user role: " + e.getMessage());
                        Toast.makeText(this, "Error checking user role.", Toast.LENGTH_SHORT).show();
                        buttonUpdateStatus.setVisibility(View.GONE);
                    });
        } else {
            buttonUpdateStatus.setVisibility(View.GONE);
        }
    }

    private void showStatusUpdateDialog() {
        final String[] statusOptions = {"Pending", "Confirmed", "Shipped", "Delivered", "Cancelled"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update order status")
                .setItems(statusOptions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newStatus = statusOptions[which];
                        // Chỉ cho phép cập nhật nếu trạng thái mới khác trạng thái hiện tại
                        if (!newStatus.equals(currentOrder.getStatus())) {
                            updateOrderStatus(newStatus);
                        } else {
                            Toast.makeText(OrderDetailsActivity.this, "Status is already " + newStatus, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateOrderStatus(String newStatus) {
        if (currentOrder == null || currentOrder.getOrderId() == null) {
            Toast.makeText(this, "There are no orders to update.", Toast.LENGTH_SHORT).show();
            return;
        }

        final String oldStatus = currentOrder.getStatus();

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentReference orderRef = db.collection("orders").document(currentOrder.getOrderId());
                    DocumentSnapshot orderSnapshot = transaction.get(orderRef); // Đọc order đầu tiên

                    if (!orderSnapshot.exists()) {
                        Log.e(TAG, "Transaction Aborted: Order document does not exist for ID: " + currentOrder.getOrderId());
                        throw new FirebaseFirestoreException("Order does not exist!", Code.NOT_FOUND);
                    }

                    Orders orderInTransaction = orderSnapshot.toObject(Orders.class);
                    if (orderInTransaction == null) {
                        Log.e(TAG, "Transaction Aborted: Failed to parse order data for ID: " + currentOrder.getOrderId());
                        throw new FirebaseFirestoreException("Failed to parse order data.", Code.DATA_LOSS);
                    }

                    if (!orderInTransaction.getStatus().equals(oldStatus)) {
                        Log.w(TAG, "Transaction Aborted: Order status changed by another process. Old: " + oldStatus + ", Current in DB: " + orderInTransaction.getStatus());
                        throw new FirebaseFirestoreException("Order status changed by another process. Please refresh.", Code.ABORTED);
                    }

                    // --- BẮT ĐẦU ĐỌC TẤT CẢ CÁC TÀI LIỆU CẦN THIẾT TRƯỚC KHI GHI ---
                    Map<String, DocumentSnapshot> itemSnapshots = new HashMap<>();
                    Map<String, DocumentSnapshot> userSnapshots = new HashMap<>();

                    List<OrderItem> itemsInOrder = orderInTransaction.getItems();

                    if (itemsInOrder != null) {
                        for (OrderItem itemInCart : itemsInOrder) {
                            DocumentReference productRef = db.collection("Items").document(itemInCart.getItemId());
                            DocumentSnapshot productSnapshot = transaction.get(productRef); // Đọc sản phẩm
                            itemSnapshots.put(itemInCart.getItemId(), productSnapshot);

                            if (productSnapshot.exists()) {
                                Items product = productSnapshot.toObject(Items.class);
                                if (product != null && product.getUserId() != null) {
                                    DocumentReference ownerRef = db.collection("users").document(product.getUserId());
                                    DocumentSnapshot ownerSnapshot = transaction.get(ownerRef); // Đọc người dùng
                                    userSnapshots.put(product.getUserId(), ownerSnapshot);
                                }
                            }
                        }
                    }
                    // --- KẾT THÚC ĐỌC TẤT CẢ CÁC TÀI LIỆU CẦN THIẾT ---


                    // --- BẮT ĐẦU GHI SAU KHI TẤT CẢ CÁC ĐỌC ĐÃ HOÀN THÀNH ---
                    transaction.update(orderRef, "status", newStatus); // Cập nhật trạng thái đơn hàng

                    // Xử lý cập nhật ví và giảm số lượng sản phẩm khi Đơn hàng được "Delivered"
                    if (newStatus.equals("Delivered") && !oldStatus.equals("Delivered")) {
                        if (itemsInOrder != null) {
                            for (OrderItem itemInCart : itemsInOrder) {
                                DocumentSnapshot productSnapshot = itemSnapshots.get(itemInCart.getItemId());
                                if (productSnapshot != null && productSnapshot.exists()) {
                                    Items product = productSnapshot.toObject(Items.class);
                                    if (product != null && product.getUserId() != null) {
                                        DocumentSnapshot ownerSnapshot = userSnapshots.get(product.getUserId());
                                        if (ownerSnapshot != null && ownerSnapshot.exists()) {
                                            DocumentReference ownerRef = db.collection("users").document(product.getUserId());
                                            double amountToAdd = itemInCart.getPrice() * itemInCart.getQuantity();
                                            Log.d(TAG, "Adding " + amountToAdd + " to owner " + product.getUserId() + "'s balance for item " + itemInCart.getName());
                                            transaction.update(ownerRef, "balance", FieldValue.increment(amountToAdd));
                                        } else {
                                            Log.w(TAG, "Owner document not found for userId: " + product.getUserId() + ". Balance update skipped.");
                                        }

                                        // THÊM LOGIC NÀY: Giảm số lượng sản phẩm khi trạng thái là "Delivered"
                                        DocumentReference productRef = db.collection("Items").document(itemInCart.getItemId());
                                        Log.d(TAG, "Decreasing " + itemInCart.getQuantity() + " from stock for item " + itemInCart.getName() + " on Delivered status.");
                                        transaction.update(productRef, "quantity", FieldValue.increment(-itemInCart.getQuantity()));

                                    } else {
                                        Log.w(TAG, "Product or ownerId missing for item: " + itemInCart.getName() + " (ID: " + itemInCart.getItemId() + "). Balance/Quantity update skipped.");
                                    }
                                } else {
                                    Log.w(TAG, "Product document not found in snapshot map for itemId: " + itemInCart.getItemId() + ". Balance/Quantity update skipped.");
                                }
                            }
                        }
                    }
                    // Xử lý trả lại số lượng khi Đơn hàng bị "Cancelled"
                    else if (newStatus.equals("Cancelled") && !oldStatus.equals("Cancelled")) {
                        if (itemsInOrder != null) {
                            for (OrderItem itemInCart : itemsInOrder) {
                                DocumentSnapshot productSnapshot = itemSnapshots.get(itemInCart.getItemId()); // Lấy snapshot đã đọc
                                if (productSnapshot != null && productSnapshot.exists()) {
                                    DocumentReference productRef = db.collection("Items").document(itemInCart.getItemId());
                                    Log.d(TAG, "Returning " + itemInCart.getQuantity() + " to stock for item " + itemInCart.getName() + " on Cancelled status.");
                                    transaction.update(productRef, "quantity", FieldValue.increment(itemInCart.getQuantity()));
                                } else {
                                    Log.w(TAG, "Product document not found in snapshot map for itemId: " + itemInCart.getItemId() + ". Quantity return skipped.");
                                }
                            }
                        }
                    }
                    // --- KẾT THÚC GHI ---

                    return null; // Giao dịch thành công
                })
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(OrderDetailsActivity.this, "Status update successful!", Toast.LENGTH_SHORT).show();
                    currentOrder.setStatus(newStatus); // Update local object
                    previousOrderStatus = newStatus; // Update previous status for next changes
                    displayOrderDetails(currentOrder); // Refresh UI
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Status update error: " + e.getMessage(), e);
                    // The message from the thrown Exception will be in e.getMessage()
                    Toast.makeText(OrderDetailsActivity.this, "Status update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setColorForStatus(TextView textView, String status) {
        int color;
        switch (status) {
            case "Pending":
                color = ContextCompat.getColor(textView.getContext(), R.color.status_pending);
                break;
            case "Confirmed":
                color = ContextCompat.getColor(textView.getContext(), R.color.status_confirmed);
                break;
            case "Shipped":
                color = ContextCompat.getColor(textView.getContext(), R.color.status_shipped);
                break;
            case "Delivered":
                color = ContextCompat.getColor(textView.getContext(), R.color.status_delivered);
                break;
            case "Cancelled":
                color = ContextCompat.getColor(textView.getContext(), R.color.status_cancelled);
                break;
            default:
                color = ContextCompat.getColor(textView.getContext(), android.R.color.darker_gray);
                break;
        }
        GradientDrawable background = (GradientDrawable) textView.getBackground();
        if (background == null) {
            background = (GradientDrawable) ContextCompat.getDrawable(textView.getContext(), R.drawable.rounded_status_background);
            if (background != null) {
                textView.setBackground(background);
            }
        }
        if (background != null) {
            background.setColor(color);
        }
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