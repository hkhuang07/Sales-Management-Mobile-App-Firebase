package com.example.salesmanagement;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class CategoriesListActivity extends AppCompatActivity {

    private static final String TAG = "CategoryListActivity";

    private FirebaseFirestore db;
    private CollectionReference categoriesCollection;

    private RecyclerView recyclerView;
    private CategoriesAdapter adapter;
    private TextView emptyListTextView; // Đảm bảo khai báo ở đây

    private MaterialButton btnAddCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.categories_list);

        db = FirebaseFirestore.getInstance();
        categoriesCollection = db.collection("Categories");

        recyclerView = findViewById(R.id.recycler_view_categories);
        emptyListTextView = findViewById(R.id.text_view_empty_categories); // *** ĐÃ BỎ COMMENT DÒNG NÀY ***

        btnAddCategory = findViewById(R.id.btnAddCategory);

        Toolbar toolbar = findViewById(R.id.toolbarcategory);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Categories List");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setupRecyclerView();

        btnAddCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(CategoriesListActivity.this, "Adding new category...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(CategoriesListActivity.this, CategoriesAddEditActivity.class);
                startActivity(intent);
            }
        });

        Log.d(TAG, "CategoryListActivity created and RecyclerView setup.");
    }

    private void setupRecyclerView() {
        Query query = categoriesCollection.orderBy("name", Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<Categories> options = new FirestoreRecyclerOptions.Builder<Categories>()
                .setQuery(query, Categories.class)
                .build();

        adapter = new CategoriesAdapter(options);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(new CategoriesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(DocumentSnapshot documentSnapshot, int position) {
                Categories category = documentSnapshot.toObject(Categories.class);
                if (category != null) {
                    category.setId(documentSnapshot.getId());
                    Log.d(TAG, "Category clicked: " + category.getName() + " (ID: " + category.getId() + ")");
                    Toast.makeText(CategoriesListActivity.this, "Editing: " + category.getName(), Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(CategoriesListActivity.this, CategoriesAddEditActivity.class);
                    intent.putExtra("category_id", category.getId());
                    startActivity(intent);
                }
            }

            @Override
            public void onItemLongClick(DocumentSnapshot documentSnapshot, int position) {
                Categories category = documentSnapshot.toObject(Categories.class);
                if (category != null) {
                    category.setId(documentSnapshot.getId());
                    Log.d(TAG, "Category long clicked for deletion: " + category.getName() + " (ID: " + category.getId() + ")");
                    showDeleteConfirmationDialog(category);
                }
            }
        });

        // Đảm bảo adapterDataObserver được đăng ký đúng cách
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                checkEmpty(); // Gọi hàm kiểm tra khi dữ liệu thay đổi
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                checkEmpty(); // Gọi hàm kiểm tra khi có item mới
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                checkEmpty(); // Gọi hàm kiểm tra khi item bị xóa
            }
        });

        // Gọi checkEmpty lần đầu khi setup RecyclerView để xử lý trường hợp không có dữ liệu ngay từ đầu
        checkEmpty();
    }

    // Phương thức checkEmpty() đã được chuyển ra ngoài AdapterDataObserver
    private void checkEmpty() {
        if (adapter.getItemCount() == 0) {
            emptyListTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyListTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }


    private void showDeleteConfirmationDialog(final Categories categoryToDelete) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        // Giả định activity_delete_category.xml đã được tạo và cấu hình đúng
        View dialogView = inflater.inflate(R.layout.category_delete, null);
        builder.setView(dialogView);

        TextView dialogTitle = dialogView.findViewById(R.id.dialog_catetitle);
        TextView dialogCategoryName = dialogView.findViewById(R.id.dialog_category_name);
        TextView dialogCategoryDescription = dialogView.findViewById(R.id.dialog_category_description);
        TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);
        Button buttonCancel = dialogView.findViewById(R.id.button_cancel);
        Button buttonDelete = dialogView.findViewById(R.id.button_delete);

        /*dialogTitle.setText("Xác nhận xóa danh mục");
        dialogCategoryName.setText("Tên: " + categoryToDelete.getName());
        dialogCategoryDescription.setText("Mô tả: " + categoryToDelete.getDescription());
        dialogMessage.setText("Bạn có chắc chắn muốn xóa danh mục này khỏi danh sách không? Hành động này không thể hoàn tác.");
        */

        final AlertDialog dialog = builder.create();

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        buttonDelete.setOnClickListener(v -> {
            if (categoryToDelete.getId() != null) {
                categoriesCollection.document(categoryToDelete.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(CategoriesListActivity.this, "Category deleted: " + categoryToDelete.getName(), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Category deleted: " + categoryToDelete.getId());
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(CategoriesListActivity.this, "Error deleting category: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error deleting category: " + categoryToDelete.getId(), e);
                            dialog.dismiss();
                        });
            }
        });

        dialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
            Log.d(TAG, "Adapter started listening.");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
            Log.d(TAG, "Adapter stopped listening.");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        /*if (id == R.id.action_refresh) {
            Toast.makeText(this, "Refreshing list...", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_search) {
            Toast.makeText(this, "Category search ...", Toast.LENGTH_SHORT).show();
            return true;
        } else*/
        if (id == R.id.action_settings) {
            Toast.makeText(this, "Opening settings...", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}