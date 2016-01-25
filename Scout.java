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
			scout();
			return;
		}
		
		rc.setIndicatorString(0, "Doing nothing.");
				
	}

	// For scouting
	private  Direction scoutingDirection = randomDirection();
	private static final int DIRECTION_CHANGE_PERIOD = 30;
	Timer directionChangeTimer = new Timer( DIRECTION_CHANGE_PERIOD );

	private void scout() throws GameActionException {
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

}


