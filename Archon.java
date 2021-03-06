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

	// Used to wait before asking the location of resources again
	private static final int RESOURCE_QUESTION_PERIOD = 30;
	Timer resourceQuestionTimer = new Timer( RESOURCE_QUESTION_PERIOD );
	
	// Used to wait before building a scout again
	private static final int SCOUT_BUILDING_PERIOD = 350;
	Timer scoutBuildingTimer = new Timer( SCOUT_BUILDING_PERIOD );
	
	// Used to wait before building a viper again
	// The timer is being updated only when the archon tries to
	// build a unit, as opposed to the scout building timer 
	// which updates every turn. Therefore the much smaller
	// period.
	
	private static final int VIPER_BUILDING_PERIOD = 18;
	Timer viperBuildingTimer = new Timer( VIPER_BUILDING_PERIOD );
	
	RobotType typeToBeBuilt = RobotType.SCOUT;
	
	// Ugly hack
	boolean shouldBuildFirstViper = true; 
	
	void repeat() throws GameActionException {

		rc.setIndicatorString(0, ".");
		rc.setIndicatorString(1, ".");
		rc.setIndicatorString(2, ".");
		
		readings.update( rc.getLocation() , rc.senseNearbyRobots(), rc.sensePartLocations(-1), rc.emptySignalQueue() );
		teamGoals.update( readings );
		
		if( !scoutBuildingTimer.isWaiting() && !teamGoals.weHaveScouts ) {
			typeToBeBuilt = RobotType.SCOUT;
			rc.setIndicatorString(1, "The next robot built should be a scout.");
			scoutBuildingTimer.reset();
		}
		
		if ( rc.isCoreReady() ){
			
			// Run if need be
			
			if ( readings.enemies.size() > 0 ) {
				
				RobotInfo[] enemyArray = readings.enemies.toArray( new RobotInfo[ readings.enemies.size() ] );
				moveDefensively( findClosestRobot( enemyArray  ) );
				teamGoals.callForHelp( rc, readings.allies.size() );
				
				if( lastGoal == PARTS && teamGoals.parts.size()>0 ) {
					teamGoals.parts.remove();
				} else if( lastGoal == NEUTRAL_ROBOTS && teamGoals.neutralRobots.size()>0 ) {
					teamGoals.neutralRobots.remove();
				}
				
				rc.setIndicatorString(0, "Fleeing from enemies.");
				return;
			}
			
			// Update team goals and reply to any requests
			teamGoals.replyWithDensAndResources( rc, readings );
			rc.setIndicatorString(0, "Sending messages.");
			if( !rc.isCoreReady() ) {
				return;
			}
			
			// If safe, build a robot if possible. 

			Direction dir = findDirectionToBuid();
			if ( dir != Direction.NONE && rc.isCoreReady() ) {
				rc.build( dir, typeToBeBuilt );
				if ( !viperBuildingTimer.isWaiting() || shouldBuildFirstViper ) {
					typeToBeBuilt = RobotType.VIPER;
					viperBuildingTimer.reset();
					shouldBuildFirstViper = false;
				} else {
					typeToBeBuilt = RobotType.SOLDIER;
				}
				rc.setIndicatorString(0, "Building robot.");
				return;
			}

			if ( readings.neutrals.size() >0 ) {
				// Go for neutral robots nearby
				RobotInfo[] neutralsArray = readings.neutrals.toArray( new RobotInfo[ readings.neutrals.size() ] );
				RobotInfo closest = findClosestRobot( neutralsArray );

				if ( rc.getLocation().isAdjacentTo( closest.location ) && rc.isCoreReady() ){
					rc.activate( closest.location );
					rc.setIndicatorString(0, "Activating neutral robot.");
					return;
				} else {
					simpleMove( rc.getLocation().directionTo( closest.location ) );
					lastGoal = NEUTRAL_ROBOTS;
					rc.setIndicatorString(0, "Going for neutral robots nearby.");
					return;
				}
			}

			if ( readings.parts.size() > 0 ) {
				MapLocation[] partsArray = readings.parts.toArray( new MapLocation[ readings.parts.size() ] );

				simpleMove( rc.getLocation().directionTo( findClosestLocation( partsArray ) ) );
				rc.setIndicatorString(0, "Going for parts nearby.");
				return;
			}
			
			LinkedList<MapLocation> targetLocations = new LinkedList<MapLocation>();
			targetLocations = teamGoals.neutralRobots;
			targetLocations.addAll( teamGoals.parts );
			
			if ( !targetLocations.isEmpty() ) {
				// Go to the nearest resource location known
				MapLocation[] targetLocationsArray = targetLocations.toArray( new MapLocation[ targetLocations.size() ] ); 

				MapLocation closestLocation = findClosestLocation ( targetLocationsArray );
				simpleMove( rc.getLocation().directionTo( closestLocation ) );
				rc.setIndicatorString(0, "Going to a known neutral robot or parts location.");
				return;

			} else if ( !resourceQuestionTimer.isWaiting() ) {
				// Should no resource locaiton be known, ask other robots every once in a while.
				teamGoals.askForResourceLocations(rc);
				resourceQuestionTimer.reset();
				rc.setIndicatorString(1, "Asking for resource location." );
			}

			rc.setIndicatorString(0, "Nothing to do.");

		}

	}

	private Direction findDirectionToBuid() {
		Direction dir = randomDirection();
		int i;
		for( i=1; i<=8; i++ ) {
			dir.rotateLeft();
			if ( rc.canBuild( dir, typeToBeBuilt ) ){
				break;
			}
		}
		if ( i<= 8 ) 
			return dir;
		else
			return Direction.NONE;
	}

	
	
}