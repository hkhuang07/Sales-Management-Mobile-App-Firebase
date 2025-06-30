// File: app/src/main/java/com/example/salesmanagement/CartsAdapter.java
package com.example.salesmanagement;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CartsAdapter extends RecyclerView.Adapter<CartsAdapter.CartItemHolder> {

    private static final String TAG = "CartsAdapter"; // Thêm TAG cho log
    private List<OrderItem> orderItems; // Sử dụng CartItem thay cho OrderItem
    private OnCartItemActionListener listener;

    // Giao diện listener cho các hành động trên mỗi mục giỏ hàng
    public interface OnCartItemActionListener {
        void onUpdateQuantity(OrderItem item, int newQuantity); // Sử dụng CartItem
        void onDeleteItem(OrderItem item); // Sử dụng CartItem
    }

    public void setOnCartItemActionListener(OnCartItemActionListener listener) {
        this.listener = listener;
    }

    public CartsAdapter(List<OrderItem> orderItems) { // Constructor với CartItem
        this.orderItems = orderItems;
    }

    // Phương thức này quan trọng để cập nhật dữ liệu và làm mới RecyclerView
    public void updateCartItems(List<OrderItem> newItems) { // Với CartItem
        this.orderItems.clear();
        this.orderItems.addAll(newItems);
        notifyDataSetChanged(); // Thông báo cho RecyclerView rằng dữ liệu đã thay đổi
    }

    @NonNull
    @Override
    public CartItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cart_item_row, parent, false);
        return new CartItemHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartItemHolder holder, int position) {
        OrderItem currentItem = orderItems.get(position); // Lấy CartItem

        holder.textViewName.setText(currentItem.getName()); // Sử dụng getName()
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.textViewPrice.setText(currencyFormatter.format(currentItem.getPrice())); // Giá của một sản phẩm
        // Hiển thị tổng phụ của mục đó: Giá * Số lượng
        holder.textViewSubtotal.setText(currencyFormatter.format(currentItem.getSubtotal())); // Thêm TextView subtotal vào layout cart_item_row

        // Cập nhật TextView số lượng
        holder.textViewQuantity.setText(String.valueOf(currentItem.getQuantity()));

        String base64Image = currentItem.getImageUrl();
        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.imageViewThumbnail.setImageBitmap(decodedByte);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Base64 string is invalid for item " + currentItem.getItemId() + ": " + e.getMessage());
                holder.imageViewThumbnail.setImageResource(R.drawable.ic_error); // Hiển thị ảnh lỗi
            }
        } else {
            holder.imageViewThumbnail.setImageResource(R.drawable.ic_photo); // Hiển thị ảnh mặc định
        }

        // Listener cho nút tăng số lượng
        holder.buttonAdd.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUpdateQuantity(currentItem, currentItem.getQuantity() + 1);
            }
        });

        // Listener cho nút giảm số lượng hoặc xóa sản phẩm
        holder.buttonRemove.setOnClickListener(v -> {
            if (listener != null) {
                int currentQuantity = currentItem.getQuantity();
                if (currentQuantity > 1) { // Nếu số lượng hiện tại lớn hơn 1, giảm đi 1
                    listener.onUpdateQuantity(currentItem, currentQuantity - 1);
                } else {
                    // Nếu số lượng là 1 và người dùng nhấn trừ, xóa hẳn sản phẩm khỏi giỏ hàng
                    listener.onDeleteItem(currentItem);
                    // Thông báo Toast sẽ được xử lý ở CartsActivity hoặc ShoppingCartManager
                }
            }
        });

        // Listener cho nút xóa hẳn sản phẩm
        holder.buttonDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteItem(currentItem);
                // Thông báo Toast sẽ được xử lý ở CartsActivity hoặc ShoppingCartManager
            }
        });
    }

    @Override
    public int getItemCount() {
        return orderItems.size();
    }

    class CartItemHolder extends RecyclerView.ViewHolder {
        ImageView imageViewThumbnail;
        TextView textViewName;
        TextView textViewPrice;
        TextView textViewSubtotal; // Thêm TextView này
        ImageButton buttonRemove;
        TextView textViewQuantity;
        ImageButton buttonAdd;
        ImageButton buttonDelete;

        public CartItemHolder(@NonNull View itemView) {
            super(itemView);
            imageViewThumbnail = itemView.findViewById(R.id.image_view_cart_item_thumbnail);
            textViewName = itemView.findViewById(R.id.text_view_cart_item_name);
            textViewPrice = itemView.findViewById(R.id.text_view_cart_item_price);
            textViewSubtotal = itemView.findViewById(R.id.text_view_cart_item_subtotal); // Ánh xạ TextView subtotal
            buttonRemove = itemView.findViewById(R.id.button_cart_remove);
            textViewQuantity = itemView.findViewById(R.id.text_view_cart_quantity_display);
            buttonAdd = itemView.findViewById(R.id.button_cart_add);
            buttonDelete = itemView.findViewById(R.id.button_cart_delete);
        }
    }
}