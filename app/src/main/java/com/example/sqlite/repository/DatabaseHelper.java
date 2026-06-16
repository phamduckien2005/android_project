package com.example.sqlite.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.sqlite.entity.Message;
import com.example.sqlite.entity.Transaction;
import com.example.sqlite.service.UserSession;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "SmartWallet.db";
    private static final int DATABASE_VERSION = 8;
    public static final String COLUMN_USER_ID = "user_id";
    public static final String TABLE_MESSAGES = "messages";
    public static final String COLUMN_MSG_ID = "id";
    public static final String COLUMN_MSG_TITLE = "title";
    public static final String COLUMN_MSG_CONTENT = "content";
    public static final String COLUMN_MSG_TIME = "time";
    public static final String TABLE_TRANSACTIONS = "transactions";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_AMOUNT = "amount";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_IS_EXPENSE = "is_expense";
    public static final String TABLE_BUDGETS = "budgets";
    public static final String COLUMN_BUDGET_CATEGORY = "category";
    public static final String COLUMN_BUDGET_LIMIT = "limit_amount";
    public static final String COLUMN_BUDGET_RESET_AT = "reset_at";
    private final Context appContext;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        appContext = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_TRANSACTIONS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_AMOUNT + " REAL, " +
                COLUMN_TIME + " TEXT, " +
                COLUMN_TIMESTAMP + " INTEGER, " +
                COLUMN_CATEGORY + " TEXT, " +
                COLUMN_IS_EXPENSE + " INTEGER, " +
                COLUMN_USER_ID + " TEXT DEFAULT 'guest');");
        db.execSQL("CREATE TABLE " + TABLE_MESSAGES + " (" +
                COLUMN_MSG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_MSG_TITLE + " TEXT, " +
                COLUMN_MSG_CONTENT + " TEXT, " +
                COLUMN_MSG_TIME + " TEXT, " +
                COLUMN_USER_ID + " TEXT DEFAULT 'guest');");
        createBudgetsTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_TRANSACTIONS + " ADD COLUMN " + COLUMN_TIMESTAMP + " INTEGER DEFAULT 0");
        }
        if (oldVersion < 5) {
            Cursor cursor = db.rawQuery("SELECT " + COLUMN_ID + ", " + COLUMN_TIME + " FROM " + TABLE_TRANSACTIONS, null);
            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(0);
                    String timeStr = cursor.getString(1);
                    long ts = parseTimeToTimestamp(timeStr);
                    if (ts > 0) {
                        db.execSQL("UPDATE " + TABLE_TRANSACTIONS + " SET " + COLUMN_TIMESTAMP + " = " + ts + " WHERE " + COLUMN_ID + " = " + id);
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        if (oldVersion < 6) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MESSAGES + " (" +
                    COLUMN_MSG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_MSG_TITLE + " TEXT, " +
                    COLUMN_MSG_CONTENT + " TEXT, " +
                    COLUMN_MSG_TIME + " TEXT);");
        }
        if (oldVersion < 7) {
            addUserIdColumnIfNeeded(db, TABLE_TRANSACTIONS);
            addUserIdColumnIfNeeded(db, TABLE_MESSAGES);
        }
        if (oldVersion < 8) {
            createBudgetsTable(db);
        }
    }

    private void createBudgetsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_BUDGETS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_BUDGET_CATEGORY + " TEXT NOT NULL, " +
                COLUMN_BUDGET_LIMIT + " REAL DEFAULT 0, " +
                COLUMN_BUDGET_RESET_AT + " INTEGER DEFAULT 0, " +
                COLUMN_USER_ID + " TEXT DEFAULT 'guest', " +
                "UNIQUE(" + COLUMN_BUDGET_CATEGORY + ", " + COLUMN_USER_ID + "));");
    }

    private void addUserIdColumnIfNeeded(SQLiteDatabase db, String tableName) {
        try {
            db.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + COLUMN_USER_ID + " TEXT DEFAULT 'guest'");
        } catch (Exception ignored) {
        }

        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, getCurrentUserId());
        db.update(tableName, values, COLUMN_USER_ID + " IS NULL OR " + COLUMN_USER_ID + " = ?", new String[]{"guest"});
    }

    private String getCurrentUserId() {
        return UserSession.getCurrentUserId(appContext);
    }

    private long parseTimeToTimestamp(String timeStr) {
        try {
            String[] parts = timeStr.split("-");
            if (parts.length < 2) return 0;
            String datePart = parts[1].trim(); 
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            int year = datePart.endsWith("/10") || datePart.endsWith("/11") || datePart.endsWith("/12") ? 2025 : 2026;
            Date date = sdf.parse(datePart + "/" + year);
            return date != null ? date.getTime() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // --- Transaction Methods ---
    public long addTransaction(String title, double amount, String time, String category, boolean isExpense, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_AMOUNT, amount);
        values.put(COLUMN_TIME, time);
        values.put(COLUMN_TIMESTAMP, timestamp);
        values.put(COLUMN_CATEGORY, category);
        values.put(COLUMN_IS_EXPENSE, isExpense ? 1 : 0);
        values.put(COLUMN_USER_ID, getCurrentUserId());
        long id = db.insert(TABLE_TRANSACTIONS, null, values);
        db.close();
        return id;
    }

    public void updateTransaction(int id, String title, double amount, String time, String category, boolean isExpense, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_AMOUNT, amount);
        values.put(COLUMN_TIME, time);
        values.put(COLUMN_TIMESTAMP, timestamp);
        values.put(COLUMN_CATEGORY, category);
        values.put(COLUMN_IS_EXPENSE, isExpense ? 1 : 0);
        db.update(TABLE_TRANSACTIONS, values, COLUMN_ID + " = ? AND " + COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(id), getCurrentUserId()});
        db.close();
    }

    public void deleteTransaction(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRANSACTIONS, COLUMN_ID + " = ? AND " + COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(id), getCurrentUserId()});
        db.close();
    }

    public void deleteAllTransactions() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRANSACTIONS, COLUMN_USER_ID + " = ?", new String[]{getCurrentUserId()});
        db.close();
    }

    public List<Transaction> getFilteredTransactions(long start, long end) {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_TRANSACTIONS + 
                       " WHERE " + COLUMN_TIMESTAMP + " >= ? AND " + COLUMN_TIMESTAMP + " <= ?" +
                       " AND " + COLUMN_USER_ID + " = ?" +
                       " ORDER BY " + COLUMN_TIMESTAMP + " DESC";
        
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(start), String.valueOf(end), getCurrentUserId()});
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY));
                boolean isExpense = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_EXPENSE)) == 1;
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));
                transactions.add(new Transaction(id, title, time, amount, isExpense, timestamp, category));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return transactions;
    }

    public double getTotalIncome(long start, long end) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + COLUMN_AMOUNT + ") FROM " + TABLE_TRANSACTIONS +
                " WHERE " + COLUMN_IS_EXPENSE + " = 0 AND " + COLUMN_TIMESTAMP + " BETWEEN ? AND ? AND " + COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(start), String.valueOf(end), getCurrentUserId()});
        double total = 0;
        if (cursor.moveToFirst()) total = cursor.getDouble(0);
        cursor.close();
        db.close();
        return total;
    }

    public double getTotalExpense(long start, long end) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + COLUMN_AMOUNT + ") FROM " + TABLE_TRANSACTIONS +
                " WHERE " + COLUMN_IS_EXPENSE + " = 1 AND " + COLUMN_TIMESTAMP + " BETWEEN ? AND ? AND " + COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(start), String.valueOf(end), getCurrentUserId()});
        double total = 0;
        if (cursor.moveToFirst()) total = cursor.getDouble(0);
        cursor.close();
        db.close();
        return total;
    }

    public Map<String, Double> getSpendingStats(long start, long end) {
        Map<String, Double> stats = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_CATEGORY + ", SUM(" + COLUMN_AMOUNT + ") as total " +
                       "FROM " + TABLE_TRANSACTIONS + " " +
                       "WHERE " + COLUMN_IS_EXPENSE + " = 1 AND " + COLUMN_TIMESTAMP + " BETWEEN ? AND ?" +
                       " AND " + COLUMN_USER_ID + " = ?" +
                       " GROUP BY " + COLUMN_CATEGORY;
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(start), String.valueOf(end), getCurrentUserId()});
        if (cursor.moveToFirst()) {
            do {
                stats.put(cursor.getString(0), cursor.getDouble(1));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return stats;
    }

    public void saveBudgetLimit(String category, double limitAmount) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_BUDGET_CATEGORY, category);
        values.put(COLUMN_BUDGET_LIMIT, limitAmount);
        values.put(COLUMN_USER_ID, getCurrentUserId());

        int updated = db.update(TABLE_BUDGETS, values,
                COLUMN_BUDGET_CATEGORY + " = ? AND " + COLUMN_USER_ID + " = ?",
                new String[]{category, getCurrentUserId()});
        if (updated == 0) {
            values.put(COLUMN_BUDGET_RESET_AT, 0);
            db.insert(TABLE_BUDGETS, null, values);
        }
        db.close();
    }

    public Map<String, Double> getBudgetLimits() {
        Map<String, Double> budgets = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_BUDGET_CATEGORY + ", " + COLUMN_BUDGET_LIMIT +
                        " FROM " + TABLE_BUDGETS + " WHERE " + COLUMN_USER_ID + " = ?",
                new String[]{getCurrentUserId()});
        if (cursor.moveToFirst()) {
            do {
                budgets.put(cursor.getString(0), cursor.getDouble(1));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return budgets;
    }

    public double getBudgetLimit(String category) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_BUDGET_LIMIT + " FROM " + TABLE_BUDGETS +
                        " WHERE " + COLUMN_BUDGET_CATEGORY + " = ? AND " + COLUMN_USER_ID + " = ?",
                new String[]{category, getCurrentUserId()});
        double limit = 0;
        if (cursor.moveToFirst()) limit = cursor.getDouble(0);
        cursor.close();
        db.close();
        return limit;
    }

    public long getWalletResetAt() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT MAX(" + COLUMN_BUDGET_RESET_AT + ") FROM " + TABLE_BUDGETS +
                        " WHERE " + COLUMN_USER_ID + " = ?",
                new String[]{getCurrentUserId()});
        long resetAt = 0;
        if (cursor.moveToFirst()) resetAt = cursor.getLong(0);
        cursor.close();
        db.close();
        return resetAt;
    }

    public void resetWallet() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_BUDGET_RESET_AT, System.currentTimeMillis());
        values.put(COLUMN_BUDGET_LIMIT, 0);
        db.update(TABLE_BUDGETS, values, COLUMN_USER_ID + " = ?", new String[]{getCurrentUserId()});
        db.close();
    }

    public double getCategoryExpenseSince(String category, long startTimestamp) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + COLUMN_AMOUNT + ") FROM " + TABLE_TRANSACTIONS +
                        " WHERE " + COLUMN_IS_EXPENSE + " = 1 AND " + COLUMN_CATEGORY + " = ?" +
                        " AND " + COLUMN_TIMESTAMP + " >= ? AND " + COLUMN_USER_ID + " = ?",
                new String[]{category, String.valueOf(startTimestamp), getCurrentUserId()});
        double total = 0;
        if (cursor.moveToFirst()) total = cursor.getDouble(0);
        cursor.close();
        db.close();
        return total;
    }

    public double getTotalBudgetLimit() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + COLUMN_BUDGET_LIMIT + ") FROM " + TABLE_BUDGETS +
                        " WHERE " + COLUMN_USER_ID + " = ?",
                new String[]{getCurrentUserId()});
        double total = 0;
        if (cursor.moveToFirst()) total = cursor.getDouble(0);
        cursor.close();
        db.close();
        return total;
    }

    public double getTotalBudgetSpentSinceReset() {
        long resetAt = getWalletResetAt();
        Map<String, Double> budgets = getBudgetLimits();
        double total = 0;
        for (String category : budgets.keySet()) {
            total += getCategoryExpenseSince(category, resetAt);
        }
        return total;
    }

    public Map<String, Double> getIncomeStats(long start, long end) {
        Map<String, Double> stats = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_CATEGORY + ", SUM(" + COLUMN_AMOUNT + ") as total " +
                       "FROM " + TABLE_TRANSACTIONS + " " +
                       "WHERE " + COLUMN_IS_EXPENSE + " = 0 AND " + COLUMN_TIMESTAMP + " BETWEEN ? AND ?" +
                       " AND " + COLUMN_USER_ID + " = ?" +
                       " GROUP BY " + COLUMN_CATEGORY;
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(start), String.valueOf(end), getCurrentUserId()});
        if (cursor.moveToFirst()) {
            do {
                stats.put(cursor.getString(0), cursor.getDouble(1));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return stats;
    }
    public void insertMessage(String title, String content, String time) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MSG_TITLE, title);
        values.put(COLUMN_MSG_CONTENT, content);
        values.put(COLUMN_MSG_TIME, time);
        values.put(COLUMN_USER_ID, getCurrentUserId());
        db.insert(TABLE_MESSAGES, null, values);
        db.close();
    }
    public List<Message> getAllMessages() {
        List<Message> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_MESSAGES + " WHERE " + COLUMN_USER_ID + " = ? ORDER BY id DESC",
                new String[]{getCurrentUserId()});

        if (cursor.moveToFirst()) {
            do {
                list.add(new Message(
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MSG_TITLE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MSG_CONTENT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MSG_TIME))
                ));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }

}
