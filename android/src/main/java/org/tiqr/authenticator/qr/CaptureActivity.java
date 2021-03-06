/* This code is based on code from the ZXing project.
 *  
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tiqr.authenticator.qr;

import java.io.IOException;

import org.tiqr.authenticator.ActivityDialog;
import org.tiqr.authenticator.Application;
import org.tiqr.authenticator.R;
import org.tiqr.authenticator.TiqrActivity;
import org.tiqr.authenticator.auth.AuthenticationChallenge;
import org.tiqr.authenticator.auth.EnrollmentChallenge;
import org.tiqr.authenticator.authentication.AuthenticationActivityGroup;
import org.tiqr.authenticator.enrollment.EnrollmentActivityGroup;
import org.tiqr.authenticator.qr.camera.CameraManager;
import org.tiqr.service.authentication.AuthenticationService;
import org.tiqr.service.authentication.ParseAuthenticationChallengeError;
import org.tiqr.service.enrollment.EnrollmentService;
import org.tiqr.service.enrollment.ParseEnrollmentChallengeError;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.zxing.Result;

import javax.inject.Inject;

/**
 * Capture activity.
 */
public class CaptureActivity extends TiqrActivity implements SurfaceHolder.Callback
{
    private static final String TAG = CaptureActivity.class.getSimpleName();

    private CaptureActivityHandler handler;
    private ViewfinderView viewfinderView;
    private boolean hasSurface;
    private BeepManager beepManager;
    private ActivityDialog activityDialog;

    protected @Inject AuthenticationService _authenticationService;
    protected @Inject EnrollmentService _enrollmentService;

    public ViewfinderView getViewfinderView()
    {
        return viewfinderView;
    }

    public Handler getHandler()
    {
        return handler;
    }

    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle, R.layout.capture);

        ((Application)getApplication()).inject(this);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        disableIdentityButton();
        hideLeftButton();
        setTitle(R.string.scan_button);

        CameraManager.init(getApplication());
        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
        handler = null;
        hasSurface = false;
        beepManager = new BeepManager(this);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still
            // exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the
            // camera.
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        beepManager.updatePrefs();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        CameraManager.get().closeDriver();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {

    }

    /**
     * A valid barcode has been found, so give an indication of success and show
     * the results.
     * 
     * @param rawResult
     *            The contents of the barcode.
     * @param barcode
     *            A greyscale bitmap of the camera data which was decoded.
     */
    public void handleDecode(final Result rawResult, Bitmap barcode)
    {
        activityDialog = ActivityDialog.show(this);
        beepManager.playBeepSoundAndVibrate();

        String rawChallenge = rawResult.getText();
        if (_enrollmentService.isEnrollmentChallenge(rawChallenge)) {
            _enroll(rawChallenge);
        } else {
            _authenticate(rawChallenge);
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder)
    {
        try {
            CameraManager.get().openDriver(surfaceHolder);
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            return;
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.lang.RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializating camera", e);
            return;
        }
        
        if (handler == null) {
            handler = new CaptureActivityHandler(this);
        
            Message msg = new Message();
            msg.what = R.id.scan_inactivity;
            handler.sendMessageDelayed(msg, 3000);
        }
    }

    public void drawViewfinder()
    {
        viewfinderView.drawViewfinder();
    }

	public void handleInactivity() {
		// TODO Auto-generated method stub
		TextView statusView = (TextView) findViewById(R.id.status_view);
		statusView.setVisibility(View.VISIBLE);
	}

    /**
     * Parse authentication challenge and start authentication process.
     *
     * @param challenge
     */
    private void _authenticate(String challenge) {
        _authenticationService.parseAuthenticationChallenge(challenge, new AuthenticationService.OnParseAuthenticationChallengeListener() {
            @Override
            public void onParseAuthenticationChallengeSuccess(AuthenticationChallenge challenge) {
                activityDialog.cancel();
                Intent intent = new Intent(getApplicationContext(), AuthenticationActivityGroup.class);
                intent.putExtra("org.tiqr.challenge", challenge);
                intent.putExtra("org.tiqr.protocolVersion", "2");
                startActivity(intent);
            }

            @Override
            public void onParseAuthenticationChallengeError(ParseAuthenticationChallengeError error) {
                activityDialog.cancel();

                new AlertDialog.Builder(CaptureActivity.this)
                        .setTitle(error.getTitle())
                        .setMessage(error.getMessage())
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok_button, null)
                        .show();
            }
        });
    }

    /**
     * Parse enrollment challenge and start enrollment process.
     *
     * @param challenge
     */
    private void _enroll(String challenge) {
        _enrollmentService.parseEnrollmentChallenge(challenge, new EnrollmentService.OnParseEnrollmentChallengeListener() {
            @Override
            public void onParseEnrollmentChallengeSuccess(EnrollmentChallenge challenge) {
                activityDialog.cancel();
                Intent intent = new Intent(getApplicationContext(), EnrollmentActivityGroup.class);
                intent.putExtra("org.tiqr.challenge", challenge);
                intent.putExtra("org.tiqr.protocolVersion", "2");
                startActivity(intent);
            }

            @Override
            public void onParseEnrollmentChallengeError(ParseEnrollmentChallengeError error) {
                activityDialog.cancel();

                new AlertDialog.Builder(CaptureActivity.this)
                        .setTitle(error.getTitle())
                        .setMessage(error.getMessage())
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok_button, null)
                        .show();
            }
        });
    }
}
