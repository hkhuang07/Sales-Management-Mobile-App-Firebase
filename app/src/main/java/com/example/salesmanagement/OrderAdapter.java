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
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class OrderAdapter extends FirestoreRecyclerAdapter<Orders, OrderAdapter.OrderHolder> {

    private OnItemClickListener listener;
    // Khai báo SimpleDateFormat ở đây nếu bạn muốn sử dụng lại nó để tránh tạo mới mỗi lần
    // private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());


    public OrderAdapter(@NonNull FirestoreRecyclerOptions<Orders> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull OrderHolder holder, int position, @NonNull Orders model) {
        holder.textViewOrderId.setText("NO: " + model.getOrderId());

        // Định dạng ngày giờ
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        if (model.getOrderDate() != null) {
            holder.textViewOrderDate.setText(sdf.format(model.getOrderDate()));
        } else {
            holder.textViewOrderDate.setText("N/A");
        }

        // Định dạng tổng tiền
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.textViewTotalAmount.setText("Total price: " + currencyFormatter.format(model.getTotalAmount()));

        holder.textViewDeliveryAddress.setText("Address: " + model.getDeliveryAddress());

        // Hiển thị và định dạng trạng thái
        // Sử dụng textViewStatus thay vì textViewOrderStatus để khớp với OrderHolder
        holder.textViewStatus.setText(model.getStatus());
        // Gọi hàm setColorForStatus của riêng bạn
        setColorForStatus(holder.textViewStatus, model.getStatus());
    }

    // Hàm setColorForStatus của bạn, đã được tái sử dụng nguyên vẹn từ bản gốc bạn cung cấp
    private void setColorForStatus(TextView textView, String status) {
        int color;
        switch (status) {
            case "Pending":
                color = ContextCompat.getColor(textView.getContext(), R.color.status_pending); // Màu vàng cam
                break;
            case "Confirmed":
                color = ContextCompat.getColor(textView.getContext(), R.color.status_confirmed); // Màu xanh dương
                break;
            case "Shipped":
                color = ContextCompat.getColor(textView.getContext(), R.color.status_shipped); // Màu xanh ngọc
                break;
            case "Delivered":
                color = ContextCompat.getColor(textView.getContext(), R.color.status_delivered); // Màu xanh lá
                break;
            case "Cancelled":
                color = ContextCompat.getColor(textView.getContext(), R.color.status_cancelled); // Màu đỏ
                break;
            default:
                color = ContextCompat.getColor(textView.getContext(), android.R.color.darker_gray); // Màu mặc định
                break;
        }
        // Áp dụng màu nền cho Drawable (đảm bảo drawable là shape có solid color)
        GradientDrawable background = (GradientDrawable) textView.getBackground();
        if (background == null) {
            // Nếu background chưa được set, tạo mới từ drawable resource
            background = (GradientDrawable) ContextCompat.getDrawable(textView.getContext(), R.drawable.rounded_status_background);
            if (background != null) {
                textView.setBackground(background);
            }
        }
        // Set màu cho background
        if (background != null) {
            background.setColor(color);
        }
    }


    @NonNull
    @Override
    public OrderHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Đảm bảo layout 'order_item_history_row' tồn tại và có các TextView với ID tương ứng
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.order_item_history_row, parent, false);
        return new OrderHolder(view);
    }

    class OrderHolder extends RecyclerView.ViewHolder {
        TextView textViewOrderId;
        TextView textViewOrderDate;
        TextView textViewTotalAmount;
        TextView textViewStatus; // <-- Tên TextView cho trạng thái, khớp với layout
        TextView textViewDeliveryAddress;

        public OrderHolder(@NonNull View itemView) {
            super(itemView);
            textViewOrderId = itemView.findViewById(R.id.text_view_order_id);
            textViewOrderDate = itemView.findViewById(R.id.text_view_order_date);
            textViewTotalAmount = itemView.findViewById(R.id.text_view_order_total_amount);
            textViewStatus = itemView.findViewById(R.id.text_view_order_status); // <-- Ánh xạ đúng ID
            textViewDeliveryAddress = itemView.findViewById(R.id.text_view_order_delivery_address);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        listener.onItemClick(getSnapshots().getSnapshot(position), position);
                    }
                }
            });
        }
    }

    public interface OnItemClickListener {
        void onItemClick(DocumentSnapshot documentSnapshot, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}