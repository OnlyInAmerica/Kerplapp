
package net.binaryparadox.kerplapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class QrWizardWifiNetworkActivity extends Activity {
    private static final String TAG = "QrWizardWifiNetworkActivity";

    private WifiManager wifiManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        setContentView(R.layout.qr_wizard_activity);
        TextView instructions = (TextView) findViewById(R.id.qrWizardInstructions);
        instructions.setText(R.string.qr_wizard_wifi_network_instructions);
        Button next = (Button) findViewById(R.id.qrNextButton);
        next.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), QrWizardDownloadActivity.class);
                startActivityForResult(intent, 0);
                finish();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        int wifiState = wifiManager.getWifiState();
        if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            // http://zxing.appspot.com/generator/
            // WIFI:S:openwireless.org;; // no pw
            // WIFI:S:openwireless.org;T:WPA;;
            // WIFI:S:openwireless.org;T:WEP;;
            // WIFI:S:openwireless.org;H:true;; // hidden
            // WIFI:S:openwireless.org;T:WPA;H:true;; // all
            String qrString = "WIFI:S:";
            qrString += wifiInfo.getSSID();
            // TODO get encryption state (none, WEP, WPA)
            /*
             * WifiConfiguration wc = null; for (WifiConfiguration i :
             * wifiManager.getConfiguredNetworks()) { if (i.status ==
             * WifiConfiguration.Status.CURRENT) { wc = i; break; } } if (wc !=
             * null)
             */
            if (wifiInfo.getHiddenSSID())
                qrString += ";H:true";
            qrString += ";;";
            Bitmap qrBitmap = Utils.generateQrCode(this, qrString);
            ImageView repoQrCodeImageView = (ImageView) findViewById(R.id.qrWizardImage);
            repoQrCodeImageView.setImageBitmap(qrBitmap);
            Log.i(TAG, "qr: " + qrString);

            TextView wifiNetworkName = (TextView) findViewById(R.id.qrWifiNetworkName);
            wifiNetworkName.setText(wifiInfo.getSSID());
            Log.i(TAG, "wifi network name: " + wifiInfo.getSSID());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // this wizard is done, clear this Activity from the history
        if (resultCode == RESULT_OK)
            finish();
    }
}
