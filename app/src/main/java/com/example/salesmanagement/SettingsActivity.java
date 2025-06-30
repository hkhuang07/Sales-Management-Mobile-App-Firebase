package com.example.salesmanagement;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.EditTextPreference; // Import EditTextPreference
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyThemeFromPreferences(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Settings");
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static void applyThemeFromPreferences(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String theme = sharedPref.getString("theme_selection", "system_default");

        switch (theme) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "system_default":
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        private FirebaseAuth mAuth;
        private GoogleSignInClient mGoogleSignInClient;
        private static final String NOTIFICATION_TOPIC = "general_notifications";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mAuth = FirebaseAuth.getInstance();

            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            // --- Enable Notifications Preference ---
            SwitchPreferenceCompat notificationsPreference = findPreference("notifications_enabled");
            if (notificationsPreference != null) {
                notificationsPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        boolean isEnabled = (boolean) newValue;
                        if (isEnabled) {
                            FirebaseMessaging.getInstance().subscribeToTopic(NOTIFICATION_TOPIC)
                                    .addOnCompleteListener(task -> {
                                        String msg = task.isSuccessful() ? "Subscribed to " + NOTIFICATION_TOPIC : "Subscribe failed";
                                        Toast.makeText(getContext(), "Notifications enabled: " + msg, Toast.LENGTH_SHORT).show();
                                        Log.d(TAG, "Notifications switched ON. " + msg);
                                    });
                        } else {
                            FirebaseMessaging.getInstance().unsubscribeFromTopic(NOTIFICATION_TOPIC)
                                    .addOnCompleteListener(task -> {
                                        String msg = task.isSuccessful() ? "Unsubscribed from " + NOTIFICATION_TOPIC : "Unsubscribe failed";
                                        Toast.makeText(getContext(), "Notifications disabled: " + msg, Toast.LENGTH_SHORT).show();
                                        Log.d(TAG, "Notifications switched OFF. " + msg);
                                    });
                        }
                        return true;
                    }
                });
            }

            // --- App Theme Preference ---
            ListPreference themePreference = findPreference("theme_selection");
            if (themePreference != null) {
                themePreference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
                themePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                        String newTheme = (String) newValue;
                        Toast.makeText(getContext(), "Applying " + newTheme + " theme...", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Theme changed to: " + newTheme);
                        applyThemeFromPreferences(getContext());
                        if (getActivity() != null) {
                            getActivity().recreate();
                        }
                        return true;
                    }
                });
            }

            // =================================================================
            // --- Username Preference: Mở UserProfileActivity khi click ---
            // =================================================================
            EditTextPreference usernamePreference = findPreference("username");
            if (usernamePreference != null) {
                // Giữ chức năng chỉnh sửa trực tiếp (nếu muốn)
                //usernamePreference.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());

                usernamePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(@NonNull Preference preference) {
                        // Khởi tạo Intent để mở UserProfileActivity
                        Intent intent = new Intent(getContext(), UserProfileActivity.class);
                        startActivity(intent);
                        return true; // Trả về true để chỉ ra rằng sự kiện đã được xử lý
                    }
                });
            }


            // --- Logout Preference ---
            Preference logoutPreference = findPreference("logout_account");
            if (logoutPreference != null) {
                logoutPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(@NonNull Preference preference) {
                        new AlertDialog.Builder(getContext())
                                .setTitle("Logout Confirmation")
                                .setMessage("Are you sure you want to log out?")
                                .setPositiveButton("Logout", (dialog, which) -> {
                                    mAuth.signOut();
                                    Log.d(TAG, "Firebase user signed out.");

                                    if (mGoogleSignInClient != null) {
                                        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                Log.d(TAG, "Google Sign-In client signed out.");
                                            } else {
                                                Log.e(TAG, "Failed to sign out from Google Sign-In client.", task.getException());
                                            }
                                        });
                                    }

                                    Toast.makeText(getContext(), "Logged out successfully.", Toast.LENGTH_SHORT).show();

                                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    if (getActivity() != null) {
                                        getActivity().finish();
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                        return true;
                    }
                });
            }

            // --- About App Preference ---
            Preference aboutPreference = findPreference("about_app");
            if (aboutPreference != null) {
                aboutPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(@NonNull Preference preference) {
                        String versionName = "N/A";
                        try {
                            versionName = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0).versionName;
                        } catch (Exception e) {
                            Log.e(TAG, "Error getting app version: " + e.getMessage());
                        }

                        new AlertDialog.Builder(getContext())
                                .setTitle("About Sales Management App")
                                .setMessage("Version: " + versionName + "\n\n" +
                                        "Copyright © by Huynh Quoc Huy \n\n" +
                                        "This app helps manage sales and products efficiently.")
                                .setPositiveButton("OK", null)
                                .show();
                        return true;
                    }
                });
            }

            // --- Contact Website Preference ---
            Preference contactPreference = findPreference("contact");
            if (contactPreference != null) {
                contactPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(@NonNull Preference preference) {
                        String githubUrl = "https://github.com/hkhuang07/";
                        try {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl));
                            startActivity(browserIntent);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(getContext(), "No web browser found to open the URL.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "No browser found: " + e.getMessage());
                        }
                        return true;
                    }
                });
            }
        }
    }
}