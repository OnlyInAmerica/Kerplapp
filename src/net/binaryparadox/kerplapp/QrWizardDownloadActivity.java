
package net.binaryparadox.kerplapp;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class QrWizardDownloadActivity extends Activity {
    private static final String TAG = "QrWizardDownloadActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qr_wizard_activity);
        TextView instructions = (TextView) findViewById(R.id.qrWizardInstructions);
        instructions.setText(R.string.qr_wizard_download_instructions);
        Button next = (Button) findViewById(R.id.qrNextButton);
        next.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String qrString = "";
        if (prefs.getBoolean("use_https", false))
            qrString += "https";
        else
            qrString += "http";
        qrString += "://" + KerplappApplication.ipAddressString;
        qrString += ":" + KerplappApplication.port;

        Bitmap qrBitmap = Utils.generateQrCode(this, qrString);
        ImageView repoQrCodeImageView = (ImageView) findViewById(R.id.qrWizardImage);
        repoQrCodeImageView.setImageBitmap(qrBitmap);
        Log.i(TAG, "qr: " + qrString);

        TextView wifiNetworkName = (TextView) findViewById(R.id.qrWifiNetworkName);
        wifiNetworkName.setText(qrString.replaceFirst("http://", ""));
    }
}
