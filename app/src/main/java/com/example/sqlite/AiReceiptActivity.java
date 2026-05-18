package com.example.sqlite;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AiReceiptActivity extends AppCompatActivity {
    private ImageView ivReceiptPreview;
    private TextView tvAiStatus;
    private EditText edtAiTitle, edtAiAmount, edtAiCategory;
    private ProgressBar progressAi;
    private Bitmap selectedBitmap;
    private DatabaseHelper dbHelper;
    private boolean currentReceiptSaved = false;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Void> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    selectedBitmap = bitmap;
                    currentReceiptSaved = false;
                    ivReceiptPreview.setImageBitmap(bitmap);
                    tvAiStatus.setText("Ảnh đã sẵn sàng để AI phân tích.");
                }
            });

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    loadBitmapFromUri(uri);
                }
            });

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    takePictureLauncher.launch(null);
                } else {
                    Toast.makeText(this, "Cần quyền camera để chụp hóa đơn", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_receipt);

        dbHelper = new DatabaseHelper(this);
        initViews();
        setupToolbar();
        handleEvents();
    }

    private void initViews() {
        ivReceiptPreview = findViewById(R.id.iv_receipt_preview);
        tvAiStatus = findViewById(R.id.tv_ai_status);
        edtAiTitle = findViewById(R.id.edt_ai_title);
        edtAiAmount = findViewById(R.id.edt_ai_amount);
        edtAiCategory = findViewById(R.id.edt_ai_category);
        progressAi = findViewById(R.id.progress_ai);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_ai_receipt);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void handleEvents() {
        findViewById(R.id.btn_take_receipt_photo).setOnClickListener(v -> openCamera());
        findViewById(R.id.btn_pick_receipt_photo).setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        findViewById(R.id.btn_analyze_receipt).setOnClickListener(v -> analyzeReceipt());
        findViewById(R.id.btn_save_ai_transaction).setOnClickListener(v -> saveTransactionFromFields());
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            takePictureLauncher.launch(null);
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void loadBitmapFromUri(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            selectedBitmap = BitmapFactory.decodeStream(inputStream);
            currentReceiptSaved = false;
            ivReceiptPreview.setImageBitmap(selectedBitmap);
            tvAiStatus.setText("Ảnh đã sẵn sàng để AI phân tích.");
        } catch (Exception e) {
            Toast.makeText(this, "Không đọc được ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void analyzeReceipt() {
        if (selectedBitmap == null) {
            Toast.makeText(this, "Hãy chụp hoặc chọn ảnh hóa đơn trước", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        tvAiStatus.setText("AI đang đọc hóa đơn...");

        executorService.execute(() -> {
            try {
                GeminiReceiptService.ReceiptResult result = new GeminiReceiptService().analyzeReceipt(selectedBitmap);
                runOnUiThread(() -> {
                    setLoading(false);
                    fillReceiptFields(result);
                    saveTransaction(result);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    tvAiStatus.setText(e.getMessage());
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void fillReceiptFields(GeminiReceiptService.ReceiptResult result) {
        edtAiTitle.setText(result.title);
        edtAiAmount.setText(String.format(Locale.US, "%.0f", result.amount));
        edtAiCategory.setText(result.category);
    }

    private void saveTransactionFromFields() {
        if (currentReceiptSaved) {
            Toast.makeText(this, "Hóa đơn này đã được lưu", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = edtAiTitle.getText().toString().trim();
        String amountText = edtAiAmount.getText().toString().trim();
        String category = edtAiCategory.getText().toString().trim();

        if (title.isEmpty()) title = "Hóa đơn";
        if (category.isEmpty()) category = "Khác";
        if (amountText.isEmpty()) {
            Toast.makeText(this, "Chưa có số tiền để lưu", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            saveTransaction(new GeminiReceiptService.ReceiptResult(title, amount, category, ""));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveTransaction(GeminiReceiptService.ReceiptResult result) {
        if (result.amount <= 0) {
            tvAiStatus.setText("AI chưa đọc được số tiền hợp lệ. Bạn có thể sửa và bấm Lưu giao dịch.");
            return;
        }

        long now = System.currentTimeMillis();
        String timeDisplay = new SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault()).format(new Date(now));
        dbHelper.addTransaction(result.title, result.amount, timeDisplay, result.category, true, now);
        currentReceiptSaved = true;

        double income = dbHelper.getTotalIncome(0, Long.MAX_VALUE);
        double expense = dbHelper.getTotalExpense(0, Long.MAX_VALUE);
        double balance = income - expense;

        tvAiStatus.setText(String.format(
                Locale.getDefault(),
                "Đã trừ %,.0f đ từ hóa đơn. Số dư mới: %,.0f đ",
                result.amount,
                balance
        ));
        Toast.makeText(this, "Đã lưu giao dịch từ hóa đơn", Toast.LENGTH_SHORT).show();
    }

    private void setLoading(boolean isLoading) {
        progressAi.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        findViewById(R.id.btn_analyze_receipt).setEnabled(!isLoading);
        findViewById(R.id.btn_save_ai_transaction).setEnabled(!isLoading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
