package artanis;

import java.util.*;
import battlecode.common.*;

public class Soldier {

	private static RobotController rc = RobotPlayer.rc;
	
	private static Readings readings = new Readings( rc );
	
	private static ArrayList<RobotInfo> enemies = new ArrayList<RobotInfo>();
	private static ArrayList<RobotInfo> allies = new ArrayList<RobotInfo>();
	private static ArrayList<RobotInfo> enemiesInRange = new ArrayList<RobotInfo>();
	private static ArrayList<RobotInfo> densNearby = new ArrayList<RobotInfo>();
	private static ArrayList<RobotInfo> neutralsNearby = new ArrayList<RobotInfo>();
	private static ArrayList<MapLocation> partsNearby = new ArrayList<MapLocation>();
	private static LinkedList<Signal> incomingSignals = new LinkedList<Signal>();
	
	private static LinkedList<Signal> archonsInDanger = new LinkedList<Signal>();
	private static LinkedList<Signal> soldiersInDanger = new LinkedList<Signal>();
	private static LinkedList<MapLocation> zombieDens = new LinkedList<MapLocation>();
	private static LinkedList<MapLocation> parts = new LinkedList<MapLocation>();
	private static LinkedList<MapLocation> neutralRobots = new LinkedList<MapLocation>();
	private static final int MAX_QUEUE_SIZE = 5;
	
	// For scouting
	private static int timeGoingOneWay = 0;
	private static Direction scoutingDirection = Direction.NONE;
	private static final int MAX_TIME_ONE_WAY = 5;
	
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
		checkForParts();
		//checkForNeutrals();
		
		rc.setIndicatorString( 4 , "Soldiers needing help: " + soldiersInDanger.size() );

		if ( enemies.size() > 0 ) {
			fight();
			rc.setIndicatorString(2, "Fighting right now.");
			return;
		}
		
		if ( !archonsInDanger.isEmpty() ) {
			attackMove( rc.getLocation().directionTo( soldiersInDanger.element().getLocation() ) );
			rc.setIndicatorString(2, "A soldier is in peril!");
			return;
		} 

		if ( !soldiersInDanger.isEmpty() ) {
			attackMove( rc.getLocation().directionTo( soldiersInDanger.element().getLocation() ) );
			rc.setIndicatorString(2, "A soldier is in peril!");
			return;
		} 
		
		addZombieDensAsEnemies();
		if ( densNearby.size() > 0 ){
			fight();
			rc.setIndicatorString(2, "I am fighting a Zombie den.");
			return;
		} else if ( !zombieDens.isEmpty() ) {
			attackMove( rc.getLocation().directionTo( zombieDens.element() ) );
			rc.setIndicatorString(2, "I am moving towards a Zombie Den.");
			return;
		}
		
		if ( rc.isCoreReady() ) {
			if ( timeGoingOneWay < MAX_TIME_ONE_WAY ) {
				attackMove( scoutingDirection );
				timeGoingOneWay++;
				rc.setIndicatorString(2, "I am happily scouting.");
				return;
			} else {
				scoutingDirection = Movement.randomDirection();
				attackMove( scoutingDirection );
				timeGoingOneWay = 0;
				rc.setIndicatorString(2, "I am happily scouting.");
				return;
			}
		} 

