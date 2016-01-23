package artanis;

import java.util.*;
import battlecode.common.*;

public class Soldier extends BasicRobot {

	Soldier(RobotController rcIn) {
		super(rcIn);
	}

	// Soldier will flee if infected and has less than 
	// INFECTED_THRESHOLD of its total life.
	private  final double INFECTED_THRESHOLD = 0.7;

	// For scouting
	private  int timeGoingOneWay = 0;
	private  Direction scoutingDirection = Direction.NONE;
	private  final int MAX_TIME_ONE_WAY = 5;
	
	// Give up killing den
	private int turnsSinceADenWasSeen = 1000;
	private static final int DEN_MEMORY = 15;
	
	void repeat() throws GameActionException {
		
		rc.setIndicatorString(0, "");
		rc.setIndicatorString(1, "");
		
		readings.update( rc.getLocation() , rc.senseNearbyRobots(), rc.sensePartLocations(-1), rc.emptySignalQueue() );
		
		if ( readings.enemies.size() > 0 ) {
			rc.setIndicatorString(0, "SOLDIER AI: Fighting." );
			fight();
			return;
		}
		
		teamGoals.update( readings );
		teamGoals.transmitNewGoal( rc );
		
		if ( teamGoals.stayClear != null && rc.isCoreReady() ) {
			simpleMove( rc.getLocation().directionTo( teamGoals.stayClear ).opposite() );
			rc.setIndicatorString(0, "SOLDIER AI: Making way for an archon.");
			return;
		}
		
		if ( readings.dens.size() > 0 ){
			turnsSinceADenWasSeen = 0;
			readings.considerDensAsEnemies();
			fight();
			rc.setIndicatorString(0, "SOLDIER AI: Fighting dens." );
			return;
		} else {
			turnsSinceADenWasSeen++;
		}
		
		if ( turnsSinceADenWasSeen <= DEN_MEMORY ) {
			if ( !teamGoals.zombieDens.isEmpty() && rc.isCoreReady() ) {
				simpleMove( rc.getLocation().directionTo( teamGoals.zombieDens.element() ) );
				rc.setIndicatorString(0, "SOLDIER AI: Moving to a den." );
				return;
			}
		}
		
		if ( !teamGoals.archonsInDanger.isEmpty() && rc.isCoreReady() ) {
			simpleMove( rc.getLocation().directionTo( teamGoals.archonsInDanger.element().getLocation() ) );
			rc.setIndicatorString(0, "SOLDIER AI: Going to help an archon." );
			return;
		} 
		
		if ( !teamGoals.soldiersInDanger.isEmpty() && rc.isCoreReady() ) {
			simpleMove( rc.getLocation().directionTo( teamGoals.soldiersInDanger.element().getLocation() ) );
			rc.setIndicatorString(0, "SOLDIER AI: Going to help a soldier." );
			return;
		} 
		
		if ( !teamGoals.zombieDens.isEmpty() && rc.isCoreReady() ) {
			simpleMove( rc.getLocation().directionTo( teamGoals.zombieDens.element() ) );
			rc.setIndicatorString(0, "SOLDIER AI: Moving to a den." );
			return;
		}
		
		if ( rc.isCoreReady() ) {
			if ( timeGoingOneWay < MAX_TIME_ONE_WAY && rc.onTheMap( rc.getLocation().add( scoutingDirection ) )) {
				simpleMove( scoutingDirection );
				timeGoingOneWay++;
				rc.setIndicatorString(0, "SOLDIER AI: Scouting" );
				return;
			} else {
				scoutingDirection = randomDirection();
				simpleMove( scoutingDirection );
				timeGoingOneWay = 0;
				rc.setIndicatorString(0, "SOLDIER AI: Changing direction" );
				return;
			}
		} 

		rc.setIndicatorString(0, "SOLDIER AI: Nothing to do..." );
		return;

	}

	//////////////////////////////////////////////////////////////////////
	/////////////// Begin attackMove() and fight() methods ///////////////
	//////////////////////////////////////////////////////////////////////
	

