package artanis;

import battlecode.common.*;

public class Communication {
	
	private static RobotController rc = RobotPlayer.rc;
	private static Team myTeam = rc.getTeam(); 

	public static final int SMALL_RADIUS = 1000;
	public static final int LARGE_RADIUS = 10000;
	
	public static Signal getSignal ( Signal[] signals, int messageType ) {
		Signal latestSignal = null;
		for( int i=signals.length-1; i >= 0; i-- ) {
			if ( signals[i].getTeam() == myTeam  && 
					signals[i].getMessage()[1] == messageType ) {
				latestSignal = signals[i];
				break;
			}
		}
		return latestSignal;
	}
	
	// This implementation broadcasts the displacement from the archon to the
	// target location. The displacement is encoded using the
	// positionToInteger() method.
	public static void broadcastLocation( MapLocation location, int messageType, int broadcastRadius ) throws GameActionException {
		int v[] = new int[2];
		v[0] = location.x - rc.getLocation().x;
		v[1] = location.y - rc.getLocation().y;
		rc.broadcastMessageSignal( positionToInteger(v), messageType, broadcastRadius );
	}
	
	public static MapLocation getLocation( Signal[] signals, int messageType ) {
		Signal signal = getSignal( signals, messageType );
		MapLocation senderLocation = null, targetLocation = null;
		int[] message, displacement;
		if ( signal != null ) {
			senderLocation =  signal.getLocation();
			message = signal.getMessage();
			if ( message.length>0 ) {
				displacement = integerToPosition( message[0] );
				targetLocation = senderLocation.add( displacement[0], displacement[1] );
			}
		}
		return targetLocation;
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

}