		rc.setIndicatorString(2, "Nothing to do..." );
		return;

	}

	private static void addZombieDensAsEnemies() {
		for( RobotInfo den : densNearby ) {
			enemies.add( den );
			if( rc.canAttackLocation( den.location ) ){
				enemiesInRange.add( den );
			}
		}
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
			if( !oldNeutral && !sentASignalThisTurn ){
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
		
		if( !knownPartsLocation && partsNearby.size() > 0 && !sentASignalThisTurn ) {
			parts.add( rc.getLocation() );
			if( parts.size() > MAX_QUEUE_SIZE ) {
				parts.remove();
			}
			rc.broadcastSignal( MEDIUM_RADIUS );
			rc.broadcastSignal( MEDIUM_RADIUS );
			rc.broadcastSignal( MEDIUM_RADIUS );
		}
	}

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
				enemies.add( robots[i] );
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
			if( !oldDen && !sentASignalThisTurn ){
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
		
		// If I am in attacking range of the distress signal of a soldier
		// and there are no enemy soldiers nearby, dismiss the distress
		// signal.

		if ( enemies.size() == 0 ) {
			
			Iterator<Signal> iterator = archonsInDanger.iterator();
			while( iterator.hasNext() ){
				if( rc.canAttackLocation( iterator.next().getLocation() ) ) {
					iterator.remove();
				}
			}
			
			Iterator<Signal> iterator1 = soldiersInDanger.iterator();
			while( iterator1.hasNext() ){
				if( rc.canAttackLocation( iterator1.next().getLocation() ) ) {
					iterator1.remove();
				}
			}
			
			if ( densNearby.size() == 0 ){
				Iterator<MapLocation> iterator2 = zombieDens.iterator();
				while( iterator2.hasNext() ){
					if( rc.canAttackLocation( iterator2.next() ) ) {
						iterator2.remove();
					}
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
			case 1:
				Iterator<Signal> iterator = soldiersInDanger.iterator();
				while( iterator.hasNext() ){
					if( iterator.next().getID() == beep.getID() ) {
						iterator.remove();
					}
				}
				soldiersInDanger.add( beep );
				if( soldiersInDanger.size() > MAX_QUEUE_SIZE ) {
					soldiersInDanger.remove();
				}
				break;
			case 2:
				Iterator<MapLocation> iterator2 = zombieDens.iterator();
				while( iterator2.hasNext() ){
					if( iterator2.next().distanceSquaredTo( beep.getLocation() ) < rc.getType().attackRadiusSquared ) {
						iterator2.remove();
					}
				}
				zombieDens.add( beep.getLocation() );
				if( zombieDens.size() > MAX_QUEUE_SIZE ) {
					zombieDens.remove();
				}
				break;
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
					Iterator<Signal> iterator3 = archonsInDanger.iterator();
					while( iterator3.hasNext() ){
						if( iterator3.next().getID() == ding.getID() ) {
							iterator3.remove();
						}
					}
					archonsInDanger.add( ding );
					if( archonsInDanger.size() > MAX_QUEUE_SIZE ) {
						archonsInDanger.remove();
					}
					break;
				}
				
			}
		}

		checkForCompletedTasks();
	}

	private static void attackMove( Direction dir ) throws GameActionException { 
		if ( enemies.size() > 0 ) {
			fight();
		} else {
			Movement.simpleMove( dir );
		}
	}

	private static void fight() throws GameActionException {

		if ( enemies.size() > 0 ) {
			
			// If there are enemies nearby and the soldier is infected and with
			// less than 50% life, simpleMove in the direction opposite to that
			// pointing to the nearest enemy.
			
			if ( rc.getInfectedTurns() > 0 && rc.getHealth() < INFECTED_THRESHOLD * rc.getType().maxHealth ) {
				if ( rc.isCoreReady() ){
					moveDefensively( getClosestEnemy() );
				}
				rc.setIndicatorString(0, "Fleeing because of infection.");
			} else {
				
				// If there are enemies in range and the weapon is ready, shoot the weakest one in range.
				// Try to move otherwise.
				
				if ( rc.isWeaponReady() && enemiesInRange.size() > 0 ) {
					rc.attackLocation( getWeakestEnemy().location );
					rc.setIndicatorString(0, "Attacking nearest enemy.");
				} else if (rc.isCoreReady() ){
					
					// If there are at least as many allies as enemies nearby, move offensively,
					// to a 'sweet spot' that is a good place to attack the weakest enemy nearby.
					// Move defensively otherwise, in a direction that maximizes the distance to
					// the closest enemy.
					
					if ( allies.size() >= enemies.size() ){
						moveOffensively( getClosestEnemy() );
						rc.setIndicatorString(0, "Cannot attack. Moving to sweetspot.");
					} else {
						moveDefensively( getClosestEnemy() );
						
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
	

	//////////////////////////////////////////////////////////////////////
	//////////// Methods for offensive and defensive movement ////////////
	//////////////////////////////////////////////////////////////////////
	
	// Added break command in the Direction loops of both moveOffensively and
	// moveDefensively to improve bytecode performance
	
	private static void moveOffensively(RobotInfo enemy ) throws GameActionException {
		int myAttackRadiusSquared = rc.getType().attackRadiusSquared;
		MapLocation myLocation = rc.getLocation();
		
		Direction bestDirection = null;
		int smallestError = myAttackRadiusSquared - myLocation.distanceSquaredTo( enemy.location );
		MapLocation adjacent;
		int adjacentError;

		for ( Direction dir : Direction.values() ) {
			adjacent = myLocation.add( dir );
			adjacentError = myAttackRadiusSquared - adjacent.distanceSquaredTo( enemy.location );
			if ( rc.canMove( dir ) && adjacentError < smallestError ) {
				bestDirection = dir;
				smallestError = adjacentError;
				break;
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
				break;
			}
		}
		
		if ( bestDirection == null ) {
			return;
		} else {
			rc.move( bestDirection );
		}
	}
	
	/////////////////////////////////////////////////////////////////////
	////// Methods for finding the weakest and the closest enemies //////
	/////////////////////////////////////////////////////////////////////
	
	private static RobotInfo getWeakestEnemy() {
		ArrayList<RobotInfo> candidates = new ArrayList<RobotInfo>();
		if ( enemiesInRange.size() > 0 ){
			for ( int i=0; i<enemiesInRange.size() && i<5; i++ ) {
				candidates.add( enemiesInRange.get(i) );
			}
		} else {
			for ( int i=0; i<enemies.size() && i<5; i++ ) {
				candidates.add( enemies.get(i) );
			}
		}
		return findWeakest( candidates );
	}

	private static RobotInfo getClosestEnemy() {
		ArrayList<RobotInfo> candidates = new ArrayList<RobotInfo>();
		if ( enemiesInRange.size() > 0 ){
			for ( int i=0; i<enemiesInRange.size() && i<5; i++ ) {
				candidates.add( enemiesInRange.get(i) );
			}
		} else {
			for ( int i=0; i<enemies.size() && i<5; i++ ) {
				candidates.add( enemies.get(i) );
			}
		}
		return findClosest( candidates );
	}

	private static RobotInfo findWeakest( ArrayList<RobotInfo> robots ) {
		if( robots.size() > 0 ){
			int weakestIndex = 0;
			double largestWeakness = robots.get( weakestIndex ).maxHealth - robots.get( weakestIndex ).health;

			double weakness;

			for(int i=1; i<robots.size(); i++) {
				weakness = robots.get(i).maxHealth - robots.get(i).health;
				if ( weakness > largestWeakness ){
					weakestIndex = i;
					largestWeakness = weakness;
				}
			}
			return robots.get(weakestIndex);
		} else {
			return null;
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