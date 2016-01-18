package artanis;

import java.util.*;
import battlecode.common.*;

public class Soldier {
	
	private static RobotController rc;

	private static RobotInfo[] enemies;
	private static RobotInfo[] allies;
	private static RobotInfo[] enemiesInRange;
	
	private static Signal[] incomingSignals;
	
	private static Queue<MapLocation> soldiersInDanger = new LinkedList<MapLocation>();
	
	// Soldier will flee if infected and has less life than 
	// INFECTED_THRESHOLD times its total life.
	private static final double INFECTED_THRESHOLD = 0.5;
	// Broadcast radius
	private static final int SMALL_RADIUS = 225;
	
	
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
		
		// TODO: Extract everything from senseNearbyRobots();
		enemies = rc.senseHostileRobots( rc.getLocation() , rc.getType().sensorRadiusSquared ) ;
		allies = rc.senseNearbyRobots( rc.getType().sensorRadiusSquared, rc.getTeam() );
		enemiesInRange = rc.senseHostileRobots( rc.getLocation() , rc.getType().attackRadiusSquared );
		
		incomingSignals = rc.emptySignalQueue();
		updateSoldiersInDanger( incomingSignals );
		
		if ( enemies.length > 0 ) {
			fight();
			return;
		}
		
		if ( !soldiersInDanger.isEmpty() ) {
			attackMove( rc.getLocation().directionTo( soldiersInDanger.element() ) );
			rc.setIndicatorString(2, "A soldier is in peril!");
		} else {
			rc.setIndicatorString(2, "Nearby soldiers seem to be safe.");
		}
		
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
	
	private static void updateSoldiersInDanger(Signal[] signals ) {
		
		// If I am in attacking range of the distress signal of a soldier
		// and there are no enemy soldiers nearby, dismiss the distress
		// signal.
		
		if ( enemies.length == 0 ) {
			for( MapLocation soldier : soldiersInDanger ){
				if ( rc.canSenseLocation( soldier ) ) {
					soldiersInDanger.remove( soldier );
				}
			}
		}
		
		// Any signal without a message is interpreted as a distress signal from
		// a soldier.
		
		for ( int i=0; i<signals.length; i++ ) {
			if ( signals[i].getMessage() == null ) {
				soldiersInDanger.add( signals[i].getLocation() );
			}
		}
		
	}

	private static void attackMove( Direction dir ) throws GameActionException { 
		if ( enemies.length > 0 ) {
			fight();
		} else {
			Movement.simpleMove( dir );
		}
	}
	
	
	
	private static void fight() throws GameActionException {
		
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
						callForHelp();
						rc.setIndicatorString(0, "Cannot attack. Fleeing from enemy.");
					}	
				} else {
					rc.setIndicatorString(0, "I didn't do anything this turn");
				}
			}
		}
	}

	// Any signal without a message is interpreted as a distress signal from
	// a soldier.
	
	private static void callForHelp() throws GameActionException {
		rc.broadcastSignal( SMALL_RADIUS );
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
	
}