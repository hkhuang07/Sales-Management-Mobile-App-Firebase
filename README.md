# Sales Management Mobile App Firebase

An Android application designed for efficient sales and product management, offering distinct functionalities for both administrators and regular users. This app streamlines inventory, order processing, and user interactions within a sales ecosystem.

---

## 🚀 Project Overview

This mobile application provides a robust and intuitive platform for managing sales and products directly from an Android device. It features a clear distinction between administrative and regular user functionalities, making it versatile for various roles within a sales ecosystem. A core strength of this project lies in its seamless integration with **Google Firebase**, leveraging its powerful **Authentication** for secure user access and **Cloud Firestore** for flexible, real-time data storage.

This approach ensures a scalable, reliable, and modern backend solution, allowing the app to handle user authentication, product catalogs, shopping carts, and order management with ease and efficiency.

---

## 📸 Visualization

See the app in action with these animated demonstrations:

<p align="center">
  <h3>Authentication Flow</h3>
  <img src="demo/authentication.gif" width="800" height="400">
  <br>
  <em>Secure login and registration with Firebase Authentication (Email/Password & Google Sign-In).</em>
</p>

<p align="center">
  <h3>Product & Category Management (Admin View)</h3>
  <img src="demo/categories.gif" width="800" height="400">
  <img src="demo/items.gif" width="800" height="400">
  <br>
  <em>Admins can easily manage product categories and individual items.</em>
</p>

<p align="center">
  <h3>Shopping & Checkout Experience (Regular User View)</h3>
  <img src="demo/buynow.gif" width="800" height="400">
  <br>
  <em>Browse products, adding to cart, and proceeding to checkout.</em>
</p>

<p align="center">
  <h3>Cart & Order Processing</h3>
  <img src="demo/checkout.gif" width="800" height="400">
  <br>
  <em>Manage items in the cart and complete an order.</em>
</p>

<p align="center">
  <h3>Order & User Management</h3>
  <img src="demo/orders.gif" width="150" height="400">
  <img src="demo/users.gif" width="150" height="400">
  <br>
  <em>Admins can view all orders and manage user accounts. Regular users can view their order history.</em>
</p>

<p align="center">
  <h3>User Profile & Settings</h3>
  <img src="demo/userprofile.gif" width="150" height="400">
  <img src="demo/settings.gif" width="150" height="400">
  <br>
  <em>View and update user profiles, along with app settings for a personalized experience.</em>
</p>

---

## 🛠️ Technologies Used

This application is built on a robust and modern technology stack, with **Google Firebase** at its core for backend services:

* **Language:** Java
* **Platform:** Android
* **Backend:** Google Firebase
    * **Firebase Authentication:** For secure user sign-up, sign-in, and session management.
    * **Cloud Firestore:** A flexible, scalable NoSQL cloud database used for storing all application data (products, categories, user profiles, and orders) in real-time.
    * **Firebase Cloud Messaging (FCM):** Enables reliable push notifications to users for updates and alerts.
* **UI Components:** AndroidX, Material Design Components – for a consistent, modern, and user-friendly interface.

---

## 🌟 Features

The application is packed with features designed to enhance sales operations and user experience:

* **User Authentication:** Secure login/logout using Firebase Authentication, supporting both Email/Password and Google Sign-In for a smooth user onboarding experience.
* **Role-Based Access Control:**
    * **Admin Users:** Full administrative privileges to manage products, categories, users, and view all orders.
    * **Regular Users:** Can browse products, add items to their cart, place orders, and track their personal order history.
* **Product Management:**
    * View a comprehensive list of products.
    * **(Admin)** Complete CRUD (Create, Read, Update, Delete) operations for products.
* **Category Management:**
    * View product categories to organize items efficiently.
    * **(Admin)** Full CRUD capabilities for categories.
* **Shopping Cart System:** (For Regular Users)
    * Seamlessly add products to a dynamic shopping cart.
    * Manage quantities and remove items directly from the cart.
    * Proceed to a guided checkout process for order placement.
* **Order Management:**
    * **(Admin)** Gain a complete overview of all placed orders for comprehensive tracking.
    * **(Regular User)** Access personal order history, reviewing past purchases and their statuses.
