package com.example.sqlite.controller;

import com.example.sqlite.R;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.sqlite.repository.DatabaseHelper;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class WalletActivity extends AppCompatActivity {
    public static final String[] CATEGORIES = {
            "Ăn uống", "Di chuyển", "Mua sắm", "Sắc đẹp", "Ăn vặt",
            "Học tập", "Giải trí", "Tiền nhà", "Sức khỏe", "Tiền điện",
            "Tiền nước", "Internet", "Quà tặng", "Khác"
    };

    private DatabaseHelper dbHelper;
    private LinearLayout layoutCategories;
    private final Map<String, EditText> budgetInputs = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        dbHelper = new DatabaseHelper(this);
        layoutCategories = findViewById(R.id.layout_wallet_categories);
        setupToolbar();
        renderCategories();
        handleEvents();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_wallet);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void renderCategories() {
        Map<String, Double> budgets = dbHelper.getBudgetLimits();
        long resetAt = dbHelper.getWalletResetAt();
        layoutCategories.removeAllViews();
        budgetInputs.clear();

        for (int i = 0; i < CATEGORIES.length; i++) {
            String category = CATEGORIES[i];
            double limit = budgets.containsKey(category) ? budgets.get(category) : 0;
            double spent = dbHelper.getCategoryExpenseSince(category, resetAt);
            double remaining = Math.max(0, limit - spent);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(16), dp(16), dp(16), dp(16));
            row.setBackground(roundedRect("#F8FCFF", 18, "#D6E8FA", 1));

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowParams.setMargins(0, 0, 0, dp(16));
            layoutCategories.addView(row, rowParams);

            LinearLayout titleRow = new LinearLayout(this);
            titleRow.setGravity(Gravity.CENTER_VERTICAL);
            titleRow.setOrientation(LinearLayout.HORIZONTAL);
            row.addView(titleRow, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            TextView badge = new TextView(this);
            badge.setText(category.substring(0, 1));
            badge.setGravity(Gravity.CENTER);
            badge.setTextColor(Color.WHITE);
            badge.setTextSize(16);
            badge.setTypeface(null, Typeface.BOLD);
            badge.setBackground(roundedRect("#58A9F7", 16));
            LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(dp(38), dp(38));
            badgeParams.setMargins(0, 0, dp(12), 0);
            titleRow.addView(badge, badgeParams);

            LinearLayout textBox = new LinearLayout(this);
            textBox.setOrientation(LinearLayout.VERTICAL);
            titleRow.addView(textBox, new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1
            ));

            TextView title = new TextView(this);
            title.setText(category);
            title.setTextSize(16);
            title.setTextColor(getColorCompat(R.color.text_main));
            title.setTypeface(null, Typeface.BOLD);
            textBox.addView(title);

            TextView usage = new TextView(this);
            usage.setText(String.format(Locale.getDefault(), "Đã chi: %,.0f đ | Còn lại: %,.0f đ", spent, remaining));
            usage.setTextColor(getColorCompat(R.color.text_sub));
            usage.setTextSize(13);
            textBox.addView(usage);

            View divider = new View(this);
            divider.setBackgroundColor(Color.parseColor("#E1EDF8"));
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(1)
            );
            dividerParams.setMargins(0, dp(14), 0, dp(12));
            row.addView(divider, dividerParams);

            TextView inputLabel = new TextView(this);
            inputLabel.setText("Hạn mức tối đa");
            inputLabel.setTextColor(getColorCompat(R.color.text_sub));
            inputLabel.setTextSize(12);
            inputLabel.setTypeface(null, Typeface.BOLD);
            row.addView(inputLabel);

            EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setHint("Nhập số tiền cho mục này");
            input.setGravity(Gravity.CENTER_VERTICAL);
            input.setSingleLine(true);
            input.setMinHeight(dp(48));
            input.setPadding(dp(14), 0, dp(14), 0);
            input.setBackgroundResource(R.drawable.bg_wallet_input);
            input.setTextColor(getColorCompat(R.color.text_main));
            input.setHintTextColor(getColorCompat(R.color.text_sub));
            if (limit > 0) {
                input.setText(String.format(Locale.US, "%.0f", limit));
            }
            LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            inputParams.setMargins(0, dp(6), 0, 0);
            row.addView(input, inputParams);
            budgetInputs.put(category, input);
        }
    }

    private int getColorCompat(int colorRes) {
        return androidx.core.content.ContextCompat.getColor(this, colorRes);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable roundedRect(String fillColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor(fillColor));
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private GradientDrawable roundedRect(String fillColor, int radiusDp, String strokeColor, int strokeDp) {
        GradientDrawable drawable = roundedRect(fillColor, radiusDp);
        drawable.setStroke(dp(strokeDp), Color.parseColor(strokeColor));
        return drawable;
    }

    private void handleEvents() {
        Button btnSave = findViewById(R.id.btn_save_wallet);
        Button btnReset = findViewById(R.id.btn_reset_wallet);
        Button btnDeleteAll = findViewById(R.id.btn_delete_all_transactions);

        btnSave.setOnClickListener(v -> saveBudgets());
        btnReset.setOnClickListener(v -> confirmResetWallet());
        btnDeleteAll.setOnClickListener(v -> confirmDeleteAllTransactions());
    }

    private void saveBudgets() {
        for (Map.Entry<String, EditText> entry : budgetInputs.entrySet()) {
            String text = entry.getValue().getText().toString().trim();
            double limit;
            try {
                limit = text.isEmpty() ? 0 : Double.parseDouble(text);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Hạn mức của " + entry.getKey() + " không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }
            dbHelper.saveBudgetLimit(entry.getKey(), limit);
        }
        Toast.makeText(this, "Đã lưu ví chi tiêu", Toast.LENGTH_SHORT).show();
        renderCategories();
    }

    private void confirmResetWallet() {
        new AlertDialog.Builder(this)
                .setTitle("Làm mới ví")
                .setMessage("Bạn muốn đưa toàn bộ hạn mức danh mục về 0?")
                .setPositiveButton("Có", (dialog, which) -> {
                    dbHelper.resetWallet();
                    Toast.makeText(this, "Đã làm mới ví và đưa hạn mức về 0", Toast.LENGTH_SHORT).show();
                    renderCategories();
                })
                .setNegativeButton("Không", null)
                .show();
    }

    private void confirmDeleteAllTransactions() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa lịch sử giao dịch")
                .setMessage("Bạn muốn xóa tất cả giao dịch và làm mới lại từ đầu?")
                .setPositiveButton("Có", (dialog, which) -> {
                    dbHelper.deleteAllTransactions();
                    dbHelper.resetWallet();
                    Toast.makeText(this, "Đã xóa toàn bộ lịch sử giao dịch", Toast.LENGTH_SHORT).show();
                    renderCategories();
                })
                .setNegativeButton("Không", null)
                .show();
    }
}

