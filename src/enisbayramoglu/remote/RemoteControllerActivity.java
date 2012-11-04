package enisbayramoglu.remote;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

public class RemoteControllerActivity extends Activity {
	static final int DIALOG_UNKNOWN_HOST_ID = 0;
	static final int DIALOG_IMPROPER_HOST_PORT_ID = 1;
	static final int DIALOG_SOCKET_EXCEPTION_ID = 2;
	static final int DIALOG_DISCONNECTED_ID = 3;
	public DataOutputStream os;
	
	public AutoCompleteTextView host_port_view;
	public JoystickView joystick_view;
	private Button connect_button;
	private Spinner command_spinner;
	private Button send_button;
	
	private MotorController motor_controller;

	Socket s = null;
	private String connections_file = "connections.txt";
	LinkedList<String> connections;
	ArrayAdapter<String> adapter;
	boolean connected = false;
	String command = "";
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.main);
    	host_port_view = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView1);
    	joystick_view = (JoystickView) findViewById(R.id.joystickView1);
    	connect_button = (Button) findViewById(R.id.button1);
    	connect_button.setOnClickListener(new ConnectButtonListener());
    	connections = new LinkedList<String>();
    	FileInputStream fis;
    	try {
			fis = openFileInput(connections_file);
			BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
			String line;
			while((line = reader.readLine())!=null) {
				connections.add(line);
			}
		} catch (FileNotFoundException e) {	} catch (IOException e) { }
        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, connections);
        host_port_view.setAdapter(adapter);
        host_port_view.setThreshold(0);
        command_spinner = (Spinner) findViewById(R.id.spinner1);
        send_button = (Button) findViewById(R.id.button2);
        send_button.setOnClickListener(new SendCommandListener());
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.special_commands, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        command_spinner.setAdapter(adapter);
        command_spinner.setOnItemSelectedListener(new CommandSpinnerListener());
        motor_controller = new MotorController(this);
    }
    
    
    class ConnectButtonListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if(!connected) {
				String connection = host_port_view.getText().toString();
				String fields[] = connection.split(":");
				if(fields.length==2) {
					try {
						if(s!=null) s.close();
						s = new Socket(fields[0], Integer.parseInt(fields[1]));

						os = new DataOutputStream(s.getOutputStream());
						if(s.isConnected()) {
							connect_button.setText(R.string.connect_button_connected_text);
							connected=true;
						}
						//os.writeUTF("Hello Android!\n");
					} catch (UnknownHostException e) {
						showDialog(DIALOG_UNKNOWN_HOST_ID);
					} catch (SocketException e) {
						showDialog(DIALOG_SOCKET_EXCEPTION_ID);
					}catch (IOException e) {
						e.printStackTrace();
					}
				} else showDialog(DIALOG_IMPROPER_HOST_PORT_ID);
				FileOutputStream fos = null;
				if(!connections.contains(connection)) {
					try {
						fos = openFileOutput(connections_file, Context.MODE_APPEND);
						fos.write(connection.getBytes());
						connections.add(connection);
						adapter.add(connection);
					} catch (Exception e1) {
					} finally {
						if(fos!=null) try {fos.close();} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			} else { // connected
				disconnected();
				if(s!=null) {
					try {
						s.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
    }
    public void disconnected() {
		connected=false;
		connect_button.setText(R.string.connect_button_text);    	
    }
    
    class SendCommandListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			String commands = "flushcmds\n" + command;
			if(os!=null)
				try {
					os.writeBytes(commands);
				} catch (IOException e) {
					showDialog(DIALOG_DISCONNECTED_ID);
					disconnected();
					os=null;
				}
		} 	
    }

    
    class CommandSpinnerListener implements OnItemSelectedListener {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
			command = parent.getItemAtPosition(pos).toString()+"\n";
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			command = "idle\n";
		}
    	
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	Dialog dialog;
    	switch (id) {
		case DIALOG_UNKNOWN_HOST_ID:
			dialog = createErrorDialog(R.string.unknown_host_error_message);
			break;
		case DIALOG_IMPROPER_HOST_PORT_ID:
			dialog = createErrorDialog(R.string.improper_host_port_text);
			break;
		case DIALOG_SOCKET_EXCEPTION_ID:
			dialog = createErrorDialog(R.string.socket_exception_text);
			break;
		case DIALOG_DISCONNECTED_ID:
			dialog = createErrorDialog(R.string.disconnected_text);
			break;
		default:
			dialog = null;
			break;
		}
    	return dialog;
    }
    
    private Dialog createErrorDialog(int message) {
    	return new AlertDialog.Builder(this)
		.setMessage(message)
		.setCancelable(false)
		.setNeutralButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.dismiss();
			}
		}).create();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	motor_controller.pause();
    }
    @Override
    protected void onResume() {
    	super.onResume();
    	motor_controller.resume();
    }
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            
//            return true;
//        }
//        return super.onKeyDown(keyCode, event);
//    }
}