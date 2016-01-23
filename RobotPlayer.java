package artanis;

import battlecode.common.*;

public class RobotPlayer {
	
		public static void run(RobotController rcIn){
		
		RobotType myType = rcIn.getType();
		
		if( myType == RobotType.ARCHON ) {
			new Archon( rcIn ).code();
		} else if( myType == RobotType.SCOUT ) {
			new Soldier( rcIn ).code( );
		} else if( myType == RobotType.SOLDIER ) {
			new Soldier( rcIn ).code( );
		} if( myType == RobotType.GUARD ) {
			new Soldier( rcIn ).code( );
		} else if( myType == RobotType.VIPER ) {
			new Soldier( rcIn ).code( );
		}
	}
}