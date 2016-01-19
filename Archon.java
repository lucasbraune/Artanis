package artanis;

import java.util.*;
import battlecode.common.*;

public class Archon {
	
	private static RobotController rc;

	private static ArrayList<RobotInfo> enemies = new ArrayList<RobotInfo>();
	private static ArrayList<RobotInfo> allies = new ArrayList<RobotInfo>();
	private static ArrayList<RobotInfo> enemiesInRange = new ArrayList<RobotInfo>();
	private static ArrayList<RobotInfo> densNearby = new ArrayList<RobotInfo>();
	private static ArrayList<RobotInfo> neutralsNearby = new ArrayList<RobotInfo>();
	private static ArrayList<MapLocation> partsNearby = new ArrayList<MapLocation>();
	
	private static LinkedList<Signal> incomingSignals = new LinkedList<Signal>();
	private static LinkedList<MapLocation> zombieDens = new LinkedList<MapLocation>();
	private static LinkedList<MapLocation> parts = new LinkedList<MapLocation>();
	private static LinkedList<MapLocation> neutralRobots = new LinkedList<MapLocation>();
	private static final int MAX_QUEUE_SIZE = 5;
	
	// Broadcast radius for signals
	private static final int SMALL_RADIUS = 225;
	private static final int MEDIUM_RADIUS = 625;
	
	// Message meanings
	public static final int HELP = 1000;
	
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

		// updateNearbyRobots() for archons is different from the
		// method with the same name that soldiers have. Namely, archons do
		// not consider the opponent's scouts as enemies (for the purposes
		// of fleeing).
		updateNearbyRobots();
		updateIncomingSignals();
		
		updateTasks();
		checkForDens();
		checkForParts();
		//checkForNeutrals();
		
		rc.setIndicatorString( 1 , "Number of locations with parts that have been spotted: " + parts.size() );

