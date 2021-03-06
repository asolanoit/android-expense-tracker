package com.antso.expenses.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Map;

public class Settings {

    static public String getCurrencySymbol(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString("currency_symbol", null);
    }

    static public String getDefaultExpenseAccountId(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString("accounts_list_expenses", null);
    }

    static public String getDefaultRevenueAccountId(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString("accounts_list_revenues", null);
    }

    static public String getDefaultTransferFromAccountId(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString("accounts_list_transfer_from", null);
    }

    static public String getDefaultTransferToAccountId(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString("accounts_list_transfer_to", null);
    }

    static public String getDefaultExpenseBudgetId(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString("budgets_list_expenses", null);
    }

    static public String getDefaultRevenueBudgetId(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString("budgets_list_revenues", null);
    }

    static public String getDefaultTransferBudgetId(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString("budgets_list_transfer", null);
    }


    public static boolean getMultilineDescriptionInTransactionList(Context applicationContext) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        return preferences.getBoolean("multiline_transaction_list_description", false);
    }

    public static void saveMultilineDescriptionInTransactionList(Context applicationContext, boolean checked) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("multiline_transaction_list_description", checked);
        editor.commit();
    }
    
    public static int getAccountIndex(Context context, String id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getInt("account_index_" + id, -1);
    }
    
    public static void saveAccountIndex(Context applicationContext, String id, int index) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("account_index_" + id, index);
        editor.commit();
    }

    public static int getBudgetIndex(Context context, String id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getInt("budget_index_" + id, -1);
    }

    public static void saveBudgetIndex(Context applicationContext, String id, int index) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("budget_index_" + id, index);
        editor.commit();
    }

    public static Map<String, ?> getAllSettings(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getAll();
    }

}
