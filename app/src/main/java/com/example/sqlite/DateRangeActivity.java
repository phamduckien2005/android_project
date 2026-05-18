package com.example.sqlite;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.util.Calendar;
import java.util.Locale;

public class DateRangeActivity extends AppCompatActivity {

    private TextView tvStartDate, tvEndDate;
    private Calendar startCalendar, endCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_date_range);

        // Khởi tạo mốc thời gian mặc định
        startCalendar = Calendar.getInstance();
        endCalendar = Calendar.getInstance();

        initViews();
        setupToolbar();
        handleEvents();
        updateDateLabels();
    }

    private void initViews() {
        tvStartDate = findViewById(R.id.tv_start_date);
        tvEndDate = findViewById(R.id.tv_end_date);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_date_range);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void handleEvents() {
        // CHỌN NGÀY THỦ CÔNG
        findViewById(R.id.layout_start_date).setOnClickListener(v -> showDatePicker(true));
        findViewById(R.id.layout_end_date).setOnClickListener(v -> showDatePicker(false));

        // NÚT ÁP DỤNG LỌC (Nút màu hồng dưới cùng)
        findViewById(R.id.btn_confirm_date).setOnClickListener(v -> {
            // Chuẩn hóa thời gian trước khi kiểm tra
            startCalendar.set(Calendar.HOUR_OF_DAY, 0);
            startCalendar.set(Calendar.MINUTE, 0);
            startCalendar.set(Calendar.SECOND, 0);
            
            endCalendar.set(Calendar.HOUR_OF_DAY, 23);
            endCalendar.set(Calendar.MINUTE, 59);
            endCalendar.set(Calendar.SECOND, 59);

            if (startCalendar.after(endCalendar)) {
                Toast.makeText(this, "Ngày bắt đầu không được sau ngày kết thúc", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Trả kết quả về màn hình trước đó
            returnResult(startCalendar, endCalendar, formatDateRangeLabel(startCalendar, endCalendar));
        });

        // CÁC ĐƯỜNG TẮT (Bấm là lọc luôn)
        findViewById(R.id.opt_today).setOnClickListener(v -> returnResult(Calendar.getInstance(), Calendar.getInstance(), "Hôm nay"));
        
        findViewById(R.id.opt_yesterday).setOnClickListener(v -> {
            Calendar s = Calendar.getInstance();
            s.add(Calendar.DAY_OF_YEAR, -1);
            returnResult(s, s, "Hôm qua");
        });

        findViewById(R.id.opt_this_month).setOnClickListener(v -> {
            Calendar s = Calendar.getInstance();
            s.set(Calendar.DAY_OF_MONTH, 1);
            returnResult(s, Calendar.getInstance(), "Tháng này");
        });

        findViewById(R.id.opt_last_30_days).setOnClickListener(v -> {
            Calendar s = Calendar.getInstance();
            s.add(Calendar.DAY_OF_YEAR, -30);
            returnResult(s, Calendar.getInstance(), "30 ngày qua");
        });

        findViewById(R.id.opt_all).setOnClickListener(v -> {
            Calendar s = Calendar.getInstance();
            s.set(1970, 0, 1);
            returnResult(s, Calendar.getInstance(), "Tất cả");
        });
    }

    private void returnResult(Calendar start, Calendar end, String label) {
        Intent intent = new Intent();
        intent.putExtra("startDate", start.getTimeInMillis());
        intent.putExtra("endDate", end.getTimeInMillis());
        intent.putExtra("label", label);
        setResult(RESULT_OK, intent);
        finish();
    }

    private String formatDateRangeLabel(Calendar start, Calendar end) {
        if (start.get(Calendar.DAY_OF_YEAR) == end.get(Calendar.DAY_OF_YEAR) && start.get(Calendar.YEAR) == end.get(Calendar.YEAR)) {
            return String.format(Locale.getDefault(), "%d/%d", start.get(Calendar.DAY_OF_MONTH), start.get(Calendar.MONTH) + 1);
        }
        return String.format(Locale.getDefault(), "%d/%d-%d/%d", 
            start.get(Calendar.DAY_OF_MONTH), start.get(Calendar.MONTH) + 1,
            end.get(Calendar.DAY_OF_MONTH), end.get(Calendar.MONTH) + 1);
    }

    private void updateDateLabels() {
        tvStartDate.setText(formatDate(startCalendar));
        tvEndDate.setText(formatDate(endCalendar));
    }

    private String formatDate(Calendar calendar) {
        return String.format(Locale.getDefault(), "%d/%d/%d", 
            calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR));
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = isStartDate ? startCalendar : endCalendar;
        new DatePickerDialog(this, R.style.Theme_Sqlite, (view, year, month, dayOfMonth) -> {
            if (isStartDate) {
                startCalendar.set(year, month, dayOfMonth);
                // Nếu ngày bắt đầu vượt quá ngày kết thúc, tự động đẩy ngày kết thúc lên theo
                if (startCalendar.after(endCalendar)) {
                    endCalendar.setTimeInMillis(startCalendar.getTimeInMillis());
                }
            } else {
                endCalendar.set(year, month, dayOfMonth);
                // Nếu ngày kết thúc nhỏ hơn ngày bắt đầu, tự động lùi ngày bắt đầu về theo
                if (endCalendar.before(startCalendar)) {
                    startCalendar.setTimeInMillis(endCalendar.getTimeInMillis());
                }
            }
            updateDateLabels();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }
}