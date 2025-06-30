package com.example.salesmanagement;

import android.util.Log; // THÊM DÒNG NÀY
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast; // THÊM DÒNG NÀY

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;

// Thay đổi từ RecyclerView.Adapter thành FirestoreRecyclerAdapter
public class UsersListAdapter extends FirestoreRecyclerAdapter<Users, UsersListAdapter.UserViewHolder> {

    private static final String TAG = "UsersListAdapter"; // THÊM DÒNG NÀY
    private OnUserActionListener listener;
    private String currentAdminUid; // THÊM DÒNG NÀY để lưu UID của admin hiện tại

    // CẬP NHẬT CONSTRUCTOR để nhận thêm UID của admin
    public UsersListAdapter(@NonNull FirestoreRecyclerOptions<Users> options, String currentAdminUid) {
        super(options);
        this.currentAdminUid = currentAdminUid;
    }

    public interface OnUserActionListener {
        void onViewProfileClick(Users user);
        void onEditUserClick(Users user); // Giữ nguyên nếu bạn có nút Edit riêng
        void onDeleteUserClick(Users user); // Giữ nguyên nếu bạn có nút xóa
        void onManageUserClick(Users user); // THÊM PHƯƠNG THỨC MỚI
    }

    public void setOnUserActionListener(OnUserActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_list_item_row, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull UserViewHolder holder, int position, @NonNull Users user) {
        Log.d(TAG, "Binding user: " + user.getName() + ", Email: " + user.getEmail() + ", Role: " + user.getRole());

        holder.textViewUserName.setText("User Name: " + (user.getName() != null && !user.getName().isEmpty() ? user.getName() : "N/A"));
        holder.textViewUserEmail.setText("Email: " + (user.getEmail() != null && !user.getEmail().isEmpty() ? user.getEmail() : "N/A"));
        holder.textViewUserRole.setText("Role: " + (user.getRole() != null && !user.getRole().isEmpty() ? user.getRole() : "N/A"));

        // Lấy DocumentSnapshot để lấy Document ID và gán vào userId của đối tượng Users
        DocumentSnapshot snapshot = getSnapshots().getSnapshot(holder.getAdapterPosition());
        String documentId = snapshot.getId();
        user.setUserId(documentId); // Đảm bảo userId trong đối tượng user được gán là Document ID

        // Xử lý click cho nút "Manage"
        if (holder.btnManageUser != null) {
            // Ẩn nút Manage cho chính người dùng hiện tại (admin)
            if (user.getUserId() != null && user.getUserId().equals(currentAdminUid)) {
                holder.btnManageUser.setVisibility(View.GONE);
            } else {
                holder.btnManageUser.setVisibility(View.VISIBLE);
                holder.btnManageUser.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onManageUserClick(user); // Gửi đối tượng user về Activity
                    }
                });
            }
        }

        // Xử lý click cho toàn bộ item View (tùy chọn, có thể mở profile)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                // Sử dụng đối tượng 'user' đã được cập nhật userId
                listener.onViewProfileClick(user);
            }
        });

        // Xử lý click cho nút "View Profile"
        if (holder.btnViewProfile != null) {
            holder.btnViewProfile.setOnClickListener(v -> {
                if (listener != null) {
                    // Sử dụng đối tượng 'user' đã được cập nhật userId
                    listener.onViewProfileClick(user);
                }
            });
        }

        // Nếu bạn vẫn có nút "Edit User" riêng biệt và muốn nó làm gì đó khác với "Manage", hãy giữ lại.
        // Hiện tại, code của bạn đang ánh xạ btnManageUser vào onEditUserClick,
        // điều này có thể gây nhầm lẫn. Tôi đã thay đổi để btnManageUser gọi onManageUserClick.
        // Bạn có thể xóa phần này nếu nút Edit không cần thiết hoặc bạn đã gán lại ID.
        // if (holder.btnEditUser != null) { // Kiểm tra null
        //     holder.btnEditUser.setOnClickListener(v -> {
        //         if (listener != null) {
        //             listener.onEditUserClick(user); // Gọi phương thức riêng cho Edit
        //         }
        //     });
        // }


        // Uncomment nếu bạn thêm nút xóa và đã ánh xạ trong UserViewHolder
        // if (holder.btnDeleteUser != null) {
        //     holder.btnDeleteUser.setOnClickListener(v -> {
        //         if (listener != null) {
        //             listener.onDeleteUserClick(user);
        //         }
        //     });
        // }
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();
        Log.d(TAG, "Data changed. Item count: " + getItemCount());
        if (listener instanceof UsersListActivity) {
            ((UsersListActivity) listener).checkEmptyState();
        }
    }

    @Override
    public void onError(@NonNull com.google.firebase.firestore.FirebaseFirestoreException e) {
        super.onError(e);
        Log.e(TAG, "Error fetching data: ", e);
        if (listener instanceof UsersListActivity) {
            // Hiển thị Toast hoặc thông báo lỗi cho người dùng
            Toast.makeText(((UsersListActivity) listener).getApplicationContext(), "Error loading users: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textViewUserName, textViewUserEmail, textViewUserRole;
        MaterialButton btnViewProfile, btnEditUser, btnDeleteUser, btnManageUser; // Đảm bảo btnEditUser vẫn còn nếu có trong XML

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewUserName = itemView.findViewById(R.id.text_view_user_name);
            textViewUserEmail = itemView.findViewById(R.id.text_view_user_email);
            textViewUserRole = itemView.findViewById(R.id.text_view_user_role);
            btnViewProfile = itemView.findViewById(R.id.btn_view_user_profile);
            btnManageUser = itemView.findViewById(R.id.btn_manage_user); // Ánh xạ nút Manage
            //btnDeleteUser = itemView.findViewById(R.id.btn_delete_user); // Ánh xạ nếu có nút xóa trong users_list_item_row.xml
        }
    }
}