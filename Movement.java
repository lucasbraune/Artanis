package artanis;

import java.util.*;
import battlecode.common.*;

public class Movement {
	
	private static RobotController rc = RobotPlayer.rc;
	
	// Units will avoid going where they have already been.
	private static final int[] possibleDirections = {0,1,-1,2,-2,3,-3};
	private static ArrayList<MapLocation> pastLocations = new ArrayList<MapLocation>();
	private static final int LOCATIONS_REMEMBERED = 10;
	
	// Start clearing rubble if patience drops below zero;
	private static final int MAX_PATIENCE = 10;
	private static final int MIN_PATIENCE = -10;
	private static final int PATIENCE_DECREASE = 3;
	private static final int PATIENCE_INCREASE = 1;
	private static int patience = MAX_PATIENCE; 
	
	// Units will gather a few steps ahead of the master archon.
	private static final int STEPS_AHEAD = 3;
	
	// This is used in the randomDirection() method
	private static Random rnd = new Random ( rc.getID() );
	
	// This was taken from Max Mann's tutorials.
	public static void simpleMove ( Direction dir ) throws GameActionException {
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
	
	public static Direction randomDirection() {
		return Direction.values()[(int)(rnd.nextDouble()*8)];
	}
	
	public static MapLocation getLocationAhead ( Direction dir ) {
		return rc.getLocation().add( dir , STEPS_AHEAD );
	}
	
	public static MapLocation findClosest( MapLocation[] locations ) {	
		MapLocation closest; 
		int distanceToClosest, distanceToThisOne;
		
		if( locations.length > 0 ) {
			closest = locations[0];
			distanceToClosest = rc.getLocation().distanceSquaredTo( closest );
			for( int i=1; i<locations.length; i++ ) {
				distanceToThisOne = rc.getLocation().distanceSquaredTo( locations[i] );
				if( distanceToThisOne < distanceToClosest ) {
					closest = locations[i];
					distanceToClosest = distanceToThisOne;
				}
			}
			return closest;
		}
		else {
			return null;
		}
	}
	
	// TODO Implement rubble.
	public static ArrayList<MapLocation> blockedLocations( RobotInfo[] robots ){
		ArrayList<MapLocation> blocked = new ArrayList<MapLocation>();
		for( int i=0; i<robots.length; i++ ){
			blocked.add( robots[i].location );
		}
		return blocked;
	}
	
	// Implementation taken from:
	// http://www.redblobgames.com/pathfinding/a-star/introduction.html
	// This method returns a linked list whose first element is the location of the first
	// step one has to take to go from given start location to the given finish location.
	public static LinkedList<MapLocation> findPath( MapLocation start, MapLocation finish,
			ArrayList<MapLocation> blockedLocations ) throws GameActionException {
		
		LinkedList<MapLocation> frontier = new LinkedList<MapLocation>();
		// A key in the hash table visitedLocations is a location already visited by
		// the algorithm. The value of a given key is the neighbor from which
		// the algorithm reached the given key.
		Hashtable<MapLocation, MapLocation> pastLocations = new Hashtable<MapLocation, MapLocation>();
		
		frontier.add( start );
		pastLocations.put( start, null );
		
		while( !frontier.isEmpty() ){			
			MapLocation current = frontier.removeFirst();
			
			if ( current == finish )
				break;
			
			ArrayList<MapLocation> freeNeighbors = freeNeighbors( current, blockedLocations );

			for ( MapLocation next : freeNeighbors ) {
				if ( !pastLocations.containsKey( next ) ){
					frontier.add( next );
					pastLocations.put( next, current );
				}
			}
		}

		LinkedList<MapLocation> path = new LinkedList<MapLocation>();
		MapLocation beforeThat = finish;
		
		while( beforeThat != start ) {
			path.addFirst( pastLocations.get( beforeThat ) );
			beforeThat = pastLocations.get( beforeThat );
		}
		path.removeFirst();
		return path;
	}

	public static ArrayList<MapLocation> freeNeighbors( MapLocation center, ArrayList<MapLocation> blockedLocations ) throws GameActionException{
		MapLocation here;
		ArrayList<MapLocation> freeTiles = new ArrayList<MapLocation>();
		
		for( int i=0; i<=7; i++ ){
			here = center.add( Direction.values()[i] );
			if ( !blockedLocations.contains( here ) )
				freeTiles.add( here );
		}
		return freeTiles;
	}
	
}
