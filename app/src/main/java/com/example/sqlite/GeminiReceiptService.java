package com.example.sqlite;

import android.graphics.Bitmap;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GeminiReceiptService {
    private static final String MODEL_NAME = "gemini-2.5-flash";
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL_NAME + ":generateContent?key=";

    public ReceiptResult analyzeReceipt(Bitmap bitmap) throws Exception {
        String apiKey = BuildConfig.GEMINI_API_KEY;
        if (apiKey.trim().isEmpty() || apiKey.equals("replace_with_your_gemini_api_key")) {
            throw new IllegalStateException("Bạn cần điền GEMINI_API_KEY trong file .env");
        }

        String base64Image = bitmapToBase64(bitmap);
        JSONObject requestBody = buildRequestBody(base64Image);

        HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT + apiKey).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = connection.getResponseCode();
        InputStream responseStream = responseCode >= 200 && responseCode < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        String response = readStream(responseStream);
        connection.disconnect();

        if (responseCode < 200 || responseCode >= 300) {
            throw new IllegalStateException("Gemini API lỗi: " + response);
        }

        return parseReceiptResult(response);
    }

    private JSONObject buildRequestBody(String base64Image) throws Exception {
        String prompt = "Bạn là AI đọc hóa đơn cho app quản lý chi tiêu. "
                + "Hãy phân tích ảnh hóa đơn và chỉ trả về JSON hợp lệ, không markdown, không giải thích. "
                + "Schema: {\"title\":\"tên giao dịch ngắn\", \"amount\": số_tiền_vnd, "
                + "\"category\":\"Ăn uống|Di chuyển|Mua sắm|Sắc đẹp|Ăn vặt|Học tập|Giải trí|Tiền nhà|Sức khỏe|Tiền điện|Tiền nước|Internet|Quà tặng|Khác\", "
                + "\"note\":\"ghi chú ngắn\"}. Nếu không chắc danh mục thì dùng Khác.";

        JSONObject textPart = new JSONObject().put("text", prompt);
        JSONObject inlineData = new JSONObject()
                .put("mime_type", "image/jpeg")
                .put("data", base64Image);
        JSONObject imagePart = new JSONObject().put("inline_data", inlineData);

        JSONArray parts = new JSONArray().put(textPart).put(imagePart);
        JSONObject content = new JSONObject().put("parts", parts);
        return new JSONObject().put("contents", new JSONArray().put(content));
    }

    private ReceiptResult parseReceiptResult(String response) throws Exception {
        JSONObject root = new JSONObject(response);
        JSONArray candidates = root.getJSONArray("candidates");
        JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
        JSONArray parts = content.getJSONArray("parts");
        String text = parts.getJSONObject(0).getString("text").trim();

        text = text.replace("```json", "").replace("```", "").trim();
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start >= 0 && end > start) {
            text = text.substring(start, end + 1);
        }

        JSONObject json = new JSONObject(text);
        String title = json.optString("title", "Hóa đơn");
        double amount = json.optDouble("amount", 0);
        String category = json.optString("category", "Khác");
        String note = json.optString("note", "");
        return new ReceiptResult(title, amount, category, note);
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
    }

    private String readStream(InputStream inputStream) throws Exception {
        if (inputStream == null) return "";
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    public static class ReceiptResult {
        public final String title;
        public final double amount;
        public final String category;
        public final String note;

        public ReceiptResult(String title, double amount, String category, String note) {
            this.title = title;
            this.amount = amount;
            this.category = category;
            this.note = note;
        }
    }
}
