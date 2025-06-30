package com.example.salesmanagement;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CheckoutAdapter extends RecyclerView.Adapter<CheckoutAdapter.CheckoutItemHolder> {

    // Thay đổi từ List<OrderItem> sang List<CartItem>
    private List<OrderItem> checkoutItems;

    // Cập nhật constructor để chấp nhận List<CartItem>
    public CheckoutAdapter(List<OrderItem> checkoutItems) {
        this.checkoutItems = checkoutItems;
    }

    @NonNull
    @Override
    public CheckoutItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.checkout_item_row, parent, false);
        return new CheckoutItemHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CheckoutItemHolder holder, int position) {
        // Lấy đối tượng CartItem
        OrderItem currentItem = checkoutItems.get(position);

        // Sử dụng các getter của CartItem
        holder.textViewName.setText(currentItem.getName()); // Sử dụng getName() thay vì getItemName()
        holder.textViewQuantity.setText("x " + currentItem.getQuantity());

        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.textViewSubtotal.setText(currencyFormatter.format(currentItem.getSubtotal()));
    }

    @Override
    public int getItemCount() {
        return checkoutItems.size();
    }

    class CheckoutItemHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        TextView textViewQuantity;
        TextView textViewSubtotal;

        public CheckoutItemHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.text_view_checkout_item_name);
            textViewQuantity = itemView.findViewById(R.id.text_view_checkout_item_quantity);
            textViewSubtotal = itemView.findViewById(R.id.text_view_checkout_item_subtotal);
        }
    }
}