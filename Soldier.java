package artanis;

import battlecode.common.*;

public class Soldier {
	
	private static RobotController rc;
	
	// Soldier will flee if infected and has less life than 
	// INFECTED_THRESHOLD times its total life.
	private static final double INFECTED_THRESHOLD = 0.5;
	
	public static void code( ){
		rc = RobotPlayer.rc;		
		
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
	
	private static void runsOnce() throws GameActionException {
		return;
	}
	
	
	private static void repeat() throws GameActionException {
		fight();
	}

//		int mySensorRadius = rc.getType().sensorRadiusSquared;
//		int myAttackRadius = rc.getType().attackRadiusSquared;
//		RobotInfo[] enemyRobots = rc.senseHostileRobots( rc.getLocation(), -1 );
//		RobotInfo[] enemiesInRange = rc.senseHostileRobots( rc.getLocation(), myAttackRadius );
//		//RobotInfo[] enemiesInRange = getEnemiesInRange( enemyRobots );
//		MapLocation weakest, target;
//		
//		signalQueue = rc.emptySignalQueue();
//		target = Communication.getLocation( signalQueue, Archon.TARGET_LOCATION );
//		
//		if( target != null ) {
//			if( enemyRobots.length>0 && 
//					rc.getLocation().distanceSquaredTo( target ) <= 2*mySensorRadius ){
//				if( enemiesInRange.length > 0 && rc.isWeaponReady() ){
//					weakest = Attack.findWeakestEnemy( enemiesInRange );
//					rc.attackLocation( weakest );
//				} else {
//					weakest = Attack.findWeakestEnemy( enemyRobots );
//					myDirection = rc.getLocation().directionTo( weakest );
//					Movement.simpleMove( myDirection );
//				}
//			} else {
//				myDirection = rc.getLocation().directionTo( target );
//				Movement.simpleMove( myDirection );
//			}
//		} else {
//			if( enemyRobots.length>0 ){
//				if( enemiesInRange.length > 0 && rc.isWeaponReady() ){
//					weakest = Attack.findWeakestEnemy( enemiesInRange );
//					rc.attackLocation( weakest );
//				} else {
//					weakest = Attack.findWeakestEnemy( enemyRobots );
//					myDirection = rc.getLocation().directionTo( weakest );
//					Movement.simpleMove( myDirection );
//				}
//			} 
//		}
	
//	private static void attackMove( Direction dir, RobotInfo[] enemies ) throws GameActionException { 
//		if ( enemies.length > 0 ) {
//			fight();
//		} else {
//			Movement.simpleMove( dir );
//		}
//	}
	
	private static void fight() throws GameActionException {
		// In principle I should extract everything from senseNearbyRobots();
		// RobotInfo[] robots = rc.senseNearbyRobots();
		RobotInfo[] enemies = rc.senseHostileRobots( rc.getLocation() , rc.getType().sensorRadiusSquared ) ;
		RobotInfo[] allies = rc.senseNearbyRobots( rc.getType().sensorRadiusSquared, rc.getTeam() );
		RobotInfo[] enemiesInRange = rc.senseHostileRobots( rc.getLocation() , rc.getType().attackRadiusSquared );
		
		if ( enemies.length > 0 ) {
			
			// If there are enemies nearby and the soldier is infected and with
			// less than 50% life, simpleMove in the direction opposite to that
			// pointing to the nearest enemy.
			
			if ( rc.getInfectedTurns() > 0 && rc.getHealth() < INFECTED_THRESHOLD * rc.getType().maxHealth ) {
				if ( rc.isCoreReady() ){
					moveDefensively( findClosest( enemies ) );
				}
				rc.setIndicatorString(0, "Fleeing because of infection.");
			} else {
				
				// If there are enemies in range and the weapon is ready, shoot the weakest one in range.
				// Try to move otherwise.
				
				if ( rc.isWeaponReady() && enemiesInRange.length > 0 ) {
					rc.attackLocation( findWeakest( enemiesInRange ).location );
					rc.setIndicatorString(0, "Attacking nearest enemy.");
				} else if (rc.isCoreReady() ){
					
					// If there are at least as many allies as enemies nearby, move offensively,
					// to a 'sweet spot' that is a good place to attack the weakest enemy nearby.
					// Move defensively otherwise, in a direction that maximizes the distance to
					// the closest enemy.
					
					if ( allies.length >= enemies.length ){
						moveOffensively( findClosest( enemies ) );
						rc.setIndicatorString(0, "Cannot attack. Moving to sweetspot.");
					} else {
						moveDefensively( findClosest( enemies ) );
						rc.setIndicatorString(0, "Cannot attack. Fleeing from enemy.");
					}	
				} else {
					rc.setIndicatorString(0, "I didn't do anything this turn");
				}
			}
		}
	}

	private static void moveOffensively(RobotInfo enemy ) throws GameActionException {
		int myAttackRadiusSquared = rc.getType().attackRadiusSquared;
		MapLocation myLocation = rc.getLocation();
		
		Direction bestDirection = null;
		int smallestError = Math.abs( myLocation.distanceSquaredTo( enemy.location ) - myAttackRadiusSquared );
		MapLocation adjacent;
		int adjacentError;
		
		for ( Direction dir : Direction.values() ) {
			adjacent = myLocation.add( dir );
			adjacentError = Math.abs( adjacent.distanceSquaredTo( enemy.location ) - myAttackRadiusSquared );
			if ( rc.canMove( dir ) && adjacentError < smallestError ) {
				bestDirection = dir;
				smallestError = adjacentError;
			}
		}
		
		if ( bestDirection == null ) {
			return;
		} else {
			rc.move( bestDirection );
		}
	}

	private static void moveDefensively( RobotInfo enemy ) throws GameActionException {
		MapLocation myLocation = rc.getLocation();
		int toEnemy = myLocation.distanceSquaredTo( enemy.location );
		
		Direction bestDirection = null;
		int largestDistanceSquared = toEnemy;
		
		for( Direction dir : Direction.values() ) {
			if ( rc.canMove( dir ) && enemy.location.distanceSquaredTo( myLocation.add( dir ) ) > largestDistanceSquared ) {
				bestDirection = dir;
				largestDistanceSquared = enemy.location.distanceSquaredTo( myLocation.add( dir ) );
			}
		}
		
		if ( bestDirection == null ) {
			return;
		} else {
			rc.move( bestDirection );
		}
	}

//	private static Direction getOpposite( Direction dir ){
//		for( int i=1; i<= 4; i++ )
//			dir.rotateRight();
//		return dir;
//	}

	private static RobotInfo findWeakest( RobotInfo[] robots ) {
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
			return robots[weakestIndex];
		} else {
			return null;
		}
	}

