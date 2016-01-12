package artanis;

import battlecode.common.*;

public class Scout {
	
	private static RobotController rc;
	private static Signal[] signalQueue;
	private static Direction myDirection;

	private static MapLocation enemyLocation = null;
	private static boolean shouldBroadcast = true;

	// Message types. Start at 2001.
	public static final int ENEMY_LOCATION = 2000;

	public static void code( ){
		rc = RobotPlayer.rc;
		myDirection = Movement.randomDirection();

		try {
			runsOnce();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		while(true){	
			try{
				repeat();
			}catch ( Exception e ) {
				e.printStackTrace();
			}
			Clock.yield();
		}
	}
	
	private static void runsOnce() throws GameActionException {
		return;
	}
	
	private static void repeat() throws GameActionException {
		RobotInfo[] enemies;
		Signal request;
		
		enemies = rc.senseNearbyRobots( RobotType.SCOUT.sensorRadiusSquared, rc.getTeam().opponent() );
		
		if( enemies.length>3 ){
			enemyLocation = enemies[0].location;
		}
		
		signalQueue = rc.emptySignalQueue();
		request = Communication.getSignal( signalQueue, Archon.WHERE_IS_THE_ENEMY );
		
		if( request != null ){
			shouldBroadcast = true;
		}
		
		if( shouldBroadcast && enemyLocation != null ) {
			Communication.broadcastLocation( enemyLocation, ENEMY_LOCATION, Communication.LARGE_RADIUS);
			shouldBroadcast = false;
			rc.setIndicatorString(0, "At some point I broadcast an enemy's location." );
		}
		
		// this changes directions if current one leads out of the map.
		if ( !rc.onTheMap( rc.getLocation().add( myDirection ) ) ) {
			myDirection = Movement.randomDirection();	
		}
		else {
			Movement.moveAvoidingEnemies( myDirection, rc.senseHostileRobots( rc.getLocation(), -1 ));
		}
	}
}