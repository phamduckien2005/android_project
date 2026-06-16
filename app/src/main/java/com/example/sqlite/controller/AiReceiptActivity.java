package com.example.sqlite.controller;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.sqlite.R;
import com.example.sqlite.dto.ReceiptResultDto;
import com.example.sqlite.repository.DatabaseHelper;
import com.example.sqlite.service.GeminiReceiptService;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AiReceiptActivity extends AppCompatActivity {
    private static final int MAX_IMAGE_SIZE = 1600;

    private LinearLayout layoutChatMessages, layoutResultCard;
    private ScrollView scrollChat;
    private TextView tvAiStatus, tvAiJson, tvResultTitle, tvResultAmount, tvResultCategory, tvResultNote;
    private EditText edtChatMessage, edtAiTitle, edtAiAmount, edtAiCategory;
    private ProgressBar progressAi;
    private Bitmap selectedBitmap;
    private ReceiptResultDto pendingReceiptResult;
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
        scrollChat = findViewById(R.id.scroll_chat);
        layoutChatMessages = findViewById(R.id.layout_bubble_stream);
        layoutResultCard = findViewById(R.id.layout_result_card);
        tvAiStatus = findViewById(R.id.tv_ai_status);
        tvAiJson = findViewById(R.id.tv_ai_json);
        tvResultTitle = findViewById(R.id.tv_result_title);
        tvResultAmount = findViewById(R.id.tv_result_amount);
        tvResultCategory = findViewById(R.id.tv_result_category);
        tvResultNote = findViewById(R.id.tv_result_note);
        edtChatMessage = findViewById(R.id.edt_chat_message);
        edtAiTitle = findViewById(R.id.edt_ai_title);
        edtAiAmount = findViewById(R.id.edt_ai_amount);
        edtAiCategory = findViewById(R.id.edt_ai_category);
        progressAi = findViewById(R.id.progress_ai);
        addAssistantBubble("Mình là AI quản lý chi tiêu. Bạn có thể nhắn bình thường, gửi ảnh hóa đơn bằng nút +, rồi bấm Phân tích để lấy kết quả.");
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
            clearReceiptFields();
            pendingReceiptResult = null;
            layoutResultCard.setVisibility(View.GONE);
            addImageBubble(selectedBitmap);
            addAssistantBubble("Mình đã nhận ảnh. Bạn có thể hỏi về ảnh này hoặc bấm Phân tích để mình đọc hóa đơn.");
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
        addUserBubble(message);
        TextView loadingBubble = addAssistantBubble("Mình đang trả lời...");

        executorService.execute(() -> {
            try {
                String answer = new GeminiReceiptService().sendChatMessage(message, selectedBitmap);
                runOnUiThread(() -> {
                    setLoading(false);
                    loadingBubble.setText(answer);
                    tvAiStatus.setText("AI đã trả lời.");
                    scrollToBottom();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    loadingBubble.setText("Mình chưa trả lời được lúc này.");
                    tvAiStatus.setText(e.getMessage());
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    scrollToBottom();
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
        TextView loadingBubble = addAssistantBubble("Mình đang phân tích ảnh và chuẩn hóa kết quả...");

        executorService.execute(() -> {
            try {
                ReceiptResultDto result = new GeminiReceiptService().analyzeReceipt(selectedBitmap);
                runOnUiThread(() -> {
                    setLoading(false);
                    pendingReceiptResult = result;
                    fillReceiptFields(result);
                    updateResultCard(result);
                    loadingBubble.setText("Đã phân tích xong. Mình đã trình bày kết quả bên dưới, bạn bấm Áp dụng để lưu giao dịch này.");
                    tvAiStatus.setText("AI đã phân tích xong hóa đơn.");
                    scrollToBottom();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    tvAiStatus.setText(e.getMessage());
                    loadingBubble.setText("Mình chưa phân tích được hóa đơn này. Hãy kiểm tra ảnh hoặc API key rồi thử lại.");
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    scrollToBottom();
                });
            }
        });
    }

    private void fillReceiptFields(ReceiptResultDto result) {
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
            saveTransaction(new ReceiptResultDto(title, amount, category, "", tvAiJson.getText().toString()));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveTransaction(ReceiptResultDto result) {
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
        addAssistantBubble("Giao dịch đã được lưu từ kết quả AI.");
        Toast.makeText(this, "Đã lưu giao dịch từ hóa đơn", Toast.LENGTH_SHORT).show();
    }

    private TextView addUserBubble(String message) {
        return addTextBubble(message, true);
    }

    private TextView addAssistantBubble(String message) {
        return addTextBubble(message, false);
    }

    private TextView addTextBubble(String message, boolean isUser) {
        TextView bubble = new TextView(this);
        bubble.setText(message);
        bubble.setTextSize(14);
        bubble.setTextColor(isUser ? getColor(android.R.color.white) : getColor(R.color.text_main));
        bubble.setBackgroundResource(isUser ? R.drawable.bg_chat_user : R.drawable.bg_chat_assistant);
        bubble.setPadding(dp(14), dp(10), dp(14), dp(10));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = isUser ? Gravity.END : Gravity.START;
        params.setMargins(isUser ? dp(54) : 0, dp(8), isUser ? 0 : dp(54), dp(4));
        layoutChatMessages.addView(bubble, params);
        scrollToBottom();
        return bubble;
    }

    private void addImageBubble(Bitmap bitmap) {
        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(bitmap);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setBackgroundResource(R.drawable.bg_chat_user);
        imageView.setPadding(dp(4), dp(4), dp(4), dp(4));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(180), dp(180));
        params.gravity = Gravity.END;
        params.setMargins(dp(54), dp(8), 0, dp(4));
        layoutChatMessages.addView(imageView, params);
        scrollToBottom();
    }

    private void updateResultCard(ReceiptResultDto result) {
        tvResultTitle.setText("Tên giao dịch: " + result.title);
        tvResultAmount.setText(String.format(Locale.getDefault(), "%,.0f đ", result.amount));
        tvResultCategory.setText("Danh mục: " + result.category);
        tvResultNote.setText(result.note == null || result.note.trim().isEmpty()
                ? "Ghi chú: Chưa có ghi chú"
                : "Ghi chú: " + result.note);
        tvAiJson.setText(formatJsonPreview(result));
        layoutResultCard.setVisibility(View.VISIBLE);
    }

    private String formatJsonPreview(ReceiptResultDto result) {
        return "{\n"
                + "  \"title\": \"" + result.title + "\",\n"
                + "  \"amount\": " + String.format(Locale.US, "%.0f", result.amount) + ",\n"
                + "  \"category\": \"" + result.category + "\",\n"
                + "  \"note\": \"" + (result.note == null ? "" : result.note) + "\"\n"
                + "}";
    }

    private void setLoading(boolean isLoading) {
        progressAi.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        findViewById(R.id.btn_pick_receipt_photo).setEnabled(!isLoading);
        findViewById(R.id.btn_send_chat).setEnabled(!isLoading);
        findViewById(R.id.btn_analyze_receipt).setEnabled(!isLoading);
        findViewById(R.id.btn_save_ai_transaction).setEnabled(!isLoading);
    }

    private void scrollToBottom() {
        scrollChat.post(() -> scrollChat.fullScroll(View.FOCUS_DOWN));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
