package com.example.salesmanagement;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot; // Thêm import này
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Locale;

// =====================================================================
// THÊM TRIỂN KHAI CÁC INTERFACE MỚI TỪ ITEMSADAPTER VÀ SHOPPINGCARTMANAGER
// =====================================================================
public class ItemsListActivity extends AppCompatActivity
        implements ShoppingCartManager.OnCartChangeListener,
        ItemsAdapter.OnItemClickListener,           // Cho click ngắn (Sửa)
        ItemsAdapter.OnItemLongClickListener,       // Cho click dài (Xóa)
        ItemsAdapter.OnCartButtonClickListener {    // Cho nút thêm/bớt giỏ hàng (nếu dùng)

    private static final String TAG = "ItemsListActivity";

    private FirebaseFirestore db;
    private CollectionReference itemsCollection;

    private RecyclerView recyclerView;
    private TextView emptyListTextView;
    private ItemsAdapter adapter;
    private MaterialButton btnAdd;

    private SearchView searchView;
    private String currentSearchQuery = "";

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String currentUserRole = "guest"; // Mặc định là guest
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.items_list);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        itemsCollection = db.collection("Items");

        recyclerView = findViewById(R.id.recycler_view_items);
        emptyListTextView = findViewById(R.id.text_view_empty_list);
        btnAdd = findViewById(R.id.btnAdd);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Gọi phương thức để lấy vai trò và sau đó thiết lập RecyclerView
        getCurrentUserRoleAndLoadItems();

        btnAdd.setOnClickListener(v -> {
            // Chỉ admin hoặc user mới được thêm sản phẩm
            if ("admin".equalsIgnoreCase(currentUserRole) || "user".equalsIgnoreCase(currentUserRole)) {
                Toast.makeText(ItemsListActivity.this, "Adding new item...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(ItemsListActivity.this, ItemsAddEditActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(ItemsListActivity.this, "You do not have permission to add products.", Toast.LENGTH_SHORT).show();
            }
        });

        // Đăng ký OnCartChangeListener cho ShoppingCartManager
        ShoppingCartManager.getInstance().setOnCartChangeListener(this);

        Log.d(TAG, "ItemsListActivity created and RecyclerView setup initialized.");
    }

    private void getCurrentUserRoleAndLoadItems() {
        currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            db.collection("users").document(currentUserId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && documentSnapshot.contains("role")) {
                            String role = documentSnapshot.getString("role");
                            if (role != null) {
                                currentUserRole = role.toLowerCase(Locale.getDefault());
                            } else {
                                Log.w(TAG, "User document for " + currentUserId + " has no 'role' field. Assuming 'user' role.");
                                currentUserRole = "user";
                            }
                        } else {
                            Log.w(TAG, "User document not found or role missing for " + currentUserId + ". Defaulting to 'user' role.");
                            currentUserRole = "user";
                        }
                        Log.d(TAG, "Current user role fetched: " + currentUserRole + " (ID: " + currentUserId + ")");
                        adjustUIBasedOnRole();
                        setupRecyclerView(currentSearchQuery); // Gọi setupRecyclerView sau khi có role
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting user role: " + e.getMessage());
                        currentUserRole = "user"; // Mặc định là user nếu có lỗi
                        adjustUIBasedOnRole();
                        setupRecyclerView(currentSearchQuery); // Gọi setupRecyclerView sau khi có role
                        Toast.makeText(this, "Error fetching user role.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.w(TAG, "No user logged in. Defaulting to 'guest' role.");
            currentUserRole = "guest";
            currentUserId = null;
            adjustUIBasedOnRole();
            setupRecyclerView(currentSearchQuery); // Gọi setupRecyclerView sau khi có role
        }
    }

    private void adjustUIBasedOnRole() {
        if ("admin".equals(currentUserRole) || "user".equals(currentUserRole)) {
            btnAdd.setVisibility(View.VISIBLE);
            Log.d(TAG, "FAB Add Item VISIBLE for Admin/User.");
        } else {
            btnAdd.setVisibility(View.GONE);
            Log.d(TAG, "FAB Add Item GONE for Guest/Other roles.");
        }
    }

    private void setupRecyclerView(String queryText) {
        Query query;
        Log.d(TAG, "setupRecyclerView called with role: " + currentUserRole + ", userId: " + currentUserId + ", query: " + queryText);

        if ("admin".equalsIgnoreCase(currentUserRole)) {
            if (queryText == null || queryText.isEmpty()) {
                query = itemsCollection.orderBy("name", Query.Direction.ASCENDING);
            } else {
                query = itemsCollection
                        .orderBy("name")
                        .startAt(queryText.toLowerCase(Locale.getDefault()))
                        .endAt(queryText.toLowerCase(Locale.getDefault()) + "\uf8ff");
            }
            Log.d(TAG, "Querying all items for Admin.");
        } else if ("user".equalsIgnoreCase(currentUserRole) && currentUserId != null) {
            if (queryText == null || queryText.isEmpty()) {
                query = itemsCollection
                        .whereEqualTo("userId", currentUserId)
                        .orderBy("name", Query.Direction.ASCENDING);
            } else {
                query = itemsCollection
                        .whereEqualTo("userId", currentUserId)
                        .orderBy("name")
                        .startAt(queryText.toLowerCase(Locale.getDefault()))
                        .endAt(queryText.toLowerCase(Locale.getDefault()) + "\uf8ff");
            }
            Log.d(TAG, "Querying items for User: " + currentUserId);
        } else {
            // Nếu là guest hoặc không có quyền xem, hiển thị thông báo và không tải dữ liệu
            Toast.makeText(this, "You do not have permission to view products.", Toast.LENGTH_LONG).show();
            emptyListTextView.setText("You do not have permission to view products.");
            emptyListTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            if (adapter != null) {
                adapter.stopListening();
                adapter = null; // Hủy adapter nếu không có quyền
            }
            return;
        }

        FirestoreRecyclerOptions<Items> options = new FirestoreRecyclerOptions.Builder<Items>()
                .setQuery(query, Items.class)
                .build();

        if (adapter != null) {
            // Nếu adapter đã tồn tại, cập nhật options và khởi động lại listening
            adapter.stopListening();
            adapter.updateOptions(options);
            adapter.clearCategoryCache();
            // Cập nhật lại vai trò người dùng trong adapter khi nó đã tồn tại
            // (Bạn có thể thêm một setter cho role trong adapter nếu muốn)
            // Hiện tại, nếu role thay đổi, cần khởi tạo lại adapter.
            // Để đơn giản, tôi sẽ khởi tạo lại nếu role thay đổi đáng kể.
            // Hoặc bạn có thể thêm: adapter.setCurrentUserRole(currentUserRole);
            adapter.startListening();
            Log.d(TAG, "Existing adapter updated and started listening.");
        } else {
            // Khởi tạo adapter lần đầu, truyền vai trò người dùng vào constructor
            adapter = new ItemsAdapter(options, currentUserRole);

            // ĐẶT CÁC LISTENER CHO ADAPTER TẠI ĐÂY
            adapter.setOnItemClickListener(this);           // Cho click ngắn
            adapter.setOnItemLongClickListener(this);       // Cho click dài
            adapter.setOnCartButtonClickListener(this);     // Cho nút giỏ hàng

            recyclerView.setAdapter(adapter);
            adapter.startListening();
            Log.d(TAG, "New adapter created and started listening.");

            // Đăng ký DataObserver sau khi adapter được tạo lần đầu
            adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    checkEmpty();
                }

                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    super.onItemRangeInserted(positionStart, itemCount);
                    checkEmpty();
                }

                @Override
                public void onItemRangeRemoved(int positionStart, int itemCount) {
                    super.onItemRangeRemoved(positionStart, itemCount);
                    checkEmpty();
                }
            });
        }
        checkEmpty(); // Kiểm tra ban đầu sau khi setup/update adapter
    }

    private void checkEmpty() {
        if (emptyListTextView != null) {
            if (adapter != null && adapter.getItemCount() == 0) {
                emptyListTextView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyListTextView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        } else {
            Log.e(TAG, "emptyListTextView is null in checkEmpty()! Check if it's correctly initialized.");
        }
    }

    private void showDeleteConfirmationDialog(final Items itemToDelete) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        // Đảm bảo R.layout.item_delete tồn tại và là layout đúng cho dialog xóa
        View dialogView = inflater.inflate(R.layout.item_delete, null);
        builder.setView(dialogView);

        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
        ImageView dialogItemImage = dialogView.findViewById(R.id.dialog_item_image);
        TextView dialogItemName = dialogView.findViewById(R.id.dialog_item_name);
        TextView dialogItemPrice = dialogView.findViewById(R.id.dialog_item_price);
        TextView dialogItemQuantity = dialogView.findViewById(R.id.dialog_item_quantity);
        TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);
        Button buttonCancel = dialogView.findViewById(R.id.button_cancel);
        Button buttonDelete = dialogView.findViewById(R.id.button_delete);

        dialogTitle.setText("Confirm product deletion");
        dialogItemName.setText(itemToDelete.getName());
        dialogItemPrice.setText(String.format(Locale.getDefault(), "Price: %.2f ", itemToDelete.getPrice()));
        dialogItemQuantity.setText(String.format(Locale.getDefault(), "Quantity: %d", itemToDelete.getQuantity()));
        dialogMessage.setText("Are you sure you want to remove this product from your list?");

        String base64Image = itemToDelete.getImageUrl();
        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                dialogItemImage.setImageBitmap(decodedByte);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Base64 string is invalid for dialog item " + itemToDelete.getId() + ": " + e.getMessage());
                dialogItemImage.setImageResource(R.drawable.ic_error);
            }
        } else {
            dialogItemImage.setImageResource(R.drawable.ic_photo);
        }

        final AlertDialog dialog = builder.create();

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        buttonDelete.setOnClickListener(v -> {
            if (itemToDelete.getId() != null) {
                itemsCollection.document(itemToDelete.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(ItemsListActivity.this, "Item deleted: " + itemToDelete.getName(), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Item deleted: " + itemToDelete.getId());
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(ItemsListActivity.this, "Error deleting item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error deleting item: " + itemToDelete.getId(), e);
                            dialog.dismiss();
                        });
            }
        });

        dialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Luôn gọi getCurrentUserRoleAndLoadItems() ở onStart để đảm bảo cập nhật trạng thái
        // mỗi khi activity trở lại foreground (ví dụ: sau khi thêm/sửa item hoặc thay đổi vai trò)
        getCurrentUserRoleAndLoadItems();

        // Adapter sẽ được khởi tạo/startListening trong getCurrentUserRoleAndLoadItems -> setupRecyclerView
        // if (adapter != null) { adapter.startListening(); } // Không cần gọi lại ở đây
        // Đăng ký OnCartChangeListener ở đây để đảm bảo nó luôn được đăng ký khi activity bắt đầu
        ShoppingCartManager.getInstance().setOnCartChangeListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
            Log.d(TAG, "Adapter stopped listening.");
        }
        // Hủy đăng ký OnCartChangeListener khi activity dừng để tránh rò rỉ bộ nhớ
        ShoppingCartManager.getInstance().removeOnCartChangeListener();
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        //MenuItem searchItem = menu.findItem(R.id.action_search); // Đảm bảo ID này tồn tại trong menu.xml
        if (searchItem != null) {
            searchView = (SearchView) searchItem.getActionView();
        }

        if (searchView != null) {
            searchView.setQueryHint("Search for products...");
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    currentSearchQuery = query;
                    performSearch(currentSearchQuery);
                    searchView.clearFocus();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    currentSearchQuery = newText;
                    performSearch(currentSearchQuery);
                    return true;
                }
            });

            // Bỏ comment nếu muốn xử lý hành động khi Search View mở/đóng
            searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    currentSearchQuery = "";
                    performSearch(""); // Load lại toàn bộ danh sách khi đóng search
                    return true;
                }
            });
        }
        return true;
    }*/

    private void performSearch(String queryText) {
        setupRecyclerView(queryText);
        Log.d(TAG, "Search query applied: " + queryText);
    }

    /*@Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        // Đảm bảo R.id.action_refresh tồn tại trong menu.xml nếu bạn muốn dùng
        if (id == R.id.action_refresh) {
            currentSearchQuery = "";
            if (searchView != null) {
                searchView.setQuery("", false);
                searchView.setIconified(true);
            }
            setupRecyclerView("");
            Toast.makeText(this, "Refreshing list...", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_settings) {
            Toast.makeText(this, "Opening settings...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }*/

    @Override
    public void onCartChanged() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        Log.d(TAG, "Cart changed. Total items: " + ShoppingCartManager.getInstance().getCartItemCount());
    }

    // =====================================================================
    // TRIỂN KHAI CÁC PHƯƠNG THỨC TỪ INTERFACE CỦA ITEMSADAPTER
    // =====================================================================

    @Override
    public void onItemClick(DocumentSnapshot documentSnapshot, int position) {
        // Xử lý sự kiện click ngắn (cho phép SỬA)
        // Chỉ cho phép admin hoặc user sở hữu sản phẩm sửa
        if ("admin".equalsIgnoreCase(currentUserRole) ||
                (documentSnapshot.exists() && documentSnapshot.getString("userId") != null &&
                        documentSnapshot.getString("userId").equals(currentUserId))) {

            Items item = documentSnapshot.toObject(Items.class);
            if (item != null) {
                Intent intent = new Intent(ItemsListActivity.this, ItemsAddEditActivity.class);
                intent.putExtra("item_id", documentSnapshot.getId()); // Truyền ID của item
                startActivity(intent);
                Toast.makeText(this, "Editing: " + item.getName(), Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Opening for edit: " + item.getId());
            }
        } else {
            Toast.makeText(this, "You do not have permission to edit this product.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Permission denied for edit. Role: " + currentUserRole + ", Item userId: " + (documentSnapshot.exists() ? documentSnapshot.getString("userId") : "N/A"));
        }
    }

    @Override
    public void onItemLongClick(DocumentSnapshot documentSnapshot, int position) {
        // Xử lý sự kiện click dài (cho phép XÓA)
        // Chỉ cho phép admin hoặc user sở hữu sản phẩm xóa
        if ("admin".equalsIgnoreCase(currentUserRole) ||
                (documentSnapshot.exists() && documentSnapshot.getString("userId") != null &&
                        documentSnapshot.getString("userId").equals(currentUserId))) {

            Items item = documentSnapshot.toObject(Items.class);
            if (item != null) {
                showDeleteConfirmationDialog(item);
                Toast.makeText(this, "Confirming deletion for: " + item.getName(), Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Long click for delete: " + item.getId());
            }
        } else {
            Toast.makeText(this, "You do not have permission to delete this product.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Permission denied for delete. Role: " + currentUserRole + ", Item userId: " + (documentSnapshot.exists() ? documentSnapshot.getString("userId") : "N/A"));
        }
    }

    /*/ Triển khai phương thức onAddItemClick từ ItemsAdapter.OnCartButtonClickListener
    @Override
    public void onAddItemClick(Items item) {
        if (item.getQuantity() > 0) {
            ShoppingCartManager.getInstance().addItemToCart(item);
            Toast.makeText(this, item.getName() + " added to cart!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, item.getName() + " is out of stock!", Toast.LENGTH_SHORT).show();
        }
    }

    // Triển khai phương thức onRemoveItemClick từ ItemsAdapter.OnCartButtonClickListener
    @Override
    public void onRemoveItemClick(Items item) {
        ShoppingCartManager.getInstance().removeItemFromCart(item);
        Toast.makeText(this, item.getName() + " removed from cart!", Toast.LENGTH_SHORT).show();
    }*/
}