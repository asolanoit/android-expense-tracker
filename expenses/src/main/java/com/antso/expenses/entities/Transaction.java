package com.antso.expenses.entities;

import android.content.Context;

import com.antso.expenses.enums.TimeUnit;
import com.antso.expenses.enums.TransactionDirection;
import com.antso.expenses.enums.TransactionType;
import com.antso.expenses.utils.Utils;

import org.joda.time.DateTime;

import java.math.BigDecimal;

/**
 * Created by asolano on 5/11/2014.
 *
 * This bean represent a financial menu_transaction_list as:
 *  - money revenue
 *  - money transfer
 *  - money payment
 */
public class Transaction {
    private String id;

    private String description;
    private TransactionDirection direction;
    private TransactionType type;
    private String accountId;
    private String budgetId;

    private BigDecimal value;
    private DateTime date;

    private String linkedTransactionId;

    private boolean hasFee;
    private String feeTransactionId;

    private boolean recurrent;
    private int frequency;
    private TimeUnit frequencyUnit;
    private DateTime endDate;
    private int repetitionNum;

    //not persisted
    private boolean autoGenerated;

    public Transaction(final String id, final String description,
            final TransactionDirection direction,
            final TransactionType type,
            final String accountId,
            final String budgetId,
            final BigDecimal value,
            final DateTime date) {
        this.id = id;
        this.description = description;
        this.direction = direction;
        this.type = type;
        this.accountId = accountId;
        this.budgetId = budgetId;
        this.value = value;
        this.date = date;

        this.linkedTransactionId = "";

        this.hasFee = false;
        this.feeTransactionId = "";

        this.frequency = 0;
        this.frequencyUnit = TimeUnit.Undef;
        this.endDate = Utils.DEFAULT_DATE;
        this.repetitionNum = 0;
        this.autoGenerated = false;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public TransactionDirection getDirection() {
        return direction;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public TransactionType getType() {
        return type;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccount(String accountId) {
        this.accountId = accountId;
    }

    public String getBudgetId() {
        return budgetId;
    }

    public void setBudget(String budgetId) {
        this.budgetId = budgetId;
    }

    public BigDecimal getValue() {
        return value;
    }

    public DateTime getDate() {
        return date;
    }

    public String getLinkedTransactionId() {
        return linkedTransactionId;
    }

    public void setLinkedTransactionId(String transactionId) {
        linkedTransactionId = transactionId;
    }

    public boolean hasFee() {
        return hasFee;
    }

    public String getFeeTransactionId() {
        return feeTransactionId;
    }

    public void setFeeTransactionId(String transactionId) {
        if (!transactionId.isEmpty()) {
            hasFee = true;
            feeTransactionId = transactionId;
        }
    }

    public void setRecurrent(boolean recurrent) {
        this.recurrent = recurrent;
    }

    public boolean getRecurrent() {
        return recurrent;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public void setFrequencyUnit(TimeUnit frequencyUnit) {
        this.frequencyUnit = frequencyUnit;
    }

    public void setEndDate(DateTime endDate) {
        this.endDate = endDate;
    }

    public void setRepetitionNum(int repetitionNum) {
        this.repetitionNum = repetitionNum;
    }

    public int getFrequency() {
        return frequency;
    }

    public TimeUnit getFrequencyUnit() {
        return frequencyUnit;
    }

    public DateTime getEndDate() {
        return endDate;
    }

    public int getRepetitionNum() {
        return repetitionNum;
    }

    public boolean isAutoGenerated() {
        return autoGenerated;
    }

    public void setAutoGenerated(boolean autoGenerated) {
        this.autoGenerated = autoGenerated;
    }

    @Override
    public String toString() {
        return description;
    }

    public String toDetailedString(Context context) {
        String fullDescription = getDescription() +
                " - " + Utils.getCurrencyString(context) + " " +
                getValue().setScale(2).toPlainString();
        return fullDescription;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction)) return false;

        Transaction that = (Transaction) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
