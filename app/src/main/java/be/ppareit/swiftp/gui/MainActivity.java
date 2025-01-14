/*******************************************************************************
 * Copyright (c) 2012-2013 Pieter Pareit.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * Contributors:
 * Pieter Pareit - initial API and implementation
 ******************************************************************************/

package be.ppareit.swiftp.gui;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import net.vrallev.android.cat.Cat;

import java.util.Arrays;

import be.ppareit.swiftp.App;
import be.ppareit.swiftp.BuildConfig;
import be.ppareit.swiftp.FsService;
import be.ppareit.swiftp.FsSettings;
import be.ppareit.swiftp.R;
import be.ppareit.swiftp.Util;

/**
 * This is the main activity for swiftp, it enables the user to start the server service
 * and allows the users to change the settings.
 */
public class MainActivity extends AppCompatActivity {

    final static int PERMISSIONS_REQUEST_CODE = 12;
    private static final String TAG = "MainActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Cat.d("created");
        setTheme(FsSettings.getTheme());
        super.onCreate(savedInstanceState);

        if (!haveReadWritePermissions()) {
            requestReadWritePermissions();
        }

        if (!haveNotiPermissions()) {
            requestNotiPermissions();
        }

        if (App.isFreeVersion() && App.isPaidVersionInstalled()) {
            Cat.d("Running demo while paid is installed");
            AlertDialog ad = new AlertDialog.Builder(this)
                    .setTitle(R.string.demo_while_paid_dialog_title)
                    .setMessage(R.string.demo_while_paid_dialog_message)
                    .setPositiveButton(getText(android.R.string.ok), (d, w) -> finish())
                    .create();
            ad.show();
        }

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new PreferenceFragment())
                .commit();
    }

    @Override
    protected void onStart() {
        Intent intent = getIntent();
        if (intent != null) {
            String action = intent.getStringExtra("action");
            Log.w(TAG, " value " + action);
                if (action != null) {
                    if (action.equals("start")) {
                        if (!Util.useScopedStorage() || FsSettings.getExternalStorageUri() != null) {
                            FsService.start();
                        }
                    }
                    else if (action.equals("stop")) {
                        FsService.stop();
                    }
                }
        }
        super.onStart();
    }

    private boolean haveReadWritePermissions() {
        if (VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED
                    && checkSelfPermission(WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestReadWritePermissions() {
        if (VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        // We can no longer request READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE,
        // This is not allowed. We use scoped storage in stead
        if (VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        String[] permissions = new String[]{READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE};
        requestPermissions(permissions, PERMISSIONS_REQUEST_CODE);
    }

    private boolean haveNotiPermissions() {
        if (VERSION.SDK_INT >= 33) {
            return checkSelfPermission(POST_NOTIFICATIONS) == PERMISSION_GRANTED
                    && checkSelfPermission(POST_NOTIFICATIONS) == PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestNotiPermissions() {
        if (VERSION.SDK_INT < 33) return;
        String[] permissions = new String[]{POST_NOTIFICATIONS};
        requestPermissions(permissions, PERMISSIONS_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != PERMISSIONS_REQUEST_CODE) {
            Cat.e("Unhandled request code");
            return;
        }
        Cat.d("permissions: " + Arrays.toString(permissions));
        Cat.d("grantResults: " + Arrays.toString(grantResults));
        if (grantResults.length > 0) {
            // Permissions not granted, close down
            for (int result : grantResults) {
                if (result != PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.unable_to_proceed_no_permissions,
                            Toast.LENGTH_LONG).show();
                    Cat.e("Unable to proceed, no permissions given.");
                    finish();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_feedback) {
            String to = "pieter.pareit@gmail.com";
            String subject = "FTP Server feedback";
            String message = "Device: " + Build.MODEL + "\n" +
                    "Android version: " + VERSION.RELEASE + "-" + VERSION.SDK_INT + "\n" +
                    "Application: " + BuildConfig.APPLICATION_ID + " (" + BuildConfig.FLAVOR + ")\n" +
                    "Application version: " + BuildConfig.VERSION_NAME + " - " + BuildConfig.VERSION_CODE + "\n" +
                    "Feedback: \n_";

            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{to});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            emailIntent.putExtra(Intent.EXTRA_TEXT, message);
            emailIntent.setType("message/rfc822");

            try {
                startActivity(emailIntent);
                Toast.makeText(this, R.string.use_english, Toast.LENGTH_LONG).show();
            } catch (ActivityNotFoundException exception) {
                Toast.makeText(this, R.string.unable_to_start_mail_client, Toast.LENGTH_LONG).show();
            }

        } else if (item.getItemId() == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
        }

        return true;
    }
}