* **User Profile Management:** Users can view and easily update their profile information.
* **Settings:** Personalize the app experience by customizing preferences such as theme (light/dark/system default) and notification settings.
* **Firebase Cloud Messaging (FCM):** Integrated for efficient push notifications, allowing users to opt-in or opt-out of receiving important updates.
* **Intuitive UI/UX:** Built with modern Material Design components, ensuring a clean, responsive, and user-friendly interface.
* **Gesture Navigation:** A convenient swipe gesture (swipe left) to quickly access the Sales Activity for regular users, enhancing navigation speed.

---

## 🚀 Getting Started

To get a local copy of this project up and running on your development machine, follow these simple steps.

### Prerequisites

* **Android Studio Arctic Fox** or newer
* **Java Development Kit (JDK) 11** or newer
* A **Google Firebase Project** (properly set up for Android)

### Installation & Configuration

If you'd like to use this codebase and connect it to your own Firebase project for data storage and authentication, here's how to do it:

1.  **Clone the Repository:**
    First, you'll need to clone the project to your local machine. Open your terminal or Git Bash and run:
    ```bash
    git clone [https://github.com/hkhuang07/Sales-Management-Mobile-App-Firebase.git](https://github.com/hkhuang07/Sales-Management-Mobile-App-Firebase.git)
    cd Sales-Management-Mobile-App-Firebase
    ```

2.  **Open in Android Studio:**
    Open the cloned project folder in Android Studio. Android Studio will begin syncing the project and downloading necessary dependencies.

