package artanis;

import java.util.ArrayList;
import java.util.Random;
import battlecode.common.*;

public class Movement {
	
	private static RobotController rc = RobotPlayer.rc;
	
	private static Random rnd = new Random ( rc.getID() );
	
	private static final int[] possibleDirections = {0,1,-1,2,-2,3,-3};
	private static ArrayList<MapLocation> pastLocations = new ArrayList<MapLocation>();
	private static final int LOCATIONS_REMEMBERED = 10;
	
	private static final int MAX_PATIENCE = 10;
	private static final int MIN_PATIENCE = -10;
	private static final int PATIENCE_DECREASE = 3;
	private static final int PATIENCE_INCREASE = 1;
	private static int patience = MAX_PATIENCE; // Start clearing rubble if this drops below zero;
	
	public static final int STEPS_AHEAD_OF_MASTER_ARCHON = 3;
	
	public static void simpleMove ( Direction dir ) throws GameActionException {
		// This was taken from Max Mann's tutorials.
		
		Direction candidateDirection = dir;
		boolean coreReady;
		
		if ( dir == null ) 
			return;
		
		coreReady = rc.isCoreReady();
		
		if ( coreReady ) {
			
			pastLocations.add( rc.getLocation() );
			if (pastLocations.size() > LOCATIONS_REMEMBERED ) {
				pastLocations.remove(0);
			}
			
			for(int i=0; i<possibleDirections.length; i++ ){
				
				candidateDirection = Direction.values()[ ( dir.ordinal() + possibleDirections[i] + 8 ) % 8 ];
				MapLocation candidateLocation = rc.getLocation().add( candidateDirection );
				
				if( rc.canMove(candidateDirection) && !pastLocations.contains( candidateLocation ) ){
					rc.move(candidateDirection);
					coreReady = false;
					increasePatience();
					break;
				} else {
					decreasePatience();
				}
				
			}
			if (patience < 0 && coreReady ) {
				if ( rc.senseRubble( rc.getLocation().add( dir ) ) > GameConstants.RUBBLE_OBSTRUCTION_THRESH ) {
					rc.clearRubble( dir );
					patience += PATIENCE_INCREASE;
				}
			}
		
		}
	}
	
	private static void increasePatience() {
		if ( patience <= MAX_PATIENCE - PATIENCE_INCREASE) {
			patience += PATIENCE_INCREASE;
		} else {
			patience = MAX_PATIENCE;
		}
	}
	
	private static void decreasePatience() {
		patience -= PATIENCE_DECREASE;
		if (patience <= MIN_PATIENCE) {
			patience = MIN_PATIENCE;
		}
	}
	
	public static Direction randomDirection() {
		return Direction.values()[(int)(rnd.nextDouble()*8)];
	}

	

}