	private static RobotInfo findClosest( RobotInfo[] robots ) {
		if( robots.length > 0 ){
			int closestIndex = 0;
			double smallestDistanceSquared = rc.getLocation().distanceSquaredTo( robots[ closestIndex ].location );

			double distanceSquared;

			for(int i=1; i<robots.length; i++) {
				distanceSquared = robots[i].maxHealth - robots[i].health;
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

//	// Here we find a 'sweet spot' from which to attack an enemy.
//	// We search in the disk of radius rc.getType().attackRadiusSquared
//	// for a place that is as far away from enemy as possible.
//	// Given the choice of two map locations that are the same distance
//	// away from the enemy, we elect the one that is closest to us.
//	// The map locations of nearby robots are not eligible to be the
//	// sweet spot.
//	private static MapLocation findSweetSpot ( RobotInfo enemy, RobotInfo[] nearbyRobots ) throws GameActionException {
//
//		int myAttackRadiusSquared = rc.getType().attackRadiusSquared;
//		int myAttackRadius = 0;
//
//		myAttackRadius = 0;
//		while ( myAttackRadius*myAttackRadius < myAttackRadiusSquared )
//			myAttackRadius++;
//		if ( myAttackRadius*myAttackRadius > myAttackRadiusSquared )
//			myAttackRadius--;
//
//		MapLocation sweetSpot = rc.getLocation();
//		int fromMeToSweetSpot = 0;
//
//		MapLocation here;
//		int hereToEnemySquared; 
//
//		for ( int i=-myAttackRadius; i<=myAttackRadius; i++ ){
//			for ( int j=-myAttackRadius; j<=myAttackRadius; j++ ){
//				
//				here = enemy.location.add(i,j);
//				hereToEnemySquared = i*i + j*j;
//
//				if ( hereToEnemySquared <= myAttackRadiusSquared && !isAtThisLocation( nearbyRobots, here ) && rc.canSense( here ) && rc.onTheMap( here ) ){
//					// Begin code iterated in a disk around the enemy
//					if (hereToEnemySquared > enemy.location.distanceSquaredTo( sweetSpot ) ){
//						sweetSpot = here;
//						fromMeToSweetSpot = rc.getLocation().distanceSquaredTo( sweetSpot );
//					} else if ( hereToEnemySquared == enemy.location.distanceSquaredTo( sweetSpot ) ) {
//						if ( fromMeToSweetSpot > rc.getLocation().distanceSquaredTo( here ) ) {
//							sweetSpot = here;
//							fromMeToSweetSpot = rc.getLocation().distanceSquaredTo( sweetSpot );
//						}
//					}
//					// End code iterated in a disk around the enemy
//				}
//			}
//		}
//		return sweetSpot;
//	}
	
//	private static boolean isAtThisLocation( RobotInfo[] robots, MapLocation location ){
//		if ( robots.length == 0 || location == null ) {
//			return false;
//		} else {
//			for ( int i=0; i<robots.length; i++ ){
//				if ( robots[i].location == location ) {
//					return true;
//				}
//			}
//			return false;
//		}
//	}
	
}