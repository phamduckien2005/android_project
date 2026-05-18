package com.example.sqlite;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    public interface OnItemLongClickListener {
        void onItemLongClick(Transaction transaction, int position);
    }

    private OnItemLongClickListener longClickListener;

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public static class Transaction {
        int id;
        String title;
        String time;
        double amount;
        boolean isExpense;
        long timestamp; 
        boolean isHeader = false;
        String daySummary = "";

        public Transaction(int id, String title, String time, double amount, boolean isExpense, long timestamp) {
            this.id = id;
            this.title = title;
            this.time = time;
            this.amount = amount;
            this.isExpense = isExpense;
            this.timestamp = timestamp;
        }

        public Transaction(String date, String summary) {
            this.title = date;
            this.daySummary = summary;
            this.isHeader = true;
        }
    }

    private List<Transaction> displayList;

    public TransactionAdapter(List<Transaction> transactions) {
        this.displayList = groupTransactionsByDate(transactions);
    }

    private List<Transaction> groupTransactionsByDate(List<Transaction> originalList) {
        List<Transaction> grouped = new ArrayList<>();
        if (originalList == null || originalList.isEmpty()) return grouped;

        String lastDateLabel = "";
        double dayIncome = 0;
        double dayExpense = 0;
        List<Transaction> tempDayItems = new ArrayList<>();

        for (Transaction t : originalList) {
            String currentDateLabel = formatDateLabel(t.timestamp);

            if (!currentDateLabel.equals(lastDateLabel)) {
                if (!tempDayItems.isEmpty()) {
                    String summary = String.format("Chi tiêu: %,.0f  Thu nhập: %,.0f", dayExpense, dayIncome);
                    grouped.add(new Transaction(lastDateLabel, summary));
                    grouped.addAll(tempDayItems);
                }
                lastDateLabel = currentDateLabel;
                dayIncome = 0;
                dayExpense = 0;
                tempDayItems.clear();
            }

            if (t.isExpense) dayExpense += t.amount;
            else dayIncome += t.amount;
            tempDayItems.add(t);
        }

        if (!tempDayItems.isEmpty()) {
            String summary = String.format("Chi tiêu: %,.0f  Thu nhập: %,.0f", dayExpense, dayIncome);
            grouped.add(new Transaction(lastDateLabel, summary));
            grouped.addAll(tempDayItems);
        }

        return grouped;
    }

    private String formatDateLabel(long timestamp) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp);
        int day = c.get(Calendar.DAY_OF_MONTH);
        int month = c.get(Calendar.MONTH) + 1;
        int year = c.get(Calendar.YEAR);
        
        Calendar now = Calendar.getInstance();
        int currentYear = now.get(Calendar.YEAR);

        if (year == currentYear) {
            return String.format("%02d/%02d", day, month);
        } else {
            return String.format("%02d/%02d/%d", day, month, year);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return displayList.get(position).isHeader ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Transaction item = displayList.get(position);

        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder h = (HeaderViewHolder) holder;
            h.tvDate.setText(item.title);
            h.tvSummary.setText(item.daySummary);
        } else {
            ItemViewHolder h = (ItemViewHolder) holder;
            h.tvTitle.setText(item.title);
            h.tvTime.setText(item.time);
            if (item.isExpense) {
                h.tvAmount.setTextColor(Color.parseColor("#FF5252"));
                h.tvAmount.setText("- " + String.format("%,.0f", item.amount));
            } else {
                h.tvAmount.setTextColor(Color.parseColor("#4CAF50"));
                h.tvAmount.setText("+ " + String.format("%,.0f", item.amount));
            }

            h.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onItemLongClick(item, position);
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    public List<Transaction> getTransactionList() {
        return displayList;
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvSummary;
        public HeaderViewHolder(View v) {
            super(v);
            tvDate = v.findViewById(R.id.tv_header_date);
            tvSummary = v.findViewById(R.id.tv_header_day_summary);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime, tvAmount;
        public ItemViewHolder(View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tv_item_title);
            tvTime = v.findViewById(R.id.tv_item_time);
            tvAmount = v.findViewById(R.id.tv_item_amount);
        }
    }
}