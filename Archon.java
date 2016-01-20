package artanis;

import java.util.*;
import battlecode.common.*;

public class Archon {
	
	private static RobotController rc = RobotPlayer.rc;

	private static Readings readings = new Readings( rc.getType(), rc.getTeam() );
	private static Goals teamGoals = new Goals( rc.getType() );
	
	public static int lastGoal = 0;
	
	private static final int NEUTRAL_ROBOTS = 1;
	private static final int PARTS = 2;
	
	// Message meanings
	public static final int HELP = 1000;
	
	public static void code( ){
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

		readings.update( rc.getLocation() , rc.senseNearbyRobots(), rc.sensePartLocations(-1), rc.emptySignalQueue() );
		teamGoals.update( readings );
		teamGoals.transmitNewGoal( rc );
		
		if ( rc.isCoreReady() ){
			
			readings.update( rc.getLocation() , rc.senseNearbyRobots(), rc.sensePartLocations(-1), rc.emptySignalQueue() );
			
			
			// Run if need be
			
			if ( readings.enemies.size() > 0 ) {
				Soldier.moveDefensively( getClosestEnemy() );
				teamGoals.callForHelp( rc );
				
				if( lastGoal == PARTS && teamGoals.parts.size()>0 ) {
					teamGoals.parts.remove();
				} else if( lastGoal == NEUTRAL_ROBOTS && teamGoals.neutralRobots.size()>0 ) {
					teamGoals.neutralRobots.remove();
				}
				
				rc.setIndicatorString(0, "Fleeing from enemies.");
				return;
			}

			teamGoals.update( readings );
			teamGoals.transmitNewGoal( rc );
			
			// If safe, build a soldier if possible

			Direction dir = findDirectionToBuid();
			if ( dir != Direction.NONE ){
				rc.build( dir , RobotType.SOLDIER );
				rc.setIndicatorString(0, "Building soldier.");
				return;
			}

			// If there are no parts nearby, go for neutral robots
			
			if ( readings.neutrals.size() > 0 ) {
				RobotInfo closest = findClosest( readings.neutrals );
				if ( rc.getLocation().isAdjacentTo( closest.location ) && rc.isCoreReady() ){
					rc.activate( closest.location );
					rc.setIndicatorString(0, "Activating neutral robot.");
					return;
				} else {
					Movement.simpleMove( rc.getLocation().directionTo( closest.location ) );
					lastGoal = NEUTRAL_ROBOTS;
					rc.setIndicatorString(0, "Going for neutral robots nearby.");
				}
			} else if ( readings.parts.size() > 0 ) {
				Movement.simpleMove( rc.getLocation().directionTo( findClosestLocation( readings.parts ) ) );
				rc.setIndicatorString(0, "Going for parts nearby.");
				return;
			} else if ( !teamGoals.neutralRobots.isEmpty() || !teamGoals.parts.isEmpty() ) {
				LinkedList<MapLocation> targetLocations = new LinkedList<MapLocation>();
				targetLocations = teamGoals.neutralRobots;
				targetLocations.addAll( teamGoals.parts );
				MapLocation closestLocation = findClosestLocation ( targetLocations );
				Movement.simpleMove( rc.getLocation().directionTo( closestLocation ) );
				rc.setIndicatorString(0, "Going to a known neutral robot or parts location.");
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
	
	public static MapLocation findClosestLocation( LinkedList<MapLocation> robots ) {
		if( robots.size() > 0 ){
			int closestIndex = 0;
			double smallestDistanceSquared = rc.getLocation().distanceSquaredTo( robots.get( closestIndex ) );

			double distanceSquared;

			for(int i=1; i<robots.size(); i++) {
				distanceSquared = rc.getLocation().distanceSquaredTo( robots.get(i) );
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
	
	public static MapLocation findClosestLocation( ArrayList<MapLocation> robots ) {
		if( robots.size() > 0 ){
			int closestIndex = 0;
			double smallestDistanceSquared = rc.getLocation().distanceSquaredTo( robots.get( closestIndex ) );

			double distanceSquared;

			for(int i=1; i<robots.size(); i++) {
				distanceSquared = rc.getLocation().distanceSquaredTo( robots.get(i) );
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
	
	public static void moveDefensively( RobotInfo enemy ) throws GameActionException {
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
			Movement.simpleMove( bestDirection );
		}
	}
	
	public static RobotInfo getClosestEnemy() {
		ArrayList<RobotInfo> candidates = new ArrayList<RobotInfo>();
		if ( readings.enemiesInRange.size() > 0 ){
			for ( int i=0; i<readings.enemiesInRange.size(); i++ ) {
				candidates.add( readings.enemiesInRange.get(i) );
			}
		} else {
			for ( int i=0; i<readings.enemies.size(); i++ ) {
				candidates.add( readings.enemies.get(i) );
			}
		}
		return findClosest( candidates );
	}
	
	public static RobotInfo findClosest( ArrayList<RobotInfo> robots ) {
		if( robots.size() > 0 ){
			int closestIndex = 0;
			double smallestDistanceSquared = rc.getLocation().distanceSquaredTo( robots.get( closestIndex ).location );

			double distanceSquared;

			for(int i=1; i<robots.size(); i++) {
				distanceSquared = rc.getLocation().distanceSquaredTo( robots.get(i).location );
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