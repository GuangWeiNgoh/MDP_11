package com.android.mdp_11;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "MainActivity";
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;
    private static Context context;
    private static boolean autoUpdate = false;

    private static long exploreTimer;
    private static long fastestTimer;
    private static boolean startActivityStatus = true;
    public String connStatus = "Disconnected";

    GridMap gridMap;
    MessageBox messageBox;
    TextView connStatusTextView, P1String, P2String;
    MenuItem bluetoothMenuItem, messageMenuItem, getMapMenuItem;
    TextView exploreTimeTextView, fastestTimeTextView;
    ToggleButton exploreToggleBtn, fastestToggleBtn;
    ImageButton exploreResetImageBtn, fastestResetImageBtn;
//    ImageButton calibrationBtn;
    TextView robotStatusTextView;
    ImageButton moveForwardImageBtn, turnRightImageBtn, moveBackwardImageBtn, turnLeftImageBtn;
    Switch phoneTiltSwitch;
    Button resetMapBtn, left45Btn, right45Btn, calibrationBtn;
    ToggleButton setStartPointToggleBtn, setWaypointToggleBtn;
    TextView xAxisTextView, yAxisTextView, directionAxisTextView;
    ImageButton directionChangeImageBtn, exploredImageBtn, obstacleImageBtn, clearImageBtn;
    static TextView messageSentTextView;
    TextView messageReceivedTextView;
    ToggleButton manualAutoToggleBtn;
    Button f1Btn, f2Btn, reconfigureBtn;

    Intent intent;

    StringBuilder message;
    BluetoothConnectionService mBluetoothConnection;
    private static UUID myUUID;
    BluetoothDevice mBTDevice;
    ProgressDialog myDialog;

    private Sensor mSensor;
    private SensorManager mSensorManager;

    Handler timerHandler = new Handler();
    Runnable timerRunnableExplore = new Runnable() {
        @Override
        public void run() {
            long millisExplore = System.currentTimeMillis() - exploreTimer;
            int secondsExplore = (int) (millisExplore / 1000);
            int minutesExplore = secondsExplore / 60;
            secondsExplore = secondsExplore % 60;

            exploreTimeTextView.setText(String.format("%02d:%02d", minutesExplore, secondsExplore));

            timerHandler.postDelayed(this, 500);
        }
    };

    Runnable timerRunnableFastest = new Runnable() {
        @Override
        public void run() {
            long millisFastest = System.currentTimeMillis() - fastestTimer;
            int secondsFastest = (int) (millisFastest / 1000);
            int minutesFastest = secondsFastest / 60;
            secondsFastest = secondsFastest % 60;

            fastestTimeTextView.setText(String.format("%02d:%02d", minutesFastest, secondsFastest));

            timerHandler.postDelayed(this, 500);
        }
    };

    Runnable timedMessage = new Runnable(){
        @Override
        public void run() {
            refreshMessage();
            timerHandler.postDelayed(timedMessage, 1000);
        }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        showLog("Entering onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridMap = new GridMap(this);
        messageBox = new MessageBox();

        exploreTimer = 0;
        fastestTimer = 0;

        gridMap = findViewById(R.id.mapView);
        connStatusTextView = findViewById(R.id.connStatusTextView);
        bluetoothMenuItem = findViewById(R.id.bluetoothMenuItem);
        messageMenuItem = findViewById(R.id.messageMenuItem);
        getMapMenuItem = findViewById(R.id.getMapMenuItem);
        exploreTimeTextView = findViewById(R.id.exploreTimeTextView);
        fastestTimeTextView = findViewById(R.id.fastestTimeTextView);
        exploreToggleBtn = findViewById(R.id.exploreToggleBtn);
        exploreResetImageBtn = findViewById(R.id.exploreResetImageBtn);
        fastestToggleBtn = findViewById(R.id.fastestToggleBtn);
        fastestResetImageBtn = findViewById(R.id.fastestResetImageBtn);
        robotStatusTextView = findViewById(R.id.robotStatusTextView);
        moveForwardImageBtn = findViewById(R.id.moveForwardImageBtn);
        turnRightImageBtn = findViewById(R.id.turnRightImageBtn);
        moveBackwardImageBtn = findViewById(R.id.moveBackwardImageBtn);
        turnLeftImageBtn = findViewById(R.id.turnLeftImageBtn);
        left45Btn = findViewById(R.id.left45Btn);
        right45Btn = findViewById(R.id.right45Btn);
        phoneTiltSwitch = findViewById(R.id.phoneTiltSwitch);
        resetMapBtn = findViewById(R.id.resetMapBtn);
        setStartPointToggleBtn = findViewById(R.id.setStartPointToggleBtn);
        setWaypointToggleBtn = findViewById(R.id.setWaypointToggleBtn);
        xAxisTextView = findViewById(R.id.xAxisTextView);
        yAxisTextView = findViewById(R.id.yAxisTextView);
        calibrationBtn = findViewById(R.id.calibrationBtn);
        directionAxisTextView = findViewById(R.id.directionAxisTextView);
        directionChangeImageBtn = findViewById(R.id.directionChangeImageBtn);
        exploredImageBtn = findViewById(R.id.exploredImageBtn);
        obstacleImageBtn = findViewById(R.id.obstacleImageBtn);
        clearImageBtn = findViewById(R.id.clearImageBtn);
        messageSentTextView = findViewById(R.id.messageSentTextView);
        messageReceivedTextView = findViewById(R.id.messageReceivedTextView);
        manualAutoToggleBtn = findViewById(R.id.manualAutoToggleBtn);
        f1Btn = findViewById(R.id.f1Btn);
        f2Btn = findViewById(R.id.f2Btn);
        reconfigureBtn = findViewById(R.id.reconfigureBtn);
        P1String = findViewById(R.id.P1String);
        P2String = findViewById(R.id.P2String);

        MainActivity.context = getApplicationContext();
        this.sharedPreferences();
        editor.putString("sentText", "");
        editor.putString("receivedText", "");
        editor.putString("direction","None");
        editor.putString("connStatus", connStatus);
        editor.commit();

        timerHandler.post(timedMessage);

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("incomingMessage"));

        if (savedInstanceState != null) {
            showLog("Entering savedInstanceState");
        }

        final FragmentManager fragmentManager = getFragmentManager();
        final ReconfigureFragment reconfigureFragment = new ReconfigureFragment();
        final DirectionFragment directionFragment = new DirectionFragment();

        if (sharedPreferences.contains("F1")) {
            f1Btn.setContentDescription(sharedPreferences.getString("F1", ""));
            showLog("setText for f1Btn: " + f2Btn.getContentDescription().toString());
        }
        if (sharedPreferences.contains("F2")) {
            f2Btn.setContentDescription(sharedPreferences.getString("F2", ""));
            showLog("setText for f2Btn: " + f2Btn.getContentDescription().toString());
        }

        robotStatusTextView.setMovementMethod(new ScrollingMovementMethod());
        messageSentTextView.setMovementMethod(new ScrollingMovementMethod());
        messageReceivedTextView.setMovementMethod(new ScrollingMovementMethod());

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        exploreToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked exploreToggleBtn");
                Button exploreToggleBtn = (Button) view;
                if (exploreToggleBtn.getText().equals("EXPLORE")) {
                    showToast("Exploration timer stop!");
                    timerHandler.removeCallbacks(timerRunnableExplore);
                }
                else if (exploreToggleBtn.getText().equals("STOP")) {
                    showToast("Exploration timer start!");
                    printMessage("AL:x");
                    exploreTimer = System.currentTimeMillis();
                    timerHandler.postDelayed(timerRunnableExplore, 0);
                }
                else {
                    showToast("Else statement: " + exploreToggleBtn.getText());
                }
                showLog("Exiting exploreToggleBtn");
            }
        });

        exploreResetImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked exploreResetImageBtn");
                showToast("Reseting exploration time...");
                exploreTimeTextView.setText("00:00");
                if(exploreToggleBtn.isChecked())
                    exploreToggleBtn.toggle();
                timerHandler.removeCallbacks(timerRunnableExplore);
                showLog("Exiting exploreResetImageBtn");
            }
        });



        fastestToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked fastestToggleBtn");
                Button fastestToggleBtn = (Button) view;
                if (fastestToggleBtn.getText().equals("FASTEST")) {
                    showToast("Fastest timer stop!");
                    timerHandler.removeCallbacks(timerRunnableFastest);
                }
                else if (fastestToggleBtn.getText().equals("STOP")) {
                    showToast("Fastest timer start!");
                    printMessage("AL:f");
                    fastestTimer = System.currentTimeMillis();
                    timerHandler.postDelayed(timerRunnableFastest, 0);
                }
                else
                    showToast(fastestToggleBtn.getText().toString());
                showLog("Exiting fastestToggleBtn");
            }
        });

        fastestResetImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked fastestResetImageBtn");
                showToast("Reseting fastest time...");
                fastestTimeTextView.setText("00:00");
                if (fastestToggleBtn.isChecked())
                    fastestToggleBtn.toggle();
                timerHandler.removeCallbacks(timerRunnableFastest);
                showLog("Exiting fastestResetImageBtn");
            }
        });

        moveForwardImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked moveForwardImageBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus("Please press 'MANUAL'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("forward");
                    refreshLabel();
                    if (gridMap.getValidPosition())
                        updateStatus("moving forward");
                    else
                        updateStatus("Unable to move forward");
                    printMessage("AR:1");
                }
                else
                    updateStatus("Please press 'STARTING POINT'");
                showLog("Exiting moveForwardImageBtn");
            }
        });

        turnRightImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked turnRightImageBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus("Please press 'MANUAL'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("right");
                    refreshLabel();
                    printMessage("AR:d");
                    //printMessage("StRight~");
                }
                else
                    updateStatus("Please press 'STARTING POINT'");
                showLog("Exiting turnRightImageBtn");
            }
        });

        moveBackwardImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked moveBackwardImageBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus("Please press 'MANUAL'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("back");
                    refreshLabel();
                    if (gridMap.getValidPosition())
                        updateStatus("moving backward");
                    else
                        updateStatus("Unable to move backward");
                    //printMessage("Back~");
                    printMessage("AR:d");
                    printMessage("AR:d");
                }
                else
                    updateStatus("Please press 'STARTING POINT'");
                showLog("Exiting moveBackwardImageBtn");
            }
        });

        turnLeftImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked turnLeftImageBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus("Please press 'MANUAL'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("left");
                    refreshLabel();
                    updateStatus("turning left");
                    printMessage("AR:a");
                }
                else
                    updateStatus("Please press 'STARTING POINT'");
                showLog("Exiting turnLeftImageBtn");
            }
        });

        left45Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked left45Btn");
                if (gridMap.getAutoUpdate())
                    updateStatus("Please press 'MANUAL'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("left45");
                    refreshLabel();
                    updateStatus("turning left 45");
                    printMessage("AR:q");
                }
                else
                    updateStatus("Please press 'STARTING POINT'");
                showLog("Exiting left45Btn");
            }
        });

        right45Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked right45Btn");
                if (gridMap.getAutoUpdate())
                    updateStatus("Please press 'MANUAL'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("right45");
                    refreshLabel();
                    updateStatus("turning right 45");
                    printMessage("AR:e");
                }
                else
                    updateStatus("Please press 'STARTING POINT'");
                showLog("Exiting right45Btn");
            }
        });

        phoneTiltSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){
                    compoundButton.setText("TILT ON");
                }else
                {
                    compoundButton.setText("TILT OFF");
                }
                if (gridMap.getAutoUpdate()) {
                    updateStatus("Please press 'MANUAL'");
                    phoneTiltSwitch.setChecked(false);
                }
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    if(phoneTiltSwitch.isChecked()){
                        showToast("Tilt motion control: ON");
                        phoneTiltSwitch.setPressed(true);

                        mSensorManager.registerListener(MainActivity.this, mSensor, mSensorManager.SENSOR_DELAY_NORMAL);
                        sensorHandler.post(sensorDelay);
                    }else{
                        showToast("Tilt motion control: OFF");
                        showLog("unregistering Sensor Listener");
                        try {
                            mSensorManager.unregisterListener(MainActivity.this);
                        }catch(IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                        sensorHandler.removeCallbacks(sensorDelay);
                    }
                } else {
                    updateStatus("Please press 'STARTING POINT'");
                    phoneTiltSwitch.setChecked(false);
                }
            }
        });

        resetMapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked resetMapBtn");
                showToast("Reseting map...");
                gridMap.resetMap();
                resetStrings();
                exploreTimeTextView.setText("00:00");
                fastestTimeTextView.setText("00:00");
            }
        });

        setStartPointToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked setStartPointToggleBtn");
                if (setStartPointToggleBtn.getText().equals("STARTING POINT"))
                    showToast("Cancelled selecting starting point");
                else if (setStartPointToggleBtn.getText().equals("CANCEL") && !gridMap.getAutoUpdate()) {
                    showToast("Please select starting point");
                    gridMap.setStartCoordStatus(true);
                    gridMap.toggleCheckedBtn("setStartPointToggleBtn");
                } else
                    showToast("Please select manual mode");
                showLog("Exiting setStartPointToggleBtn");
            }
        });



        setWaypointToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked setWaypointToggleBtn");
                if (setWaypointToggleBtn.getText().equals("WAYPOINT"))
                    showToast("Cancelled selecting waypoint");
                else if (setWaypointToggleBtn.getText().equals("CANCEL")) {
                    showToast("Please select waypoint");
                    gridMap.setWaypointStatus(true);
                    gridMap.toggleCheckedBtn("setWaypointToggleBtn");
                }
                else
                    showToast("Please select manual mode");
                showLog("Exiting setWaypointToggleBtn");
            }
        });

        directionChangeImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked directionChangeImageBtn");
                directionFragment.show(fragmentManager, "Direction Fragment");
                showLog("Exiting directionChangeImageBtn");
            }
        });

        exploredImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked exploredImageBtn");
                if (!gridMap.getExploredStatus()) {
                    showToast("Please check cell");
                    gridMap.setExploredStatus(true);
                    gridMap.toggleCheckedBtn("exploredImageBtn");
                }
                else if (gridMap.getExploredStatus())
                    gridMap.setSetObstacleStatus(false);
                showLog("Exiting exploredImageBtn");
            }
        });

        calibrationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked calibrationBtn");
                printMessage("AR:c");
                showToast("Calibrating...");
                showLog("Exiting calibrationBtn");
            }
        });



        obstacleImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked obstacleImageBtn");
                if (!gridMap.getSetObstacleStatus()) {
                    showToast("Please plot obstacles");
                    gridMap.setSetObstacleStatus(true);
                    gridMap.toggleCheckedBtn("obstacleImageBtn");
                }
                else if (gridMap.getSetObstacleStatus())
                    gridMap.setSetObstacleStatus(false);
                showLog("Exiting obstacleImageBtn");
            }
        });

        clearImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked clearImageBtn");
                if (!gridMap.getUnSetCellStatus()) {
                    showToast("Please remove cells");
                    gridMap.setUnSetCellStatus(true);
                    gridMap.toggleCheckedBtn("clearImageBtn");
                }
                else if (gridMap.getUnSetCellStatus())
                    gridMap.setUnSetCellStatus(false);
                showLog("Exiting clearImageBtn");
            }
        });

        manualAutoToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked manualAutoToggleBtn");
                if (manualAutoToggleBtn.getText().equals("MANUAL")) {
                    try {
                        gridMap.setAutoUpdate(true);
                        autoUpdate = true;
                        gridMap.toggleCheckedBtn("None");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    showToast("AUTO mode");
                }
                else if (manualAutoToggleBtn.getText().equals("AUTO")) {
                    try {
                        gridMap.setAutoUpdate(false);
                        autoUpdate = false;
                        gridMap.toggleCheckedBtn("None");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    showToast("MANUAL mode");
                }
                showLog("Exiting manualAutoToggleBtn");
            }
        });

        f1Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked f1Btn");
                if (!f1Btn.getContentDescription().toString().equals("empty"))
                    printMessage(f1Btn.getContentDescription().toString());
                showLog("f1Btn value: " + f1Btn.getContentDescription().toString());
                showLog("Exiting f1Btn");
            }
        });

        f2Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked f2Btn");
                if (!f2Btn.getContentDescription().toString().equals("empty"))
                    printMessage(f2Btn.getContentDescription().toString());
                showLog("f2Btn value: " + f2Btn.getContentDescription().toString());
                showLog("Exiting f2Btn");
            }
        });

        reconfigureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked reconfigureBtn");
                reconfigureFragment.show(fragmentManager, "Reconfigure Fragment");
                showLog("Exiting reconfigureBtn");
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ImageButton backBtn = findViewById(R.id.backBtn);

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory( Intent.CATEGORY_HOME );
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(homeIntent);
            }
        });

        myDialog = new ProgressDialog(MainActivity.this);
        myDialog.setMessage("Waiting for other device to reconnect...");
        myDialog.setCancelable(false);
        myDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }

    private void refreshLabel() {
        xAxisTextView.setText(String.valueOf(gridMap.getCurCoord()[0]));
        yAxisTextView.setText(String.valueOf(gridMap.getCurCoord()[1]));
        directionAxisTextView.setText(sharedPreferences.getString("direction",""));
    }

    public void resetStrings(){
        P1String.setText("Awaiting P1 Hex String Value...");
        P2String.setText("Awaiting P2 Hex String Value...");
    }

    public void setStrings(String p1,String p2){
        P1String.setText(p1);
        P2String.setText(p2);
    }

    public void refreshMessage() {
        messageReceivedTextView.setText(sharedPreferences.getString("receivedText", ""));
        messageSentTextView.setText(sharedPreferences.getString("sentText", ""));
        directionAxisTextView.setText(sharedPreferences.getString("direction",""));
        connStatusTextView.setText(sharedPreferences.getString("connStatus", ""));
    }

    public void refreshDirection(String direction) {
        gridMap.setRobotDirection(direction);
        printMessage("Direction is set to " + direction);
    }

    private void updateStatus(String message) {
    }

    public static void receiveMessage(String message) {
        showLog("Entering receiveMessage");
        sharedPreferences();
        editor.putString("receivedText", sharedPreferences.getString("receivedText", "") + "\n " + message);
        editor.commit();
        showLog("Exiting receiveMessage");
    }

    public static void printMessage(String name, int x, int y) throws JSONException {
        showLog("Entering printMessage");
        sharedPreferences();

        JSONObject jsonObject = new JSONObject();
        String message;
        x = x-1;
        y = y-1;
        switch(name) {
            case "starting":
                jsonObject.put(name, name);
                jsonObject.put("x", x);
                jsonObject.put("y", y);
                //message = name + " (" + x + "," + y + ")";
                message = "AL:st:" + x + "," + y;
                break;
            case "waypoint":
                jsonObject.put(name, name);
                jsonObject.put("x", x);
                jsonObject.put("y", y);
                //message = name + " (" + x + "," + y + ")";
                message = "AL:wp:" + x + "," + y;
                break;
            default:
                message = "Unexpected default for printMessage: " + name;
                break;
        }
        editor.putString("sentText", messageSentTextView.getText() + "\n " + message);
        editor.commit();
        //printMessage("X" + String.valueOf(jsonObject));
        if (BluetoothConnectionService.BluetoothConnectionStatus == true) {
            byte[] bytes = message.getBytes(Charset.defaultCharset());
            BluetoothConnectionService.write(bytes);
        }
        showLog("Exiting printMessage");
    }
    public static void printMessage(String message) {
        showLog("Entering printMessage");
        sharedPreferences();

        if (BluetoothConnectionService.BluetoothConnectionStatus == true) {
            byte[] bytes = message.getBytes(Charset.defaultCharset());
            BluetoothConnectionService.write(bytes);
        }
        showLog(message);
        editor.putString("sentText", messageSentTextView.getText() + "\n " + message);
        editor.commit();
        showLog("Exiting printMessage");
    }

    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.example_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        this.sharedPreferences();
        switch(item.getItemId()) {
            case R.id.bluetoothMenuItem:
                showToast("Entering Bluetooth Configuration...");
                intent = new Intent(MainActivity.this, BluetoothPopUp.class);
                startActivityStatus = false;
                startActivityForResult(intent, 1);
                break;
            case R.id.messageMenuItem:
                showToast("Message Box selected");
                intent = new Intent(MainActivity.this, MessageBox.class);
                editor.putString("receivedText", messageReceivedTextView.getText().toString());
                editor.putString("sentText",  messageSentTextView.getText().toString());
                break;
            case R.id.getMapMenuItem:
                showToast("Get Map Information selected");
                intent = new Intent(MainActivity.this, MapInformation.class);
                break;
            case R.id.menuMenuItem:
                startActivityStatus = false;
                break;
            case R.id.saveMapMenuItem:
                showToast("Map saved");
                showLog("saveMapMenuItem: " + String.valueOf(gridMap.getMapInformation()));
                editor.putString("mapSaved", String.valueOf(gridMap.getMapInformation()));
                startActivityStatus = false;
                break;
            case R.id.loadMapMenuItem:
                if(sharedPreferences.contains("mapSaved")) {
                    try {
                        showLog("loadMapMenuItem: " + sharedPreferences.getString("mapSaved", ""));
                        gridMap.setReceivedJsonObject(new JSONObject(sharedPreferences.getString("mapSaved", "")));
                        gridMap.updateMapInformation();
                        showToast("Map loaded");
                        showLog("loadMapMenuItem try success");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        showLog("loadMapMenuItem try fail");
                    }
                }
                else
                    showToast("No map found");
                startActivityStatus = false;
                break;

            default:
                showToast("onOptionsItemSelected has reached default");
                return false;
        }
        editor.putString("mapJsonObject", String.valueOf(gridMap.getCreateJsonObject()));
        editor.putString("connStatus", connStatusTextView.getText().toString());
        editor.commit();
        if (startActivityStatus)
            startActivity(intent);
        startActivityStatus = true;
        return super.onOptionsItemSelected(item);
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
    }

    public static void sharedPreferences() {
        sharedPreferences = MainActivity.getSharedPreferences(MainActivity.context);
        editor = sharedPreferences.edit();
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private static void showLog(String message) {
        Log.d(TAG, message);
    }

    private BroadcastReceiver mBroadcastReceiver5 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice mDevice = intent.getParcelableExtra("Device");
            String status = intent.getStringExtra("Status");
            sharedPreferences();

            if(status.equals("connected")){
                try {
                    myDialog.dismiss();
                } catch(NullPointerException e){
                    e.printStackTrace();
                }

                Log.d(TAG, "mBroadcastReceiver5: Device now connected to "+mDevice.getName());
                Toast.makeText(MainActivity.this, "Device now connected to "+mDevice.getName(), Toast.LENGTH_LONG).show();
                editor.putString("connStatus", "Connected to " + mDevice.getName());
                connStatusTextView.setText("Connected to " + mDevice.getName());
            }
            else if(status.equals("disconnected")){
                Log.d(TAG, "mBroadcastReceiver5: Disconnected from "+mDevice.getName());
                Toast.makeText(MainActivity.this, "Disconnected from "+mDevice.getName(), Toast.LENGTH_LONG).show();
                mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
                mBluetoothConnection.startAcceptThread();

                editor.putString("connStatus", "Disconnected");
                TextView connStatusTextView = findViewById(R.id.connStatusTextView);
                connStatusTextView.setText("Disconnected");

                myDialog.show();
            }
            editor.commit();
        }
    };

    BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String premessage = intent.getStringExtra("receivedMessage");
            String message;
            if (premessage.substring(0,3).equals("RPi")){
                Pattern pattern1 = Pattern.compile(":(.*?),");
                Matcher matcher1 = pattern1.matcher(premessage);
                Pattern pattern2 = Pattern.compile(",(.*?),");
                Matcher matcher2 = pattern2.matcher(premessage);
                Pattern pattern3 = Pattern.compile(",\\S,(.*?)$");
                Matcher matcher3 = pattern3.matcher(premessage);
                if (matcher1.find() && matcher2.find() && matcher3.find())
                {
                    message = "{\"image\":[{\"id\":\"" + matcher1.group(1) + "\",\"x\":" + matcher2.group(1) + ",\"y\":" + matcher3.group(1) + "}]}";
                } else {
                    message = premessage;
                }
            }

            else if (premessage.length() == 155){
                String premessage1 = premessage.substring(0,76);
                String premessage2 = premessage.substring(77,153);
                String premessage3 = premessage.substring(154);

                StringBuilder bin1 = new StringBuilder(new BigInteger(premessage1, 16).toString(2));
                StringBuilder bin2 = new StringBuilder(new BigInteger(premessage2, 16).toString(2));
                String bin2post;
                int i;
                ArrayList<Integer> notex = new ArrayList<>();

                for (i=0; i<bin1.length(); i++){
                    if(bin1.substring(i,i+1).equals("0")){
                        notex.add(i);
                    }
                }

                //showLog("" + notex);
                showLog("Binary 1:" + bin1 +"\n");
                showLog("Binary 2:" + bin2 +"\n");

                for (i=0; i<notex.size(); i++){
                    bin2.setCharAt(notex.get(i), 'x');
                }

                showLog("Binary 1 Post:" + bin1 +"\n");
                showLog("Binary 2 Post:" + bin2 +"\n");

                bin2post = bin2.toString();
                bin2post = bin2post.replaceAll("x","");
                showLog("Binary 2 Replaced:" + bin2post +"\n");
                bin2post = bin2post.substring(2,bin2post.length()-2);
                showLog("Binary 2 Replaced:" + bin2post +"\n");

                while (bin2post.length() % 4 != 0) {
                    bin2post = bin2post + "0";
                }
                int length = bin2post.length();
                int hexlength = length / 4 + ((length % 4 == 0) ? 0 : 1);
                showLog("Binary 2 Length:" + length +"\n");
                showLog("Binary 2 Hex Length:" + hexlength +"\n");
                BigInteger bin2BI = new BigInteger(bin2post, 2);
                showLog("Binary 2 Big Int:" + bin2BI +"\n");
                String bin2hex = bin2BI.toString(16);

                while (bin2hex.length() < hexlength){
                    bin2hex = "0" + bin2hex;
                }
                showLog("Binary 2 Hex:" + bin2hex +"\n");

                setStrings(premessage1,bin2hex);

                String autoDirection = "";
                switch (premessage3) {
                    case "1":
                        autoDirection = "forward";
                        break;
                    case "d":
                        autoDirection = "right";
                        break;
                    case "a":
                        autoDirection = "left";
                        break;
                    case "c":
                        return;
                }

                message = "{\"map\":[{\"explored\":" + premessage1 + ",\"length\":304,\"obstacle\":" + premessage2 + "}],\"move\":[{\"direction\":\"" + autoDirection + "\"}]}";

            }

            else if (premessage.length() > 156){
                String premessage1 = premessage.substring(0,76);
                String premessage2 = premessage.substring(77,153);
                String premessage3 = premessage.substring(154);

                StringBuilder bin1 = new StringBuilder(new BigInteger(premessage1, 16).toString(2));
                StringBuilder bin2 = new StringBuilder(new BigInteger(premessage2, 16).toString(2));
                String bin2post;
                int i;
                ArrayList<Integer> notex = new ArrayList<>();

                for (i=0; i<bin1.length(); i++){
                    if(bin1.substring(i,i+1).equals("0")){
                        notex.add(i);
                    }
                }

                //showLog("" + notex);
                showLog("Binary 1:" + bin1 +"\n");
                showLog("Binary 2:" + bin2 +"\n");

                for (i=0; i<notex.size(); i++){
                    bin2.setCharAt(notex.get(i), 'x');
                }

                showLog("Binary 1 Post:" + bin1 +"\n");
                showLog("Binary 2 Post:" + bin2 +"\n");

                bin2post = bin2.toString();
                bin2post = bin2post.replaceAll("x","");
                showLog("Binary 2 Replaced:" + bin2post +"\n");
                bin2post = bin2post.substring(2,bin2post.length()-2);
                showLog("Binary 2 Replaced:" + bin2post +"\n");

                while (bin2post.length() % 4 != 0) {
                    bin2post = bin2post + "0";
                }
                int length = bin2post.length();
                int hexlength = length / 4 + ((length % 4 == 0) ? 0 : 1);
                showLog("Binary 2 Length:" + length +"\n");
                showLog("Binary 2 Hex Length:" + hexlength +"\n");
                BigInteger bin2BI = new BigInteger(bin2post, 2);
                showLog("Binary 2 Big Int:" + bin2BI +"\n");
                String bin2hex = bin2BI.toString(16);

                while (bin2hex.length() < hexlength){
                    bin2hex = "0" + bin2hex;
                }
                showLog("Binary 2 Hex:" + bin2hex +"\n");

                setStrings(premessage1,bin2hex);

                showLog(premessage3);
                Pattern pattern1 = Pattern.compile("^(.*?),");
                Matcher matcher1 = pattern1.matcher(premessage3);
                Pattern pattern2 = Pattern.compile("\\S+,(.*?),\\S+");
                Matcher matcher2 = pattern2.matcher(premessage3);
                Pattern pattern3 = Pattern.compile(",\\S+,(.*?)$");
                Matcher matcher3 = pattern3.matcher(premessage3);
//                matcher1.find();
//                matcher2.find();
//                matcher3.find();
                if (matcher1.find() && matcher2.find() && matcher3.find())
                {
                    //showLog("Test enter!!!!");
                    showLog(matcher1.group(1));
                    showLog(matcher2.group(1));
                    showLog(matcher3.group(1));
                    String autoFace = "";
                    switch (matcher1.group(1)) {
                        case "n":
                            autoFace = "up";
                            break;
                        case "s":
                            autoFace = "back";
                            break;
                        case "e":
                            autoFace = "right";
                            break;
                        case "w":
                            autoFace = "left";
                            break;
                    }

                    int xcoordint = Integer.parseInt(matcher2.group(1)) + 1;
                    int ycoordint = Integer.parseInt(matcher3.group(1)) + 1;
                    String xcoord = Integer.toString(xcoordint);
                    String ycoord = Integer.toString(ycoordint);

                    showLog(autoFace);
                    //message = "{\"map\":[{\"explored\":" + premessage1 + ",\"length\":304,\"obstacle\":" + premessage2 + "}],\"robot\":[{direction:\"" + autoFace + "\",\"x\":" + matcher2.group(1) + ",\"y\":" + matcher3.group(1) + "}]}";
                    message = "{\"robot\":[{direction:\"" + autoFace + "\",\"x\":" + xcoord + ",\"y\":" + ycoord + "}],\"map\":[{\"explored\":" + premessage1 + ",\"length\":304,\"obstacle\":" + premessage2 + "}]}";
                    showLog(message);
                } else {
                    message = premessage;
                }
            }

            else if (premessage.substring(0,2).equals("P1") && (premessage.length() == 156)){
                String p1 = premessage.substring(3,79);
                String p2 = premessage.substring(80);

                StringBuilder bin1 = new StringBuilder(new BigInteger(p1, 16).toString(2));
                StringBuilder bin2 = new StringBuilder(new BigInteger(p2, 16).toString(2));
                String bin2post;
                int i;
                ArrayList<Integer> notex = new ArrayList<>();

                for (i=0; i<bin1.length(); i++){
                    if(bin1.substring(i,i+1).equals("0")){
                        notex.add(i);
                    }
                }

                for (i=0; i<notex.size(); i++){
                    bin2.setCharAt(notex.get(i), 'x');
                }

                bin2post = bin2.toString();
                bin2post = bin2post.replaceAll("x","");
                bin2post = bin2post.substring(2,bin2post.length()-2);

                while (bin2post.length() % 4 != 0) {
                    bin2post = bin2post + "0";
                }
                int length = bin2post.length();
                int hexlength = length / 4 + ((length % 4 == 0) ? 0 : 1);

                BigInteger bin2BI = new BigInteger(bin2post, 2);
                String bin2hex = bin2BI.toString(16);

                while (bin2hex.length() < hexlength){
                    bin2hex = "0" + bin2hex;
                }

                setStrings(p1,bin2hex);

                message = "P1 Hexa-String:\n" + p1 + "\n\nP2 Hexa-String:\n" + bin2hex;
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("P1 & P2 Strings");
                alertDialog.setMessage(message);
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();

            }

            //else if (premessage.length() == 76){
            //    message = "{\"map\":[{\"explored\":" + premessage + "\",\"length\":0,\"obstacle\":\"ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff11111111111111\"}]}";
            //}



            else {
                message = premessage;
            }

            //String message = premessage + matcher1.group(1);
            showLog("receivedMessage: message --- " + message);

            try {
                if (message.length() > 7 && message.substring(2,6).equals("grid")) {
                    String resultString = "";
                    String amdString = message.substring(11,message.length()-2);
                    showLog("amdString: " + amdString);
                    BigInteger hexBigIntegerExplored = new BigInteger(amdString, 16);
                    String exploredString = hexBigIntegerExplored.toString(2);

                    while (exploredString.length() < 300)
                        exploredString = "0" + exploredString;

                    for (int i=0; i<exploredString.length(); i=i+15) {
                        int j=0;
                        String subString = "";
                        while (j<15) {
                            subString = subString + exploredString.charAt(j+i);
                            j++;
                        }
                        resultString = subString + resultString;
                    }
                    hexBigIntegerExplored = new BigInteger(resultString, 2);
                    resultString = hexBigIntegerExplored.toString(16);

                    JSONObject amdObject = new JSONObject();
                    amdObject.put("explored", "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
                    amdObject.put("length", amdString.length()*4);
                    amdObject.put("obstacle", resultString);
                    JSONArray amdArray = new JSONArray();
                    amdArray.put(amdObject);
                    JSONObject amdMessage = new JSONObject();
                    amdMessage.put("map", amdArray);
                    message = String.valueOf(amdMessage);
                    showLog("Executed for AMD message, message: " + message);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (gridMap.getAutoUpdate()) {
                try {
                    gridMap.setReceivedJsonObject(new JSONObject(message));
                    gridMap.updateMapInformation();
                    showLog("messageReceiver: try decode successful");
                } catch (JSONException e) {
                    showLog("messageReceiver: try decode unsuccessful");
                }
            }
            sharedPreferences();
            String receivedText = sharedPreferences.getString("receivedText", "") + "\n " + message;
            editor.putString("receivedText", receivedText);
            editor.commit();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case 1:
                if(resultCode == Activity.RESULT_OK){
                    mBTDevice = (BluetoothDevice) data.getExtras().getParcelable("mBTDevice");
                    myUUID = (UUID) data.getSerializableExtra("myUUID");
                }
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        try{
            LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver5);
            mSensorManager.unregisterListener(this);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        try{
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver5);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        try{
            IntentFilter filter2 = new IntentFilter("ConnectionStatus");
            LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver5, filter2);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        showLog("Entering onSaveInstanceState");
        super.onSaveInstanceState(outState);

        outState.putString(TAG, "onSaveInstanceState");
        showLog("Exiting onSaveInstanceState");
    }

    Handler sensorHandler = new Handler();
    boolean sensorFlag= false;

    private final Runnable sensorDelay = new Runnable() {
        @Override
        public void run() {
            sensorFlag = true;
            sensorHandler.postDelayed(this,1000);
        }
    };

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        showLog("SensorChanged X: "+x);
        showLog("SensorChanged Y: "+y);
        showLog("SensorChanged Z: "+z);

        if(sensorFlag) {
            if (y < -2) {
                showLog("Sensor Move Forward Detected");
                gridMap.moveRobot("forward");
                refreshLabel();
                printMessage("AR:1");
            } else if (y > 2) {
                showLog("Sensor Move Backward Detected");
                gridMap.moveRobot("back");
                refreshLabel();
                //printMessage("Back~");
                printMessage("AR:d");
                printMessage("AR:d");
            } else if (x > 2) {
                showLog("Sensor Move Left Detected");
                gridMap.moveRobot("left");
                refreshLabel();
                printMessage("AR:a");
            } else if (x < -2) {
                showLog("Sensor Move Right Detected");
                gridMap.moveRobot("right");
                refreshLabel();
                printMessage("AR:d");
                //printMessage("StRight~");
            }
        }
        sensorFlag = false;
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }
}
