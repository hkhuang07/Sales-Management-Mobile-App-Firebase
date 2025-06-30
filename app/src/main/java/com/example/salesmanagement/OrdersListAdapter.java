package com.example.salesmanagement;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class OrdersListAdapter extends FirestoreRecyclerAdapter<Orders, OrdersListAdapter.OrderViewHolder> {

    private OnOrderActionListener listener;
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public OrdersListAdapter(@NonNull FirestoreRecyclerOptions<Orders> options) {
        super(options);
    }

    public interface OnOrderActionListener {
        void onOrderDetailsClick(Orders order);
        void onUpdateOrderStatusClick(Orders order);
    }

    public void setOnOrderActionListener(OnOrderActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.orders_list_item_row, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull OrderViewHolder holder, int position, @NonNull Orders order) {
        holder.textViewOrderId.setText("Order ID: #" + order.getOrderId());

        String userInfo = "";
        if (order.getUserName() != null && !order.getUserName().isEmpty()) {
            userInfo += order.getUserName();
        }
        if (order.getUserEmail() != null && !order.getUserEmail().isEmpty()) {
            if (!userInfo.isEmpty()) userInfo += " (";
            userInfo += order.getUserEmail();
            if (!userInfo.isEmpty()) userInfo += ")";
        }
        if (userInfo.isEmpty()) {
            userInfo = "User ID: " + order.getUserId();
        }
        holder.textViewUserInfo.setText("User: " + userInfo);

        if (order.getOrderDate() != null) {
            holder.textViewOrderDate.setText("Date: " + dateFormatter.format(order.getOrderDate()));
        } else {
            holder.textViewOrderDate.setText("Date: N/A");
        }
        holder.textViewTotalAmount.setText("Total: " + currencyFormatter.format(order.getTotalAmount()));
        holder.textViewOrderStatus.setText(order.getStatus()); // Chỉ set text, màu sẽ được áp dụng sau

        // Gọi phương thức để đặt màu cho trạng thái
        setColorForStatus(holder.textViewOrderStatus, order.getStatus());

        StringBuilder itemDetails = new StringBuilder("Items: ");
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (int i = 0; i < order.getItems().size(); i++) {
                // ĐÂY LÀ DÒNG CẦN SỬA: Đổi OrderItem thành CartItem
                OrderItem item = order.getItems().get(i);
                itemDetails.append(item.getName()).append(" (x").append(item.getQuantity()).append(")");
                if (i < order.getItems().size() - 1) {
                    itemDetails.append(", ");
                }
            }
        } else {
            itemDetails.append("No items listed.");
        }
        holder.textViewItemDetails.setText(itemDetails.toString());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                DocumentSnapshot snapshot = getSnapshots().getSnapshot(holder.getAdapterPosition());
                Orders clickedOrder = snapshot.toObject(Orders.class);
                if (clickedOrder != null) {
                    clickedOrder.setOrderId(snapshot.getId());
                    listener.onOrderDetailsClick(clickedOrder);
                }
            }
        });

        if (holder.btnViewDetails != null) {
            holder.btnViewDetails.setOnClickListener(v -> {
                if (listener != null) {
                    DocumentSnapshot snapshot = getSnapshots().getSnapshot(holder.getAdapterPosition());
                    Orders clickedOrder = snapshot.toObject(Orders.class);
                    if (clickedOrder != null) {
                        clickedOrder.setOrderId(snapshot.getId());
                        listener.onOrderDetailsClick(clickedOrder);
                    }
                }
            });
        }

        // Bỏ comment dòng này để nút "Update Status" hoạt động
        if (holder.btnUpdateStatus != null) {
            holder.btnUpdateStatus.setOnClickListener(v -> {
                if (listener != null) {
                    DocumentSnapshot snapshot = getSnapshots().getSnapshot(holder.getAdapterPosition());
                    Orders clickedOrder = snapshot.toObject(Orders.class);
                    if (clickedOrder != null) {
                        clickedOrder.setOrderId(snapshot.getId());
                        listener.onUpdateOrderStatusClick(clickedOrder);
                    }
                }
            });
        }
    }

    /**
     * Đặt màu nền và màu chữ cho TextView trạng thái đơn hàng.
     * @param textView TextView hiển thị trạng thái.
     * @param status Chuỗi trạng thái của đơn hàng.
     */
    private void setColorForStatus(TextView textView, String status) {
        int color;
        int textColor = ContextCompat.getColor(textView.getContext(), android.R.color.white); // Màu chữ trắng mặc định

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

        // Đảm bảo textViewOrderStatus có background là GradientDrawable
        // Nếu không có, gán rounded_status_background.xml
        GradientDrawable background = (GradientDrawable) textView.getBackground();
        if (background == null || !(background instanceof GradientDrawable)) {
            background = (GradientDrawable) ContextCompat.getDrawable(textView.getContext(), R.drawable.rounded_status_background);
            if (background != null) {
                textView.setBackground(background);
            }
        }
        if (background != null) {
            background.setColor(color);
        }
        textView.setTextColor(textColor); // Đặt màu chữ
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView textViewOrderId, textViewUserInfo, textViewOrderDate, textViewTotalAmount, textViewOrderStatus, textViewItemDetails;
        MaterialButton btnViewDetails, btnUpdateStatus;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewOrderId = itemView.findViewById(R.id.text_view_order_id);
            textViewUserInfo = itemView.findViewById(R.id.text_view_user_info);
            textViewOrderDate = itemView.findViewById(R.id.text_view_order_date);
            textViewTotalAmount = itemView.findViewById(R.id.text_view_total_amount);
            textViewOrderStatus = itemView.findViewById(R.id.text_view_order_status);
            textViewItemDetails = itemView.findViewById(R.id.text_view_item_details);

            btnViewDetails = itemView.findViewById(R.id.btn_view_order_details);
            // Bỏ comment dòng này để ánh xạ nút "Update Status"
            //btnUpdateStatus = itemView.findViewById(R.id.btn_update_order_status);
        }
    }
}