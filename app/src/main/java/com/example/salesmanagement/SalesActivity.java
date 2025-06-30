package com.example.salesmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreException.Code;


public class SalesActivity extends AppCompatActivity implements SalesAdapter.OnSaleActionListener {

    private static final String TAG = "SalesActivity";

    private RecyclerView recyclerViewSales;
    private SalesAdapter salesAdapter;
    private ProgressBar progressBar;
    private TextView textViewNoSalesProducts;
    private EditText editTextSearch;
    private FloatingActionButton fabCart;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private CollectionReference productsCollection;
    private CollectionReference cartCollection; // Vẫn giữ để AddToCart

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sales);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        productsCollection = db.collection("Items");
        cartCollection = db.collection("carts"); // Vẫn dùng cho logic thêm vào giỏ hàng

        Toolbar toolbar = findViewById(R.id.toolbar_sales);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Available Products");
        }

        recyclerViewSales = findViewById(R.id.recycler_view_sales_products);
        progressBar = findViewById(R.id.progress_bar_sales);
        textViewNoSalesProducts = findViewById(R.id.text_view_no_sales_products);
        editTextSearch = findViewById(R.id.edit_text_search_sales);
        fabCart = findViewById(R.id.fab_cart);

        fabCart.setOnClickListener(v -> {
            Intent intent = new Intent(SalesActivity.this, CartsActivity.class);
            startActivity(intent);
        });

        recyclerViewSales.setLayoutManager(new LinearLayoutManager(this));

        setupRecyclerViewAndStartListening("");

        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchText = s.toString().trim();
                setupRecyclerViewAndStartListening(searchText);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRecyclerViewAndStartListening(String searchQuery) {
        progressBar.setVisibility(View.VISIBLE);

        Query query;
        if (searchQuery.isEmpty()) {
            Log.d(TAG, "Search query is empty. Loading all items ordered by name.");
            query = productsCollection.orderBy("name", Query.Direction.ASCENDING);
        } else {
            Log.d(TAG, "Search query: '" + searchQuery + "', searching for items starting with this text.");
            query = productsCollection
                    .whereGreaterThanOrEqualTo("name", searchQuery)
                    .whereLessThanOrEqualTo("name", searchQuery + '\uf8ff')
                    .orderBy("name", Query.Direction.ASCENDING);
        }

        FirestoreRecyclerOptions<Items> options = new FirestoreRecyclerOptions.Builder<Items>()
                .setQuery(query, Items.class)
                .build();

        if (salesAdapter == null) {
            salesAdapter = new SalesAdapter(options);
            recyclerViewSales.setAdapter(salesAdapter);
            salesAdapter.setOnSaleActionListener(this);
            salesAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
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

                @Override
                public void onChanged() {
                    super.onChanged();
                    checkEmptyState();
                }
            });
            salesAdapter.startListening();
            Log.d(TAG, "New SalesAdapter created and started listening.");
        } else {
            salesAdapter.stopListening();
            salesAdapter.updateOptions(options);
            salesAdapter.startListening();
            Log.d(TAG, "Existing SalesAdapter updated options and restarted listening.");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (salesAdapter != null) {
            salesAdapter.startListening();
            Log.d(TAG, "Sales adapter started listening from onStart.");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (salesAdapter != null) {
            salesAdapter.stopListening();
            Log.d(TAG, "Sales adapter stopped listening from onStop.");
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

    public void checkEmptyState() {
        progressBar.setVisibility(View.GONE);
        if (salesAdapter != null && salesAdapter.getItemCount() == 0) {
            textViewNoSalesProducts.setVisibility(View.VISIBLE);
            recyclerViewSales.setVisibility(View.GONE);
            Log.d(TAG, "No sales products found. Showing empty state message.");
        } else {
            textViewNoSalesProducts.setVisibility(View.GONE);
            recyclerViewSales.setVisibility(View.VISIBLE);
            Log.d(TAG, "Sales products found. Hiding empty state message. Item count: " + (salesAdapter != null ? salesAdapter.getItemCount() : "null adapter"));
        }
    }

    @Override
    public void onSaleClick(Items item) {
        Toast.makeText(this, "View Item Details: " + item.getName(), Toast.LENGTH_SHORT).show();
        // Intent intent = new Intent(SalesActivity.this, ItemDetailActivity.class);
        // intent.putExtra("itemId", item.getId());
        // startActivity(intent);
    }

    @Override
    public void onAddToCartClick(Items item) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in to add items to cart.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(SalesActivity.this, LoginActivity.class));
            return;
        }

        String userId = user.getUid();
        String itemId = item.getId();
        if (itemId == null || itemId.isEmpty()) {
            Log.e(TAG, "Item ID is null or empty. Cannot add to cart.");
            Toast.makeText(SalesActivity.this, "Cannot add to cart: Item ID is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        int quantityToAdd = 1;

        DocumentReference cartItemRef = cartCollection.document(userId).collection("cartItems").document(itemId);
        DocumentReference productRef = productsCollection.document(itemId);

        db.runTransaction(transaction -> {
            DocumentSnapshot cartSnapshot = transaction.get(cartItemRef);
            DocumentSnapshot productSnapshot = transaction.get(productRef);

            if (!productSnapshot.exists()) {
                throw new FirebaseFirestoreException("Product does not exist!", Code.NOT_FOUND);
            }

            Items product = productSnapshot.toObject(Items.class);
            if (product == null) {
                throw new FirebaseFirestoreException("Failed to parse product data.", Code.DATA_LOSS);
            }

            int currentProductStock = product.getQuantity();
            int currentCartQuantity = 0;
            if (cartSnapshot.exists()) {
                Long quantityInCart = cartSnapshot.getLong("quantity");
                currentCartQuantity = (quantityInCart != null) ? quantityInCart.intValue() : 0;
            }

            if (currentCartQuantity + quantityToAdd > currentProductStock) {
                throw new FirebaseFirestoreException(
                        "Not enough stock available for " + item.getName() + ".\nAvailable: " + currentProductStock + ", In cart: " + currentCartQuantity,
                        Code.ABORTED
                );
            }

            int newQuantityInCart = currentCartQuantity + quantityToAdd;
            OrderItem newOrderItem = new OrderItem(
                    item.getId(),
                    item.getName(),
                    item.getPrice(),
                    item.getImageUrl(),
                    newQuantityInCart,
                    System.currentTimeMillis()
            );
            transaction.set(cartItemRef, newOrderItem);

            // Uncomment to decrement product stock in 'Items' collection
            // transaction.update(productRef, "quantity", currentProductStock - quantityToAdd);

            return null; // Transaction success
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(SalesActivity.this, "Added to Cart: " + item.getName(), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Item " + item.getName() + " added/updated in cart for user " + userId);
        }).addOnFailureListener(e -> {
            String errorMessage = e.getMessage();
            if (e instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) e;
                Log.e(TAG, "Firestore error adding to cart (" + firestoreException.getCode() + "): ", firestoreException);
                errorMessage = firestoreException.getMessage();
            } else {
                Log.e(TAG, "Error adding to cart: ", e);
                if (errorMessage == null) errorMessage = "Unknown error";
            }
            Toast.makeText(SalesActivity.this, "Failed to add to cart: " + errorMessage, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onBuyNowClick(Items item) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in to make a purchase.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(SalesActivity.this, LoginActivity.class));
            return;
        }

        // Kiểm tra xem mặt hàng có đủ số lượng không (chỉ 1 cho Buy Now)
        if (item.getQuantity() < 1) {
            Toast.makeText(this, item.getName() + " is out of stock.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo một list tạm thời chỉ chứa sản phẩm này
        // Chúng ta sẽ truyền dữ liệu này qua Intent đến CheckoutActivity
        // CheckoutActivity sẽ nhận một ArrayList<CartItem> từ Intent thay vì lấy từ ShoppingCartManager
        // cho trường hợp Buy Now.
        OrderItem buyNowItem = new OrderItem(
                item.getId(),
                item.getName(),
                item.getPrice(),
                item.getImageUrl(),
                1, // Luôn mua 1 sản phẩm khi bấm Buy Now
                System.currentTimeMillis()
        );

        // Chuyển sang CheckoutActivity
        Intent intent = new Intent(SalesActivity.this, CheckoutActivity.class);
        // Đánh dấu đây là chế độ "Buy Now"
        intent.putExtra("isBuyNow", true);
        // Truyền thông tin của sản phẩm duy nhất này.
        // Bạn cần đảm bảo `CartItem` là `Serializable` hoặc `Parcelable`.
        // Nếu `CartItem` là một POJO đơn giản, `Serializable` là đủ.
        intent.putExtra("buyNowItem", buyNowItem);

        startActivity(intent);
        Toast.makeText(SalesActivity.this, "Proceeding to checkout with: " + item.getName(), Toast.LENGTH_SHORT).show();
    }
}