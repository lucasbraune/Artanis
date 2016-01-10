package artanis;

import battlecode.common.*;

public class Communication {
	
	private static RobotController rc = RobotPlayer.rc;
	
	public static void electMasterArchon() throws GameActionException {
		// this method will run at the beginning of the game for all archons in some
		// order. Only the first one will statisfy the 'if' statement.
		Signal latestSignal = latestSignal( rc.emptySignalQueue() );
		
		if( latestSignal == null ) {
			RobotPlayer.isMasterArchon = true;
			rc.setIndicatorString(0, "Hello. I am the MASTER ARCHON.");
		}
		
		rc.broadcastMessageSignal(rc.getID(), 0, 10000);
	}
	
	public static Signal latestSignal( Signal[] signals ) {
		Signal latestSignal = null;
		for( int i=signals.length-1; i >= 0; i-- ) {
			if ( signals[i].getTeam() == RobotPlayer.myTeam  ) {
				latestSignal = signals[i];
				break;
			}
		}
		return latestSignal;
	}
	
	public static Direction getDirectionOfGroupTarget() {

		Direction masterArchonDirection, groupTargetDirection = null;
		MapLocation masterArchonLocation, inFrontOfMasterArchonLocation;
		Signal latestSignal = Communication.latestSignal( RobotPlayer.signalQueue );
		int[] message;

		if ( latestSignal != null ) {
			message = latestSignal.getMessage();
			if ( message.length>0 ) {
				// The mod 8 in the next line avoids exceptions because
				// of an index out of bounds
				masterArchonDirection = Direction.values()[ message[0] %8 ];
				masterArchonLocation = latestSignal.getLocation();
				inFrontOfMasterArchonLocation =
						masterArchonLocation.add( masterArchonDirection, Movement.STEPS_AHEAD_OF_MASTER_ARCHON );
				groupTargetDirection = rc.getLocation().directionTo( inFrontOfMasterArchonLocation );
			}
		}

		return groupTargetDirection;
	}
}