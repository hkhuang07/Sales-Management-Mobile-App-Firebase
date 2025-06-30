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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.firebase.ui.firestore.FirestoreRecyclerOptions; // Đảm bảo import này

// Đã đổi tên class thành OrderHistoryActivity cho nhất quán
public class OrderHistoryActivity extends AppCompatActivity {

    private static final String TAG = "OrderHistory";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private CollectionReference ordersCollection;

    private RecyclerView recyclerView;
    private OrderAdapter adapter; // Adapter cho RecyclerView
    private TextView emptyOrdersTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.order_history);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        ordersCollection = db.collection("orders"); // Đảm bảo tên collection là "orders" (chữ thường)

        Toolbar toolbar = findViewById(R.id.toolbar_order_history);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.recycler_view_order_history);
        emptyOrdersTextView = findViewById(R.id.text_view_empty_orders);

        // setupRecyclerView() sẽ được gọi trong onStart()
        // để đảm bảo nó được thiết lập lại khi trạng thái đăng nhập thay đổi
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Gọi setupRecyclerView ở đây để nó được khởi tạo hoặc khởi tạo lại
        // mỗi khi Activity hiển thị, đảm bảo truy vấn đúng với người dùng hiện tại
        setupRecyclerViewAndStartListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening(); // Dừng lắng nghe khi Activity không còn hiển thị
            Log.d(TAG, "Order adapter stopped listening.");
        }
    }

    private void setupRecyclerViewAndStartListening() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // Người dùng chưa đăng nhập
            Toast.makeText(this, "Please login to view order history.", Toast.LENGTH_LONG).show();
            emptyOrdersTextView.setText("Please login to view order history.");
            emptyOrdersTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);

            // Dừng adapter nếu nó đang lắng nghe từ phiên trước (của người dùng khác hoặc guest)
            if (adapter != null) {
                adapter.stopListening();
            }
            return;
        }

        // Tạo truy vấn Firestore để lấy đơn hàng của người dùng hiện tại
        Query query = ordersCollection
                .whereEqualTo("userId", currentUser.getUid())
                .orderBy("orderDate", Query.Direction.DESCENDING); // Sắp xếp theo ngày mới nhất

        // Xây dựng FirestoreRecyclerOptions
        FirestoreRecyclerOptions<Orders> options = new FirestoreRecyclerOptions.Builder<Orders>()
                .setQuery(query, Orders.class)
                .build();

        // Nếu adapter đã tồn tại, cập nhật options và khởi động lại
        if (adapter != null) {
            adapter.updateOptions(options); // Cập nhật truy vấn cho adapter hiện có
            adapter.startListening();
            Log.d(TAG, "Order adapter updated options and started listening.");
        } else {
            // Nếu adapter chưa tồn tại, tạo mới
            adapter = new OrderAdapter(options);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);

            // Thiết lập OnItemClickListener cho adapter
            adapter.setOnItemClickListener(new OrderAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(DocumentSnapshot documentSnapshot, int position) {
                    Orders order = documentSnapshot.toObject(Orders.class);
                    if (order != null) {
                        order.setOrderId(documentSnapshot.getId()); // Đặt OrderId từ Document ID
                        Log.d(TAG, "Order clicked: " + order.getOrderId());
                        // Chuyển sang OrderDetailsActivity
                        Intent intent = new Intent(OrderHistoryActivity.this, OrderDetailsActivity.class);
                        intent.putExtra("order", order); // Truyền toàn bộ đối tượng Order
                        startActivity(intent);
                    }
                }
            });

            // Đăng ký AdapterDataObserver để kiểm tra trạng thái rỗng
            adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    checkEmptyState();
                }

                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    super.onItemRangeInserted(positionStart, itemCount);
                    checkEmptyState();
                }

                @Override
                public void onItemRangeRemoved(int positionStart, int itemCount) {
                    super.onItemRangeRemoved(positionStart, itemCount);
                    checkEmptyState();
                }
            });

            adapter.startListening(); // Bắt đầu lắng nghe dữ liệu
            Log.d(TAG, "New order adapter created and started listening.");
        }

        // Kiểm tra trạng thái rỗng ban đầu (sau khi adapter đã được thiết lập)
        checkEmptyState();
    }

    private void checkEmptyState() {
        if (emptyOrdersTextView != null && recyclerView != null) {
            if (adapter != null && adapter.getItemCount() == 0) {
                emptyOrdersTextView.setText("You have no orders yet."); // Thông báo cụ thể hơn
                emptyOrdersTextView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyOrdersTextView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        } else {
            Log.e(TAG, "Views are null in checkEmptyState()!");
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