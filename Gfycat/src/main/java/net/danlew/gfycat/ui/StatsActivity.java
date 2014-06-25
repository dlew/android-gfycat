package net.danlew.gfycat.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import net.danlew.gfycat.R;
import net.danlew.gfycat.Stats;

/**
 * Show stats on Gfycat usage
 */
public class StatsActivity extends Activity {

    @InjectView(R.id.savings)
    TextView mSavingsTextView;

    @InjectView(R.id.explanation)
    TextView mExplanationTextView;

    private Stats mStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStats = new Stats(this);

        setContentView(R.layout.activity_stats);

        ButterKnife.inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        long savings = mStats.getTotalSavings();
        if (savings == 0) {
            mSavingsTextView.setText(R.string.diddly_squat);

            mExplanationTextView.setVisibility(View.VISIBLE);
        }
        else {
            String formattedSavings = Formatter.formatFileSize(this, savings);
            mSavingsTextView.setText(formattedSavings);

            mExplanationTextView.setVisibility(View.GONE);
        }
    }
}
