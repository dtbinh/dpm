// Lab4.java

 // Group 51
 // Brian Kim - Lim 260636766
 // Jason Dias 260617554

package localization;

import lejos.ev3.tools.EV3Console;
import lejos.hardware.Button;
import lejos.hardware.LED;
import lejos.hardware.Sound;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.robotics.SampleProvider;
import lejos.utility.Delay;

public class LightLocalizer {
	private static final int FWDSPEED = 110; 
	private static final int ROTATION_SPEED = 70;
	private static final long CORRECTION_PERIOD = 5;

	//Resources
	private Odometer odo;
	private SampleProvider colorSensor;
	private EV3LargeRegulatedMotor leftMotor;
	private EV3LargeRegulatedMotor rightMotor;
	private USLocalizer usLocalizer;
	private Navigation nav;
	private float[] colorData;	
	
	//for data collection (x,y,angle)
	private double[][] collectedData = new double[4][3];
	
	
	public LightLocalizer(Odometer odo, SampleProvider colorSensor, float[] colorData,
			EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor, 
			USLocalizer usLocalizer, Navigation nav) {
		this.odo = odo;
		this.colorSensor = colorSensor;
		this.colorData = colorData;
		this.leftMotor = leftMotor;
		this.rightMotor = rightMotor;
		this.usLocalizer = usLocalizer;
		this.nav = nav;
		
	}
	
	public void doLocalization() {
		// drive to location listed in tutorial
		boolean detected = false;
		leftMotor.setSpeed(FWDSPEED);
		rightMotor.setSpeed(FWDSPEED);
	
		
		// start rotating and clock all 4 gridlines
		// do trig to compute (0,0) and 0 degrees
		// when done travel to (0,0) and turn to 0 degrees
		
		//determine distance from the wall
		nav.turnTo(270, true); //turn to and stop
		leftMotor.stop();
		rightMotor.stop();
		double yDist = usLocalizer.getFilteredData(); //get yDistance
		
		nav.turnTo(180, true); //turn to and stop
		leftMotor.stop();
		rightMotor.stop();
		double xDist = usLocalizer.getFilteredData(); //get xDistance
		
		//calculate and set new distances
		//determine position in 1x1 block
		xDist = 100*xDist - 25;
		yDist = 100*yDist - 25;
		odo.setPosition(new double[]{xDist, yDist}, new boolean[]{true,true,false});
		
		
		//odo position now set
		nav.travelTo(0, 0);
		//wait for return
		
		leftMotor.setSpeed(60);
		rightMotor.setSpeed(60);
		
		//initiate turn until correct() task completed 
		leftMotor.backward();
		rightMotor.forward();
		
		correct();
		
		
	}
	
	private void correct(){
		int count = 0;
		while (count < 4) {
			
			colorSensor.fetchSample(colorData, 0);
			
			//line detected at 13
			if(colorData[0] == 13.0) {
				Sound.beep();
				storeData(count++);
				if(count < 4) { //ensure that last line is not overshot
				Delay.msDelay(2000);
				}
			}

			
				
			
		}
		
		//stop moving
		leftMotor.stop();
		rightMotor.stop();
		
		calculateCorrection();
		
		//calculation complate, now travel to this improved position
		
		nav.travelTo(0, 0);
		nav.turnTo(0,true);
		leftMotor.stop();
		rightMotor.stop();
	}

	private void calculateCorrection() {

		
		//compute new angles
		Sound.beepSequenceUp();

		double xTheta = (collectedData[3][2] * Math.PI/180) - (collectedData[1][2] * Math.PI/180); //4th - 2nd
		double yTheta = (collectedData[2][2] * Math.PI/180) - (collectedData[0][2] * Math.PI/180); // 3rd - 1st
		
		//compute new distance
		double distance = 7.2	; // DISTANCE FROM CENTER AXEL TO LIGHT SENSOR AT THE BACK
		double x = distance * Math.cos(yTheta/2);
		double y = distance * Math.cos(xTheta/2);
		
		yTheta = yTheta * 180 / Math.PI;
		
		
		//update the odometer
		double[] updatedPos = new double[]{x,y, 0};
		odo.setPosition(updatedPos, new boolean[]{true,true,false});
		
		
		
	}

	private void storeData(int count) {
		collectedData [count][0] = odo.getX(); //store x
		collectedData [count][1] = odo.getY(); //store y
		collectedData [count][2] = odo.getAng(); //store theta
		
	}

}
