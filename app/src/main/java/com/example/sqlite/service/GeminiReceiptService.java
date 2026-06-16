package com.example.sqlite.service;

import android.graphics.Bitmap;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import com.example.sqlite.BuildConfig;
import com.example.sqlite.dto.ReceiptResultDto;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
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

    public ReceiptResultDto analyzeReceipt(Bitmap bitmap) throws Exception {
        String apiKey = BuildConfig.GEMINI_API_KEY;
        if (apiKey.trim().isEmpty() || apiKey.equals("replace_with_your_gemini_api_key")) {
            throw new IllegalStateException("Bạn cần điền GEMINI_API_KEY trong file .env");
        }

        String base64Image = bitmapToBase64(bitmap);
        JSONObject requestBody = buildRequestBody(base64Image);
        String response = generateContent(apiKey, requestBody);

        return parseReceiptResult(response);
    }

    public String sendChatMessage(String message, Bitmap bitmap) throws Exception {
        String apiKey = BuildConfig.GEMINI_API_KEY;
        if (apiKey.trim().isEmpty() || apiKey.equals("replace_with_your_gemini_api_key")) {
            throw new IllegalStateException("Bạn cần điền GEMINI_API_KEY trong file .env");
        }

        JSONObject requestBody = buildChatRequestBody(message, bitmap);
        String response = generateContent(apiKey, requestBody);
        return parseTextResponse(response);
    }

    private String generateContent(String apiKey, JSONObject requestBody) throws Exception {
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

        return response;
    }

    private JSONObject buildRequestBody(String base64Image) throws Exception {
        String prompt = "Bạn là chatbot AI đọc hóa đơn cho app quản lý chi tiêu. "
                + "Hãy phân tích ảnh hóa đơn, nhận diện tổng tiền cần ghi chi tiêu, tên giao dịch ngắn, "
                + "danh mục phù hợp và ghi chú ngắn. Chỉ trả về dữ liệu JSON đúng schema.";

        JSONObject textPart = new JSONObject().put("text", prompt);
        JSONObject inlineData = new JSONObject()
                .put("mime_type", "image/jpeg")
                .put("data", base64Image);
        JSONObject imagePart = new JSONObject().put("inline_data", inlineData);

        JSONArray parts = new JSONArray().put(textPart).put(imagePart);
        JSONObject content = new JSONObject().put("parts", parts);
        return new JSONObject()
                .put("contents", new JSONArray().put(content))
                .put("generationConfig", buildGenerationConfig());
    }

    private JSONObject buildChatRequestBody(String message, Bitmap bitmap) throws Exception {
        JSONArray parts = new JSONArray()
                .put(new JSONObject().put("text",
                        "Bạn là chatbot AI hỗ trợ người dùng quản lý chi tiêu. "
                                + "Trả lời tự nhiên bằng tiếng Việt, ngắn gọn và hữu ích. "
                                + "Nếu có ảnh hóa đơn đính kèm, hãy dùng ảnh để trả lời câu hỏi.\n\nCâu hỏi: " + message));

        if (bitmap != null) {
            JSONObject inlineData = new JSONObject()
                    .put("mime_type", "image/jpeg")
                    .put("data", bitmapToBase64(bitmap));
            parts.put(new JSONObject().put("inline_data", inlineData));
        }

        JSONObject content = new JSONObject().put("parts", parts);
        return new JSONObject()
                .put("contents", new JSONArray().put(content))
                .put("generationConfig", new JSONObject()
                        .put("temperature", 0.5));
    }

    private JSONObject buildGenerationConfig() throws Exception {
        JSONObject schema = new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("title", new JSONObject()
                                .put("type", "string")
                                .put("description", "Tên giao dịch ngắn bằng tiếng Việt"))
                        .put("amount", new JSONObject()
                                .put("type", "number")
                                .put("description", "Tổng số tiền VND, chỉ lấy số"))
                        .put("category", new JSONObject()
                                .put("type", "string")
                                .put("enum", new JSONArray()
                                        .put("Ăn uống")
                                        .put("Di chuyển")
                                        .put("Mua sắm")
                                        .put("Sắc đẹp")
                                        .put("Ăn vặt")
                                        .put("Học tập")
                                        .put("Giải trí")
                                        .put("Tiền nhà")
                                        .put("Sức khỏe")
                                        .put("Tiền điện")
                                        .put("Tiền nước")
                                        .put("Internet")
                                        .put("Quà tặng")
                                        .put("Khác")))
                        .put("note", new JSONObject()
                                .put("type", "string")
                                .put("description", "Ghi chú ngắn về hóa đơn")))
                .put("required", new JSONArray()
                        .put("title")
                        .put("amount")
                        .put("category")
                        .put("note"));

        return new JSONObject()
                .put("temperature", 0.2)
                .put("responseMimeType", "application/json")
                .put("responseSchema", schema);
    }

    private ReceiptResultDto parseReceiptResult(String response) throws Exception {
        String text = parseTextResponse(response);

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
        return new ReceiptResultDto(title, amount, category, note, json.toString(2));
    }

    private String parseTextResponse(String response) throws Exception {
        JSONObject root = new JSONObject(response);
        JSONArray candidates = root.getJSONArray("candidates");
        JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
        JSONArray parts = content.getJSONArray("parts");
        return parts.getJSONObject(0).getString("text").trim();
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

}
