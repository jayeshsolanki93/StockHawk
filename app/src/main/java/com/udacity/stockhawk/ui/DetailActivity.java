package com.udacity.stockhawk.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract.Quote;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.view.LineChartView;

public class DetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_ID = 800;

    static final String EXTRA_STOCK_SYMBOL = "stock_symbol_extra";

    @BindView(R.id.chart)
    LineChartView chart;

    @BindView(R.id.symbol)
    TextView tvSymbol;

    @BindView(R.id.price)
    TextView tvPrice;

    @BindView(R.id.change)
    TextView tvChange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        ButterKnife.bind(this);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        Intent intent = getIntent();
        String symbol = intent.getStringExtra(EXTRA_STOCK_SYMBOL);

        Bundle args = new Bundle();
        args.putString(EXTRA_STOCK_SYMBOL, symbol);
        getSupportLoaderManager().initLoader(LOADER_ID, args, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String symbol = args.getString(EXTRA_STOCK_SYMBOL);

        String[] projection = new String[Quote.QUOTE_COLUMNS.size()];
        projection = Quote.QUOTE_COLUMNS.toArray(projection);

        String selection = Quote.COLUMN_SYMBOL + " = ?";

        String[] selectionArgs = new String[]{symbol};

        return new CursorLoader(this,
                Quote.URI,
                projection,
                selection,
                selectionArgs,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToFirst()) {
            String symbol = data.getString(data.getColumnIndex(Quote.COLUMN_SYMBOL));
            tvSymbol.setText(symbol);

            float price = data.getFloat(data.getColumnIndex(Quote.COLUMN_PRICE));
            tvPrice.setText(String.format("$%s", price));

            float absoluteChange = data.getFloat(data.getColumnIndex(Quote.COLUMN_ABSOLUTE_CHANGE));
            float percentChange = data.getFloat(data.getColumnIndex(Quote.COLUMN_PERCENTAGE_CHANGE));

            tvChange.setText((String.format("%s (%s%%)", absoluteChange, percentChange)));
            String history = data.getString(data.getColumnIndex(Quote.COLUMN_HISTORY));

            String[] weeklyStockData = history.split("\n");

            List<PointValue> values = new ArrayList<>();

            long oldestTime = Long.MAX_VALUE;
            long latestTime = Long.MIN_VALUE;
            for (int i = weeklyStockData.length - 1; i >= 0 ; i--) {
                String stockData = weeklyStockData[i];
                String[] temp = stockData.split(", ");

                long timeInMillis = Long.parseLong(temp[0]);
                float stockPrice = Float.parseFloat(temp[1]);

                if (timeInMillis < oldestTime) {
                    oldestTime = timeInMillis;
                }

                if (timeInMillis > latestTime) {
                    latestTime = timeInMillis;
                }

                values.add(new PointValue(timeInMillis / 1000, stockPrice));
            }
            Line line = new Line(values)
                    .setColor(getResources().getColor(R.color.colorPrimaryDark))
                    .setCubic(false)
                    .setFilled(false)
                    .setHasLabels(false)
                    .setHasLabelsOnlyForSelected(false)
                    .setHasLines(true)
                    .setHasPoints(false);

            List<Line> lines = new ArrayList<>();
            lines.add(line);

            Axis yAxis = new Axis()
                    .setName("Stock Price")
                    .setHasLines(true);

            String from = convertMillisToFormattedDate(oldestTime);
            String to = convertMillisToFormattedDate(latestTime);

            Axis xAxis = new Axis()
                    .setValues(Arrays.asList(new AxisValue(1)))
                    .setName(String.format("Weekly History (%s - %s)", from, to));

            LineChartData chartData = new LineChartData();
            chartData.setLines(lines);
            chartData.setAxisYLeft(yAxis);
            chartData.setAxisXBottom(xAxis);

            chart.setLineChartData(chartData);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    public String convertMillisToFormattedDate(long milliSeconds) {
        String dateFormat = "dd/MM/yy";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return simpleDateFormat.format(calendar.getTime());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

}
