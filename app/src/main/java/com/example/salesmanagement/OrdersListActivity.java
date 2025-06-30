package com.example.salesmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class OrdersListActivity extends AppCompatActivity implements OrdersListAdapter.OnOrderActionListener {

    private static final String TAG = "OrdersList";

    private RecyclerView recyclerViewOrders;
    private OrdersListAdapter ordersAdapter;
    private ProgressBar progressBar;
    private TextView textViewNoOrders;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FloatingActionButton btnMyOrder;

    private CollectionReference ordersCollection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.orders_list);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        ordersCollection = db.collection("orders");

        Toolbar toolbar = findViewById(R.id.toolbar_orders_list);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Orders List");
        }

        recyclerViewOrders = findViewById(R.id.recycler_view_orders);
        progressBar = findViewById(R.id.progress_bar_orders);
        textViewNoOrders = findViewById(R.id.text_view_no_orders);
        btnMyOrder = findViewById(R.id.btn_my_order);

        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(this));
        btnMyOrder.setOnClickListener(v -> {
            Intent intent = new Intent(OrdersListActivity.this, OrderHistoryActivity.class);
            startActivity(intent);
        });

        setupRecyclerViewAndStartListening();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ordersAdapter != null) {
            ordersAdapter.startListening();
            Log.d(TAG, "OrdersList adapter started listening.");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (ordersAdapter != null) {
            ordersAdapter.stopListening();
            Log.d(TAG, "OrdersList adapter stopped listening.");
        }
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

    private void setupRecyclerViewAndStartListening() {
        progressBar.setVisibility(View.VISIBLE);

        Query query = ordersCollection
                .orderBy("orderDate", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<Orders> options = new FirestoreRecyclerOptions.Builder<Orders>()
                .setQuery(query, Orders.class)
                .build();

        if (ordersAdapter == null) {
            ordersAdapter = new OrdersListAdapter(options);
            recyclerViewOrders.setAdapter(ordersAdapter);
            ordersAdapter.setOnOrderActionListener(this);

            ordersAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
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

        } else {
            ordersAdapter.updateOptions(options);
        }

        checkEmptyState();
    }

    private void checkEmptyState() {
        progressBar.setVisibility(View.GONE);

        if (textViewNoOrders != null && recyclerViewOrders != null) {
            if (ordersAdapter != null && ordersAdapter.getItemCount() == 0) {
                textViewNoOrders.setText("No orders found.");
                textViewNoOrders.setVisibility(View.VISIBLE);
                recyclerViewOrders.setVisibility(View.GONE);
            } else {
                textViewNoOrders.setVisibility(View.GONE);
                recyclerViewOrders.setVisibility(View.VISIBLE);
            }
        } else {
            Log.e(TAG, "Views are null in checkEmptyState()!");
        }
    }

    @Override
    public void onOrderDetailsClick(Orders order) {
        Toast.makeText(this, "Viewing details for Order: " + order.getOrderId(), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(OrdersListActivity.this, OrderDetailsActivity.class);
        intent.putExtra("order", order);
        startActivity(intent);
    }

    @Override
    public void onUpdateOrderStatusClick(Orders order) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists() && "admin".equals(document.getString("role"))) {
                                Toast.makeText(this, "Updating status for Order: " + order.getOrderId(), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "You do not have permission to update order status.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "Failed to check your role for updating status.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "Please log in to update order status.", Toast.LENGTH_SHORT).show();
        }
    }
}