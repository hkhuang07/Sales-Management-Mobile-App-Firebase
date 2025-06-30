// File: app/src/main/java/com/example/salesmanagement/ShoppingCartManager.java

package com.example.salesmanagement;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreException.Code;
import com.google.firebase.firestore.WriteBatch; // <-- THÊM DÒNG NÀY

import java.util.ArrayList;
import java.util.List;

public class ShoppingCartManager {
    private static final String TAG = "ShoppingCartManager";
    private static ShoppingCartManager instance;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private CollectionReference cartItemsCollection;
    private ListenerRegistration cartListenerRegistration;

    private List<OnCartChangeListener> listeners = new ArrayList<>();
    private List<OrderItem> currentOrderItems = new ArrayList<>();
    private double totalAmount = 0.0;
    private int cartItemCount = 0;

    public interface OnCartChangeListener {
        void onCartChanged();
    }

    public interface OnOperationCompleteListener {
        void onSuccess(String message);
        void onFailure(Exception e);
    }

    private ShoppingCartManager() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        setupUserCartListener();

        mAuth.addAuthStateListener(firebaseAuth -> {
            Log.d(TAG, "Auth state changed. User: " + (firebaseAuth.getCurrentUser() != null ? firebaseAuth.getCurrentUser().getUid() : "null"));
            setupUserCartListener();
        });
    }

    public static synchronized ShoppingCartManager getInstance() {
        if (instance == null) {
            instance = new ShoppingCartManager();
        }
        return instance;
    }

    // Đăng ký listener
    public void setOnCartChangeListener(OnCartChangeListener listener) {
        if (listener == null) {
            Log.w(TAG, "Attempted to add a null OnCartChangeListener. Ignoring.");
            return;
        }
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            Log.d(TAG, "OnCartChangeListener added. Total listeners: " + listeners.size());
        } else {
            Log.d(TAG, "OnCartChangeListener already registered. No action taken.");
        }
    }

    // Hủy đăng ký listener cụ thể
    public void removeOnCartChangeListener(OnCartChangeListener listener) {
        if (listener == null) {
            Log.w(TAG, "Attempted to remove a null OnCartChangeListener. Ignoring.");
            return;
        }
        if (listeners.remove(listener)) {
            Log.d(TAG, "OnCartChangeListener removed. Total listeners: " + listeners.size());
        } else {
            Log.d(TAG, "OnCartChangeListener not found for removal. No action taken.");
        }
    }

    // Phương thức để loại bỏ tất cả các listener (để giải quyết lỗi "Expected 1 argument but found 0"
    // khi ItemsListActivity gọi removeOnCartChangeListener() không tham số)
    public void removeOnCartChangeListener() { // Đã đổi tên/thêm overload để tương thích ngược
        removeAllOnCartChangeListeners();
    }

    // Phương thức để loại bỏ tất cả các listener (ví dụ khi Activity bị hủy)
    public void removeAllOnCartChangeListeners() {
        Log.d(TAG, "Removing all OnCartChangeListeners. Count: " + listeners.size());
        listeners.clear();
    }

    // Thông báo cho tất cả các listener đã đăng ký
    private void notifyCartChanged() {
        Log.d(TAG, "Notifying " + listeners.size() + " listeners about cart change.");
        // Tạo một bản sao của danh sách listeners để tránh ConcurrentModificationException
        // nếu một listener tự hủy đăng ký trong khi đang được thông báo
        List<OnCartChangeListener> currentListeners = new ArrayList<>(listeners);
        for (OnCartChangeListener listener : currentListeners) {
            if (listener != null) { // Đảm bảo listener không null
                listener.onCartChanged();
            }
        }
    }

    // --- Quản lý kết nối Firestore và dữ liệu giỏ hàng ---

    public void setupUserCartListener() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (cartListenerRegistration != null) {
            cartListenerRegistration.remove();
            cartListenerRegistration = null;
            Log.d(TAG, "Previous cart listener removed.");
        }

        if (user != null) {
            String userId = user.getUid();
            cartItemsCollection = db.collection("carts").document(userId).collection("cartItems");
            startListeningToCart();
            Log.d(TAG, "Setting up cart listener for user: " + userId);
        } else {
            Log.d(TAG, "User logged out, clearing local cart and stopping listener.");
            clearLocalCart();
            cartItemsCollection = null;
        }
    }

    private void startListeningToCart() {
        if (cartItemsCollection == null) {
            Log.w(TAG, "cartItemsCollection is null, cannot start listening.");
            return;
        }

        cartListenerRegistration = cartItemsCollection
                .orderBy("addedAt", Query.Direction.ASCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        currentOrderItems.clear();
                        totalAmount = 0.0;
                        cartItemCount = 0;

                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            OrderItem orderItem = doc.toObject(OrderItem.class);
                            if (orderItem != null) {
                                orderItem.setId(doc.getId());
                                currentOrderItems.add(orderItem);
                                totalAmount += (orderItem.getPrice() * orderItem.getQuantity());
                                cartItemCount += orderItem.getQuantity();
                            }
                        }
                        Log.d(TAG, "Cart data updated from Firestore. Total items: " + currentOrderItems.size() + ", Total amount: " + totalAmount);
                        notifyCartChanged();
                    }
                });
    }

    public void stopListeningToCart() {
        if (cartListenerRegistration != null) {
            cartListenerRegistration.remove();
            cartListenerRegistration = null;
            Log.d(TAG, "Firestore cart listener stopped.");
        }
    }

    private void clearLocalCart() {
        currentOrderItems.clear();
        totalAmount = 0.0;
        cartItemCount = 0;
        notifyCartChanged();
    }

    // --- Phương thức cung cấp dữ liệu giỏ hàng cho UI ---

    public List<OrderItem> getCartItems() {
        return new ArrayList<>(currentOrderItems);
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public int getCartItemCount() {
        return cartItemCount;
    }

    public int getItemQuantityInCart(String itemId) {
        for (OrderItem item : currentOrderItems) {
            if (item.getItemId().equals(itemId)) {
                return item.getQuantity();
            }
        }
        return 0;
    }

    public OrderItem getCartItemById(String itemId) {
        for (OrderItem item : currentOrderItems) {
            if (item.getItemId().equals(itemId)) {
                return item;
            }
        }
        return null;
    }

    // --- Thao tác giỏ hàng (ghi vào Firestore) ---

    // Phương thức mới để chủ động lấy dữ liệu giỏ hàng từ Firestore
    public void fetchCartItems(String userId, OnOperationCompleteListener listener) {
        if (userId == null || userId.isEmpty()) {
            if (listener != null) listener.onFailure(new IllegalArgumentException("User ID cannot be null or empty."));
            return;
        }

        CollectionReference userCartItemsRef = db.collection("carts").document(userId).collection("cartItems");
        userCartItemsRef.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    currentOrderItems.clear();
                    totalAmount = 0.0;
                    cartItemCount = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        OrderItem orderItem = doc.toObject(OrderItem.class);
                        if (orderItem != null) {
                            orderItem.setId(doc.getId());
                            currentOrderItems.add(orderItem);
                            totalAmount += (orderItem.getPrice() * orderItem.getQuantity());
                            cartItemCount += orderItem.getQuantity();
                        }
                    }
                    Log.d(TAG, "fetchCartItems: Cart data fetched and updated. Total items: " + currentOrderItems.size() + ", Total amount: " + totalAmount);
                    notifyCartChanged(); // Thông báo cho UI sau khi dữ liệu được cập nhật
                    if (listener != null) listener.onSuccess("Cart items fetched successfully.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "fetchCartItems: Failed to fetch cart items.", e);
                    if (listener != null) listener.onFailure(e);
                });
    }

    // Phương thức chính để thêm một sản phẩm vào giỏ hàng
    public void addItemToCart(Items item, int quantityToAdd, OnOperationCompleteListener listener) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            if (listener != null) listener.onFailure(new Exception("User not logged in."));
            return;
        }
        // Đảm bảo cartItemsCollection được khởi tạo
        if (cartItemsCollection == null) {
            cartItemsCollection = db.collection("carts").document(user.getUid()).collection("cartItems");
        }


        String itemId = item.getId();
        if (itemId == null || itemId.isEmpty()) {
            if (listener != null) listener.onFailure(new Exception("Item ID is missing."));
            return;
        }
        if (quantityToAdd <= 0) {
            if (listener != null) listener.onFailure(new Exception("Quantity to add must be positive."));
            return;
        }

        DocumentReference cartItemRef = cartItemsCollection.document(itemId);
        DocumentReference productRef = db.collection("Items").document(itemId);

        db.runTransaction(transaction -> {
            DocumentSnapshot cartSnapshot = transaction.get(cartItemRef);
            DocumentSnapshot productSnapshot = transaction.get(productRef);

            if (!productSnapshot.exists()) {
                throw new FirebaseFirestoreException("Product does not exist!", Code.NOT_FOUND);
            }

            Items product = productSnapshot.toObject(Items.class);
            if (product == null) {
                throw new FirebaseFirestoreException("Failed to parse product data.", Code.DATA_LOSS);
            }

            int currentProductStock = product.getQuantity();
            int currentCartQuantity = 0;
            if (cartSnapshot.exists()) {
                Long quantityInCart = cartSnapshot.getLong("quantity");
                currentCartQuantity = (quantityInCart != null) ? quantityInCart.intValue() : 0;
            }

            if (currentCartQuantity + quantityToAdd > currentProductStock) {
                throw new FirebaseFirestoreException(
                        "Quantity for " + item.getName() + " not enough in stock. Available: " + currentProductStock + ", In cart: " + currentCartQuantity,
                        Code.ABORTED
                );
            }

            int newQuantityInCart = currentCartQuantity + quantityToAdd;
            OrderItem newOrderItem = new OrderItem(
                    item.getId(),
                    item.getName(),
                    item.getPrice(),
                    item.getImageUrl(),
                    newQuantityInCart,
                    System.currentTimeMillis()
            );
            transaction.set(cartItemRef, newOrderItem);

            return null;
        }).addOnSuccessListener(aVoid -> {
            if (listener != null) listener.onSuccess("Item added to cart successfully.");
        }).addOnFailureListener(e -> {
            if (listener != null) listener.onFailure(e);
        });
    }

    /**
     * Phương thức tiện ích để thêm sản phẩm vào giỏ hàng mà không cần OnOperationCompleteListener.
     * Giải quyết lỗi "Expected 3 arguments but found 2" trong ItemsListActivity.
     */
    public void addItemToCart(Items item, int quantityToAdd) {
        // Gọi phương thức chính với listener là null, vì ItemsListActivity không cần callback trực tiếp tại đây.
        addItemToCart(item, quantityToAdd, null);
    }


    // Phương thức chính để cập nhật số lượng của một mục đã có trong giỏ hàng
    public void updateCartItemQuantity(String itemId, int newQuantity, OnOperationCompleteListener listener) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            if (listener != null) listener.onFailure(new Exception("User not logged in."));
            return;
        }
        // Đảm bảo cartItemsCollection được khởi tạo
        if (cartItemsCollection == null) {
            cartItemsCollection = db.collection("carts").document(user.getUid()).collection("cartItems");
        }

        DocumentReference cartItemRef = cartItemsCollection.document(itemId);
        DocumentReference productRef = db.collection("Items").document(itemId);

        db.runTransaction(transaction -> {
            DocumentSnapshot cartSnapshot = transaction.get(cartItemRef);
            DocumentSnapshot productSnapshot = transaction.get(productRef);

            if (!cartSnapshot.exists()) {
                throw new FirebaseFirestoreException("Item not found in cart!", Code.NOT_FOUND);
            }
            if (!productSnapshot.exists()) {
                throw new FirebaseFirestoreException("Original product no longer exists!", Code.NOT_FOUND);
            }

            Items product = productSnapshot.toObject(Items.class);
            if (product == null) {
                throw new FirebaseFirestoreException("Failed to parse product data.", Code.DATA_LOSS);
            }

            int currentProductStock = product.getQuantity();
            int oldQuantityInCart = cartSnapshot.getLong("quantity").intValue();

            if (newQuantity <= 0) {
                transaction.delete(cartItemRef);
                return null;
            }

            if (newQuantity > currentProductStock) {
                throw new FirebaseFirestoreException(
                        "Quantity for " + product.getName() + " not enough in stock. Available: " + currentProductStock,
                        Code.ABORTED
                );
            }

            transaction.update(cartItemRef,
                    "quantity", newQuantity,
                    "addedAt", System.currentTimeMillis()
            );

            return null;
        }).addOnSuccessListener(aVoid -> {
            if (listener != null) listener.onSuccess("Cart item quantity updated successfully.");
        }).addOnFailureListener(e -> {
            if (listener != null) listener.onFailure(e);
        });
    }

    /**
     * Phương thức tiện ích để cập nhật số lượng sản phẩm trong giỏ mà không cần OnOperationCompleteListener.
     * Giải quyết lỗi "Cannot resolve method 'updateItemQuantity' in 'ShoppingCartManager'"
     * và "Expected 1 argument but found 0" (nếu bạn có một phiên bản cũ hơn của phương thức này không có tham số).
     */
    public void updateItemQuantity(String itemId, int newQuantity) {
        // Gọi phương thức chính với listener là null, vì ItemsListActivity không cần callback trực tiếp tại đây.
        updateCartItemQuantity(itemId, newQuantity, null);
    }


    public void removeItemFromCart(String itemId, OnOperationCompleteListener listener) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            if (listener != null) listener.onFailure(new Exception("User not logged in."));
            return;
        }
        // Đảm bảo cartItemsCollection được khởi tạo
        if (cartItemsCollection == null) {
            cartItemsCollection = db.collection("carts").document(user.getUid()).collection("cartItems");
        }

        DocumentReference cartItemRef = cartItemsCollection.document(itemId);

        db.runTransaction(transaction -> {
            DocumentSnapshot cartSnapshot = transaction.get(cartItemRef);

            if (!cartSnapshot.exists()) {
                throw new FirebaseFirestoreException("Item not found in cart!", Code.NOT_FOUND);
            }
            transaction.delete(cartItemRef);
            return null;
        }).addOnSuccessListener(aVoid -> {
            if (listener != null) listener.onSuccess("Item removed from cart successfully.");
        }).addOnFailureListener(e -> {
            if (listener != null) listener.onFailure(e);
        });
    }

    // --- THÊM PHƯƠNG THỨC clearAllCartItems VÀO ĐÂY ---
    public void clearAllCartItems(OnOperationCompleteListener listener) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            if (listener != null) listener.onFailure(new Exception("User not logged in. Cannot clear cart."));
            return;
        }

        // Đảm bảo cartItemsCollection được khởi tạo
        if (cartItemsCollection == null) {
            cartItemsCollection = db.collection("carts").document(user.getUid()).collection("cartItems");
        }

        cartItemsCollection.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        batch.delete(document.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                // Xóa dữ liệu cục bộ sau khi xóa thành công trên Firestore
                                clearLocalCart();
                                if (listener != null) listener.onSuccess("All cart items cleared successfully.");
                                Log.d(TAG, "All cart items cleared from Firestore and local cache.");
                            })
                            .addOnFailureListener(e -> {
                                if (listener != null) listener.onFailure(e);
                                Log.e(TAG, "Error clearing all cart items: " + e.getMessage(), e);
                            });
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onFailure(e);
                    Log.e(TAG, "Error fetching cart items to clear: " + e.getMessage(), e);
                });
    }
}