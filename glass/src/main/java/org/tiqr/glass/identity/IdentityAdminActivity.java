package org.tiqr.glass.identity;

import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import org.tiqr.authenticator.auth.AuthenticationChallenge;
import org.tiqr.authenticator.datamodel.DbAdapter;
import org.tiqr.authenticator.datamodel.Identity;
import org.tiqr.glass.R;

/**
 * Manage identities.
 */
public class IdentityAdminActivity extends Activity {
    private CardScrollView _scrollView;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        _scrollView = new CardScrollView(this);
        setContentView(_scrollView);

        final IdentityScrollAdapter scrollAdapter = new IdentityScrollAdapter(this);
        scrollAdapter.setFootnote(getString(R.string.authentication_identity_select_footnote));

        DbAdapter db = new DbAdapter(this);
        Identity[] identities = db.getAllIdentitiesAsObjects();
        scrollAdapter.setIdentities(identities);

        _scrollView.setAdapter(scrollAdapter);

        _scrollView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                _onIdentityTap(scrollAdapter.getItem(position));
            }
        });
    }

    /**
     * Identity tap.
     *
     * @param identity
     */
    private void _onIdentityTap(Identity identity) {

    }


    @Override
    protected void onResume() {
        super.onResume();
        _scrollView.activate();
    }

    @Override
    protected void onPause() {
        _scrollView.deactivate();
        super.onPause();
    }
}
