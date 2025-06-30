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

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ItemsAdapter extends FirestoreRecyclerAdapter<Items, ItemsAdapter.ItemViewHolder> {

    private static final String TAG = "ItemsAdapter";

    // --- INTERFACES ---
    public interface OnItemClickListener {
        void onItemClick(DocumentSnapshot documentSnapshot, int position);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(DocumentSnapshot documentSnapshot, int position);
    }

    public interface OnCartButtonClickListener {
        //void onAddItemClick(Items item);
        //void onRemoveItemClick(Items item);
    }

    // --- LISTENERS ---
    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;
    private OnCartButtonClickListener cartButtonListener;

    // --- PROPERTIES ---
    private FirebaseFirestore db;
    private Map<String, String> categoryNameCache;
    private String currentUserRole; // Thêm biến để lưu vai trò người dùng

    // --- CONSTRUCTORS ---
    // Constructor cũ: sẽ mặc định currentUserRole là "guest" hoặc không xử lý
    public ItemsAdapter(@NonNull FirestoreRecyclerOptions<Items> options) {
        super(options);
        db = FirebaseFirestore.getInstance();
        categoryNameCache = new ConcurrentHashMap<>();
        this.currentUserRole = "guest"; // Mặc định nếu không được truyền
        Log.d(TAG, "ItemsAdapter initialized without specific role. Defaulting to 'guest'.");
    }

    // Constructor mới: nhận vai trò người dùng để xử lý hiển thị UI (ví dụ: nút Add to Cart)
    public ItemsAdapter(@NonNull FirestoreRecyclerOptions<Items> options, String currentUserRole) {
        super(options);
        db = FirebaseFirestore.getInstance();
        categoryNameCache = new ConcurrentHashMap<>();
        this.currentUserRole = (currentUserRole != null) ? currentUserRole.toLowerCase(Locale.getDefault()) : "guest";
        Log.d(TAG, "ItemsAdapter initialized with role: " + this.currentUserRole);
    }

    // --- ON BIND VIEW HOLDER ---
    @Override
    protected void onBindViewHolder(@NonNull ItemViewHolder holder, int position, @NonNull Items model) {
        holder.textViewName.setText(model.getName());
        holder.textViewPrice.setText(String.format(Locale.getDefault(), "Price: %.2f", model.getPrice()));
        holder.textViewQuantity.setText(String.format(Locale.getDefault(), "Inventory quantity: %d", model.getQuantity()));

        // Fetch and set category name
        String categoryId = model.getCategoryId();
        if (categoryId != null && !categoryId.isEmpty()) {
            if (categoryNameCache.containsKey(categoryId)) {
                holder.textViewCategory.setText("Category: " + categoryNameCache.get(categoryId));
            } else {
                db.collection("Categories").document(categoryId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String categoryName = documentSnapshot.getString("name");
                                if (categoryName != null) {
                                    holder.textViewCategory.setText("Category: " + categoryName);
                                    categoryNameCache.put(categoryId, categoryName);
                                } else {
                                    holder.textViewCategory.setText("Category: N/A");
                                }
                            } else {
                                holder.textViewCategory.setText("Category: Not available");
                                categoryNameCache.put(categoryId, "Does not exist");
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error fetching category for item " + model.getId() + ": " + e.getMessage());
                            holder.textViewCategory.setText("Category: Error");
                            categoryNameCache.put(categoryId, "Loading error");
                        });
            }
        } else {
            holder.textViewCategory.setText("Category: Unknown");
        }

        // Load image from Base64
        String base64Image = model.getImageUrl();
        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.imageViewThumbnail.setImageBitmap(decodedByte);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Base64 string is invalid for item " + model.getId() + ": " + e.getMessage());
                holder.imageViewThumbnail.setImageResource(R.drawable.ic_error);
            }
        } else {
            holder.imageViewThumbnail.setImageResource(R.drawable.ic_photo);
        }

        // ================================================================
        // LOGIC HIỂN THỊ NÚT GIỎ HÀNG DỰA TRÊN VAI TRÒ NGƯỜI DÙNG
        // ================================================================
        // Bỏ comment khối này nếu bạn có các ImageButton cho giỏ hàng trong item_row.xml
        // và muốn điều khiển chúng dựa trên vai trò admin/user.
        /*
        if (holder.buttonAddToCart != null && holder.buttonRemoveFromCart != null && holder.textViewQuantityInCart != null) {
            if ("admin".equalsIgnoreCase(currentUserRole)) {
                holder.buttonAddToCart.setVisibility(View.GONE);
                holder.buttonRemoveFromCart.setVisibility(View.GONE);
                holder.textViewQuantityInCart.setVisibility(View.GONE);
            } else {
                holder.buttonAddToCart.setVisibility(View.VISIBLE);

                int quantityInCart = ShoppingCartManager.getInstance().getItemQuantityInCart(model.getId());
                holder.textViewQuantityInCart.setText(String.valueOf(quantityInCart));

                if (quantityInCart > 0) {
                    holder.buttonRemoveFromCart.setVisibility(View.VISIBLE);
                    holder.textViewQuantityInCart.setVisibility(View.VISIBLE);
                    holder.buttonRemoveFromCart.setEnabled(true);
                    holder.buttonRemoveFromCart.setAlpha(1.0f);
                } else {
                    holder.buttonRemoveFromCart.setVisibility(View.GONE);
                    holder.textViewQuantityInCart.setVisibility(View.GONE);
                    holder.buttonRemoveFromCart.setEnabled(false);
                    holder.buttonRemoveFromCart.setAlpha(0.5f);
                }

                if (model.getQuantity() <= 0 || quantityInCart >= model.getQuantity()) {
                    holder.buttonAddToCart.setEnabled(false);
                    holder.buttonAddToCart.setAlpha(0.5f);
                } else {
                    holder.buttonAddToCart.setEnabled(true);
                    holder.buttonAddToCart.setAlpha(1.0f);
                }
            }
        } else {
            Log.d(TAG, "Cart buttons/text views not found in layout or null. Skipping visibility logic.");
        }
        */
    }

    // --- ON CREATE VIEW HOLDER ---
    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_row, parent, false);
        return new ItemViewHolder(view);
    }

    // --- SETTER METHODS FOR LISTENERS ---
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    public void setOnCartButtonClickListener(OnCartButtonClickListener cartButtonListener) {
        this.cartButtonListener = cartButtonListener;
    }

    // --- HELPER METHODS ---
    public void clearCategoryCache() {
        if (categoryNameCache != null) {
            categoryNameCache.clear();
            Log.d(TAG, "Category cache cleared.");
        }
    }

    // --- VIEW HOLDER CLASS ---
    class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        TextView textViewPrice;
        TextView textViewQuantity;
        ImageView imageViewThumbnail;
        TextView textViewCategory;

        // Biến cho phần giỏ hàng (bỏ comment nếu có trong item_row.xml)
        // ImageButton buttonAddToCart;
        // ImageButton buttonRemoveFromCart;
        // TextView textViewQuantityInCart;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.text_view_item_name);
            textViewPrice = itemView.findViewById(R.id.text_view_item_price);
            textViewQuantity = itemView.findViewById(R.id.text_view_item_current_quantity_detail);
            imageViewThumbnail = itemView.findViewById(R.id.image_view_item);
            textViewCategory = itemView.findViewById(R.id.text_view_item_category);

            // Ánh xạ các nút giỏ hàng (bỏ comment nếu bạn có chúng trong item_row.xml)
            /*
            buttonAddToCart = itemView.findViewById(R.id.button_add_item);
            buttonRemoveFromCart = itemView.findViewById(R.id.button_remove_item);
            textViewQuantityInCart = itemView.findViewById(R.id.text_view_cart_quantity);
            */

            // ================================================================
            // THIẾT LẬP LISTENERS CHO CLICK VÀ LONG CLICK TRÊN ITEM VIEW
            // ================================================================

            // Click ngắn cho mục (mở màn hình chỉnh sửa)
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(getSnapshots().getSnapshot(position), position);
                }
            });

            // Click dài cho mục (mở hộp thoại xóa)
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && longClickListener != null) {
                    longClickListener.onItemLongClick(getSnapshots().getSnapshot(position), position);
                    return true; // Rất quan trọng: trả về TRUE để tiêu thụ sự kiện long click
                    // và ngăn sự kiện click ngắn kích hoạt đồng thời.
                }
                return false; // Trả về false nếu long click không được xử lý
            });

            // ================================================================
            // LISTENERS CHO NÚT GIỎ HÀNG (BỎ COMMENT NẾU SỬ DỤNG)
            // ================================================================
            /*
            if (buttonAddToCart != null) {
                buttonAddToCart.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && cartButtonListener != null) {
                        cartButtonListener.onAddItemClick(getItem(position));
                    }
                });
            }

            if (buttonRemoveFromCart != null) {
                buttonRemoveFromCart.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && cartButtonListener != null) {
                        cartButtonListener.onRemoveItemClick(getItem(position));
                    }
                });
            }
            */
        }
    }
}