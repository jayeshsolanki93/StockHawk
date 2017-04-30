package com.udacity.stockhawk.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.Contract.*;
import com.udacity.stockhawk.data.PrefUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

class StockAppWidgetFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context mContext;

    private Cursor mCursor;

    private final DecimalFormat dollarFormatWithPlus;
    private final DecimalFormat dollarFormat;
    private final DecimalFormat percentageFormat;

    StockAppWidgetFactory(Context context) {
        mContext = context;

        dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus.setPositivePrefix("+$");
        percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
        percentageFormat.setMaximumFractionDigits(2);
        percentageFormat.setMinimumFractionDigits(2);
        percentageFormat.setPositivePrefix("+");
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {
        if (mCursor !=  null) {
            mCursor.close();
        }
        String[] projection = {
                Quote._ID,
                Quote.COLUMN_SYMBOL,
                Quote.COLUMN_PRICE,
                Quote.COLUMN_ABSOLUTE_CHANGE,
                Quote.COLUMN_PERCENTAGE_CHANGE,
        };

        final long token = Binder.clearCallingIdentity();

        mCursor = mContext.getContentResolver().query(Quote.URI, projection, null, null, null);

        Binder.restoreCallingIdentity(token);
    }

    @Override
    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
        }
    }

    @Override
    public int getCount() {
        return mCursor == null? 0 : mCursor.getCount();
    }

    @Override
    public RemoteViews getViewAt(int i) {
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_list_item_quote);
        if (mCursor.moveToPosition(i)) {
            rv.setTextViewText(R.id.symbol, mCursor.getString(Quote.POSITION_SYMBOL));
            rv.setTextViewText(R.id.price, dollarFormat.format(mCursor.getFloat(Quote.POSITION_PRICE)));

            float rawAbsoluteChange = mCursor.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
            float percentageChange = mCursor.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

            if (rawAbsoluteChange > 0) {
                rv.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_green);
            } else {
                rv.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_red);
            }

            String change = dollarFormatWithPlus.format(rawAbsoluteChange);
            String percentage = percentageFormat.format(percentageChange / 100);

            if (PrefUtils.getDisplayMode(mContext)
                    .equals(mContext.getString(R.string.pref_display_mode_absolute_key))) {
                rv.setTextViewText(R.id.change, change);
            } else {
                rv.setTextViewText(R.id.change, percentage);
            }
        }
        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int i) {
        if (mCursor.moveToPosition(i)) {
            return mCursor.getLong(mCursor.getColumnIndex(Quote._ID));
        }
        return i;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
