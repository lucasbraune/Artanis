package artanis;

import battlecode.common.*;

public class Scout {
	
	private static RobotController rc;
	private static Signal[] signalQueue;
	private static Direction myDirection;

	MapLocation enemyLocation = null;
	public static boolean shouldBroadcast = true;

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
		if( shouldBroadcast ) {
			enemies = rc.senseNearbyRobots( RobotType.SCOUT.sensorRadiusSquared, rc.getTeam().opponent() );
			if ( enemies.length > 0 ) {
				Communication.broadcastLocation( enemies[0].location, ENEMY_LOCATION, Communication.LARGE_RADIUS);
				shouldBroadcast = false;
			}
		}
		
		if ( !rc.onTheMap( rc.getLocation().add( myDirection ) ) ) {
			myDirection = Movement.randomDirection();
			// this changes directions if current one leads out of the map.
		}
		else {
			Movement.moveAvoidingEnemies( myDirection, rc.senseHostileRobots( rc.getLocation(), -1 ));
		}
	}
}