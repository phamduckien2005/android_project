package com.example.sqlite;

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

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AiReceiptActivity extends AppCompatActivity {
    private static final int MAX_IMAGE_SIZE = 1600;

    private ImageView ivReceiptPreview;
    private TextView tvAiStatus, tvChatUser, tvChatAssistant, tvAiJson;
    private EditText edtChatMessage, edtAiTitle, edtAiAmount, edtAiCategory;
    private ProgressBar progressAi;
    private Bitmap selectedBitmap;
    private GeminiReceiptService.ReceiptResult pendingReceiptResult;
    private DatabaseHelper dbHelper;
    private boolean currentReceiptSaved = false;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    loadBitmapFromUri(uri);
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
        tvChatUser = findViewById(R.id.tv_chat_user);
        tvChatAssistant = findViewById(R.id.tv_chat_assistant);
        tvAiJson = findViewById(R.id.tv_ai_json);
        edtChatMessage = findViewById(R.id.edt_chat_message);
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
        findViewById(R.id.btn_pick_receipt_photo).setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        findViewById(R.id.btn_send_chat).setOnClickListener(v -> sendChatMessage());
        findViewById(R.id.btn_analyze_receipt).setOnClickListener(v -> analyzeReceipt());
        findViewById(R.id.btn_save_ai_transaction).setOnClickListener(v -> saveTransactionFromFields());
    }

    private void loadBitmapFromUri(Uri uri) {
        try {
            selectedBitmap = decodeScaledBitmap(uri);
            if (selectedBitmap == null) {
                throw new IllegalStateException("File ảnh không hợp lệ");
            }

            currentReceiptSaved = false;
            ivReceiptPreview.setImageBitmap(selectedBitmap);
            ivReceiptPreview.setVisibility(View.VISIBLE);
            clearReceiptFields();
            pendingReceiptResult = null;
            tvChatUser.setText("Bạn đã tải lên một ảnh hóa đơn.");
            tvChatAssistant.setText("Mình đã nhận ảnh. Bạn có thể hỏi về ảnh này hoặc bấm Phân tích hóa đơn để lấy JSON.");
            tvAiJson.setText("{ }");
            tvAiStatus.setText("Ảnh đã sẵn sàng để AI phân tích.");
        } catch (Exception e) {
            Toast.makeText(this, "Không đọc được ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap decodeScaledBitmap(Uri uri) throws Exception {
        BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
        boundsOptions.inJustDecodeBounds = true;
        try (InputStream boundsStream = getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(boundsStream, null, boundsOptions);
        }

        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inSampleSize = calculateInSampleSize(boundsOptions, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE);
        try (InputStream imageStream = getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(imageStream, null, decodeOptions);
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        while ((height / inSampleSize) > reqHeight || (width / inSampleSize) > reqWidth) {
            inSampleSize *= 2;
        }

        return inSampleSize;
    }

    private void sendChatMessage() {
        String message = edtChatMessage.getText().toString().trim();
        if (message.isEmpty()) {
            Toast.makeText(this, "Nhập câu hỏi cho AI trước", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        edtChatMessage.setText("");
        tvChatUser.setText(message);
        tvChatAssistant.setText("Mình đang trả lời...");

        executorService.execute(() -> {
            try {
                String answer = new GeminiReceiptService().sendChatMessage(message, selectedBitmap);
                runOnUiThread(() -> {
                    setLoading(false);
                    tvChatAssistant.setText(answer);
                    tvAiStatus.setText("AI đã trả lời.");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    tvChatAssistant.setText("Mình chưa trả lời được lúc này.");
                    tvAiStatus.setText(e.getMessage());
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void analyzeReceipt() {
        if (selectedBitmap == null) {
            Toast.makeText(this, "Hãy tải ảnh hóa đơn lên trước", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        tvAiStatus.setText("AI đang đọc hóa đơn...");
        tvChatAssistant.setText("Mình đang phân tích ảnh và chuẩn hóa kết quả thành JSON.");

        executorService.execute(() -> {
            try {
                GeminiReceiptService.ReceiptResult result = new GeminiReceiptService().analyzeReceipt(selectedBitmap);
                runOnUiThread(() -> {
                    setLoading(false);
                    pendingReceiptResult = result;
                    fillReceiptFields(result);
                    tvAiJson.setText(result.rawJson);
                    tvChatAssistant.setText("Đã phân tích xong:\n" + result.rawJson + "\n\nBấm Áp dụng để lưu giao dịch này.");
                    tvAiStatus.setText("AI đã phân tích xong hóa đơn.");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    tvAiStatus.setText(e.getMessage());
                    tvChatAssistant.setText("Mình chưa phân tích được hóa đơn này. Hãy kiểm tra ảnh hoặc API key rồi thử lại.");
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

    private void clearReceiptFields() {
        edtAiTitle.setText("");
        edtAiAmount.setText("");
        edtAiCategory.setText("");
    }

    private void saveTransactionFromFields() {
        if (currentReceiptSaved) {
            Toast.makeText(this, "Hóa đơn này đã được lưu", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pendingReceiptResult != null) {
            saveTransaction(pendingReceiptResult);
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
            saveTransaction(new GeminiReceiptService.ReceiptResult(title, amount, category, "", tvAiJson.getText().toString()));
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
        tvChatAssistant.setText("Giao dịch đã được lưu từ kết quả AI.");
        Toast.makeText(this, "Đã lưu giao dịch từ hóa đơn", Toast.LENGTH_SHORT).show();
    }

    private void setLoading(boolean isLoading) {
        progressAi.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        findViewById(R.id.btn_pick_receipt_photo).setEnabled(!isLoading);
        findViewById(R.id.btn_send_chat).setEnabled(!isLoading);
        findViewById(R.id.btn_analyze_receipt).setEnabled(!isLoading);
        findViewById(R.id.btn_save_ai_transaction).setEnabled(!isLoading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
