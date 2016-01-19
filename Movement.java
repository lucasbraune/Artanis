package artanis;

import java.util.*;
import battlecode.common.*;

public class Movement {
	
	private static RobotController rc = RobotPlayer.rc;
	
	// Units will avoid going where they have already been.
	private static final int[] possibleDirections = {0,1,-1,2,-2, 3, -3};
	private static ArrayList<MapLocation> pastLocations = new ArrayList<MapLocation>();
	private static final int LOCATIONS_REMEMBERED = 5;
	
	// Start clearing rubble if patience drops below zero;
	private static final int MAX_PATIENCE = 10;
	private static final int MIN_PATIENCE = -10;
	private static final int PATIENCE_DECREASE = 3;
	private static final int PATIENCE_INCREASE = 1;
	private static int patience = MAX_PATIENCE; 
	
	// This was taken from Max Mann's tutorials.
	// See: www.battlecode.org
	public static void simpleMove ( Direction dir ) throws GameActionException {
		Direction candidateDirection = dir;
		boolean coreReady;
		
		rc.setIndicatorString(0, "SIMPLE MOVEMENT: Begin. Patience: " + patience);
		
		if ( dir == null || dir == Direction.NONE || dir == Direction.OMNI ) {
			rc.setIndicatorString(0, "MOVEMENT: Bad direction. Patience: " + patience);
			return;
		}
			
		
		coreReady = rc.isCoreReady();
		if ( coreReady ) {
			
			rc.setIndicatorString(0, "SIMPLE MOVEMENT: Core is ready. Patience: " + patience);
			
			pastLocations.add( rc.getLocation() );
			if (pastLocations.size() > LOCATIONS_REMEMBERED ) {
				pastLocations.remove(0);
			}
			
			for( int i=0; (i<possibleDirections.length && patience >= 0) || i==0; i++ ){
				candidateDirection = Direction.values()[ ( dir.ordinal() + possibleDirections[i] + 8 ) % 8 ];
				MapLocation candidateLocation = rc.getLocation().add( candidateDirection );
				
				if( rc.canMove(candidateDirection) && !pastLocations.contains( candidateLocation ) ){
					rc.move(candidateDirection);
					coreReady = false;
					if ( i==0 ) {
						increasePatience();
						rc.setIndicatorString(0, "MOVEMENT: I just moved just like I wanted to! Patience: " + patience );
					} else {
						decreasePatience();
						rc.setIndicatorString(0, "MOVEMENT: I just moved, but not quite in the direction I planned! Patience: " + patience );
					}
					break;
				}
			}
			
			if (patience < 0 && coreReady ) {
				if ( rc.senseRubble( rc.getLocation().add( dir ) ) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH ) {
					rc.clearRubble( dir );
					increasePatience();
					rc.setIndicatorString(1, "MOVEMENT: Clearing rubble.");
				} else {
					rc.setIndicatorString(1, "MOVEMENT: I could not move, but there is no reason to clear rubble where I am going.");
				}
			}

		} else { 
			rc.setIndicatorString(1, "MOVEMENTE WARNING: simpleMove was cast without check if the core is ready.");
		}
	}

	public static void moveAvoidingEnemies ( Direction dir, RobotInfo[] enemies ) throws GameActionException {
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

				if( rc.canMove(candidateDirection) && !pastLocations.contains( candidateLocation )
						&& enemiesCannotShoot( enemies, candidateLocation )){
					rc.move(candidateDirection);
					coreReady = false;
					break;
				}
			}
			
			// If scout hasn't moved, it is possibly because it was afraid of enemies.
			// Now it will become brave and try to move again.
			if ( coreReady ) {
				for(int i=0; i<possibleDirections.length; i++ ){
					candidateDirection = Direction.values()[ ( dir.ordinal() + possibleDirections[i] + 8 ) % 8 ];
					MapLocation candidateLocation = rc.getLocation().add( candidateDirection );

					if( rc.canMove(candidateDirection) && !pastLocations.contains( candidateLocation ) ){
						rc.move(candidateDirection);
						coreReady = false;
						break;
					} 
				}
			}

		}
	}
	
	private static boolean enemiesCannotShoot(RobotInfo[] enemies, MapLocation candidateLocation) {
		boolean safe = true;
		int enemyRange;
		for( int i=0; i<enemies.length; i++ ){
			enemyRange = enemies[i].location.distanceSquaredTo( candidateLocation ); 
			if( enemyRange <= RobotType.SOLDIER.attackRadiusSquared ) {
				safe = false;
			}
		}
		return safe;
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

	// This is used in the randomDirection() method
	private static Random rnd = new Random ( rc.getID() );
	
	public static Direction randomDirection() {
		return Direction.values()[(int)(rnd.nextDouble()*8)];
	}
	
	private static MapLocation findClosest( ArrayList<MapLocation> locations ) {
		if( locations.size() > 0 ){
			int closestIndex = 0;
			double smallestDistanceSquared = rc.getLocation().distanceSquaredTo( locations.get( closestIndex ) );

			double distanceSquared;

			for(int i=1; i<locations.size(); i++) {
				distanceSquared = rc.getLocation().distanceSquaredTo( locations.get(i) );
				if ( distanceSquared < smallestDistanceSquared ){
					closestIndex = i;
					smallestDistanceSquared = distanceSquared;
				}
			}
			return locations.get( closestIndex );
		} else {
			return null;
		}
	}

	
}
