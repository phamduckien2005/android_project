package com.example.sqlite;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddTransactionActivity extends AppCompatActivity {

    private EditText edtAmount;
    private TextView tvTransactionDate;
    private Button btnAddThousand, btnAddMillion, btnSave;
    private RecyclerView rvCategories;
    private TabLayout tabLayout;
    private Toolbar toolbar;
    private String selectedCategory = "Khác";
    private boolean isExpense = true;
    private DatabaseHelper dbHelper;
    private Calendar selectedCalendar;
    
    private boolean isEditMode = false;
    private int transactionId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        dbHelper = new DatabaseHelper(this);
        selectedCalendar = Calendar.getInstance();
        
        initViews();
        setupToolbar();
        setupCategories();
        checkEditMode();
        handleEvents();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_add);
        edtAmount = findViewById(R.id.edtAmount);
        tvTransactionDate = findViewById(R.id.tv_transaction_date);
        btnAddThousand = findViewById(R.id.btnAddThousand);
        btnAddMillion = findViewById(R.id.btnAddMillion);
        btnSave = findViewById(R.id.btnSave);
        rvCategories = findViewById(R.id.recyclerViewCategories);
        tabLayout = findViewById(R.id.tabLayout);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupCategories() {
        List<CategoryAdapter.Category> categories = new ArrayList<>();
        categories.add(new CategoryAdapter.Category("Ăn uống", android.R.drawable.ic_menu_today, "#FF7043"));
        categories.add(new CategoryAdapter.Category("Di chuyển", android.R.drawable.ic_menu_directions, "#42A5F5"));
        categories.add(new CategoryAdapter.Category("Mua sắm", android.R.drawable.ic_menu_save, "#AB47BC"));
        categories.add(new CategoryAdapter.Category("Sắc đẹp", android.R.drawable.ic_menu_camera, "#EC407A"));
        categories.add(new CategoryAdapter.Category("Ăn vặt", android.R.drawable.ic_menu_view, "#FFA726"));
        categories.add(new CategoryAdapter.Category("Học tập", android.R.drawable.ic_menu_edit, "#66BB6A"));
        categories.add(new CategoryAdapter.Category("Giải trí", android.R.drawable.ic_menu_slideshow, "#FFCA28"));
        categories.add(new CategoryAdapter.Category("Tiền nhà", android.R.drawable.ic_menu_myplaces, "#26A69A"));
        categories.add(new CategoryAdapter.Category("Sức khỏe", android.R.drawable.ic_menu_add, "#EF5350"));
        categories.add(new CategoryAdapter.Category("Tiền điện", android.R.drawable.ic_menu_info_details, "#5C6BC0"));
        categories.add(new CategoryAdapter.Category("Tiền nước", android.R.drawable.ic_menu_gallery, "#29B6F6"));
        categories.add(new CategoryAdapter.Category("Internet", android.R.drawable.ic_menu_share, "#78909C"));
        categories.add(new CategoryAdapter.Category("Quà tặng", android.R.drawable.ic_menu_send, "#8D6E63"));
        categories.add(new CategoryAdapter.Category("Lương", android.R.drawable.ic_menu_month, "#9CCC65"));
        categories.add(new CategoryAdapter.Category("Khác", android.R.drawable.ic_menu_help, "#BDBDBD"));

        CategoryAdapter categoryAdapter = new CategoryAdapter(categories, category -> selectedCategory = category.name);
        rvCategories.setLayoutManager(new GridLayoutManager(this, 4));
        rvCategories.setAdapter(categoryAdapter);
    }

    private void checkEditMode() {
        if (getIntent().getBooleanExtra("isEdit", false)) {
            isEditMode = true;
            transactionId = getIntent().getIntExtra("id", -1);
            String title = getIntent().getStringExtra("title");
            double amount = getIntent().getDoubleExtra("amount", 0);
            isExpense = getIntent().getBooleanExtra("isExpense", true);
            long timestamp = getIntent().getLongExtra("timestamp", System.currentTimeMillis());

            selectedCalendar.setTimeInMillis(timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvTransactionDate.setText(sdf.format(selectedCalendar.getTime()));
            
            edtAmount.setText(String.valueOf((long)amount));
            selectedCategory = title; 
            
            if (isExpense) {
                if (tabLayout.getTabAt(0) != null) tabLayout.getTabAt(0).select();
            } else {
                if (tabLayout.getTabAt(1) != null) tabLayout.getTabAt(1).select();
            }
            
            // Cập nhật tiêu đề toolbar bằng cách tìm TextView trong các con của Toolbar
            for (int i = 0; i < toolbar.getChildCount(); i++) {
                View child = toolbar.getChildAt(i);
                if (child instanceof TextView) {
                    ((TextView) child).setText("Sửa giao dịch");
                    break;
                }
            }
            btnSave.setText("CẬP NHẬT");
        }
    }

    private void handleEvents() {
        findViewById(R.id.layout_select_date).setOnClickListener(v -> new DatePickerDialog(this, R.style.Theme_Sqlite, (view, year, month, dayOfMonth) -> {
            selectedCalendar.set(year, month, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvTransactionDate.setText(sdf.format(selectedCalendar.getTime()));
        }, selectedCalendar.get(Calendar.YEAR), selectedCalendar.get(Calendar.MONTH), selectedCalendar.get(Calendar.DAY_OF_MONTH)).show());

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isExpense = tab.getPosition() == 0;
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnAddThousand.setOnClickListener(v -> appendZeros("000"));
        btnAddMillion.setOnClickListener(v -> appendZeros("000000"));

        btnSave.setOnClickListener(v -> {
            String amountStr = edtAmount.getText().toString();
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Nhập số tiền", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);
            String timeDisplay = new SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault()).format(selectedCalendar.getTime());
            long timestamp = selectedCalendar.getTimeInMillis();
            
            if (isEditMode) {
                dbHelper.updateTransaction(transactionId, selectedCategory, amount, timeDisplay, selectedCategory, isExpense, timestamp);
                Toast.makeText(this, "Đã cập nhật giao dịch!", Toast.LENGTH_SHORT).show();
            } else {
                dbHelper.addTransaction(selectedCategory, amount, timeDisplay, selectedCategory, isExpense, timestamp);
                Toast.makeText(this, "Đã lưu giao dịch!", Toast.LENGTH_SHORT).show();
            }
            finish();
        });
    }

    private void appendZeros(String zeros) {
        String current = edtAmount.getText().toString();
        if (current.isEmpty()) edtAmount.setText("1" + zeros);
        else edtAmount.setText(current + zeros);
        edtAmount.setSelection(edtAmount.getText().length());
    }
}