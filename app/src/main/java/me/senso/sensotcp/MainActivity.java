package me.senso.sensotcp;

import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private SensoPoseReceiver sensoReceiver;
    private SensoServer m_sensoServer;
    private Button startStopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((EditText)findViewById(R.id.address_edit)).setText(getIPAddress());
        ((EditText)findViewById(R.id.port_edit)).setText(String.valueOf(getServerPort()));

        // Setup receiver
        sensoReceiver = new SensoPoseReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("senso.me.sensoruntime.GLOVE_POSE");
        intentFilter.setPriority(2147483647);
        registerReceiver(sensoReceiver, intentFilter);

        // Setup tcp server
        m_sensoServer = new SensoServer(53450);
        sensoReceiver.AddHandler(m_sensoServer);

        startStopButton = (Button)findViewById(R.id.start_button);
        startStopButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (m_sensoServer.GetState() == SensoServer.SensoServerStates.Stopped) {
                    m_sensoServer.Start();
                    startStopButton.setText(R.string.stop_server);
                } else {
                    m_sensoServer.Stop();
                    startStopButton.setText(R.string.start_server);
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        sensoReceiver.RemoveHandler(m_sensoServer);
        m_sensoServer.Stop();
        unregisterReceiver(sensoReceiver);
    }

    /**
     * Get IP address from first non-localhost interface
     * @return  address or empty string
     */
    public static String getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':')<0;
                        if (isIPv4)
                            return sAddr;
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }

    private int getServerPort() {
        return 53450;
    }
}