		if ( rc.isCoreReady() ){
			
			// Run if need be
			
			if ( enemies.size() > allies.size() ) {
				moveDefensively( findClosest( enemies ) );
				callForHelp();
				rc.setIndicatorString(0, "Fleeing from enemies.");
				return;
			}

			// If safe, build a soldier if possible

			Direction dir = findDirectionToBuid();
			if ( dir != Direction.NONE ){
				rc.build( dir , RobotType.SOLDIER );
				rc.setIndicatorString(0, "Building soldier.");
				return;
			}

			// If cannot build a soldier, try to collect parts.

			if ( partsNearby.size() > 0 ) {
				Movement.simpleMove( rc.getLocation().directionTo( partsNearby.get(0) ) );
				rc.setIndicatorString(0, "Going for parts.");
				return;
			} else if ( !parts.isEmpty() ) {
				Movement.simpleMove( rc.getLocation().directionTo( parts.element() ) );
				rc.setIndicatorString(0, "Going for parts.");
				return;
			} 

			rc.setIndicatorString(0, "Nothing to do.");
			
		}
		
	}

	private static Direction findDirectionToBuid() {
		Direction dir = Movement.randomDirection();
		int i;
		for( i=1; i<=8; i++ ) {
			dir.rotateLeft();
			if ( rc.canBuild( dir, RobotType.SOLDIER ) ){
				break;
			}
		}
		// i<=8 will occur if, and only if, break was called.
		if ( i<= 8 ) 
			return dir;
		else
			return Direction.NONE;
	}

	private static void checkForNeutrals() throws GameActionException {

		RobotInfo[] neutrals = rc.senseNearbyRobots(-1, Team.NEUTRAL);

		if ( neutrals.length > 0 ) {
			RobotInfo neutral = enemies.get(0);

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
		
		if( !knownPartsLocation && partsNearby.size() > 0 ) {
			parts.add( rc.getLocation() );
			if( parts.size() > MAX_QUEUE_SIZE ) {
				parts.remove();
			}
			rc.broadcastSignal( MEDIUM_RADIUS );
			rc.broadcastSignal( MEDIUM_RADIUS );
			rc.broadcastSignal( MEDIUM_RADIUS );
		}
	}

	// The following method is different from the one with the same name
	// that the soldier class has.
	
	private static void updateNearbyRobots() {
		// TODO: Extract everything from senseNearbyRobots();
		RobotInfo[] robots = rc.senseNearbyRobots();
		
		enemies.clear();
		allies.clear();
		enemiesInRange.clear();
		densNearby.clear();
		neutralsNearby.clear();
		
		for( int i=0; i<robots.length; i++ ){
			
			if ( robots[i].team == rc.getTeam() ) {
				allies.add( robots[i] );
			} else if ( robots[i].team == rc.getTeam().opponent() ){
				if( robots[i].type != RobotType.SCOUT ){
					enemies.add( robots[i] );
				}
			} else if ( robots[i].team == Team.ZOMBIE ){
				if ( robots[i].type == RobotType.ZOMBIEDEN ) {
					densNearby.add( robots[i] );
				} else {
					enemies.add( robots[i] );
				}
			} else if ( robots[i].team == Team.NEUTRAL ) {
				neutralsNearby.add( robots[i] );
			} 
			
		}
		
		for( RobotInfo enemy : enemies ){
			if ( rc.canAttackLocation( enemy.location ) ){
				enemiesInRange.add( enemy );
			}
		}
		
		partsNearby.clear();
		MapLocation[] locations = rc.sensePartLocations(-1); 
		for ( int i=0; i<locations.length; i++ ){
			partsNearby.add( locations[i] );
		}
	}

	private static void checkForDens() throws GameActionException {

		for( RobotInfo den : densNearby ) {
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
				if( zombieDens.size() > MAX_QUEUE_SIZE ) {
					zombieDens.remove();
				}
				rc.broadcastSignal( MEDIUM_RADIUS );
				rc.broadcastSignal( MEDIUM_RADIUS );
			}
			// End of code that is run near a zombie den
		}
		
	}

	private static void updateIncomingSignals() {

		Signal[] signals = rc.emptySignalQueue();
		incomingSignals.clear();
		
		for( int i=0; i<signals.length; i++ ) {
			if ( signals[i].getTeam() == rc.getTeam() )
				incomingSignals.add( signals[i] );
		}
	}

	private static void checkForCompletedTasks( ) {
		
		// If there are no spare parts nearby, cross the current location
		// from the list of locations with parts.
		
		if ( partsNearby.size() == 0 ){
			
			Iterator<MapLocation> iterator = parts.iterator();
			while( iterator.hasNext() ){
				if( rc.getLocation().distanceSquaredTo( iterator.next() ) < RobotType.SOLDIER.attackRadiusSquared ) {
					iterator.remove();
				}
			}

		}
	}

	private static void updateTasks() {

		// Start processing signals from soldiers 
		// If signal has no message, it is a signal from a soldier.

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
				if ( bop.getID() == beep.getID() ) {
					count++;
				}
			}

			switch ( count ) {
			
			case 3:
				Iterator<MapLocation> iterator3 = parts.iterator();
				while( iterator3.hasNext() ){
					if( iterator3.next().distanceSquaredTo( beep.getLocation() ) < rc.getType().attackRadiusSquared ) {
						iterator3.remove();
					}
				}
				parts.add( beep.getLocation() );
				if( parts.size() > MAX_QUEUE_SIZE ) {
					parts.remove();
				}
				break;
				
			}

		}

		// Start processing message signals

		for ( Signal ding : incomingSignals ) {
			if ( ding.getMessage() != null && ding.getMessage().length > 0 ) {
				int message = ding.getMessage()[0]; 
				
				switch ( message ) {
				case Archon.HELP:
//					Iterator<Signal> iterator3 = archonsInDanger.iterator();
//					while( iterator3.hasNext() ){
//						if( iterator3.next().getID() == ding.getID() ) {
//							iterator3.remove();
//						}
//					}
//					archonsInDanger.add( ding );
					break;
				}
				
			}
		}

		checkForCompletedTasks();
	}




	// A single signal without a message is interpreted as a distress signal from
	// a soldier.
	
	private static void callForHelp() throws GameActionException {
//		if( !sentASignalThisTurn ){
			rc.broadcastSignal( SMALL_RADIUS );
//		}
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

	private static RobotInfo findClosest( ArrayList<RobotInfo> robots ) {
		if( robots.size() > 0 ){
			int closestIndex = 0;
			double smallestDistanceSquared = rc.getLocation().distanceSquaredTo( robots.get( closestIndex ).location );

			double distanceSquared;

			for(int i=1; i<robots.size(); i++) {
				distanceSquared = robots.get(i).maxHealth - robots.get(i).health;
				if ( distanceSquared < smallestDistanceSquared ){
					closestIndex = i;
					smallestDistanceSquared = distanceSquared;
				}
			}
			return robots.get( closestIndex );
		} else {
			return null;
		}
	}
	
}