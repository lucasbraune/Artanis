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
		
		if( readings.opponents.size() > 0 ) {
			lastSeenOpponent = readings.opponents.get(0).location;
		}
		
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
				rc.broadcastMessageSignal(TeamGoals.SCOUT_ALIVE, 0, MEDIUM_RADIUS);
				groupUpdateTimer.reset();
				rc.setIndicatorString(1, "Updating group on dens, neutrals and parts, plus the fact I am alive.");
			} else {
				teamGoals.replyWithDensAndResources(rc, readings);
				rc.setIndicatorString(1, "Replying a den or resource location request.");
			}
		}
		
		if ( teamGoals.targetLocation == null ) {
			chooseTarget();
			if ( myTarget != null ) {
				if ( rc.canSense( myTarget ) ) {
					if( readings.dens.size() > 0 ) {
						if( readings.opponents.size() <= 2 ) {
							if( rc.getRoundNum() % 3 == 0 ) {
								TeamGoals.broadcastLocation(rc, readings.dens.get(0).location, TARGET_LOCATION, MEDIUM_RADIUS);
							}
							rc.setIndicatorString(0, "Broadcasting target location.");
							return;
						} else {
							RobotInfo[] opponentArray = readings.opponents.toArray( new RobotInfo[ readings.opponents.size() ] );
							TeamGoals.broadcastLocation(rc, findClosestRobot( opponentArray ).location, TARGET_LOCATION, MEDIUM_RADIUS);
							rc.setIndicatorString(0, "Broadcasting target location.");
							return;
						}
					} else if ( rc.getRoundNum() <= DEFENSIVE_PERIOD ) {
						if( rc.getLocation().distanceSquaredTo( myTarget ) > RobotType.SOLDIER.attackRadiusSquared ) {
							if( rc.isCoreReady() ) {
								RobotInfo[] enemyArray = readings.enemies.toArray( new RobotInfo[ readings.enemies.size() ] );
								moveAvoidingEnemies( rc.getLocation().directionTo( myTarget ), enemyArray );
								rc.setIndicatorString(0, "Approaching my target.");
								return;
							}
						} else {
							myTarget = null;
						}
					} else if ( readings.opponents.size() > 0 ) {
						RobotInfo[] opponentArray = readings.opponents.toArray( new RobotInfo[ readings.opponents.size() ] );
						TeamGoals.broadcastLocation(rc, findClosestRobot( opponentArray ).location, TARGET_LOCATION, MEDIUM_RADIUS);
						return;
					}
				} else {
					if( rc.isCoreReady() ) {
						RobotInfo[] enemyArray = readings.enemies.toArray( new RobotInfo[ readings.enemies.size() ] );
						moveAvoidingEnemies( rc.getLocation().directionTo( myTarget ), enemyArray );
						rc.setIndicatorString(0, "Approaching my target.");
						return;
					}
				}
			}
		}
		
		if( rc.isCoreReady() ) {
			rc.setIndicatorString(0, "SCOUT AI: Scouting.");
			randomScout();
			return;
		}
		
		rc.setIndicatorString(0, "Doing nothing.");
				
	}

	/////////////////////////////////////////////////////////////////
	/////////////////////// Begin team target ///////////////////////
	/////////////////////////////////////////////////////////////////
	
	private MapLocation myTarget = null;
	MapLocation lastSeenOpponent = null;
	
	// The defensive period is the number of turns during which
	// the scout will not pick the opponent's robots as a target.
	private static final int DEFENSIVE_PERIOD = 700;
	Timer defensiveTimer = new Timer( DEFENSIVE_PERIOD ); 
	
	private void chooseTarget() {
		if( myTarget == null ) {
			MapLocation closest = null;
			if( teamGoals.zombieDens.size()>0 ) {
				closest = teamGoals.zombieDens.get(0);
				int distanceSq = closest.distanceSquaredTo( startingLocation );
				for( MapLocation den : teamGoals.zombieDens ) {
					if( den.distanceSquaredTo( startingLocation ) < distanceSq ) {
						closest = den;
						distanceSq = den.distanceSquaredTo( startingLocation );
					}
				}
				myTarget = closest;
			} else if( !defensiveTimer.isWaiting() ){
				myTarget = lastSeenOpponent;
			}
			
		}
	}
	
	/////////////////////////////////////////////////////////////////////
	/////////////////////// Begin random scouting ///////////////////////
	/////////////////////////////////////////////////////////////////////

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
	
	private MapLocation startingLocation;
	void runsOnce() {
		startingLocation = rc.getLocation();
	}
	
}


