package artanis;

import java.util.*;
import battlecode.common.*;

public class Archon extends BasicRobot {
	
	Archon(RobotController rcIn) {
		super(rcIn);
	}
	
	private int lastGoal = 0;
	private static final int NEUTRAL_ROBOTS = 1;
	private static final int PARTS = 2;
	
	void repeat() throws GameActionException {

		readings.update( rc.getLocation() , rc.senseNearbyRobots(), rc.sensePartLocations(-1), rc.emptySignalQueue() );
		teamGoals.update( readings );
		teamGoals.transmitNewGoal( rc );
		
		if ( rc.isCoreReady() ){
			
			// Run if need be
			
			if ( readings.enemies.size() > 0 ) {
				moveDefensively( findClosestRobot( (RobotInfo[]) readings.enemies.toArray() ) );
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
				RobotInfo closest = findClosestRobot( (RobotInfo[]) readings.neutrals.toArray() );
				if ( rc.getLocation().isAdjacentTo( closest.location ) && rc.isCoreReady() ){
					rc.activate( closest.location );
					rc.setIndicatorString(0, "Activating neutral robot.");
					return;
				} else {
					simpleMove( rc.getLocation().directionTo( closest.location ) );
					lastGoal = NEUTRAL_ROBOTS;
					rc.setIndicatorString(0, "Going for neutral robots nearby.");
				}
			} else if ( readings.parts.size() > 0 ) {
				simpleMove( rc.getLocation().directionTo( findClosestLocation( (MapLocation[]) readings.parts.toArray() ) ) );
				rc.setIndicatorString(0, "Going for parts nearby.");
				return;
			} else if ( !teamGoals.neutralRobots.isEmpty() || !teamGoals.parts.isEmpty() ) {
				LinkedList<MapLocation> targetLocations = new LinkedList<MapLocation>();
				targetLocations = teamGoals.neutralRobots;
				targetLocations.addAll( teamGoals.parts );
				MapLocation closestLocation = findClosestLocation ( (MapLocation[]) targetLocations.toArray() );
				simpleMove( rc.getLocation().directionTo( closestLocation ) );
				rc.setIndicatorString(0, "Going to a known neutral robot or parts location.");
				return;
			}


			rc.setIndicatorString(0, "Nothing to do.");
			
		}
		
	}

	private Direction findDirectionToBuid() {
		Direction dir = Direction.NORTH;
		int i;
		for( i=1; i<=8; i++ ) {
			dir.rotateLeft();
			if ( rc.canBuild( dir, RobotType.SOLDIER ) ){
				break;
			}
		}
		if ( i<= 8 ) 
			return dir;
		else
			return Direction.NONE;
	}

	
	
}