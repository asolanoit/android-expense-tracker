package com.antso.expensesmanager.budgets;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.antso.expensesmanager.entities.Budget;
import com.antso.expensesmanager.entities.Transaction;
import com.antso.expensesmanager.enums.BudgetPeriodUnit;
import com.antso.expensesmanager.enums.TransactionDirection;
import com.antso.expensesmanager.enums.TransactionType;
import com.antso.expensesmanager.persistence.DatabaseHelper;
import com.antso.expensesmanager.transactions.TransactionManager;

import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum BudgetManager {
        BUDGET_MANAGER;

    private Map<String, BudgetInfo> budgets;
    private DatabaseHelper dbHelper = null;

    private BudgetManager() {
        budgets = new HashMap<String, BudgetInfo>();
    }

    public void start(Context context) {
        if (dbHelper == null) {
            dbHelper = new DatabaseHelper(context);

            Collection<Budget> budgets = dbHelper.getBudgets();
            for (Budget budget : budgets) {
                BudgetManager.BUDGET_MANAGER.addBudget(budget);
            }
        }
    }

    public void stop() {
        if (dbHelper != null) {
            dbHelper.close();
            dbHelper = null;
        }
    }

    public void onTransactionAdded(Transaction transaction) {
        BudgetInfo budgetInfo = budgets.get(transaction.getBudgetId());
        budgetInfo.addTransaction(transaction);
    }

    public void onTransactionDeleted(Transaction transaction) {
        BudgetInfo budgetInfo= budgets.get(transaction.getBudgetId());
        budgetInfo.removeTransaction(transaction);
    }

    private void addBudget(Budget budget) {
        Collection<Transaction> transactions = dbHelper.getTransactionsByBudget(budget.getId());
        BudgetInfo budgetInfo = new BudgetInfo(budget, transactions);
        budgets.put(budget.getId(), budgetInfo);
    }

    public void insertBudget(Budget budget) {
        dbHelper.insertBudget(budget);
        addBudget(budget);
    }

    public void removeBudget(Budget budget) {
        budgets.remove(budget.getId());
        dbHelper.deleteBudget(budget.getId());
    }

    public List<BudgetInfo> getBudgetInfo() {
        List<BudgetInfo> budgetInfoList = new ArrayList<BudgetInfo>(budgets.size());
        for(BudgetInfo info : budgets.values()) {
            budgetInfoList.add(info);
        }
        return budgetInfoList;
    }

    public BudgetInfo getBudgetInfo(String budgetId) {
        return budgets.get(budgetId);
    }

    public Map<String, Budget> getBudgetsByName() {
        Map<String, Budget> budgetByName = new HashMap<String, Budget>(budgets.size());
        for(BudgetInfo info : budgets.values()) {
            if(!budgetByName.containsKey(info.budget.getName())) {
                budgetByName.put(info.budget.getName(), info.budget);
            } else {
                Log. i("BudgetManager", "Error creating budgetByName map: Budget with this name already added {Name " + info.budget.getName() + "}");
            }
        }
        return budgetByName;
    }

    public Collection<Budget> getBudgets() {
        return dbHelper.getBudgets();
    }

    public class BudgetInfo {
        public Collection<Transaction> transactions = new ArrayList<Transaction>();
        public Budget budget;

        public BigDecimal periodIn;
        public BigDecimal periodOut;
        public BigDecimal periodBalance;

        public BudgetInfo(Budget budget, Collection<Transaction> transactions) {
            this.budget = budget;

            for (Transaction transaction : transactions) {
                this.transactions.add(transaction);
                if (transaction.getType().equals(TransactionType.Recurrent) &&
                        !transaction.isAutoGenerated()) {
                    this.transactions.addAll(
                            TransactionManager.explodeRecurrentTransaction(transaction, DateTime.now()));
                }
            }
            refresh(DateTime.now(), this.transactions, true, false);
        }

        public void addTransaction(Transaction transaction) {
            transactions.add(transaction);
            refresh(DateTime.now(), Collections.singleton(transaction), false, false);
        }

        public void removeTransaction(Transaction transaction) {
            transactions.remove(transaction);
            refresh(DateTime.now(), Collections.singleton(transaction), false, true);
        }

        public void refresh(DateTime currentDateTime, Collection<Transaction> transactions, boolean reset, boolean remove) {
            if (reset) {
                periodIn = BigDecimal.ZERO;
                periodOut = BigDecimal.ZERO;
                periodBalance = BigDecimal.ZERO;
            }

            Pair<DateTime, DateTime> periodStartEnd = getPeriodStartEnd(currentDateTime);
            DateTime periodStart = periodStartEnd.first;
            DateTime periodEnd = periodStartEnd.second;

            for (Transaction transaction : transactions) {
                if(transaction.getDateTime().isAfter(periodStart) &&
                        transaction.getDateTime().isBefore(periodEnd)){

                    BigDecimal value = transaction.getValue();
                    if (remove) {
                        value = value.negate();
                    }

                    if (transaction.getDirection().equals(TransactionDirection.Out)) {
                        periodBalance = periodBalance.subtract(value);
                        periodOut = periodOut.add(value);
                    }
                    if (transaction.getDirection().equals(TransactionDirection.In)) {
                        periodBalance = periodBalance.add(value);
                        periodIn = periodIn.add(value);
                    }
                }
            }
        }

        public Pair<DateTime, DateTime> getPeriodStartEnd(DateTime currentDateTime) {
            int iterationNum = 1;
            DateTime start = budget.getPeriodStart();
            int periodLength = budget.getPeriodLength();
            BudgetPeriodUnit periodUnit = budget.getPeriodUnit();
            DateTime periodStart = start;
            DateTime periodStartOld = start;
            while (periodStart.isBefore(currentDateTime)) {
                periodStartOld = periodStart;
                switch (periodUnit) {
                    case Day:
                        periodStart = start.plusDays(periodLength * iterationNum);
                        break;
                    case Week:
                        periodStart = start.plusWeeks(periodLength * iterationNum);
                        break;
                    case Month:
                        periodStart = start.plusMonths(periodLength * iterationNum);
                        break;
                    case Year:
                        periodStart = start.plusYears(periodLength * iterationNum);
                        break;
                }
                iterationNum++;
            }

            return  new Pair<DateTime, DateTime>(periodStartOld, periodStart);
        }

        public int getPercentage() {
            BigDecimal balance = periodOut.subtract(periodIn);
            if(balance.compareTo(BigDecimal.ZERO) < 0) {
                return 0;
            } else {
                double result = balance.doubleValue();
                result = result / (budget.getThreshold().doubleValue()) * 100;
                return (int) result;
            }
        }

    }

}
