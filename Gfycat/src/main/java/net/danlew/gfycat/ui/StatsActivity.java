package net.danlew.gfycat.ui;

import android.app.Activity;
import android.os.Bundle;
import net.danlew.gfycat.R;

/**
 * Show stats on Gfycat usage
 */
public class StatsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_stats);
    }
}
