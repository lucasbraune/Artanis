package artanis;

import battlecode.common.*;

public class Communication {
	
	private static RobotController rc = RobotPlayer.rc;
	private static Team myTeam = rc.getTeam();

	// normal scout broadcast square radius
	public static final int BROADCAST_RADIUS_SQUARED = 1000; 
	
	// This implementation broadcasts the displacement from the archon to the
	// target location. The displacement is encoded using the
	// positionToInteger() method.
	public static void broadcastTargetLocation ( MapLocation targetLocation ) throws GameActionException {
		int v[] = new int[2];
		v[0] = targetLocation.x - rc.getLocation().x;
		v[1] = targetLocation.y - rc.getLocation().y;
		rc.broadcastMessageSignal( positionToInteger(v), rc.getType().ordinal(), BROADCAST_RADIUS_SQUARED );
	}
	
	public static MapLocation readTargetLocation( Signal[] signals ) {
		Signal latestSignal = getSignalFromArchon( signals );
		MapLocation archonLocation = null, targetLocation = null;
		int[] message, displacement;
		if ( latestSignal != null ) {
			archonLocation =  latestSignal.getLocation();
			message = latestSignal.getMessage();
			if ( message.length>0 ) {
				displacement = integerToPosition( message[0] );
				targetLocation = archonLocation.add( displacement[0], displacement[1] );
			}
		}
		return targetLocation;
	}

	public static Signal getSignalFromArchon ( Signal[] signals ) {
		Signal latestSignal = null;
		for( int i=signals.length-1; i >= 0; i-- ) {
			if ( signals[i].getTeam() == myTeam  && 
					signals[i].getMessage()[1] == RobotType.ARCHON.ordinal() ) {
				latestSignal = signals[i];
				break;
			}
		}
		return latestSignal;
	} 

	// Encodes an array of two integers between -100 and 100 
	// into a single integer.
	public static int positionToInteger ( int[] v ) {
		int a, b;
		a = v[0]+100;
		b = (v[1]+100)*1000;
		return b+a;
	}

	// Decodes a given integer to the corresponding position
	public static int[] integerToPosition ( int n ) {
		int[] v = new int[2];  
		v[0] = (n % 1000);
		v[1] = (n-v[0])/1000;
		v[0] = v[0]-100;
		v[1] = v[1]-100;
		return v;
	}

	// This method will run at the beginning of the game for all archons in some
	// order. Only the first one will satisfy the 'if' statement.
	public static void electMasterArchon() throws GameActionException {
		Signal latestSignal = getSignalFromArchon( rc.emptySignalQueue() );

		if( latestSignal == null ) {
			RobotPlayer.isMasterArchon = true;
			rc.setIndicatorString(0, "Hello. I am the MASTER ARCHON.");
		}
		rc.broadcastMessageSignal(rc.getID(), RobotType.ARCHON.ordinal(), 10000);
	}
}