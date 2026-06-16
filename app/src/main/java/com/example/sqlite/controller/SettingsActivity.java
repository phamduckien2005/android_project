package com.example.sqlite.controller;

import com.example.sqlite.R;
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

import com.example.sqlite.entity.Transaction;
import com.example.sqlite.repository.DatabaseHelper;
import com.example.sqlite.service.UserSession;
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
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchDarkMode;
    private SwitchCompat switchReminder;
    private static final String PREF_NAME = "app_settings";
    private static final String KEY_REMINDER = "reminder_enabled";
    private static final String DB_NAME = "SmartWallet.db";
    private String pendingExcelReportContent = "";

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

    private final ActivityResultLauncher<Intent> exportExcelLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handleExportExcel(result.getData().getData());
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
        findViewById(R.id.btn_export_excel).setOnClickListener(v -> startExportExcelProcess());
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

    private void startExportExcelProcess() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        List<Transaction> transactions = dbHelper.getFilteredTransactions(0, Long.MAX_VALUE);
        if (transactions.isEmpty()) {
            Toast.makeText(this, "Chưa có giao dịch để xuất báo cáo", Toast.LENGTH_SHORT).show();
            return;
        }

        pendingExcelReportContent = buildExcelReport(transactions, dbHelper);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "SmartWallet_Report_" + timeStamp + ".xls";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.ms-excel");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        exportExcelLauncher.launch(intent);
    }

    private void handleExportExcel(Uri targetUri) {
        if (targetUri == null || pendingExcelReportContent.isEmpty()) {
            Toast.makeText(this, "Không thể tạo báo cáo Excel", Toast.LENGTH_SHORT).show();
            return;
        }

        try (OutputStream out = getContentResolver().openOutputStream(targetUri)) {
            if (out == null) {
                throw new IllegalStateException("Không mở được file đích");
            }
            out.write("\uFEFF".getBytes(StandardCharsets.UTF_8));
            out.write(pendingExcelReportContent.getBytes(StandardCharsets.UTF_8));
            pendingExcelReportContent = "";

            DatabaseHelper dbHelper = new DatabaseHelper(this);
            dbHelper.insertMessage(
                    "Xuất báo cáo Excel thành công",
                    "Báo cáo thu chi đã được xuất ra file Excel.",
                    new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date())
            );
            Toast.makeText(this, "Đã xuất báo cáo Excel thành công", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xuất Excel: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String buildExcelReport(List<Transaction> transactions, DatabaseHelper dbHelper) {
        double totalIncome = 0;
        double totalExpense = 0;
        for (Transaction transaction : transactions) {
            if (transaction.isHeader) continue;
            if (transaction.isExpense) {
                totalExpense += transaction.amount;
            } else {
                totalIncome += transaction.amount;
            }
        }

        Calendar startMonth = Calendar.getInstance();
        startMonth.set(Calendar.DAY_OF_MONTH, 1);
        startMonth.set(Calendar.HOUR_OF_DAY, 0);
        startMonth.set(Calendar.MINUTE, 0);
        startMonth.set(Calendar.SECOND, 0);
        startMonth.set(Calendar.MILLISECOND, 0);

        Calendar endMonth = Calendar.getInstance();
        endMonth.set(Calendar.DAY_OF_MONTH, endMonth.getActualMaximum(Calendar.DAY_OF_MONTH));
        endMonth.set(Calendar.HOUR_OF_DAY, 23);
        endMonth.set(Calendar.MINUTE, 59);
        endMonth.set(Calendar.SECOND, 59);
        endMonth.set(Calendar.MILLISECOND, 999);

        double monthIncome = dbHelper.getTotalIncome(startMonth.getTimeInMillis(), endMonth.getTimeInMillis());
        double monthExpense = dbHelper.getTotalExpense(startMonth.getTimeInMillis(), endMonth.getTimeInMillis());
        Map<String, Double> expenseStats = dbHelper.getSpendingStats(0, Long.MAX_VALUE);
        Map<String, Double> incomeStats = dbHelper.getIncomeStats(0, Long.MAX_VALUE);

        String reportTime = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        String displayName = UserSession.getDisplayName(this);

        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset=\"UTF-8\">")
                .append("<style>")
                .append("body{font-family:Arial,sans-serif;} table{border-collapse:collapse;margin-bottom:18px;}")
                .append("th{background:#58A9F7;color:white;font-weight:bold;} th,td{border:1px solid #9CB7D0;padding:8px;}")
                .append(".title{font-size:20px;font-weight:bold;color:#243447;} .section{font-size:15px;font-weight:bold;color:#237ED8;}")
                .append(".income{color:#2E7D32;font-weight:bold;} .expense{color:#D32F2F;font-weight:bold;}")
                .append("</style></head><body>");

        html.append("<div class=\"title\">Báo cáo thu chi Smart Wallet</div>")
                .append("<p>Người dùng: ").append(escapeHtml(displayName)).append("</p>")
                .append("<p>Thời gian xuất: ").append(reportTime).append("</p>");

        html.append("<div class=\"section\">Tổng quan toàn bộ dữ liệu</div>")
                .append("<table>")
                .append("<tr><th>Chỉ số</th><th>Giá trị</th></tr>")
                .append("<tr><td>Tổng thu</td><td class=\"income\">").append(formatMoney(totalIncome)).append("</td></tr>")
                .append("<tr><td>Tổng chi</td><td class=\"expense\">").append(formatMoney(totalExpense)).append("</td></tr>")
                .append("<tr><td>Số dư</td><td>").append(formatMoney(totalIncome - totalExpense)).append("</td></tr>")
                .append("<tr><td>Số giao dịch</td><td>").append(transactions.size()).append("</td></tr>")
                .append("</table>");

        html.append("<div class=\"section\">Tổng quan tháng hiện tại</div>")
                .append("<table>")
                .append("<tr><th>Tháng</th><th>Thu</th><th>Chi</th><th>Cân đối</th></tr>")
                .append("<tr><td>").append(new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date()))
                .append("</td><td class=\"income\">").append(formatMoney(monthIncome))
                .append("</td><td class=\"expense\">").append(formatMoney(monthExpense))
                .append("</td><td>").append(formatMoney(monthIncome - monthExpense)).append("</td></tr>")
                .append("</table>");

        appendStatsTable(html, "Chi tiêu theo danh mục", expenseStats, true);
        appendStatsTable(html, "Thu nhập theo danh mục", incomeStats, false);

        html.append("<div class=\"section\">Lịch sử giao dịch</div>")
                .append("<table>")
                .append("<tr><th>STT</th><th>Ngày giờ</th><th>Tên giao dịch</th><th>Danh mục</th><th>Loại</th><th>Số tiền</th></tr>");
        int index = 1;
        for (Transaction transaction : transactions) {
            if (transaction.isHeader) continue;
            html.append("<tr>")
                    .append("<td>").append(index++).append("</td>")
                    .append("<td>").append(escapeHtml(transaction.time)).append("</td>")
                    .append("<td>").append(escapeHtml(transaction.title)).append("</td>")
                    .append("<td>").append(escapeHtml(transaction.category == null ? transaction.title : transaction.category)).append("</td>")
                    .append("<td>").append(transaction.isExpense ? "Chi" : "Thu").append("</td>")
                    .append("<td class=\"").append(transaction.isExpense ? "expense" : "income").append("\">")
                    .append(formatMoney(transaction.amount)).append("</td>")
                    .append("</tr>");
        }
        html.append("</table></body></html>");
        return html.toString();
    }

    private void appendStatsTable(StringBuilder html, String title, Map<String, Double> stats, boolean isExpense) {
        html.append("<div class=\"section\">").append(title).append("</div>")
                .append("<table>")
                .append("<tr><th>Danh mục</th><th>Tổng tiền</th></tr>");
        if (stats == null || stats.isEmpty()) {
            html.append("<tr><td colspan=\"2\">Không có dữ liệu</td></tr>");
        } else {
            for (Map.Entry<String, Double> entry : stats.entrySet()) {
                html.append("<tr><td>").append(escapeHtml(entry.getKey())).append("</td><td class=\"")
                        .append(isExpense ? "expense" : "income").append("\">")
                        .append(formatMoney(entry.getValue())).append("</td></tr>");
            }
        }
        html.append("</table>");
    }

    private String formatMoney(double amount) {
        return String.format(Locale.getDefault(), "%,.0f đ", amount);
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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

