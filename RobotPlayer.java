package artanis;

import battlecode.common.*;

public class RobotPlayer {

	// The following is sometimes used for debugging purposes
	public static RobotController rc; 

	public static void run(RobotController rcIn){

		rc = rcIn;
		RobotType myType = rcIn.getType();

		if( myType == RobotType.ARCHON ) {
			new Archon( rcIn ).code();
		} else if( myType == RobotType.SCOUT ) {
			new Scout( rcIn ).code( );
		} else if( myType == RobotType.SOLDIER ) {
			new Soldier( rcIn ).code( );
		} else if( myType == RobotType.GUARD ) {
			new Soldier( rcIn ).code( );
		} else if( myType == RobotType.VIPER ) {
			new Soldier( rcIn ).code( );
		} else if( myType == RobotType.TURRET ) {
			new Soldier( rcIn ).code( );
		} else if( myType == RobotType.TTM ) {
			new Soldier( rcIn ).code( );
		}
	}
}