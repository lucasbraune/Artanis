package artanis;

import battlecode.common.*;

public class RobotPlayer {
	
	// variables used in Movement.java and Communication.java
	public static RobotController rc; 
	private static Signal[] signalQueue;
	private static Direction myDirection;

	public static void run(RobotController rcIn){
		
		rc = rcIn;

		RobotType myType = rc.getType();
		
		if( myType == RobotType.ARCHON ) {
			Archon.code( );
		} else if( myType == RobotType.SCOUT ) {
			Soldier.code( );
		} else if( myType == RobotType.SOLDIER ) {
			Soldier.code( );
		} if( myType == RobotType.GUARD ) {
			Soldier.code( );
		} else if( myType == RobotType.VIPER ) {
			Soldier.code( );
		}
	}
}