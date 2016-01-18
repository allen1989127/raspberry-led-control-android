
package org.sz.raspberryledcontrol;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import org.sz.raspberryledcontrol.R;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends Activity implements OnClickListener {

    private final String TAG = "led_control";

    private final int TURN_ON = 0;
    private final int TURN_OFF = 1;

    private Button mTurnBtn;
    private HandlerThread mThread;
    private Handler mHandler;
    private Handler mMainHandler;

    private int action = TURN_ON;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.layout_main);

        mTurnBtn = (Button) findViewById(R.id.btn_turn);

        mThread = new HandlerThread("btn_turn");
        mThread.start();

        mMainHandler = new Handler(Looper.getMainLooper());

        mHandler = new Handler(mThread.getLooper()) {

            private Socket ledClient;

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case TURN_ON:
                    case TURN_OFF:
                        sendTurnMessage(msg.what);
                        break;
                    default:
                        Log.e(TAG, "Error code : " + msg.what);
                        break;
                }
            }

            private void sendTurnMessage(int what) {
                DataOutputStream dout = null;
                DataInputStream din = null;

                try {
                    ledClient = new Socket("172.23.3.3", 8888);
                    dout = new DataOutputStream(ledClient.getOutputStream());
                    din = new DataInputStream(ledClient.getInputStream());

                    dout.writeByte(what);
                    dout.flush();

                    byte ret = din.readByte();
                    if (ret == 0) {
                        success(what);
                    } else {
                        failure();
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    failure();
                } catch (IOException e) {
                    e.printStackTrace();
                    failure();
                } finally {
                    try {
                        dout.close();
                        din.close();
                        ledClient.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            private void failure() {
                mMainHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        mTurnBtn.setEnabled(true);
                    }

                });
            }

            private void success(final int what) {
                mMainHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        if (what == TURN_ON) {
                            action = TURN_OFF;
                            mTurnBtn.setText(R.string.btn_show_off);
                        } else if (what == TURN_OFF) {
                            action = TURN_ON;
                            mTurnBtn.setText(R.string.btn_show_on);
                        } else {
                            Log.d(TAG, "error what in success : " + what);
                        }

                        mTurnBtn.setEnabled(true);
                    }

                });
            }

        };

        mTurnBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View arg0) {
        if (action == TURN_ON) {
            mTurnBtn.setEnabled(false);
            mHandler.sendEmptyMessage(TURN_ON);
        } else if (action == TURN_OFF) {
            mTurnBtn.setEnabled(false);
            mHandler.sendEmptyMessage(TURN_OFF);
        } else {
            Log.e(TAG, "error action : " + action);
        }
    }

}
