package me.senso.sensotcp;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.IOError;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

/**
 * Created by User on 16.05.2017.
 */

public class SensoServer implements ISensoPoseHandler {

    enum SensoServerStates {
        Stopped, Started
    };

    private Thread m_serverThread = null;
    private Thread m_senderThread = null;
    private int m_port = 53450;

    private ServerSocket serverSocket;
    private final Vector<Socket> m_clientSockets;
    private final Vector<String> m_sendBuf;

    private SensoServerStates m_state = SensoServerStates.Stopped;
    public SensoServerStates GetState() { return m_state; }

    public SensoServer(int port) {
        if (port != 0) {
            m_port = port;
        }
        m_clientSockets = new Vector<>();
        m_sendBuf = new Vector<>();
    }

    public void SetPort(int newPort) {
        m_port = newPort;
    }

    public void Start() {
        if (GetState() != SensoServerStates.Stopped) return;
        m_state = SensoServerStates.Started;
        if (m_serverThread == null) {
            m_serverThread = new Thread(new SensoServerThread());
            m_serverThread.start();
        }
        if (m_senderThread == null) {
            m_senderThread = new Thread(new SensoSender());
            m_senderThread.start();
        }
    }

    public void Stop() {
        if (GetState() != SensoServerStates.Started ) return;
        m_state = SensoServerStates.Stopped;
        m_senderThread.interrupt();
        m_senderThread = null;
        m_serverThread.interrupt();
        for (Socket s : m_clientSockets) {
            try {
                s.close();
            } catch (IOException e) {}
        }
        try {
            serverSocket.close();
        } catch (IOException e) {}
        m_serverThread = null;

        m_clientSockets.clear();
    }

    @Override
    public void OnSensoPose(String jsonPose) {
        if (GetState() != SensoServerStates.Started) return;
        synchronized (m_sendBuf) {
            m_sendBuf.add(jsonPose);
        }
    }

    private class SensoServerThread implements Runnable {

        public void run() {
            Socket socket = null;
            try {
                serverSocket = new ServerSocket(m_port);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    socket = serverSocket.accept();
                    synchronized (m_clientSockets) {
                        m_clientSockets.add(socket);
                    }
                } catch (IOException e) {
                    if (!e.getMessage().equals("Socket closed")) e.printStackTrace();
                }
            }
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class SensoSender implements Runnable {
        public void run () {
            String[] arr = new String[5];
            int sendCnt = 0;

            while (!Thread.currentThread().isInterrupted()) {

                sendCnt = m_sendBuf.size();
                if (sendCnt == 0) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // eat an error
                    }
                    continue;
                }

                synchronized (m_sendBuf) {
                    if (sendCnt > 5) {
                        while (sendCnt > 5) {
                            m_sendBuf.removeElementAt(0);
                            --sendCnt;
                        }
                    }
                    if (sendCnt > 0) {
                        m_sendBuf.copyInto(arr);
                        m_sendBuf.clear();
                    }
                }

                synchronized (m_clientSockets) {
                    BufferedWriter aWriter;
                    for (Socket s : m_clientSockets) {
                        try {
                            aWriter = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                            for (int i = 0; i < sendCnt; ++i) {
                                aWriter.write(arr[i]);
                            }
                            aWriter.flush();
                        } catch (IOException e) {
                            // eat an error
                        }
                    }
                    sendCnt = 0;
                }

            }

        }
    }
}
