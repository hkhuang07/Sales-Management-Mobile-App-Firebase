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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser; // THÊM DÒNG NÀY
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class UsersListActivity extends AppCompatActivity implements UsersListAdapter.OnUserActionListener {

    private static final String TAG = "UsersList";

    private RecyclerView recyclerViewUsers;
    private UsersListAdapter usersAdapter;
    private ProgressBar progressBar;
    private TextView textViewNoUsers; // Để hiển thị "No users found"

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private CollectionReference usersCollection;

    private String currentAdminUid = ""; // THÊM DÒNG NÀY để lưu UID của người dùng hiện tại (admin)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.users_list);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        usersCollection = db.collection("users");

        Toolbar toolbar = findViewById(R.id.toolbar_users_list);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("User Management");
        }

        recyclerViewUsers = findViewById(R.id.recycler_view_users);
        progressBar = findViewById(R.id.progress_bar_users);
        textViewNoUsers = findViewById(R.id.text_view_no_users);

        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));

        // Lấy UID của người dùng hiện tại ngay khi khởi tạo
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentAdminUid = currentUser.getUid();
        }

        // Gọi trực tiếp setupRecyclerViewAndStartListening()
        setupRecyclerViewAndStartListening();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (usersAdapter != null) {
            usersAdapter.startListening();
            Log.d(TAG, "UsersList adapter started listening.");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (usersAdapter != null) {
            usersAdapter.stopListening();
            Log.d(TAG, "UsersList adapter stopped listening.");
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

    /**
     * Thiết lập RecyclerView với FirestoreRecyclerAdapter để tải TẤT CẢ người dùng.
     * Phương thức này sẽ luôn được gọi khi Activity được tạo.
     */
    private void setupRecyclerViewAndStartListening() {
        progressBar.setVisibility(View.VISIBLE); // Hiển thị ProgressBar khi bắt đầu tải

        // Truy vấn Firestore để lấy TẤT CẢ người dùng
        // Sắp xếp theo tên để có thứ tự hợp lý
        Query query = usersCollection
                .orderBy("name", Query.Direction.ASCENDING); // Hoặc "email", "role"

        // Xây dựng FirestoreRecyclerOptions
        FirestoreRecyclerOptions<Users> options = new FirestoreRecyclerOptions.Builder<Users>()
                .setQuery(query, Users.class)
                .build();

        if (usersAdapter == null) {
            // TRUYỀN currentAdminUid VÀO CONSTRUCTOR CỦA ADAPTER
            usersAdapter = new UsersListAdapter(options, currentAdminUid);
            recyclerViewUsers.setAdapter(usersAdapter);

            usersAdapter.setOnUserActionListener(this);

            // Đăng ký AdapterDataObserver để kiểm tra trạng thái rỗng
            usersAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
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
            usersAdapter.updateOptions(options);
        }
    }

    // Phương thức này sẽ được gọi từ UsersListAdapter khi dữ liệu thay đổi
    // Để hiển thị thông báo "No users found" nếu không có người dùng nào.
    public void checkEmptyState() {
        progressBar.setVisibility(View.GONE); // Ẩn ProgressBar khi dữ liệu đã được tải

        if (textViewNoUsers != null && recyclerViewUsers != null) {
            if (usersAdapter != null && usersAdapter.getItemCount() == 0) {
                textViewNoUsers.setText("No users found.");
                textViewNoUsers.setVisibility(View.VISIBLE);
                recyclerViewUsers.setVisibility(View.GONE);
            } else {
                textViewNoUsers.setVisibility(View.GONE);
                recyclerViewUsers.setVisibility(View.VISIBLE);
            }
        } else {
            Log.e(TAG, "Views are null in checkEmptyState()!");
        }
    }

    // Các phương thức interface từ UsersListAdapter.OnUserActionListener
    @Override
    public void onViewProfileClick(Users user) {
        Toast.makeText(this, "Viewing profile for: " + user.getName(), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(UsersListActivity.this, UserProfileActivity.class);
        intent.putExtra("userId", user.getUserId());
        startActivity(intent);
    }

    @Override
    public void onEditUserClick(Users user) {
        // Tên phương thức này sẽ được đổi thành onManageUserClick trong adapter
        // nên logic này sẽ không bao giờ được gọi từ nút Manage nữa
        Toast.makeText(this, "Editing user (if this was still used): " + user.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteUserClick(Users user) {
        Toast.makeText(this, "Deleting user: " + user.getName(), Toast.LENGTH_SHORT).show();
        // Hiển thị dialog xác nhận trước khi xóa
    }

    // THÊM PHƯƠNG THỨC MỚI CHO NÚT MANAGE
    @Override
    public void onManageUserClick(Users user) {
        // Chuyển sang UserManagementActivity khi nút Manage được click
        Toast.makeText(this, "Managing user: " + user.getName(), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(UsersListActivity.this, UserManagerActivity.class);
        intent.putExtra("userId", user.getUserId()); // Truyền UID của người dùng cần quản lý
        intent.putExtra("userName", user.getName()); // Truyền tên để hiển thị
        intent.putExtra("userEmail", user.getEmail()); // Truyền email
        intent.putExtra("userRole", user.getRole()); // Truyền vai trò hiện tại
        // Đảm bảo bạn đã thêm trường 'isActive' vào Users model và nó có getter
        intent.putExtra("userAccountStatus", user.getIsActive()); // Truyền trạng thái tài khoản
        startActivity(intent);
    }
}