3.  **Set up Your Own Firebase Project:**
    This is the most crucial step to ensure your app connects to your own backend data.
    * Go to the [Firebase Console](https://console.firebase.google.com/) and sign in with your Google account.
    * **Create a New Firebase Project:** Click "Add project" and follow the on-screen instructions. Give your project a meaningful name (e.g., "MySalesAppBackend").
    * **Add an Android App to Your Firebase Project:**
        * In your newly created Firebase project, click the Android icon (looks like an Android robot) to add an Android app.
        * **Package name:** Enter your app's package name. You can find this in your Android Studio project in `app/build.gradle` under `applicationId`. It typically looks like `com.example.salesmanagement`.
        * **App nickname:** (Optional) Give your app a friendly name.
        * **SHA-1 certificate:** This is required for Google Sign-In and other Firebase services. To get your SHA-1 key:
            * In Android Studio, open the **Gradle** pane (usually on the right side).
            * Navigate to `Your Project Name > app > Tasks > android > signingReport`.
            * Double-click `signingReport`. The SHA-1 key will appear in the "Run" window at the bottom. Copy it.
            * Paste the SHA-1 key into the Firebase setup.
        * Click "Register app".
    * **Download `google-services.json`:** Firebase will prompt you to download the `google-services.json` file. **Download this file and place it directly into your Android Studio project's `app/` directory.** This file contains all the necessary configurations for your app to communicate with your Firebase project.
    * **Enable Firebase Services:** In your Firebase Console, navigate to:
        * **Authentication:** Go to "Build > Authentication" and click "Get started". Enable the **Email/Password** provider and the **Google** provider.
        * **Cloud Firestore:** Go to "Build > Firestore Database" and click "Create database". Choose "Start in production mode" (or "test mode" for quick setup, but remember to secure your rules later) and select a location for your database.
        * **Cloud Messaging:** Go to "Build > Cloud Messaging" to ensure it's enabled for push notifications.

4.  **Set up Firestore Security Rules:**
    This is crucial for controlling who can read and write data in your database.
    * In your Firebase Console, go to "Build > Firestore Database > Rules".
    * You'll need to define rules that allow your app to interact with the data. Here are some example starting points (adapt these to your specific needs for security):
        ```firestore
        rules_version = '2';
        service cloud.firestore {
          match /databases/{database}/documents {
            // Allow all authenticated users to read and write to their own user profile
            match /users/{userId} {
              allow read, write: if request.auth != null && request.auth.uid == userId;
            }

            // Allow all authenticated users to read products
            // Allow only 'admin' users to create, update, or delete products
            match /products/{productId} {
              allow read: if request.auth != null; // or if true for public read
              allow create, update, delete: if request.auth != null && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
            }

            // Similarly, for categories, carts, orders, order items:
            match /categories/{categoryId} {
              allow read: if request.auth != null;
              allow create, update, delete: if request.auth != null && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
            }

            // Carts: users can only manage their own cart items
            match /carts/{cartId} {
              allow read, write: if request.auth != null && request.auth.uid == cartId;
            }

            // Orders: users can only read their own orders; admins can read all
            match /orders/{orderId} {
              allow read: if request.auth != null && (request.auth.uid == resource.data.userId || get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin');
              allow create: if request.auth != null;
              allow update, delete: if request.auth != null && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
            }
            // For order items, ensure read access is tied to the parent order's read access
            match /orderItems/{orderItemId} {
                allow read: if request.auth != null && (get(/databases/$(database)/documents/orders/$(resource.data.orderId)).data.userId == request.auth.uid || get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin');
                allow create: if request.auth != null;
                allow update, delete: if request.auth != null && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
            }
          }
        }
        ```
    * **Important:** These rules are examples. You'll need to adapt them based on your exact data model and desired access control. Always start with strict rules and then loosen them as needed, testing thoroughly.

5.  **Sync Gradle:**
    In Android Studio, ensure all necessary dependencies are downloaded by allowing Gradle to sync the project. You'll typically see a "Sync Now" button if changes were made (like adding `google-services.json`).

6.  **Run the App:**
    Connect an Android device to your computer or use an emulator in Android Studio. Then, click the "Run" button (green play icon) in Android Studio to deploy and test the application with your newly configured Firebase backend.

---

## 📂 Project Structure (Key Files)

Here's an overview of the key files and directories within the project:

* `app/src/main/java/com/example/salesmanagement/MainActivity.java`: The main entry point and dashboard of the application, handling user roles and navigation.
* `app/src/main/java/com/example/salesmanagement/LoginActivity.java`: Manages user authentication (login, registration).
* `app/src/main/java/com/example/salesmanagement/ItemsListActivity.java`: Displays and manages product listings.
* `app/src/main/java/com/example/salesmanagement/CategoriesListActivity.java`: Handles category management (primarily for admin users).
* `app/src/main/java/com/example/salesmanagement/SalesActivity.java`: Provides the product Browse and purchasing interface for regular users.
* `app/src/main/java/com.example/salesmanagement/CartsActivity.java`: Manages the user's shopping cart.
* `app/src/main/java/com.example/salesmanagement/OrdersListActivity.java`: Displays all orders (for admin users).
* `app/src/main/java/com.example/salesmanagement/OrderHistoryActivity.java`: Shows a regular user's personal order history.
* `app/src/main/java/com.example/salesmanagement/UserProfileActivity.java`: Allows users to view and potentially update their profile information.
* `app/src/main/java/com.example/salesmanagement/SettingsActivity.java`: Contains app settings, including theme and notification preferences.
* `app/src/main/java/com.example/salesmanagement/MyFirebaseMessagingService.java`: Handles incoming FCM notifications.
* `app/src/main/res/layout/`: Contains all XML layout files for the activities and fragments, defining the UI structure.
* `app/src/main/res/xml/root_preferences.xml`: XML file defining the preferences screen for `SettingsActivity`.
* `app/google-services.json`: The crucial Firebase configuration file that connects your Android app to your Firebase project.

---

## 🤝 Contributing

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

If you have a suggestion that would make this better, please fork the repo and create a pull request. You can also simply open an issue with the tag "enhancement".
Don't forget to give the project a star! Thanks again!

1.  Fork the Project
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the Branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

---

## ⚖️ License

Distributed under the MIT License. See `LICENSE` for more information.

---

## 📞 Contact

Huynh Quoc Huy - [huykyunh.k@gmail.com](mailto:huykyunh.k@gmail.com)

Project Link: [https://github.com/hkhuang07/Sales-Management-Mobile-App-Firebase](https://github.com/hkhuang07/Sales-Management-Mobile-App-Firebase)
