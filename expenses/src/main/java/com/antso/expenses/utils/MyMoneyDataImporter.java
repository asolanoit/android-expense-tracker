package com.antso.expenses.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.antso.expenses.R;
import com.antso.expenses.accounts.AccountManager;
import com.antso.expenses.budgets.BudgetManager;
import com.antso.expenses.entities.Account;
import com.antso.expenses.entities.Budget;
import com.antso.expenses.entities.Transaction;
import com.antso.expenses.enums.TimeUnit;
import com.antso.expenses.enums.TransactionDirection;
import com.antso.expenses.enums.TransactionType;
import com.antso.expenses.persistence.DatabaseHelper;
import com.antso.expenses.persistence.EntityIdGenerator;
import com.antso.expenses.utils.csv.CSVReader;

import org.joda.time.DateTime;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

public class MyMoneyDataImporter {
    private final Context context;

    private Map<String, Account> accountsByName;
    private Map<String, Budget> budgetsByName;

    public MyMoneyDataImporter(final Context context) {
        this.context = context;
    }

    public void importData() {
        StringBuilder filePath = new StringBuilder();
        filePath.append(Environment.getExternalStorageDirectory());
        filePath.append("/MyMoney/MyMoney.csv");

        DatabaseHelper dbHelper = new DatabaseHelper(context);

        accountsByName = AccountManager.ACCOUNT_MANAGER().getAccountsByName();
        budgetsByName = BudgetManager.BUDGET_MANAGER().getBudgetsByName();

        try {
            FileReader fileReader = new FileReader(filePath.toString());
            CSVReader reader = new CSVReader(fileReader);
            //Required to jump header
            @SuppressWarnings("UnusedAssignment")
            String[] values = reader.readNext();
            values = reader.readNext();
            while (values != null) {
                StringBuilder message = new StringBuilder();
                for (String val : values) {
                    message.append(val).append(" | ");
                }

                Log.i("CVSReader", message.toString());

                if (values.length > 9) {
                    //Older myMoney versions
                    Transaction t1 = parseTransaction(values);
                    Transaction t2 = null;
                    if (t1.getType().equals(TransactionType.Transfer)) {
                        values = reader.readNext();
                        t2 = parseTransaction(values);
                        t1.setLinkedTransactionId(t2.getId());
                        t2.setLinkedTransactionId(t1.getId());
                    }

                    values = reader.readNext();

                    dbHelper.insertTransactions(t1);
                    if(t2 != null) {
                        dbHelper.insertTransactions(t2);
                    }
                } else {
                    //New myMoney versions
                    String[] nextValues = reader.readNext();
                    Transaction t1 = parseTransactionNew(values, nextValues);
                    Transaction t2 = null;
                    if (t1.getType().equals(TransactionType.Transfer)) {
                        t2 = parseTransactionNew(nextValues, null);
                        t1.setLinkedTransactionId(t2.getId());
                        t2.setLinkedTransactionId(t1.getId());
                        t2.setType(TransactionType.Transfer);
                        Log. i("TransactionParser", "Found Transfer 2 {Id1 " + t1.getId() + "} {Id2 " + t2.getId()+ "}");
                        values = reader.readNext();
                    } else {
                        values = nextValues;
                    }

                    dbHelper.insertTransactions(t1);
                    if(t2 != null) {
                        dbHelper.insertTransactions(t2);
                    }
                }
            }

        } catch (FileNotFoundException e) {
            Log.e("MyMoneyDataImporter", "Exception importing data: " + e.getMessage());
            notifyErrorToUIThread(e.getMessage());
        } catch (IOException e) {
            Log.e("MyMoneyDataImporter", "Exception importing data: " + e.getMessage());
            notifyErrorToUIThread(e.getMessage());
        } catch (Exception e) {
            Log.e("TransactionParser", "Exception importing data", e);
            notifyErrorToUIThread(e.getMessage());
        }
    }

