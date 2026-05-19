package com.example.sqlite;
import android.app.AlarmManager;
import android.app.AlertDialog;

import android.app.PendingIntent;

import java.util.Calendar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchDarkMode;
    private SwitchCompat switchReminder;
    private static final String PREF_NAME = "app_settings";
    private static final String KEY_REMINDER = "reminder_enabled";
    private static final String DB_NAME = "SmartWallet.db";

    private final ActivityResultLauncher<Intent> backupLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handleBackup(result.getData().getData());
                }
            }
    );

    private final ActivityResultLauncher<Intent> restoreLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handleRestore(result.getData().getData());
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar_settings);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        initViews();
        handleEvents();
    }

    private void initViews() {

        switchDarkMode = findViewById(R.id.switch_dark_mode);
        switchReminder = findViewById(R.id.switch_reminder);

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isReminderEnabled = prefs.getBoolean(KEY_REMINDER, false);

        switchReminder.setChecked(isReminderEnabled);

        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            switchDarkMode.setChecked(true);
        } else {
            switchDarkMode.setChecked(false);
        }
    }

    private void handleEvents() {
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
        switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            DatabaseHelper dbHelper = new DatabaseHelper(this);

            String time = new java.text.SimpleDateFormat(
                    "dd/MM/yyyy HH:mm",
                    java.util.Locale.getDefault()
            ).format(new java.util.Date());
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_REMINDER, isChecked).apply();

            if (isChecked) {
                scheduleDailyReminder();
                Toast.makeText(this,
                        "Đã bật nhắc nhở 23:30 mỗi ngày",
                        Toast.LENGTH_SHORT).show();
                dbHelper.insertMessage(
                        "Bật nhắc nhở ⏰",
                        "Bạn đã bật nhắc nhở 23:30 mỗi ngày.",
                        time
                );
            } else {
                cancelReminder();
                Toast.makeText(this,
                        "Đã tắt nhắc nhở",
                        Toast.LENGTH_SHORT).show();
                dbHelper.insertMessage(
                        "Tắt nhắc nhở 🔕",
                        "Bạn đã tắt nhắc nhở hàng ngày.",
                        time
                );
            }
        });

        findViewById(R.id.btn_backup).setOnClickListener(v -> startBackupProcess());
        findViewById(R.id.btn_restore).setOnClickListener(v -> startRestoreProcess());
        findViewById(R.id.btn_logout).setOnClickListener(v -> confirmLogout());
            
        findViewById(R.id.btn_about).setOnClickListener(v -> 
            Toast.makeText(this, "Smart Wallet v1.0 - Đồ án sinh viên", Toast.LENGTH_SHORT).show());
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Dữ liệu của tài khoản hiện tại vẫn được giữ lại. Bạn muốn đăng xuất không?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Đăng xuất", (dialog, which) -> logout())
                .show();
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        LoginManager.getInstance().logOut();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInClient.signOut().addOnCompleteListener(task -> {
            UserSession.clearCurrentUser(this);
            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, activity_login.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void startBackupProcess() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "SmartWallet_Backup_" + timeStamp + ".db";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        backupLauncher.launch(intent);
    }

    private void handleBackup(Uri targetUri) {
        try {
            File dbFile = getDatabasePath(DB_NAME);
            if (!dbFile.exists()) {
                Toast.makeText(this, "Chưa có dữ liệu để sao lưu", Toast.LENGTH_SHORT).show();
                return;
            }

            try (InputStream in = new FileInputStream(dbFile);
                 OutputStream out = getContentResolver().openOutputStream(targetUri)) {
                
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                Toast.makeText(this, "Sao lưu thành công!", Toast.LENGTH_LONG).show();
                DatabaseHelper dbHelper = new DatabaseHelper(this);
                dbHelper.insertMessage(
                        "Sao lưu thành công ✅",
                        "Dữ liệu đã được sao lưu an toàn.",
                        new SimpleDateFormat("dd/MM/yyyy HH:mm",
                                Locale.getDefault()).format(new Date())
                );
            }
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi sao lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startRestoreProcess() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        restoreLauncher.launch(intent);
    }

    private void handleRestore(Uri sourceUri) {
        try {
            File dbFile = getDatabasePath(DB_NAME);
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }

            try (InputStream in = getContentResolver().openInputStream(sourceUri);
                 OutputStream out = new FileOutputStream(dbFile)) {
                
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                
                Toast.makeText(this, "Khôi phục thành công! Hãy khởi động lại app.", Toast.LENGTH_LONG).show();
                DatabaseHelper dbHelper = new DatabaseHelper(this);
                dbHelper.insertMessage(
                        "Khôi phục dữ liệu ♻️",
                        "Dữ liệu đã được khôi phục thành công.",
                        new SimpleDateFormat("dd/MM/yyyy HH:mm",
                                Locale.getDefault()).format(new Date())
                );
                
                new android.os.Handler().postDelayed(() -> {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    Runtime.getRuntime().exit(0);
                }, 2000);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi khôi phục: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void scheduleDailyReminder() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                100,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 30);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
    }
    private void cancelReminder() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                100,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
    }
}
