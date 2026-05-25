package com.example.sqlite;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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

        for (String category : CATEGORIES) {
            double limit = budgets.containsKey(category) ? budgets.get(category) : 0;
            double spent = dbHelper.getCategoryExpenseSince(category, resetAt);
            double remaining = Math.max(0, limit - spent);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(18, 16, 18, 16);
            row.setBackgroundResource(R.drawable.bg_auth_card);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowParams.setMargins(0, 0, 0, 12);
            layoutCategories.addView(row, rowParams);

            TextView title = new TextView(this);
            title.setText(category);
            title.setTextSize(16);
            title.setTextColor(getColorCompat(R.color.text_main));
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(title);

            TextView usage = new TextView(this);
            usage.setText(String.format(Locale.getDefault(), "Đã chi: %,.0f đ | Còn lại: %,.0f đ", spent, remaining));
            usage.setTextColor(getColorCompat(R.color.text_sub));
            usage.setTextSize(13);
            row.addView(usage);

            EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setHint("Hạn mức tối đa");
            input.setGravity(Gravity.CENTER_VERTICAL);
            input.setSingleLine(true);
            input.setTextColor(getColorCompat(R.color.text_main));
            input.setHintTextColor(getColorCompat(R.color.text_sub));
            if (limit > 0) {
                input.setText(String.format(Locale.US, "%.0f", limit));
            }
            row.addView(input, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            budgetInputs.put(category, input);
        }
    }

    private int getColorCompat(int colorRes) {
        return androidx.core.content.ContextCompat.getColor(this, colorRes);
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
                .setMessage("Bạn muốn làm mới lại ví này?")
                .setPositiveButton("Có", (dialog, which) -> {
                    dbHelper.resetWallet();
                    Toast.makeText(this, "Đã làm mới ví", Toast.LENGTH_SHORT).show();
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