    private Transaction parseTransaction(String values[]) throws Exception {
        // OLD
        // ID | Type | Name | Value | Date | Budget | Account | SingleRecurrent | Frequency | End | ID_TRANSFER |
        // 1423 | Expenses | Cena Jappo Kaoru | -70.0 | 15/04/2014 | Entertainment | BNL | No |  |  |  |
        // 1386 | Revenues | Prelievo Bancomat | 60.0 | 06/03/2014 |  | Wallet | No |  |  | -2 |
        // 1387 | Expenses | Prelievo Bancomat | -60.0 | 06/03/2014 |  | BNL | No |  |  | 1386 |
        // 1038 | Expenses | Golf | -342.35 | 15/06/2013 | Car | BNL | Yes | 1 Month | 18/05/2017 |  |
        // 230 | Expenses | Divano Natuzzi | -86.45 | 15/03/2012 | Home | BNL | Yes | 1 Month | 16/01/2013 |  |
        // 65 | Expenses | Mutuo | -777.56 | 30/11/2011 | Home | BNL | Yes | 1 Month |  |  |
        String id = EntityIdGenerator.ENTITY_ID_GENERATOR.createId(Transaction.class);
        TransactionDirection direction = TransactionDirection.Undef;
        String typeStr = values[1];
        if (typeStr.equals("Expenses")) {
            direction = TransactionDirection.Out;
        } else if (typeStr.equals("Revenues")) {
            direction = TransactionDirection.In;
        }
        String name = values[2];
        BigDecimal value = BigDecimal.valueOf(Double.parseDouble(values[3]));
        value = value.abs();
        DateTime date = DateTime.parse(values[4], Utils.getDateFormatterEU());
        String budget = values[5];
        String account = values[6];
        if(accountsByName.containsKey(account)) {
            account = accountsByName.get(account).getId();
        } else {
            Account defAccount = new Account(EntityIdGenerator.ENTITY_ID_GENERATOR.createId(Account.class),
                    account, BigDecimal.ZERO, MaterialColours.getBudgetColors().get(0), false);
            AccountManager.ACCOUNT_MANAGER().insertAccount(defAccount);
            accountsByName.put(defAccount.getName(), defAccount);
            account = defAccount.getId();
            Log. i("TransactionParser", "Error converting account: Account not found {AccountId" + account + "} created default");
        }

        if(budgetsByName.containsKey(budget)) {
            budget = budgetsByName.get(budget).getId();
        } else {
            Budget defBudget = new Budget(EntityIdGenerator.ENTITY_ID_GENERATOR.createId(Budget.class),
                    budget, BigDecimal.ZERO, MaterialColours.getBudgetColors().get(0),
                    1, TimeUnit.Month, new DateTime(2000, 1, 1, 0, 0));
            BudgetManager.BUDGET_MANAGER().insertBudget(defBudget);
            budgetsByName.put(defBudget.getName(), defBudget);
            budget = defBudget.getId();
            Log. i("TransactionParser", "Error converting budget: Budget not found {BudgetId" + budget + "} created default");
        }

        TransactionType type = TransactionType.Single;
        String recurrentStr = values[7];
        //Frequency
        //End date
        String linkedId = values[10];
        if (!linkedId.isEmpty()) {
            type = TransactionType.Transfer;
        }

        boolean recurrent = false;
        if (recurrentStr.equals("Yes")) {
            recurrent = true;
        }

        Transaction t = new Transaction(id, name, direction, type, account, budget, value, date);
        if (recurrent) {
            t.setRecurrent(true);

            String frequency[] = values[8].split(" ");
            int freq = Integer.parseInt(frequency[0]);
            TimeUnit unit = TimeUnit.valueOf(frequency[1]);
            t.setFrequency(freq);
            t.setFrequencyUnit(unit);

            String end = values[9];
            if (end != null && !end.isEmpty()) {
                DateTime enddate = DateTime.parse(end, Utils.getDateFormatterEU());
                t.setEndDate(enddate);
            } else {
                //TODO remove when no end transaction supported
                t.setEndDate(new DateTime(2100, 1, 1, 0, 0));
            }
        }
        return t;
    }