	private void fight() throws GameActionException {

		rc.setIndicatorString(1, "Starting fight algorithm.");
		
		if ( readings.enemies.size() > 0 ) {
			
			// If there are readings.enemies nearby and the soldier is infected and with
			// less than 50% life, simpleMove in the direction opposite to that
			// pointing to the nearest enemy.
			
			if ( rc.getInfectedTurns() > 0 && rc.getHealth() < INFECTED_THRESHOLD * rc.getType().maxHealth ) {
				for( RobotInfo enemy : readings.enemies ) {
					if ( enemy.type != RobotType.SCOUT && enemy.type != RobotType.ZOMBIEDEN ) {
						if ( rc.isCoreReady() ){
							moveDefensively( getClosestEnemy() );
							rc.setIndicatorString(1, "Infected. Running away.");
							return;
						}
					}
				}
			}

			rc.setIndicatorString(1, "Not infected.");

			// If there are readings.enemies in range and the weapon is ready, shoot the weakest one in range.
			// Try to move otherwise.

			if ( rc.isWeaponReady() && readings.enemiesInRange.size() > 0 ) {
				rc.attackLocation( getWeakestEnemy().location );
				rc.setIndicatorString(1, "Weapon ready and can see enemies. I'm shooting them.");
			} else if (rc.isCoreReady() ){

				// If there are at least as many allies as enemies nearby, move offensively,
				// to a 'sweet spot' that is a good place to attack the weakest enemy nearby.
				// Move defensively otherwise, in a direction that maximizes the distance to
				// the closest enemy.

				if ( readings.allies.size() >= readings.enemies.size()-1 ){
					moveOffensively( getClosestEnemy() );
					rc.setIndicatorString(1, "Moving offensively.");
				} else {
					moveDefensively( getClosestEnemy() );
					rc.setIndicatorString(1, "Moving defensively.");
					teamGoals.callForHelp( rc, readings.allies.size() );
				}
			}

		} else {
			rc.setIndicatorString(1, "No enemies to fight.");
		}
	}

	//////////////////////////////////////////////////////////////////////
	//////////// Methods for offensive and defensive movement ////////////
	//////////////////////////////////////////////////////////////////////

	// Added break command in the Direction loops of both moveOffensively and
	// moveDefensively to improve byte code performance
	
	private void moveOffensively( RobotInfo enemy ) throws GameActionException {
		int myAttackRadiusSquared = rc.getType().attackRadiusSquared;
		MapLocation myLocation = rc.getLocation();
		
		if ( rc.canAttackLocation( enemy.location ) ){
			Direction bestDirection = null;
			int adjacentError, smallestError = Math.abs( myAttackRadiusSquared - myLocation.distanceSquaredTo( enemy.location ) );
			MapLocation adjacent;
			boolean someDirectionIsAvailable = false;
			
			for ( int i=0; i< 8; i++ ) {
				adjacent = myLocation.add( Direction.values()[i] );
				adjacentError = Math.abs( myAttackRadiusSquared - adjacent.distanceSquaredTo( enemy.location ) );
				if ( rc.canMove( Direction.values()[i] ) ) {
					someDirectionIsAvailable = true;
					if( adjacentError < smallestError ) {
						bestDirection = Direction.values()[i];
						smallestError = adjacentError;
					}
				}
			}
			
			if ( bestDirection == null && someDirectionIsAvailable) {
				return;
			} else {
				simpleMove( bestDirection );
			}
		} else {
			simpleMove( myLocation.directionTo( enemy.location ) );
		}
		
	}
	
	/////////////////////////////////////////////////////////////////////
	////// Methods for finding the weakest and the closest enemies //////
	/////////////////////////////////////////////////////////////////////
	
	private  RobotInfo getWeakestEnemy() {
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
		
		RobotInfo[] candidatesArray = candidates.toArray( new RobotInfo[ candidates.size() ] );
		if ( candidatesArray!=null && candidatesArray.length > 0 ) {
			rc.setIndicatorString( 1 , "There are candidate weakest enemies.");
		} else {
			rc.setIndicatorString( 1 , "No candidate weakest enemy.");
		}
		return findWeakestRobot( candidatesArray );
	}

	public  RobotInfo getClosestEnemy() {
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
		RobotInfo[] candidatesArray = candidates.toArray( new RobotInfo[ candidates.size() ] );
		if ( candidatesArray!=null && candidatesArray.length > 0 ) {
			rc.setIndicatorString( 1 , "There are candidate closest enemies.");
		} else {
			rc.setIndicatorString( 1 , "No candidate closest enemies.");
		}
		return findClosestRobot( candidatesArray );
	}
	
}