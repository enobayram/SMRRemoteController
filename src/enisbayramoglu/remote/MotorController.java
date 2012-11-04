package enisbayramoglu.remote;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MotorController {
    double interval = 1;
    double forwardSpeed = 0.2;
    double turningSpeed = 0.05;
    double multiplier = 1;
    String nextTransmission = "idle\n";
    Timer transmitTimer = new Timer();
    boolean stopping = true;
    boolean moving = false;
    RemoteControllerActivity activity;
    
    public MotorController(RemoteControllerActivity activity) {
    	this.activity = activity;
	}
    
	public void pause() {
		transmitTimer.cancel();
		transmitTimer = new Timer();
	}
	
	public void resume() {
		transmitTimer.schedule(new SendCommandTask(), 0, 100);		
	}
	
	class SendCommandTask extends TimerTask {			
		@Override
		public void run() {
			float joystickX = activity.joystick_view.joystickX;
			float joystickY = activity.joystick_view.joystickY;
			float joystickMax = activity.joystick_view.joystickMax;

			if(joystickX == 0 && joystickY == 0) {
				nextTransmission = "flushcmds\nidle\n";
				if(moving) stopping = true;
				else stopping = false;
				moving = false;
			} else {
				double speedl = multiplier*(forwardSpeed*(-joystickY)/joystickMax-turningSpeed*(-joystickX)/joystickMax);
				double speedr = multiplier*(forwardSpeed*(-joystickY)/joystickMax+turningSpeed*(-joystickX)/joystickMax);
				nextTransmission = "flushcmds\n" +
						"motorcmds "+speedl+" "+speedr+"\n" +
						"wait "+interval+"\n" +
						"idle\n";
				stopping = false;
				moving = true;
			}
			if(activity.os!=null && (stopping || moving))
				try {
					activity.os.writeBytes(nextTransmission);
					stopping = false;
				} catch (IOException e) {
					activity.runOnUiThread(new Runnable() {	public void run() {
							activity.showDialog(RemoteControllerActivity.DIALOG_DISCONNECTED_ID);
							activity.disconnected();
						}
					});
					activity.os=null;
				}
		}
	}

}
