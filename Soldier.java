package artanis;

import java.util.*;
import battlecode.common.*;

public class Soldier {
	private static RobotController rc = RobotPlayer.rc;
	
	private static Readings readings = new Readings( rc.getType(), rc.getTeam() );
	private static Goals teamGoals = new Goals( rc.getType() );
	
	// For scouting
	private static int timeGoingOneWay = 0;
	private static Direction scoutingDirection = Direction.NONE;
	private static final int MAX_TIME_ONE_WAY = 5;
	
	// Soldier will flee if infected and has less life than 
	// INFECTED_THRESHOLD times its total life.
	private static final double INFECTED_THRESHOLD = 0.5;
	

	
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
		
		rc.setIndicatorString(0, "");
		rc.setIndicatorString(1, "");
		rc.setIndicatorString(2, "");
		
		readings.update( rc.getLocation() , rc.senseNearbyRobots(), rc.sensePartLocations(-1), rc.emptySignalQueue() );
		teamGoals.update( readings );
		teamGoals.transmitNewGoal( rc );
		
		if ( readings.enemies.size() > 0 ) {
			fight();
			rc.setIndicatorString(1, "SOLDIER AI: Fighting right now.");
			return;
		}
		
		if ( !teamGoals.archonsInDanger.isEmpty() ) {
			Movement.simpleMove( rc.getLocation().directionTo( teamGoals.archonsInDanger.element().getLocation() ) );
			rc.setIndicatorString(1, "SOLDIER AI: An archon is in peril!");
			return;
		} 

		if ( !teamGoals.soldiersInDanger.isEmpty() ) {
			Movement.simpleMove( rc.getLocation().directionTo( teamGoals.soldiersInDanger.element().getLocation() ) );
			rc.setIndicatorString(1, "SOLDIER AI: The soldier at (" +
					teamGoals.soldiersInDanger.element().getLocation().x + "," + 
					teamGoals.soldiersInDanger.element().getLocation().y +") is in peril!");
			return;
		} 
		
		readings.considerDensAsEnemies();
		if ( readings.dens.size() > 0 ){
			fight();
			rc.setIndicatorString(1, "SOLDIER AI: I am fighting a Zombie den. Enemies around me, including dens: " + readings.enemies.size() );
			return;
		} else if ( !teamGoals.zombieDens.isEmpty() ) {
			Movement.simpleMove( rc.getLocation().directionTo( teamGoals.zombieDens.element() ) );
			rc.setIndicatorString(1, "SOLDIER AI: I am moving towards the Zombie Den at (" +
					teamGoals.zombieDens.element().x +
					"," + teamGoals.zombieDens.element().y + ").");
			return;
		}
		
		if ( rc.isCoreReady() ) {
			if ( timeGoingOneWay < MAX_TIME_ONE_WAY && rc.onTheMap( rc.getLocation().add( scoutingDirection ) )) {
				Movement.simpleMove( scoutingDirection );
				timeGoingOneWay++;
				rc.setIndicatorString(1, "SOLDIER AI: I am happily scouting.");
				return;
			} else {
				scoutingDirection = Movement.randomDirection();
				Movement.simpleMove( scoutingDirection );
				timeGoingOneWay = 0;
				rc.setIndicatorString(1, "SOLDIER AI: Just changed directions scouting.");
				return;
			}
		} 

		rc.setIndicatorString(1, "SOLDIER AI: Nothing to do..." );
		return;

	}

	//////////////////////////////////////////////////////////////////////
	/////////////// Begin attackMove() and fight() methods ///////////////
	//////////////////////////////////////////////////////////////////////

//	private static void attackMove( Direction dir ) throws GameActionException { 
//		if ( readings.enemies.size() > 0 ) {
//			fight();
//		} else {
//			Movement.simpleMove( dir );
//		}
//	}

	private static void fight() throws GameActionException {

		if ( readings.enemies.size() > 0 ) {
			
			// If there are readings.enemies nearby and the soldier is infected and with
			// less than 50% life, simpleMove in the direction opposite to that
			// pointing to the nearest enemy.
			
			if ( rc.getInfectedTurns() > 0 && rc.getHealth() < INFECTED_THRESHOLD * rc.getType().maxHealth ) {
				if ( rc.isCoreReady() ){
					moveDefensively( getClosestEnemy() );
				}
				rc.setIndicatorString(2, "FIGHTING AI: Fleeing because of infection.");
			} else {
				
				// If there are readings.enemies in range and the weapon is ready, shoot the weakest one in range.
				// Try to move otherwise.
				
				if ( rc.isWeaponReady() && readings.enemiesInRange.size() > 0 ) {
					rc.attackLocation( getWeakestEnemy().location );
					rc.setIndicatorString(2, "FIGHTING AI: Attacking nearest enemy.");
				} else if (rc.isCoreReady() ){
					
					// If there are at least as many readings.allies as readings.enemies nearby, move offensively,
					// to a 'sweet spot' that is a good place to attack the weakest enemy nearby.
					// Move defensively otherwise, in a direction that maximizes the distance to
					// the closest enemy.
					
					if ( readings.allies.size() >= readings.enemies.size()-1 ){
						moveOffensively( getClosestEnemy() );
						rc.setIndicatorString(2, "FIGHTING AI: Cannot attack. Moving to sweetspot.");
					} else {
						moveDefensively( getClosestEnemy() );
						teamGoals.callForHelp( rc );
						rc.setIndicatorString(2, "FIGHTING AI: Cannot attack. Fleeing from enemy.");
					}	
				} else {
					rc.setIndicatorString(2, "FIGHTING AI: I didn't do anything this turn");
				}
			}
		}
	}

	//////////////////////////////////////////////////////////////////////
	//////////// Methods for offensive and defensive movement ////////////
	//////////////////////////////////////////////////////////////////////
	
	// Added break command in the Direction loops of both moveOffensively and
	// moveDefensively to improve bytecode performance
	
	private static void moveOffensively( RobotInfo enemy ) throws GameActionException {
		int myAttackRadiusSquared = rc.getType().attackRadiusSquared;
		MapLocation myLocation = rc.getLocation();
		
		if ( rc.canAttackLocation( enemy.location ) ){
			Direction bestDirection = null;
			int smallestError = Math.abs( myAttackRadiusSquared - myLocation.distanceSquaredTo( enemy.location ) );
			MapLocation adjacent;
			int adjacentError;
			boolean someDirectionIsAvailable = false;
			
			for ( int i=0; i< 8; i++ ) {
				adjacent = myLocation.add( Direction.values()[i] );
				adjacentError = Math.abs( myAttackRadiusSquared - adjacent.distanceSquaredTo( enemy.location ) );
				if ( rc.canMove( Direction.values()[i] ) ) {
					someDirectionIsAvailable = true;
					if( adjacentError < smallestError ) {
						bestDirection = Direction.values()[i];
						smallestError = adjacentError;
						break;
					}
				}
			}
			
			if ( bestDirection == null && someDirectionIsAvailable) {
				return;
			} else {
				Movement.simpleMove( bestDirection );
			}
		} else {
			Movement.simpleMove( myLocation.directionTo( enemy.location ) );
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
			Movement.simpleMove( bestDirection );
		}
	}
	
	/////////////////////////////////////////////////////////////////////
	////// Methods for finding the weakest and the closest enemies //////
	/////////////////////////////////////////////////////////////////////
	
	private static RobotInfo getWeakestEnemy() {
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
		return findWeakest( candidates );
	}

	private static RobotInfo getClosestEnemy() {
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