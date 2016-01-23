package artanis;

import java.util.ArrayList;
import java.util.Random;

import battlecode.common.*;

class BasicRobot {
	
	RobotController rc;
	Readings readings;
	TeamGoals teamGoals;
	
	BasicRobot( RobotController rcIn ) {
		rc = rcIn;
		readings = new Readings( rc.getType(), rc.getTeam() );
		teamGoals = new TeamGoals( rc.getType() );
	} 
	
	void code( ){
		
		try {
			runsOnce();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		while(true){
			try {
				repeat();
			} catch ( Exception e ) {
				e.printStackTrace();
			}
			Clock.yield();
		}	
	}
	
	void runsOnce() throws GameActionException {
		return;
	}

	void repeat() throws GameActionException {
		return;
	}

	//////////////////////////////////////////////////////////////////////
	/////////////////////// Begin movement methods ///////////////////////
	//////////////////////////////////////////////////////////////////////

	// Directions to try if unable to move
	private static final int[] possibleDirections = {0,1,-1,2,-2, 3, -3};

	// Units will avoid going where they have already been.
	private ArrayList<MapLocation> pastLocations = new ArrayList<MapLocation>();
	private static final int LOCATIONS_REMEMBERED = 5;

	// Start clearing rubble if patience drops below zero;
	private static final int MAX_PATIENCE = 10;
	private static final int MIN_PATIENCE = -10;
	private static final int PATIENCE_DECREASE = 3;
	private static final int PATIENCE_INCREASE = 1;
	private static int patience = MAX_PATIENCE; 

	// This was taken from Max Mann's tutorials.
	// See: www.battlecode.org
	void simpleMove ( Direction dir ) throws GameActionException {

		if ( dir == null || dir == Direction.NONE || dir == Direction.OMNI ) {
			return;
		}

		pastLocations.add( rc.getLocation() );
		if (pastLocations.size() > LOCATIONS_REMEMBERED ) {
			pastLocations.remove(0);
		}

		Direction candidateDirection;
		MapLocation candidateLocation;
		boolean moved = false;
		for( int i=0; i == 0 || ( i < possibleDirections.length && patience >= 0 ); i++ ){
			candidateDirection = Direction.values()[ ( dir.ordinal() + possibleDirections[i] + 8 ) % 8 ];
			candidateLocation = rc.getLocation().add( candidateDirection );


			if( rc.canMove(candidateDirection) && !pastLocations.contains( candidateLocation ) ){
				rc.move(candidateDirection);
				moved = true;
				if ( i == 0 ) {
					increasePatience();
				} else {
					decreasePatience();
				}
				break;
			}
		}

		if (patience < 0 && !moved ) {
			if ( rc.senseRubble( rc.getLocation().add( dir ) ) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH ) {
				rc.clearRubble( dir );
				increasePatience();
			}
		}
		
		if ( !moved && rc.getType() == RobotType.ARCHON ) {
			teamGoals.askToClearTheWay( rc, (int) Math.max( (double) -patience , 0) );
		}
	}


	private void increasePatience() {
		if ( patience <= MAX_PATIENCE - PATIENCE_INCREASE) {
			patience += PATIENCE_INCREASE;
		} else {
			patience = MAX_PATIENCE;
		}
	}

	private void decreasePatience() {
		patience -= PATIENCE_DECREASE;
		if (patience <= MIN_PATIENCE) {
			patience = MIN_PATIENCE;
		}
	}
	
	void moveAvoidingEnemies ( Direction dir, RobotInfo[] enemies ) throws GameActionException {
		
		if ( dir == null || dir == Direction.NONE || dir == Direction.OMNI ) {
			return;
		}

		if ( rc.isCoreReady() ) {

			pastLocations.add( rc.getLocation() );
			if (pastLocations.size() > LOCATIONS_REMEMBERED ) {
				pastLocations.remove(0);
			}

			Direction candidateDirection;
			MapLocation candidateLocation;
			
			for(int i=0; i<possibleDirections.length; i++ ){
				candidateDirection = Direction.values()[ ( dir.ordinal() + possibleDirections[i] + 8 ) % 8 ];
				candidateLocation = rc.getLocation().add( candidateDirection );

				if( rc.canMove(candidateDirection) && !pastLocations.contains( candidateLocation )
						&& enemiesCannotShoot( enemies, candidateLocation )){
					rc.move(candidateDirection);
					break;
				}
			}
			
			// If robot hasn't moved, it is possibly because it was afraid of enemies.
			// Now it will become brave and try to move again.
			if ( rc.isCoreReady() ) {
				for(int i=0; i<possibleDirections.length; i++ ){
					candidateDirection = Direction.values()[ ( dir.ordinal() + possibleDirections[i] + 8 ) % 8 ];
					candidateLocation = rc.getLocation().add( candidateDirection );

					if( rc.canMove(candidateDirection) && !pastLocations.contains( candidateLocation ) ){
						rc.move(candidateDirection);
						break;
					} 
				}
			}

		}
	}
	
	private boolean enemiesCannotShoot(RobotInfo[] enemies, MapLocation candidateLocation) {
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
	
	void moveDefensively( RobotInfo enemy ) throws GameActionException {
		MapLocation myLocation = rc.getLocation();
		int toEnemy = myLocation.distanceSquaredTo( enemy.location );
		
		Direction bestDirection = null;
		int largestDistanceSquared = toEnemy;
		int fromLocationToEnemy;
		
		for( Direction dir : Direction.values() ) {
			fromLocationToEnemy = enemy.location.distanceSquaredTo( myLocation.add( dir ) );
			if ( rc.canMove( dir ) && fromLocationToEnemy > largestDistanceSquared ) {
				bestDirection = dir;
				largestDistanceSquared = fromLocationToEnemy;
			}
		}
		
		if ( bestDirection == null ) {
			simpleMove( myLocation.directionTo( enemy.location ).opposite() );
		} else {
			simpleMove( bestDirection );
		}
	}
	
	////////////////////////////////////////////////////////////////////////
	//////////////////////// Begin utility methods /////////////////////////
	////////////////////////////////////////////////////////////////////////

	Direction randomDirection( ) {
		Random rnd = new Random( rc.getRoundNum() );
		rc.setIndicatorString(3, "" + (int)(rnd.nextDouble()*8));
		return Direction.values()[(int)(rnd.nextDouble()*8)];
	}
	
	MapLocation findClosestLocation( MapLocation[] locations ) {
		if( locations.length > 0 ){
			int closestIndex = 0;
			double smallestDistanceSquared = rc.getLocation().distanceSquaredTo( locations[ closestIndex ] );

			double distanceSquared;

			for(int i=1; i<locations.length; i++) {
				distanceSquared = rc.getLocation().distanceSquaredTo( locations[i] );
				if ( distanceSquared < smallestDistanceSquared ){
					closestIndex = i;
					smallestDistanceSquared = distanceSquared;
				}
			}
			return locations[ closestIndex ];
		} else {
			return null;
		}
	}

	RobotInfo findClosestRobot( RobotInfo[] robots ) {
		if( robots.length > 0 ){
			int closestIndex = 0;
			double smallestDistanceSquared = rc.getLocation().distanceSquaredTo( robots[ closestIndex ].location );

			double distanceSquared;

			for(int i=1; i<robots.length; i++) {
				distanceSquared = rc.getLocation().distanceSquaredTo( robots[i].location );
				if ( distanceSquared < smallestDistanceSquared ){
					closestIndex = i;
					smallestDistanceSquared = distanceSquared;
				}
			}
			return robots[ closestIndex ];
		} else {
			return null;
		}
	}
	
	RobotInfo findWeakestRobot( RobotInfo[] robots ) {
		if( robots.length > 0 ){
			int weakestIndex = 0;
			double largestWeakness = robots[ weakestIndex ].maxHealth - robots[ weakestIndex ].health;

			double weakness;

			for(int i=1; i<robots.length; i++) {
				weakness = robots[i].maxHealth - robots[i].health;
				if ( weakness > largestWeakness ){
					weakestIndex = i;
					largestWeakness = weakness;
				}
			}
			return robots[ weakestIndex ];
		} else {
			return null;
		}
	}

}