    private Transaction parseTransactionNew(String tValues[], String nextTValues[]) throws Exception {
        // NEW
        // Name	        Description	Type    	Date	    Value	Currency	Account	Budget	Recurring
        // Parcheggio	        	Expenses	10/05/2014	1	    EUR	        Wallet	Car
        // Golf		                Expenses	06/15/2013	342.35	EUR	        BNL	    Car	    1 Month

        String id = EntityIdGenerator.ENTITY_ID_GENERATOR.createId(Transaction.class);

        String name = tValues[0];

        TransactionDirection direction = TransactionDirection.Undef;
        String typeStr = tValues[2];
        if (typeStr.equals("Expenses")) {
            direction = TransactionDirection.Out;
        } else if (typeStr.equals("Revenues")) {
            direction = TransactionDirection.In;
        }

        DateTime date = DateTime.parse(tValues[3], Utils.getDateFormatter());

        BigDecimal value = BigDecimal.valueOf(Double.parseDouble(tValues[4]));
        value = value.abs();

        String account = tValues[6];
        if(accountsByName.containsKey(account)) {
            account = accountsByName.get(account).getId();
        } else {
            Account defAccount = new Account(EntityIdGenerator.ENTITY_ID_GENERATOR.createId(Account.class),
                    account, BigDecimal.ZERO, MaterialColours.getBudgetColors().get(0), false);
            AccountManager.ACCOUNT_MANAGER().insertAccount(defAccount);
            accountsByName.put(defAccount.getName(), defAccount);
            account = defAccount.getId();
            Log. i("TransactionParser", "Error converting account: Account not found {AccountId" + account + "} created default");
        }

        String budget = tValues[7];
        if(budgetsByName.containsKey(budget)) {
            budget = budgetsByName.get(budget).getId();
        } else {
            Budget defBudget = new Budget(EntityIdGenerator.ENTITY_ID_GENERATOR.createId(Budget.class),
                    budget, BigDecimal.ZERO, MaterialColours.getBudgetColors().get(0),
                    1, TimeUnit.Month, new DateTime(2000, 1, 1, 0, 0));
            BudgetManager.BUDGET_MANAGER().insertBudget(defBudget);
            budgetsByName.put(defBudget.getName(), defBudget);
            budget = defBudget.getId();
            Log. i("TransactionParser", "Error converting budget: Budget not found {BudgetId" + budget + "} created default");
        }

        TransactionType type = TransactionType.Single;
        if (nextTValues != null) {
            String nextName = nextTValues[0];
            DateTime nextDate = DateTime.parse(nextTValues[3], Utils.getDateFormatter());
            BigDecimal nextValue = BigDecimal.valueOf(Double.parseDouble(nextTValues[4]));
            nextValue = nextValue.abs();
            String nextTypeStr = nextTValues[2];
            TransactionDirection nextDirection = TransactionDirection.Undef;
            if (nextTypeStr.equals("Expenses")) {
                nextDirection = TransactionDirection.Out;
            } else if (nextTypeStr.equals("Revenues")) {
                nextDirection = TransactionDirection.In;
            }

            if (name.equals(nextName) && date.isEqual(nextDate) &&
                    value.compareTo(nextValue) == 0 &&
                    direction.equals(TransactionDirection.reverse(nextDirection)) ) {
                type = TransactionType.Transfer;
                Log. i("TransactionParser", "Found Transfer {Id " + id + "} " +
                        "{Name " + name + ":" + nextName + "} " +
                        "{Direction " + direction + ":" + nextDirection + "} " +
                        "{Value " + value + ":" + nextValue + "} " +
                        "{Compare " + value.compareTo(nextValue) +"}");
            }
        }

        Transaction t = new Transaction(id, name, direction, type, account, budget, value, date);
        String recurrent = tValues[8];
        if (recurrent != null && !recurrent.isEmpty()) {
            t.setRecurrent(true);

            String frequency[] = recurrent.split(" ");
            int freq = Integer.parseInt(frequency[0]);
            TimeUnit unit = TimeUnit.valueOf(frequency[1]);
            t.setFrequency(freq);
            t.setFrequencyUnit(unit);

            //TODO remove when no end transaction supported
            t.setEndDate(new DateTime(2100, 1, 1, 0, 0));
        }
        return t;
    }

    private void notifyErrorToUIThread(final String message) {
        Handler handler = new Handler(context.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                AlertDialog dialog = new AlertDialog.Builder(context)
                        .setTitle(R.string.title_error_dialog)
                        .setMessage(context.getText(R.string.error_importing_data) + " " + message)
                        .setPositiveButton(R.string.got_it, null)
                        .create();
                dialog.show();
            }
        });
    }

}
