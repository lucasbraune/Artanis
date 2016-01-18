package artanis;

import java.util.*;
import battlecode.common.*;

public class Soldier {
	
	private static RobotController rc;

	private static RobotInfo[] enemies;
	private static RobotInfo[] allies;
	private static RobotInfo[] enemiesInRange;
	
	private static LinkedList<Signal> incomingSignals = new LinkedList<Signal>();
	private static LinkedList<Signal> archonsInDanger = new LinkedList<Signal>();
	private static LinkedList<Signal> soldiersInDanger = new LinkedList<Signal>();
	private static LinkedList<MapLocation> zombieDens = new LinkedList<MapLocation>();
	private static LinkedList<MapLocation> parts = new LinkedList<MapLocation>();
	private static LinkedList<MapLocation> neutralRobots = new LinkedList<MapLocation>();
	
	private static boolean sentASignalThisTurn = false;
	
	// Soldier will flee if infected and has less life than 
	// INFECTED_THRESHOLD times its total life.
	private static final double INFECTED_THRESHOLD = 0.5;
	
	// Broadcast radius for distress signals
	private static final int SMALL_RADIUS = 225;
	private static final int MEDIUM_RADIUS = 625;
	
	
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
	
		// Think before changing the order in which these methods are executed.
		updateNearbyRobots();
		updateIncomingSignals();
		sentASignalThisTurn = false;
		updateTasks();
		checkForDens();
//		checkForParts();
//		checkForNeutrals();
		
		if ( enemies.length > 0 ) {
			fight();
			return;
		}
		
		// TODO Go to nearest soldier instead
		
		if ( !soldiersInDanger.isEmpty() ) {
			attackMove( rc.getLocation().directionTo( soldiersInDanger.element().getLocation() ) );
			rc.setIndicatorString(2, "A soldier is in peril!");
			return;
		} 
		
		if ( !zombieDens.isEmpty() ) {
			attackMove( rc.getLocation().directionTo( zombieDens.element() ) );
			rc.setIndicatorString(2, "Let's kill a Zombie Den!");
			return;
		} 

