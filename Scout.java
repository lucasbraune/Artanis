package artanis;

import artanis.Timer;
import battlecode.common.*;
import static artanis.TeamGoals.*;

public class Scout extends BasicRobot {

	Scout(RobotController rcIn) {
		super(rcIn);
	}

	// Used to wait before asking the location of a den again
	private static final int GROUP_UPDATE_PERIOD = 50;
	Timer groupUpdateTimer = new Timer( GROUP_UPDATE_PERIOD );
	
	void repeat() throws GameActionException {
		
		rc.setIndicatorString(0, ".");
		rc.setIndicatorString(1, ".");
		rc.setIndicatorString(2, ".");
		
		readings.update( rc.getLocation(), rc.senseNearbyRobots(), rc.sensePartLocations(-1), rc.emptySignalQueue() );
		teamGoals.update( readings );
		
		if ( readings.enemies.size() == 0 ) {
			if( !groupUpdateTimer.isWaiting() ) {
				if ( !teamGoals.zombieDens.isEmpty() ) {
					broadcastLocation( rc, teamGoals.zombieDens.getLast(), HERE_IS_A_DEN, MEDIUM_RADIUS );
				}
				if ( !teamGoals.parts.isEmpty() ) {
					broadcastLocation( rc, teamGoals.parts.getLast(), HERE_ARE_PARTS, MEDIUM_RADIUS );
				}
				if ( !teamGoals.neutralRobots.isEmpty() ) { 
					broadcastLocation( rc, teamGoals.neutralRobots.getLast(), HERE_IS_A_NEUTRAL, MEDIUM_RADIUS );
				}
				groupUpdateTimer.reset();
				rc.setIndicatorString(1, "Updating group on dens, neutrals and parts.");
			} else {
				teamGoals.replyWithDensAndResources(rc, readings);
				rc.setIndicatorString(1, "Replying a location request.");
			}
		}
		
		if( rc.isCoreReady() ) {
			rc.setIndicatorString(0, "SCOUT AI: Scouting.");
			spiralScout();
			return;
		}
		
		rc.setIndicatorString(0, "Doing nothing.");
				
	}
	
	/////////////////////////////////////////////////////////////////////
	/////////////////////// Begin random scouting ///////////////////////
	/////////////////////////////////////////////////////////////////////

	// For scouting
	private Direction scoutingDirection = randomDirection();
	private static final int DIRECTION_CHANGE_PERIOD = 30;
	Timer directionChangeTimer = new Timer( DIRECTION_CHANGE_PERIOD );

	private void randomScout() throws GameActionException {
		RobotInfo[] enemyArray = readings.enemies.toArray( new RobotInfo[ readings.enemies.size() ] );
		if ( directionChangeTimer.isWaiting() ) {
			if ( rc.onTheMap( rc.getLocation().add( scoutingDirection ) ) ) {
				moveAvoidingEnemies( scoutingDirection, enemyArray );
			} else {
				scoutingDirection = randomDirection();
				moveAvoidingEnemies( scoutingDirection, enemyArray );
				directionChangeTimer.reset();
				rc.setIndicatorString(0, "SCOUT AI: Scouting. Had to change direction because of the edge of the map." );
			}
		} else {
			scoutingDirection = randomDirection();
			moveAvoidingEnemies( scoutingDirection, enemyArray );
			directionChangeTimer.reset();
			rc.setIndicatorString(0, "SCOUT AI: Scouting. It's time to change direction." );
		}
	}

	/////////////////////////////////////////////////////////////////////
	/////////////////////// Begin spiral scouting ///////////////////////
	/////////////////////////////////////////////////////////////////////
	
	private static final int RIGHT = 1;
	private static final int UP = 2;
	private static final int LEFT = 3;
	private static final int DOWN = 4;
	
	private static int timesShouldRepeatDirection = 1;
	private static int timesRepeatedDirection = 0;
	private static int spiralDirection = RIGHT;
	private static final int SPIRAL_STEPS = 7;
	
	private static MapLocation nextScoutingTarget ( MapLocation currentTarget ) {
		
		// This part of the code returns a map location that is SPIRAL_STEPS to the
		// spiralDirection of currentTarget.
		MapLocation nextTarget = null;
		
		switch( spiralDirection ) {
		case RIGHT:
			nextTarget = currentTarget.add( Direction.EAST, SPIRAL_STEPS );
			break;
		case UP:
			nextTarget = currentTarget.add( Direction.NORTH, SPIRAL_STEPS );
			break;
		case LEFT:
			nextTarget = currentTarget.add( Direction.WEST, SPIRAL_STEPS );
			break;
		case DOWN:
			nextTarget = currentTarget.add( Direction.SOUTH, SPIRAL_STEPS );
			break;
		}
		
		// This part of the code changes spiralDirection following the pattern:
		// R U LL DD RRR UUU LLLL DDDD RRRRR UUUUU ...
		timesRepeatedDirection++;
		if ( timesRepeatedDirection >= timesShouldRepeatDirection ) {
			timesRepeatedDirection = 0;
			switch( spiralDirection ) {
			case RIGHT:
				spiralDirection = UP;
				break;
			case UP:
				spiralDirection = LEFT;
				timesShouldRepeatDirection++;
				break;
			case LEFT:
				spiralDirection = DOWN;
				break;
			case DOWN:
				spiralDirection = RIGHT;
				timesShouldRepeatDirection++;
				break;
			}
		}
		
		return nextTarget;
	}
	
	private MapLocation startingLocation;
	private MapLocation scoutingTarget;
	// private Direction scoutingDirection;
	
	// Distance, as usual, means distance squared.
	private static final int SHORT_DISTANCE_FROM_TARGET = 20; 
	
	void runsOnce() {
		startingLocation = rc.getLocation();
		scoutingTarget = startingLocation;
	}
	
	private void spiralScout() throws GameActionException {
		RobotInfo[] enemyArray = readings.enemies.toArray( new RobotInfo[ readings.enemies.size() ] );
		scoutingDirection = rc.getLocation().directionTo( scoutingTarget );
		
		if ( rc.getLocation().distanceSquaredTo( scoutingTarget ) <= SHORT_DISTANCE_FROM_TARGET  ||
			!rc.onTheMap( rc.getLocation().add( scoutingDirection ) ) ) {
			scoutingTarget = nextScoutingTarget( scoutingTarget );
			rc.setIndicatorString(0, "SCOUT AI: Scouting. Changed direction." );
		}
		
		moveAvoidingEnemies( scoutingDirection, enemyArray );
		rc.setIndicatorString(0, "SCOUT AI: Scouting." );
	}
	
	////////////////////////////////////////////////////////////////////
	/////////////////////// Begin the next thing ///////////////////////
	////////////////////////////////////////////////////////////////////
}


