package com.example.sqlite.controller;

import com.example.sqlite.R;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sqlite.entity.Transaction;
import com.example.sqlite.repository.DatabaseHelper;
import com.example.sqlite.service.UserSession;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextView tvSummaryIncome, tvSummaryExpense, tvSummaryBalance;
    private TextView tvHomeGreeting;
    private TextView tvStudentTip, tvBudgetPercent, tvBudgetDesc;
    private ProgressBar pbBudget;
    private LinearLayout layoutBudgetOverview;
    private View cardTip;
    private ImageView ivTipIcon;
    private RecyclerView rvMainList;
    private BottomNavigationView bottomNavigationView;
    private ImageView btnToggleVisibility, btnMailbox;
    private Toolbar toolbar;
    private DatabaseHelper dbHelper;
    private boolean isAmountVisible = true;
    private double currentIncome = 0, currentExpense = 0, currentBalance = 0;

    private long filterStartDate = 0, filterEndDate = Long.MAX_VALUE;
    private String lastAlertType = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        dbHelper = new DatabaseHelper(this);

        initViews();
        setupToolbar();
        handleEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_home);
        tvHomeGreeting = findViewById(R.id.tv_home_greeting);
        tvSummaryIncome = findViewById(R.id.tv_summary_income);
        tvSummaryExpense = findViewById(R.id.tv_summary_expense);
        tvSummaryBalance = findViewById(R.id.tv_summary_balance);

        tvStudentTip = findViewById(R.id.tv_student_tip);
        tvBudgetPercent = findViewById(R.id.tv_budget_percent);
        tvBudgetDesc = findViewById(R.id.tv_budget_desc);
        layoutBudgetOverview = findViewById(R.id.layout_budget_overview);
        pbBudget = findViewById(R.id.pb_budget_main);
        cardTip = findViewById(R.id.card_tip);
        ivTipIcon = findViewById(R.id.iv_tip_icon);

        rvMainList = findViewById(R.id.rv_main_list);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        btnToggleVisibility = findViewById(R.id.btn_toggle_visibility);
        btnMailbox = findViewById(R.id.btn_mailbox);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadData() {
        tvHomeGreeting.setText("Xin chào bạn, " + UserSession.getDisplayName(this));
        List<Transaction> transactions = dbHelper.getFilteredTransactions(filterStartDate, filterEndDate);

        currentIncome = dbHelper.getTotalIncome(filterStartDate, filterEndDate);
        currentExpense = dbHelper.getTotalExpense(filterStartDate, filterEndDate);
        currentBalance = currentIncome - currentExpense;

        updateAmountDisplay();
        updateGenZTips();

        TransactionAdapter adapter = new TransactionAdapter(transactions);
        adapter.setOnItemLongClickListener((transaction, position) -> {
            if (transaction.isHeader) return;
            String[] options = {"Sửa giao dịch", "Xóa giao dịch"};
            new AlertDialog.Builder(this)
                    .setTitle("Tùy chọn")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            editTransaction(transaction);
                        } else if (which == 1) {
                            confirmDelete(transaction.id);
                        }
                    })
                    .show();
        });

        rvMainList.setLayoutManager(new LinearLayoutManager(this));
        rvMainList.setAdapter(adapter);
    }

    private void editTransaction(Transaction transaction) {
        Intent intent = new Intent(this, AddTransactionActivity.class);
        intent.putExtra("isEdit", true);
        intent.putExtra("id", transaction.id);
        intent.putExtra("title", transaction.title);
        intent.putExtra("amount", transaction.amount);
        intent.putExtra("isExpense", transaction.isExpense);
        intent.putExtra("timestamp", transaction.timestamp);
        startActivity(intent);
    }

    private void updateGenZTips() {

        if (tvStudentTip == null) return;

        double walletLimit = dbHelper.getTotalBudgetLimit();
        double walletSpent = dbHelper.getTotalBudgetSpentSinceReset();
        int usagePercent = walletLimit > 0 ? (int) ((walletSpent / walletLimit) * 100) : 0;
        if (usagePercent > 100) usagePercent = 100;

        if (pbBudget != null) pbBudget.setProgress(usagePercent);
        if (tvBudgetPercent != null) tvBudgetPercent.setText(usagePercent + "%");
        if (tvBudgetDesc != null) {
            if (walletLimit > 0) {
                tvBudgetDesc.setText(String.format(
                    Locale.getDefault(),
                    "Đã tiêu: %,.0f / %,.0f đ",
                    walletSpent,
                    walletLimit
            ));
            } else {
                tvBudgetDesc.setText("Chưa đặt hạn mức ví. Bấm Ví để thiết lập.");
            }
        }
        renderBudgetOverview();

        String tip;
        int color;
        String newAlertType = "NORMAL";

        if (currentBalance < 0) {
            tip = "Báo động đỏ! Ví đang 'thở oxy' rồi, ngừng chốt đơn ngay!!! 💀";
            color = Color.parseColor("#D32F2F");
            newAlertType = "NEGATIVE";

        } else if (walletLimit > 0 && usagePercent > 80) {
            tip = "Ăn mì tôm thôi chứ đợi gì nữa? Sắp hết tiền rồi bạn ơi! 🍜";
            color = Color.parseColor("#E65100");
            newAlertType = "OVER_80";

        } else if (walletLimit > 0 && usagePercent > 50) {
            tip = "Tiền trôi hơi nhanh nha. Tém tém lại kẻo cuối tháng húp không khí! 👀";
            color = Color.parseColor("#0277BD");
            newAlertType = "OVER_50";

        } else if (currentIncome > 0 && currentExpense == 0) {
            tip = "Vừa có lúa về hả? Đừng tiêu hoang đó, tiết kiệm đi nha! 🌿";
            color = Color.parseColor("#388E3C");
            newAlertType = "INCOME_ONLY";

        } else {
            tip = "Ví vẫn ổn, quản lý tiền rất 'chill'. Cứ thế phát huy nha! ✨";
            color = Color.parseColor("#F57F17");
        }

        // ===== So sánh với trạng thái cũ =====
        SharedPreferences prefs = getSharedPreferences("alert_pref", MODE_PRIVATE);
        String alertKey = "last_alert_" + UserSession.getCurrentUserId(this);
        String lastAlert = prefs.getString(alertKey, "NORMAL");

        if (!newAlertType.equals(lastAlert)) {

            if (!newAlertType.equals("NORMAL")) {
                saveMessage(tip, "Trạng thái tài chính của bạn vừa thay đổi.");
            }

            prefs.edit().putString(alertKey, newAlertType).apply();
        }

        // ===== Update UI =====
        tvStudentTip.setText(tip);
        tvStudentTip.setTextColor(color);

        if (cardTip != null)
            cardTip.setBackgroundColor(lightenColor(color));

        if (ivTipIcon != null)
            ivTipIcon.setColorFilter(color);
    }

    private void renderBudgetOverview() {
        if (layoutBudgetOverview == null) return;

        layoutBudgetOverview.removeAllViews();
        Map<String, Double> budgets = dbHelper.getBudgetLimits();
        long resetAt = dbHelper.getWalletResetAt();
        boolean hasBudget = false;

        for (String category : WalletActivity.CATEGORIES) {
            Double limitValue = budgets.get(category);
            if (limitValue == null || limitValue <= 0) continue;

            hasBudget = true;
            double limit = limitValue;
            double spent = dbHelper.getCategoryExpenseSince(category, resetAt);
            double remaining = limit - spent;
            int percent = (int) Math.min(100, (spent / limit) * 100);
            int color = remaining < 0 ? Color.parseColor("#D32F2F") : Color.parseColor("#1976D2");

            LinearLayout itemBox = new LinearLayout(this);
            itemBox.setOrientation(LinearLayout.VERTICAL);
            itemBox.setPadding(18, 16, 18, 16);
            itemBox.setBackgroundResource(R.drawable.bg_auth_input);
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            itemParams.setMargins(0, 0, 0, 12);
            layoutBudgetOverview.addView(itemBox, itemParams);

            TextView title = new TextView(this);
            title.setText(category);
            title.setTextColor(Color.parseColor("#111827"));
            title.setTextSize(16);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            itemBox.addView(title);

            TextView detail = new TextView(this);
            detail.setText(String.format(
                    Locale.getDefault(),
                    "Đã chi %,.0f / %,.0f đ",
                    spent,
                    limit
            ));
            detail.setTextColor(color);
            detail.setTextSize(14);
            detail.setPadding(0, 8, 0, 4);
            itemBox.addView(detail);

            TextView remainingView = new TextView(this);
            remainingView.setText(String.format(Locale.getDefault(), "Còn lại: %,.0f đ", Math.max(0, remaining)));
            remainingView.setTextColor(Color.parseColor("#4B5563"));
            remainingView.setTextSize(13);
            remainingView.setPadding(0, 0, 0, 8);
            itemBox.addView(remainingView);

            ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progressBar.setMax(100);
            progressBar.setProgress(percent);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
            }
            itemBox.addView(progressBar, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    8
            ));
        }

        if (!hasBudget) {
            TextView empty = new TextView(this);
            empty.setText("Chưa có danh mục nào được đặt hạn mức.");
            empty.setTextColor(Color.parseColor("#6B7280"));
            empty.setTextSize(12);
            layoutBudgetOverview.addView(empty);
        }
    }

    private int lightenColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = 0.1f;
        hsv[2] = 0.95f;
        return Color.HSVToColor(hsv);
    }

    private void updateAmountDisplay() {
        if (isAmountVisible) {
            tvSummaryIncome.setText(String.format(Locale.getDefault(), "%,.0f đ", currentIncome));
            tvSummaryExpense.setText(String.format(Locale.getDefault(), "%,.0f đ", currentExpense));
            tvSummaryBalance.setText(String.format(Locale.getDefault(), "%,.0f đ", currentBalance));
            btnToggleVisibility.setImageResource(android.R.drawable.ic_menu_view);
        } else {
            tvSummaryIncome.setText("****");
            tvSummaryExpense.setText("****");
            tvSummaryBalance.setText("****");
            btnToggleVisibility.setImageResource(android.R.drawable.button_onoff_indicator_off);
        }
    }

    private void confirmDelete(int transactionId) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn chắc chắn muốn xóa giao dịch này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    dbHelper.deleteTransaction(transactionId);
                    loadData();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void handleEvents() {
        btnToggleVisibility.setOnClickListener(v -> {
            isAmountVisible = !isAmountVisible;
            updateAmountDisplay();
            updateGenZTips();
        });

        btnMailbox.setOnClickListener(v -> startActivity(new Intent(this, MessageBoxActivity.class)));

        findViewById(R.id.btn_account_book).setOnClickListener(v -> startActivity(new Intent(MainActivity.this, HistoryActivity.class)));
        findViewById(R.id.btn_add_top).setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AddTransactionActivity.class)));
        findViewById(R.id.btn_wallet).setOnClickListener(v -> startActivity(new Intent(this, WalletActivity.class)));
        findViewById(R.id.btn_view_all).setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        findViewById(R.id.fab_ai_chat).setOnClickListener(v -> startActivity(new Intent(this, AiReceiptActivity.class)));

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class));
            } else if (id == R.id.nav_report) {
                startActivity(new Intent(this, ReportActivity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            }
            return true;
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            filterStartDate = data.getLongExtra("startDate", 0);
            filterEndDate = data.getLongExtra("endDate", Long.MAX_VALUE);
            loadData();
        }
    }

    private void saveMessage(String title, String content) {

        dbHelper.insertMessage(
                title,
                content,
                new java.text.SimpleDateFormat(
                        "dd/MM/yyyy HH:mm",
                        java.util.Locale.getDefault()
                ).format(new java.util.Date())
        );
    }
}

