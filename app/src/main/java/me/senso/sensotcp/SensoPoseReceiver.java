package me.senso.sensotcp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Sergey Birukov on 16.05.2017.
 */

public class SensoPoseReceiver extends BroadcastReceiver {

    ArrayList<ISensoPoseHandler> m_handlers;
    SensoPoseReceiver() {
        m_handlers = new ArrayList<>();
    }

    public void AddHandler(ISensoPoseHandler newHandler) {
        m_handlers.add(newHandler);
    }

    public void RemoveHandler(ISensoPoseHandler aHandler) {
        m_handlers.remove(aHandler);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String jsonPose = intent.getStringExtra("json");
        for (ISensoPoseHandler h : m_handlers) {
            h.OnSensoPose(jsonPose);
        }
    }

}
