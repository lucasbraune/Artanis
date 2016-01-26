package artanis;

import java.util.*;
import battlecode.common.*;

public class Soldier extends BasicRobot {

	Soldier( RobotController rcIn ) {
		super(rcIn);
	}

	// Soldier will flee if infected and has less than 
	// a certain portion of its total life.
	private  final double INFECTED_THRESHOLD = 0.7;

	// Give up killing den
	private int turnsSinceADenWasSeen = 1000;
	private static final int DEN_MEMORY = 15;
	
	// Used to wait before asking the location of a den again
	private static final int DEN_QUESTION_PERIOD = 50;
	Timer denQuestionTimer = new Timer( DEN_QUESTION_PERIOD );
	
	void repeat() throws GameActionException {
		
		rc.setIndicatorString(0, ".");
		rc.setIndicatorString(1, ".");
		rc.setIndicatorString(2, ".");
		
		// Update readings
		readings.update( rc.getLocation() , rc.senseNearbyRobots(), rc.sensePartLocations(-1), rc.emptySignalQueue());
		teamGoals.update( readings );
		
		// Fight enemies
		if ( readings.enemies.size() > 0 ) {
			rc.setIndicatorString(0, "SOLDIER AI: Fighting." );
			fight();
			return;
		}
		
		// Update goals and send two signals if near a zombie den or a resource
		teamGoals.replyWithDensAndResources( rc, readings );
		
		// Make way for an archon
		if ( teamGoals.stayClear != null && rc.isCoreReady() ) {
			simpleMove( rc.getLocation().directionTo( teamGoals.stayClear ).opposite() );
			rc.setIndicatorString(0, "SOLDIER AI: Making way for an archon.");
			return;
		}
		
		// Fight nearby dens
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
		
		// Move to target Location
		
		if( teamGoals.targetLocation != null && rc.isCoreReady() ) {
			simpleMove( rc.getLocation().directionTo( teamGoals.targetLocation ) );
			rc.setIndicatorString(0, "SOLDIER AI: Going to target location." );
			return;
		} 
		
		
		
		// Go to soldiers in danger
		if ( !teamGoals.soldiersInDanger.isEmpty() && rc.isCoreReady() ) {
			simpleMove( rc.getLocation().directionTo( teamGoals.soldiersInDanger.element().getLocation() ) );
			rc.setIndicatorString(0, "SOLDIER AI: Going to help a soldier." );
			return;
		} 
		
		// Go to a known zombie den location. If there are none, ask for one.
		if ( teamGoals.zombieDens.isEmpty() ) {
			if ( !denQuestionTimer.isWaiting() ) {
				teamGoals.askForDenLocation( rc );
				denQuestionTimer.reset();
				rc.setIndicatorString(1, "SOLDIER AI: Just asked for a den location." );
			}	
		} else if ( rc.isCoreReady() ) {
			simpleMove( rc.getLocation().directionTo( teamGoals.zombieDens.element() ) );
			rc.setIndicatorString(0, "SOLDIER AI: Moving to a den." );
			return;
		}
		
		// Scout
		if ( rc.isCoreReady() ) {
			rc.setIndicatorString(0, "SOLDIER AI: Scouting." );
			scout();
			return;
		} 

		rc.setIndicatorString(0, "SOLDIER AI: Nothing to do." );
		return;

	}

	// For scouting
	private  Direction scoutingDirection = randomDirection();
	private static final int DIRECTION_CHANGE_PERIOD = 4;
	Timer directionChangeTimer = new Timer( DIRECTION_CHANGE_PERIOD );
	
	private void scout() throws GameActionException {
		
		if ( directionChangeTimer.isWaiting() ) {
			if ( rc.onTheMap( rc.getLocation().add( scoutingDirection ) ) ) {
				simpleMove( scoutingDirection );
				directionChangeTimer.reset();
			} else {
				scoutingDirection = randomDirection();
				simpleMove( scoutingDirection );
				directionChangeTimer.reset();
				rc.setIndicatorString(0, "SOLDIER AI: Scouting. Had to change direction because of the edge of the map." );
			}
		} else {
			scoutingDirection = randomDirection();
			simpleMove( scoutingDirection );
			directionChangeTimer.reset();
			rc.setIndicatorString(0, "SOLDIER AI: Scouting. It's time to change direction." );
		}
	}
		

	//////////////////////////////////////////////////////////////////////
	/////////////// Begin attackMove() and fight() methods ///////////////
	//////////////////////////////////////////////////////////////////////
	

	private static final int WHAT_COUNTS_AS_ADVANTAGE = 0;
	
	private void fight() throws GameActionException {

		rc.setIndicatorString(1, "Starting fight algorithm.");
		
		if ( readings.enemies.size() > 0 ) {
			
			// If there are readings.enemies nearby and the soldier is infected and with
			// less than 50% life, simpleMove in the direction opposite to that
			// pointing to the nearest enemy.
			
			// Also run up to three times if avoiding enemies. 
			
			if ( rc.getInfectedTurns() > 0 && rc.getHealth() < INFECTED_THRESHOLD * rc.getType().maxHealth ) {
				for( RobotInfo enemy : readings.enemies ) {
					if ( enemy.type != RobotType.SCOUT && enemy.type != RobotType.ARCHON && enemy.type != RobotType.ZOMBIEDEN ) {
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
				chooseAndAttackAnEnemy();
				rc.setIndicatorString(1, "Weapon ready and can see enemies. I'm shooting them.");
			} else if (rc.isCoreReady() ){

				// If there are at least as many allies as enemies nearby, move offensively,
				// to a 'sweet spot' that is a good place to attack the weakest enemy nearby.
				// Move defensively otherwise, in a direction that maximizes the distance to
				// the closest enemy.
				
				int necessaryAdvantage = 0;
				if ( readings.opponents.size() > 0 && teamGoals.avoidingOpponent )
					necessaryAdvantage = WHAT_COUNTS_AS_ADVANTAGE;

				if ( readings.allies.size() >= readings.enemies.size()-1 + necessaryAdvantage ){
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
	
	void chooseAndAttackAnEnemy() throws GameActionException {
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
		rc.attackLocation( findWeakestRobot( candidatesArray ).location );
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