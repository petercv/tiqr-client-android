package org.tiqr.glass.about;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.google.android.glass.widget.CardBuilder;

import org.tiqr.glass.R;

/**
 * About.
 */
public class AboutActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        View cardView =
                new CardBuilder(this, CardBuilder.Layout.TEXT)
                        .setText(R.string.about)
                        .setFootnote(R.string.copyright)
                        .getView();

        setContentView(cardView);
    }
}
