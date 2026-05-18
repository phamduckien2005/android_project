package com.example.sqlite;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    private PieChart pieChart;
    private RecyclerView rvCategoryStats;
    private Toolbar toolbar;
    private TextView tvTotalIncome, tvTotalExpense;
    private TabLayout tabLayout;
    private Spinner spinnerFilter;
    private DatabaseHelper dbHelper;
    private long startDate = 0, endDate = Long.MAX_VALUE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        dbHelper = new DatabaseHelper(this);
        initViews();
        setupToolbar();
        setupSpinner();
        setupTabLayout();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_report);
        pieChart = findViewById(R.id.pie_chart_report);
        rvCategoryStats = findViewById(R.id.rv_category_stats);
        tvTotalIncome = findViewById(R.id.tv_report_total_income);
        tvTotalExpense = findViewById(R.id.tv_report_total_expense);
        tabLayout = findViewById(R.id.tab_layout_report);
        spinnerFilter = findViewById(R.id.spinner_report_filter);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSpinner() {
        List<String> filters = new ArrayList<>();filters.add("Tất cả ▾");
        filters.add("7 ngày qua ▾");
        filters.add("30 ngày qua ▾");
        for (int i = 1; i <= 12; i++) {
            filters.add("Tháng " + i + " ▾");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filters);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(adapter);
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTimeRange(position);
                updateSummary();
                loadData(tabLayout.getSelectedTabPosition() == 0);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateTimeRange(int position) {
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0);
        end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59); end.set(Calendar.SECOND, 59);
        if (position == 0) { startDate = 0; endDate = Long.MAX_VALUE; }
        else if (position == 1) { start.add(Calendar.DAY_OF_YEAR, -7); startDate = start.getTimeInMillis(); endDate = end.getTimeInMillis(); }
        else if (position == 2) { start.add(Calendar.DAY_OF_YEAR, -30); startDate = start.getTimeInMillis(); endDate = end.getTimeInMillis(); }
        else { 
            int month = position - 3;
            start.set(Calendar.MONTH, month); start.set(Calendar.DAY_OF_MONTH, 1);
            end.set(Calendar.MONTH, month); end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
            startDate = start.getTimeInMillis(); endDate = end.getTimeInMillis();
        }
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) { loadData(tab.getPosition() == 0); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void updateSummary() {
        double income = dbHelper.getTotalIncome(startDate, endDate);
        double expense = dbHelper.getTotalExpense(startDate, endDate);
        tvTotalIncome.setText(String.format(Locale.getDefault(), "%,.0f đ", income));
        tvTotalExpense.setText(String.format(Locale.getDefault(), "%,.0f đ", expense));
    }

    private void loadData(boolean showExpense) {
        Map<String, Double> stats = showExpense ? dbHelper.getSpendingStats(startDate, endDate) : dbHelper.getIncomeStats(startDate, endDate);
        if (stats == null || stats.isEmpty()) {
            pieChart.clear(); pieChart.setNoDataText("Chưa có dữ liệu"); pieChart.invalidate();
            rvCategoryStats.setAdapter(null); return;
        }

        // Sắp xếp danh sách từ cao xuống thấp (giống mẫu)
        List<Map.Entry<String, Double>> sortedStats = new ArrayList<>(stats.entrySet());
        Collections.sort(sortedStats, (e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        List<PieEntry> entries = new ArrayList<>();
        double totalAmount = 0;
        for (Map.Entry<String, Double> entry : sortedStats) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
            totalAmount += entry.getValue();
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        int[] colors = {Color.parseColor("#FF7043"), Color.parseColor("#42A5F5"), Color.parseColor("#AB47BC"), 
                        Color.parseColor("#26A69A"), Color.parseColor("#FFCA28"), Color.parseColor("#EC407A"), Color.parseColor("#78909C")};
        dataSet.setColors(colors);
        dataSet.setSliceSpace(2f);
        dataSet.setDrawValues(false);

        pieChart.setData(new PieData(dataSet));
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(72f);
        pieChart.setCenterText(String.format(Locale.getDefault(), "Tổng %s\n%,.0f đ", showExpense ? "chi" : "thu", totalAmount));
        pieChart.animateY(800);
        pieChart.invalidate();

        StatsAdapter adapter = new StatsAdapter(sortedStats, totalAmount, showExpense);
        rvCategoryStats.setLayoutManager(new LinearLayoutManager(this));
        rvCategoryStats.setAdapter(adapter);
    }

    private static class StatsAdapter extends RecyclerView.Adapter<StatsAdapter.ViewHolder> {
        private final List<Map.Entry<String, Double>> data;
        private final double total;
        private final boolean isExpense;

        public StatsAdapter(List<Map.Entry<String, Double>> data, double total, boolean isExpense) {
            this.data = data; this.total = total; this.isExpense = isExpense;
        }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_stat, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String name = data.get(position).getKey();
            double amount = data.get(position).getValue();
            int percent = (int) Math.round((amount / total) * 100);
            if (percent == 0 && amount > 0) percent = 1; // Hiển thị ít nhất 1% nếu có tiêu dùng

            holder.tvName.setText(name);
            holder.tvPercent.setText(percent + "%");
            holder.tvAmount.setText(String.format(Locale.getDefault(), "%,.0f đ", amount));
            holder.tvAmount.setTextColor(isExpense ? Color.parseColor("#FF5252") : Color.parseColor("#4CAF50"));
            
            // Ánh xạ Icon chính xác
            holder.ivIcon.setImageResource(getIconRes(name));
        }

        private int getIconRes(String name) {
            if (name.contains("Ăn uống")) return android.R.drawable.ic_menu_today;
            if (name.contains("Di chuyển")) return android.R.drawable.ic_menu_directions;
            if (name.contains("Mua sắm")) return android.R.drawable.ic_menu_save;
            if (name.contains("Học tập")) return android.R.drawable.ic_menu_edit;
            if (name.contains("Giải trí")) return android.R.drawable.ic_menu_slideshow;
            if (name.contains("nhà")) return android.R.drawable.ic_menu_myplaces;
            if (name.contains("Lương")) return android.R.drawable.ic_menu_month;
            return android.R.drawable.ic_menu_help;
        }

        @Override public int getItemCount() { return data.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvAmount, tvPercent; ImageView ivIcon;
            public ViewHolder(View v) { super(v);
                tvName = v.findViewById(R.id.tv_stat_category_name); tvAmount = v.findViewById(R.id.tv_stat_category_amount);
                tvPercent = v.findViewById(R.id.tv_stat_percent); ivIcon = v.findViewById(R.id.iv_stat_icon);
            }
        }
    }
}