		rc.setIndicatorString(2, "Nothing to do..." );

	}

	private static void checkForNeutrals() throws GameActionException {

		RobotInfo[] neutrals = rc.senseNearbyRobots(-1, Team.NEUTRAL);

		if ( neutrals.length > 0 ) {
			RobotInfo neutral = enemies[0];

			// Check if these neutrals were already accounted for
			boolean oldNeutral = false;

			Iterator<MapLocation> iterator = neutralRobots.iterator();
			while( iterator.hasNext() ){
				if( rc.canSenseLocation( iterator.next() ) ) {
					oldNeutral = true;
					break;
				}
			}

			// If not, warn other units by broadcasting four signals.
			if( !oldNeutral ){
				neutralRobots.add( neutral.location );
				rc.broadcastSignal( MEDIUM_RADIUS );
				rc.broadcastSignal( MEDIUM_RADIUS );
				rc.broadcastSignal( MEDIUM_RADIUS );
				rc.broadcastSignal( MEDIUM_RADIUS );
			}
		}
	}

	private static void checkForParts() throws GameActionException {

		// Check if you already know that there are parts near here
		
		boolean knownPartsLocation = false;

		Iterator<MapLocation> iterator = parts.iterator();
		while( iterator.hasNext() ){
			if( rc.canSenseLocation( iterator.next() ) ) {
				knownPartsLocation = true;
				break;
			}
		}

		// If not, and if there are parts nearby, let other people know
		// by broadcasting three signals and take note yourself
		// of this location yourself.
		
		if( !knownPartsLocation && rc.sensePartLocations(-1).length > 0 ) {
			parts.add( rc.getLocation() );
			rc.broadcastSignal( MEDIUM_RADIUS );
			rc.broadcastSignal( MEDIUM_RADIUS );
			rc.broadcastSignal( MEDIUM_RADIUS );
		}
	}

	private static void updateNearbyRobots() {
		// TODO: Extract everything from senseNearbyRobots();
		enemies = rc.senseHostileRobots( rc.getLocation() , rc.getType().sensorRadiusSquared ) ;
		allies = rc.senseNearbyRobots( rc.getType().sensorRadiusSquared, rc.getTeam() );
		enemiesInRange = rc.senseHostileRobots( rc.getLocation() , rc.getType().attackRadiusSquared );
	}

	private static void checkForDens() throws GameActionException {

		for( int i=0; i<enemies.length; i++ ){
			if ( enemies[i].type == RobotType.ZOMBIEDEN ) {
				
				// Begin code that is run near a zombie den
				RobotInfo den = enemies[i];
				
				// Check if this den was already accounted for
				boolean oldDen = false;
				
				Iterator<MapLocation> iterator = zombieDens.iterator();
				while( iterator.hasNext() ){
					if( rc.canSenseLocation( iterator.next() ) ) {
						oldDen = true;
						break;
					}
				}
				
				// If not, warn other units by broadcasting two signals.
				if( !oldDen ){
					zombieDens.add( den.location );
					rc.broadcastSignal( MEDIUM_RADIUS );
					rc.broadcastSignal( MEDIUM_RADIUS );
				}
				// End of code that is run near a zombie den
				
			}
		}
		
	}
	
	private static void updateIncomingSignals() {
		
		Signal[] signals = rc.emptySignalQueue();
		incomingSignals.clear();
		
		for( int i=0; i<signals.length; i++ ) {
			incomingSignals.add( signals[i] );
		}
	}

	private static void checkForCompletedTasks( ) {
		
		// If I am in attacking range of the distress signal of a soldier
		// and there are no enemy soldiers nearby, dismiss the distress
		// signal.

		if ( enemies.length == 0 ) {
			
			Iterator<Signal> iterator = soldiersInDanger.iterator();
			while( iterator.hasNext() ){
				if( rc.canSenseLocation( iterator.next().getLocation() ) ) {
					iterator.remove();
				}
			}

			Iterator<MapLocation> iterator2 = zombieDens.iterator();
			while( iterator2.hasNext() ){
				if( rc.canSenseLocation( iterator2.next() ) ) {
					iterator.remove();
				}
			}
			
		}
	}

	private static void updateTasks() {
		// If beep has no message, it is a signal from a soldier.

		LinkedList<Signal> fromSoldiers = new LinkedList<Signal>();

		for ( Signal beep : incomingSignals ) {
			if ( beep.getMessage() == null ) {
				fromSoldiers.add( beep );
			}
		}

		// If that soldier sent only one signal this (last?) turn, it is
		// calling for help. Note that removing an element from a
		// LinkedList return false if the list does not contain the element.

		for ( Signal beep : fromSoldiers ) {

			int count = 0;
			for ( Signal bop : fromSoldiers ) {
				if ( bop == beep ) {
					count++;
				}
			}

			switch ( count ) {
			
			case 1:
				Iterator<Signal> iterator = soldiersInDanger.iterator();
				while( iterator.hasNext() ){
					if( iterator.next().getID() == beep.getID() ) {
						iterator.remove();
					}
				}
				soldiersInDanger.add( beep );
				break;

			case 2:
				Iterator<MapLocation> iterator2 = zombieDens.iterator();
				while( iterator2.hasNext() ){
					if( iterator2.next().distanceSquaredTo( beep.getLocation() ) < rc.getType().attackRadiusSquared ) {
						iterator2.remove();
					}
				}
				zombieDens.add( beep.getLocation() );
				break;
				
			}
			
			checkForCompletedTasks();
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

	// A single signal without a message is interpreted as a distress signal from
	// a soldier.
	
	private static void callForHelp() throws GameActionException {
		if( !sentASignalThisTurn ){
			rc.broadcastSignal( SMALL_RADIUS );
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

	// The findWeakest method does what its name says, except
	// that it will only return a Zombie Den only if there are
	// no enemy units of other types around.
	
	private static RobotInfo findWeakest( RobotInfo[] robots ) {
		if( robots.length > 0 ){
			int weakestIndex = 0;
			double largestWeakness = robots[ weakestIndex ].maxHealth - robots[ weakestIndex ].health;

			double weakness;

			for(int i=1; i<robots.length; i++) {
				weakness = robots[i].maxHealth - robots[i].health;
				if ( weakness > largestWeakness || robots[weakestIndex].type == RobotType.ZOMBIEDEN ){
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