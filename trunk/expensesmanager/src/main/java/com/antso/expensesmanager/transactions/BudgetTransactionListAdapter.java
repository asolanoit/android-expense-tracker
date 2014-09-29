package com.antso.expensesmanager.transactions;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.antso.expensesmanager.R;
import com.antso.expensesmanager.entities.Transaction;
import com.antso.expensesmanager.enums.TransactionDirection;
import com.antso.expensesmanager.enums.TransactionType;
import com.antso.expensesmanager.utils.MaterialColours;
import com.antso.expensesmanager.utils.Utils;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class BudgetTransactionListAdapter extends AbstractTransactionListAdapter {

    private final TransactionManager transactionManager;
    private final Context context;
    private final String budget;
    private boolean nothingMoreToLoad = false;

    private List<Transaction> transactions = new ArrayList<Transaction>();

    public BudgetTransactionListAdapter(Context context, TransactionManager transactionManager, String budget) {
        this.context = context;
        this.budget = budget;
        this.transactionManager = transactionManager;
        transactionManager.resetGetBudgetNextPeriodTransactions(DateTime.now());
        load();
    }

    @Override
    public void load() {
        if(nothingMoreToLoad) {
            return;
        }

        Collection<Transaction> loaded = transactionManager.getBudgetNextPeriodTransactions(budget);
        if (loaded.size() != 0) {
            transactions.addAll(loaded);
            notifyDataSetChanged();
        } else {
            nothingMoreToLoad = true;
        }
    }

    @Override
    public int getCount() {
        return transactions.size();
    }

    @Override
    public Object getItem(int pos) {
        return transactions.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Transaction transaction = transactions.get(position);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        RelativeLayout transactionLayout = (RelativeLayout) inflater.inflate(R.layout.transaction_item_small, null, false);

        if (transaction.isAutoGenerated()) {
            transactionLayout.setBackgroundColor(MaterialColours.GREY_500);
        }

        if (transaction.getType().equals(TransactionType.Summary)) {
            transactionLayout.setBackgroundColor(MaterialColours.LIGHT_BLUE_500);
        }

        final TextView transactionDateTime = (TextView) transactionLayout.findViewById(R.id.transactionDateTime);
        DateTime d = transaction.getDateTime();
        if (transaction.getType().equals(TransactionType.Summary)) {
            transactionDateTime.setText(Utils.formatDateMonthYearOnly(d));
        } else {
            transactionDateTime.setText(Utils.formatDate(d));
        }
        transactionDateTime.setTextColor(MaterialColours.BLACK);

        final ImageView transactionRecurrent = (ImageView) transactionLayout.findViewById(R.id.transactionRecurrent);
        if (transaction.getType().equals(TransactionType.Recurrent) && !transaction.isAutoGenerated()) {
            transactionRecurrent.setVisibility(View.VISIBLE);
        } else {
            transactionRecurrent.setVisibility(View.INVISIBLE);
        }

        final TextView transactionDesc = (TextView) transactionLayout.findViewById(R.id.transactionDesc);
        transactionDesc.setText(transaction.getDescription());
        transactionDesc.setTextColor(MaterialColours.BLACK);

        final TextView transactionCurrency = (TextView) transactionLayout.findViewById(R.id.transactionCurrency);
        transactionCurrency.setText(Utils.getCurrencyString());
        transactionCurrency.setTextColor(MaterialColours.BLACK);

        final TextView transactionValue = (TextView) transactionLayout.findViewById(R.id.transactionValue);
        String balance = transaction.getValue().setScale(2).toPlainString();
        transactionValue.setText(balance);
        if (transaction.getDirection().equals(TransactionDirection.In)) {
            transactionValue.setTextColor(MaterialColours.GREEN_500);
        } else if (transaction.getDirection().equals(TransactionDirection.Out)) {
            transactionValue.setTextColor(MaterialColours.RED_500);
        }

        return transactionLayout;
    }

}
