package com.example.connectfour;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.connectfour.gameboard.GameBoardView;
import com.example.connectfour.gameboard.GameState;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice[] mDevices;
    private String[] names;

    private SendReceive mSendReceive;

    private FragmentManager mFragmentManager;

    private Button listenButton;
    private Button devicesButton;
    private TextView statusTextView;
    private ListView deviceListView;
    private Button startButton;
    private GameBoardView gameBoard;
    private RelativeLayout menuLayout;
    private ScrollView scroller;
    private LinearLayout gameBoardLayout;
    private Button disconnectButton;

    private final static int STATE_LISTENING = 1;
    private final static int STATE_CONNECTING = 2;
    private final static int STATE_CONNECTED = 3;
    private final static int STATE_CONNECTION_FAILED = 4;
    private final static int STATE_MESSAGE_RECEIVED = 5;
    private final static int STATE_GAME_STARTED = 6;

    private int playerColor;
    private int opponentColor;
    private int gameState = 0;
    private int isMyTurn = 0;


    private final static int REQUEST_ENABLE_BLUETOOTH = 1;

    private final static String APP_NAME = "Connect Four";
    private final static UUID MY_UUID = UUID.fromString("1befd2fe-9f61-4209-8d83-fa9cb53c3204");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFragmentManager = getSupportFragmentManager();
        enableBluetooth();
        init();
        showMenu();
    }


    @SuppressLint("ClickableViewAccessibility")
    private void init() {
        listenButton = findViewById(R.id.listen);
        devicesButton = findViewById(R.id.listDevices);
        statusTextView = findViewById(R.id.status);
        deviceListView = findViewById(R.id.listview);
        menuLayout = findViewById(R.id.rel_layout);
        startButton = findViewById(R.id.startButton);
        disconnectButton = findViewById(R.id.disconnectButton);

        gameBoardLayout = findViewById(R.id.game_board_layout);
        gameBoard = findViewById(R.id.game_board);

        scroller = findViewById(R.id.scroller);
        scroller.setVerticalScrollBarEnabled(false);

        scroller.setOnTouchListener( new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        devicesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
                int n = devices.size();

                if (n > 0) {
                    names = new String[n];
                    mDevices = new BluetoothDevice[n];
                    names = new String[n];
                    int i = 0;

                    for (BluetoothDevice device: devices) {
                        mDevices[i] = device;
                        names[i] = device.getName();
                        i++;
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            getApplicationContext(),
                            android.R.layout.simple_list_item_1,
                            names
                    );
                    deviceListView.setAdapter(adapter);
                }
            }
        });

        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Client client = new Client(mDevices[i]);
                client.start();

                statusTextView.setText("Connecting");
            }
        });

        listenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Server server = new Server();
                server.start();
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (gameState == 0) {
                    Toast.makeText(
                            getApplicationContext(),
                            "Connection failed",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                showGameBoard();
            }
        });

        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMenu();
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showMenu() {
        menuLayout.setVisibility(View.VISIBLE);
        gameBoardLayout.setVisibility(View.INVISIBLE);
        scroller.fullScroll(ScrollView.FOCUS_UP);
        gameBoard.clearBoard();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showGameBoard() {
        menuLayout.setVisibility(View.INVISIBLE);
        gameBoardLayout.setVisibility(View.VISIBLE);
        scroller.fullScroll(ScrollView.FOCUS_DOWN);
        gameBoard.setEnabled(true);
        gameBoard.init();

        gameBoard.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int x = (int) motionEvent.getX();
                int action = motionEvent.getAction();

                if (action == MotionEvent.ACTION_DOWN && isMyTurn == 1) {
                    int column = gameBoard.getColumn(x);
                    int row = gameBoard.getRow(column);
                    if (gameBoard.makeMove(row, column, playerColor)) {
                        isMyTurn = 0;
                        byte gameEnded = 0;
                        if (gameBoard.checkGameState(playerColor) == GameState.PLAYER_WIN)
                            gameEnded = 1;


                        sendData((byte) column, gameEnded);
                    }
                }

                return true;
            }
        });
    }

    private void sendData(byte column, byte gameEnded) {
        byte[] bytes = new byte[10];
        bytes[0] = column;
        bytes[1] = gameEnded;
        mSendReceive.write(bytes);
    }

    private void enableBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            finish();
        }
        else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLE_BLUETOOTH);
            }
        }
    }

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            switch (message.what) {
                case STATE_LISTENING: {
                    statusTextView.setText("Listening");
                    break;
                }
                case STATE_CONNECTING: {
                    statusTextView.setText("Connecting");
                    break;
                }
                case STATE_CONNECTED: {
                    statusTextView.setText("Connected");
                    break;
                }
                case STATE_CONNECTION_FAILED: {
                    statusTextView.setText("Connection failed");
                    break;
                }
                case STATE_GAME_STARTED: {
                    playerColor = message.arg1;
                    if (playerColor == getResources().getColor(R.color.pieceColor1))
                        opponentColor = getResources().getColor(R.color.pieceColor2);
                    else
                        opponentColor = getResources().getColor(R.color.pieceColor1);
                    gameState = message.arg2;
                    isMyTurn = (Integer) message.obj;
                    break;
                }
                case STATE_MESSAGE_RECEIVED: {
                    byte[] buffer = (byte[]) message.obj;
                    int column = buffer[0];
                    int row = gameBoard.getRow(column);
                    gameBoard.makeMove(row, column, opponentColor);
                    gameBoard.invalidate();
                    gameBoard.checkGameState(opponentColor);
                    byte isGameEnded = buffer[1];
                    if (isGameEnded == 1) {
                        gameBoard.setEnabled(false);
                    }
                    isMyTurn = 1;
                    break;
                }
            }
            return true;
        }
    });

    private class Client extends Thread {
        private BluetoothDevice mDevice;
        private BluetoothSocket mSocket;

        public Client(BluetoothDevice device) {
            mDevice = device;
            try {
                mSocket = mDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                mSocket.connect();
                Message message = Message.obtain();
                message.what = STATE_CONNECTED;
                mHandler.sendMessage(message);

                Message message2 = Message.obtain();
                message2.what = STATE_GAME_STARTED;
                message2.arg1 =  getResources().getColor(R.color.pieceColor2);
                message2.arg2 = 1;
                message2.obj = 0;
                mHandler.sendMessage(message2);

                mSendReceive = new SendReceive(mSocket);
                mSendReceive.start();

            } catch (IOException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                mHandler.sendMessage(message);
            }
        }
    }

    private class Server extends Thread {
        private BluetoothServerSocket mServerSocket;

        public Server() {
            try {
                mServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            BluetoothSocket bluetoothSocket = null;

            while (bluetoothSocket == null) {
                try {
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTING;
                    mHandler.sendMessage(message);

                    bluetoothSocket = mServerSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTION_FAILED;
                    mHandler.sendMessage(message);
                }

                if (bluetoothSocket != null) {
                    Message message2 = Message.obtain();
                    message2.what = STATE_GAME_STARTED;
                    message2.arg1 =  getResources().getColor(R.color.pieceColor1);
                    message2.arg2 = 1;
                    message2.obj = 1;
                    mHandler.sendMessage(message2);

                    Message message = Message.obtain();
                    message.what = STATE_CONNECTED;
                    mHandler.sendMessage(message);


                    mSendReceive = new SendReceive(bluetoothSocket);
                    mSendReceive.start();

                    break;
                }
            }
        }
    }

    private class SendReceive extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream mInputStream;
        private final OutputStream mOutputStream;

        public SendReceive(BluetoothSocket socket) {
            bluetoothSocket = socket;

            InputStream tempIn = null;
            OutputStream tempOut = null;

            try {
                tempIn = socket.getInputStream();
                tempOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mInputStream = tempIn;
            mOutputStream = tempOut;

        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mInputStream.read(buffer);
                    mHandler.obtainMessage(
                            STATE_MESSAGE_RECEIVED,
                            bytes,
                            -1,
                            buffer).sendToTarget();
                } catch (IOException e) {
                    showMenu();
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mOutputStream.write(bytes);
            } catch (IOException e) {
                showMenu();
                e.printStackTrace();
            }
        }
    }
}