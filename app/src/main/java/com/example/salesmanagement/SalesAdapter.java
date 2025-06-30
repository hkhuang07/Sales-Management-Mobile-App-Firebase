package com.example.salesmanagement;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore; // Import FirebaseFirestore

import java.util.Locale; // Import Locale
import java.util.Map; // Import Map
import java.util.concurrent.ConcurrentHashMap; // Import ConcurrentHashMap

public class SalesAdapter extends FirestoreRecyclerAdapter<Items, SalesAdapter.SaleViewHolder> {

    private static final String TAG = "SalesAdapter";
    private OnSaleActionListener listener;
    private FirebaseFirestore db; // Biến Firestore để tải Category
    private Map<String, String> categoryNameCache; // Cache để lưu tên danh mục

    public SalesAdapter(@NonNull FirestoreRecyclerOptions<Items> options) {
        super(options);
        db = FirebaseFirestore.getInstance(); // Khởi tạo Firestore
        categoryNameCache = new ConcurrentHashMap<>(); // Khởi tạo cache
        Log.d(TAG, "SalesAdapter initialized.");
    }

    public interface OnSaleActionListener {
        void onSaleClick(Items item);
        void onAddToCartClick(Items item);
        void onBuyNowClick(Items item);
    }

    public void setOnSaleActionListener(OnSaleActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public SaleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sales_row, parent, false);
        return new SaleViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull SaleViewHolder holder, int position, @NonNull Items item) {
        // Gán Document ID vào đối tượng Items.
        DocumentSnapshot snapshot = getSnapshots().getSnapshot(holder.getAdapterPosition());
        item.setId(snapshot.getId()); // RẤT QUAN TRỌNG: GÁN ID TỪ FIRESTORE SNAPSHOT

        Log.d(TAG, "onBindViewHolder: Binding item at position " + position +
                " with name: " + item.getName() + " and ID: " + item.getId());

        holder.textViewSaleName.setText(item.getName());
        // Sử dụng Locale.getDefault() để định dạng số tiền
        holder.textViewSalePrice.setText(String.format(Locale.getDefault(), "$%.2f", item.getPrice()));
        holder.textViewSaleDescription.setText(item.getDescription());
        holder.textViewSaleStock.setText(String.format(Locale.getDefault(), "In Stock: %d", item.getQuantity()));

        // Kiểm tra cache trước
        if (item.getCategoryId() != null && !item.getCategoryId().isEmpty()) {
            if (categoryNameCache.containsKey(item.getCategoryId())) {
                holder.textViewSaleCategory.setText("Category: " + categoryNameCache.get(item.getCategoryId()));
            } else {
                // Tải tên danh mục từ Firestore
                db.collection("Categories").document(item.getCategoryId())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String categoryName = documentSnapshot.getString("name");
                                if (categoryName != null) {
                                    holder.textViewSaleCategory.setText("Category: " + categoryName);
                                    categoryNameCache.put(item.getCategoryId(), categoryName); // Thêm vào cache
                                } else {
                                    holder.textViewSaleCategory.setText("Category: N/A");
                                }
                            } else {
                                holder.textViewSaleCategory.setText("Category: Not available");
                                categoryNameCache.put(item.getCategoryId(), "Does not exist"); // Cache cả trường hợp không tìm thấy
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error fetching category for item " + item.getId() + ": " + e.getMessage());
                            holder.textViewSaleCategory.setText("Category: Download Error");
                            categoryNameCache.put(item.getCategoryId(), "Loading error"); // Cache cả trường hợp lỗi
                        });
            }
        } else {
            holder.textViewSaleCategory.setText("Category: Unknown");
        }

        // --- Xử lý tải ảnh sản phẩm từ chuỗi Base64 (tương tự ItemsAdapter) ---
        String base64Image = item.getImageUrl();
        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                // Giải mã Base64 string thành byte array
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                // Tạo Bitmap từ byte array
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.imageViewSaleImage.setImageBitmap(decodedByte);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Base64 string is invalid for item " + item.getId() + ": " + e.getMessage());
                holder.imageViewSaleImage.setImageResource(R.drawable.ic_error); // Ảnh fallback khi lỗi
            }
        } else {
            holder.imageViewSaleImage.setImageResource(R.drawable.ic_photo); // Ảnh mặc định khi không có URL
        }

        // --- Cập nhật trạng thái nút "Add to Cart" và "Buy Now" dựa trên Quantity ---
        // Vô hiệu hóa nút nếu hết hàng
        if (item.getQuantity() <= 0) {
            holder.buttonAddToCart.setEnabled(false);
            holder.buttonAddToCart.setAlpha(0.5f); // Làm mờ nút
            holder.buttonBuyNow.setEnabled(false);
            holder.buttonBuyNow.setAlpha(0.5f);
        } else {
            holder.buttonAddToCart.setEnabled(true);
            holder.buttonAddToCart.setAlpha(1.0f); // Hiện rõ nút
            holder.buttonBuyNow.setEnabled(true);
            holder.buttonBuyNow.setAlpha(1.0f);
        }


        // --- Xử lý click listeners ---
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSaleClick(item);
            }
        });

        holder.buttonAddToCart.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddToCartClick(item);
            }
        });

        holder.buttonBuyNow.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBuyNowClick(item);
            }
        });
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();
        Log.d(TAG, "Sale data changed. Item count: " + getItemCount());
        if (getItemCount() == 0) {
            Log.d(TAG, "Adapter has no items after data change.");
        }
        if (listener instanceof SalesActivity) {
            ((SalesActivity) listener).checkEmptyState();
        }
    }

    @Override
    public void onError(@NonNull com.google.firebase.firestore.FirebaseFirestoreException e) {
        super.onError(e);
        Log.e(TAG, "Firestore error in SalesAdapter: ", e);
        if (listener instanceof SalesActivity) {
            Toast.makeText(((SalesActivity) listener).getApplicationContext(), "Error loading products: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Phương thức để xóa cache khi cần, ví dụ khi có cập nhật category name
    public void clearCategoryCache() {
        categoryNameCache.clear();
        Log.d(TAG, "Category name cache cleared.");
    }

    public static class SaleViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewSaleImage;
        TextView textViewSaleName, textViewSalePrice, textViewSaleDescription, textViewSaleStock, textViewSaleCategory; // Thêm textViewSaleCategory
        Button buttonAddToCart, buttonBuyNow;

        public SaleViewHolder(@NonNull View itemView) {
            super(itemView);
            // Đảm bảo các ID này khớp với file sales_row.xml của bạn
            imageViewSaleImage = itemView.findViewById(R.id.image_view_sale_image);
            textViewSaleName = itemView.findViewById(R.id.text_view_sale_name);
            textViewSalePrice = itemView.findViewById(R.id.text_view_sale_price);
            textViewSaleDescription = itemView.findViewById(R.id.text_view_sale_description);
            textViewSaleStock = itemView.findViewById(R.id.text_view_sale_stock);
            textViewSaleCategory = itemView.findViewById(R.id.text_view_sale_category); // Ánh xạ TextView category mới
            buttonAddToCart = itemView.findViewById(R.id.button_add_to_cart_sale);
            buttonBuyNow = itemView.findViewById(R.id.button_buy_now_sale);
        }
    }
}