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
  <img src="demo/checkout.gif"  width="800" height="400">
  <br>
  <em>Manage items in the cart and complete an order.</em>
</p>

<p align="center">
  <h3>Order & User Management</h3>
  <img src="demo/orders.gif" width="200" height="400">
  <img src="demo/users.gif" width="200" height="400">
  <br>
  <em>Admins can view all orders and manage user accounts. Regular users can view their order history.</em>
</p>

<p align="center">
  <h3>User Profile & Settings</h3>
  <img src="demo/userprofile.gif" width="200" height="400">
  <img src="demo/settings.gif" width="200" height="400">
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

### Installation

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/hkhuang07/Sales-Management-Mobile-App-Firebase.git](https://github.com/hkhuang07/Sales-Management-Mobile-App-Firebase.git)
    ```
2.  **Open in Android Studio:**
    Open the cloned project folder in Android Studio.
3.  **Set up Firebase:**
    * Go to the [Firebase Console](https://console.firebase.google.com/).
    * Create a new Firebase project (if you don't have one).
    * Add an Android app to your Firebase project.
    * Follow the instructions to download your `google-services.json` file.
    * Place `google-services.json` into your `app/` directory of the Android Studio project.
    * **Enable the following services in your Firebase project:**
        * **Authentication:** Enable Email/Password and Google Sign-in providers.
        * **Cloud Firestore:** Initialize a Firestore database.
        * **Cloud Messaging:** Enable this service for push notifications.
    * **Set up Firestore Security Rules:** Configure appropriate security rules in your Firestore database to control read/write access. For basic functionality, you might start with rules that allow authenticated users to read and write specific collections, and admin users to have broader access. (E.g., `match /users/{userId} { allow read, write: if request.auth != null; }`, `match /products/{productId} { allow read: if true; allow write: if request.auth.token.admin == true; }`).
4.  **Sync Gradle:**
    Allow Android Studio to sync the project with Gradle files to download all necessary dependencies.
5.  **Run the app:**
    Connect an Android device or use an emulator, then click the "Run" button in Android Studio.

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
